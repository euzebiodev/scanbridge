package br.local.scanbridge.scanner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

@Service
public class ScannerService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private final ScanProperties properties;

    public ScannerService(ScanProperties properties) {
        this.properties = properties;
    }

    public ScanDocument scan(String username, ScanRequest request) throws IOException, InterruptedException {
        migrateLooseDocuments(username);
        Files.createDirectories(properties.getOutputDirectory());

        String id = UUID.randomUUID().toString();
        String fileName = "scan-" + FILE_STAMP.format(Instant.now()) + "-" + id.substring(0, 8) + ".jpg";
        Path target = Files.createTempFile(properties.getOutputDirectory(), "scan-", ".jpg").toAbsolutePath().normalize();
        try {
            ProcessBuilder builder = isWindows()
                    ? windowsScanProcess(target, request)
                    : linuxScanProcess(target, request);

            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Tempo limite excedido ao digitalizar.");
            }

            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                throw new IOException("Falha ao digitalizar: " + output.strip());
            }
            if (!Files.exists(target)) {
                throw new IOException("O scanner terminou sem gerar arquivo.");
            }

            addToArchive(username, fileName, target);
            return toDocument(username, fileName, Files.readAllBytes(target), Instant.now());
        } finally {
            Files.deleteIfExists(target);
        }
    }

    public List<ScanDocument> listDocuments(String username) throws IOException {
        migrateLooseDocuments(username);
        Path archive = userArchive(username);
        if (!Files.isRegularFile(archive)) {
            return List.of();
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            return zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .map(entry -> toDocumentUnchecked(username, entry))
                    .sorted(Comparator.comparing(ScanDocument::createdAt).reversed())
                    .toList();
        }
    }

    public Resource load(String username, String fileName) throws IOException {
        return new ByteArrayResource(documentBytes(username, fileName));
    }

    public byte[] documentBytes(String username, String fileName) throws IOException {
        migrateLooseDocuments(username);
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IOException("Arquivo invalido.");
        }
        Path archive = userArchive(username);
        if (!Files.isRegularFile(archive)) {
            throw new IOException("Arquivo nao encontrado.");
        }
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            ZipEntry entry = zipFile.getEntry(fileName);
            if (entry == null || entry.isDirectory()) {
                throw new IOException("Arquivo nao encontrado.");
            }
            try (InputStream input = zipFile.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }

    public Duration timeout() {
        return Duration.ofSeconds(properties.getTimeoutSeconds());
    }

    /**
     * Usa o driver WIA no Windows pra capturar a pagina scaneada.
     */
    private ProcessBuilder windowsScanProcess(Path target, ScanRequest request) {
        Path script = properties.getScriptPath().toAbsolutePath().normalize();
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", script.toString(),
                "-OutputPath", target.toString(),
                "-Dpi", String.valueOf(request.getDpi()),
                "-ColorMode", request.getColorMode()
        );

        if (StringUtils.hasText(properties.getDeviceName())) {
            builder.command().add("-DeviceName");
            builder.command().add(properties.getDeviceName());
        }

        return builder;
    }

    /**
     * Runs SANE scanner command and put the image in a temp file for zip save.
     */
    private ProcessBuilder linuxScanProcess(Path target, ScanRequest request) {
        Path script = properties.getSaneScriptPath().toAbsolutePath().normalize();
        ProcessBuilder builder = new ProcessBuilder(
                "bash",
                script.toString(),
                "--output", target.toString(),
                "--dpi", String.valueOf(request.getDpi()),
                "--mode", request.getColorMode()
        );

        if (StringUtils.hasText(properties.getDeviceName())) {
            builder.command().add("--device");
            builder.command().add(properties.getDeviceName());
        }

        return builder;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private Path userDirectory(String username) {
        return properties.getOutputDirectory().resolve(safeUserDirectoryName(username));
    }

    private Path userArchive(String username) {
        return properties.getOutputDirectory().resolve(safeUserDirectoryName(username) + ".zip");
    }

    private String safeUserDirectoryName(String username) {
        String normalized = Normalizer.normalize(username, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : "usuario";
    }

    private void migrateLooseDocuments(String username) throws IOException {
        Path directory = userDirectory(username);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .toList()) {
                addToArchive(username, file.getFileName().toString(), file);
                Files.deleteIfExists(file);
            }
        }
    }

    private void addToArchive(String username, String fileName, Path source) throws IOException {
        Files.createDirectories(properties.getOutputDirectory());
        Path archive = userArchive(username);
        Path temporaryArchive = Files.createTempFile(properties.getOutputDirectory(), safeUserDirectoryName(username), ".zip");

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporaryArchive))) {
            if (Files.isRegularFile(archive)) {
                try (ZipFile existingArchive = new ZipFile(archive.toFile())) {
                    for (ZipEntry existingEntry : existingArchive.stream().toList()) {
                        if (existingEntry.isDirectory() || existingEntry.getName().equals(fileName)) {
                            continue;
                        }
                        ZipEntry copy = new ZipEntry(existingEntry.getName());
                        if (existingEntry.getTime() >= 0) {
                            copy.setTime(existingEntry.getTime());
                        }
                        output.putNextEntry(copy);
                        try (InputStream input = existingArchive.getInputStream(existingEntry)) {
                            input.transferTo(output);
                        }
                        output.closeEntry();
                    }
                }
            }

            ZipEntry newEntry = new ZipEntry(fileName);
            newEntry.setTime(Files.getLastModifiedTime(source).toMillis());
            output.putNextEntry(newEntry);
            try (InputStream input = Files.newInputStream(source)) {
                input.transferTo(output);
            }
            output.closeEntry();
        }

        try {
            Files.move(temporaryArchive, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryArchive);
        }
    }

    private ScanDocument toDocumentUnchecked(String username, ZipEntry entry) {
        try {
            long entryTime = entry.getTime() >= 0 ? entry.getTime() : Instant.now().toEpochMilli();
            return toDocument(username, entry.getName(), entry.getSize(), Instant.ofEpochMilli(entryTime));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ScanDocument toDocument(String username, String fileName, byte[] content, Instant createdAt) throws IOException {
        return toDocument(username, fileName, content.length, createdAt);
    }

    private ScanDocument toDocument(String username, String fileName, long size, Instant createdAt) throws IOException {
        String id = fileName.replaceFirst("^scan-", "").replaceFirst("\\.jpg$", "");
        return new ScanDocument(
                id,
                fileName,
                userArchive(username),
                size,
                LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault())
        );
    }
}
