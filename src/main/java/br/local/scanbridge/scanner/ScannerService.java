package br.local.scanbridge.scanner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public ScanDocument scan(ScanRequest request) throws IOException, InterruptedException {
        Files.createDirectories(properties.getOutputDirectory());

        String id = UUID.randomUUID().toString();
        String fileName = "scan-" + FILE_STAMP.format(Instant.now()) + "-" + id.substring(0, 8) + ".jpg";
        Path target = properties.getOutputDirectory().resolve(fileName).toAbsolutePath().normalize();
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

        return toDocument(target);
    }

    public List<ScanDocument> listDocuments() throws IOException {
        Files.createDirectories(properties.getOutputDirectory());
        try (Stream<Path> files = Files.list(properties.getOutputDirectory())) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .map(this::toDocumentUnchecked)
                    .sorted(Comparator.comparing(ScanDocument::createdAt).reversed())
                    .toList();
        }
    }

    public Resource load(String fileName) throws IOException {
        Path file = resolveDocument(fileName);
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException exception) {
            throw new IOException("Arquivo invalido.", exception);
        }
    }

    public Path resolveDocument(String fileName) throws IOException {
        Path base = properties.getOutputDirectory().toAbsolutePath().normalize();
        Path file = base.resolve(fileName).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw new IOException("Arquivo nao encontrado.");
        }
        return file;
    }

    public Duration timeout() {
        return Duration.ofSeconds(properties.getTimeoutSeconds());
    }

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

    private ScanDocument toDocumentUnchecked(Path path) {
        try {
            return toDocument(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ScanDocument toDocument(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        String id = fileName.replaceFirst("^scan-", "").replaceFirst("\\.jpg$", "");
        return new ScanDocument(
                id,
                fileName,
                path,
                Files.size(path),
                LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault())
        );
    }
}
