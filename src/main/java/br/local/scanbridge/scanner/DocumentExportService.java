package br.local.scanbridge.scanner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class DocumentExportService {

    private final ScannerService scannerService;

    public DocumentExportService(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    public DocumentExport export(
            String sourceFileName,
            String username,
            String documentName,
            String format,
            Integer x,
            Integer y,
            Integer width,
            Integer height
    ) throws IOException {
        Path source = scannerService.resolveDocument(username, sourceFileName);
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("Nao foi possivel ler a imagem digitalizada.");
        }

        BufferedImage selectedImage = crop(image, x, y, width, height);
        String normalizedFormat = normalizeFormat(format);
        String safeName = safeFileName(documentName, sourceFileName);

        if ("pdf".equals(normalizedFormat)) {
            return new DocumentExport(toPdf(selectedImage), safeName + ".pdf", MediaTypes.PDF);
        }

        return new DocumentExport(toJpeg(selectedImage), safeName + ".jpg", MediaTypes.JPEG);
    }

    private BufferedImage crop(BufferedImage image, Integer x, Integer y, Integer width, Integer height) {
        if (x == null || y == null || width == null || height == null || width <= 0 || height <= 0) {
            return image;
        }

        int safeX = Math.max(0, Math.min(x, image.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, image.getHeight() - 1));
        int safeWidth = Math.max(1, Math.min(width, image.getWidth() - safeX));
        int safeHeight = Math.max(1, Math.min(height, image.getHeight() - safeY));
        return image.getSubimage(safeX, safeY, safeWidth, safeHeight);
    }

    private byte[] toJpeg(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(toRgb(image), "jpg", output);
            return output.toByteArray();
        }
    }

    private BufferedImage toRgb(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private byte[] toPdf(BufferedImage image) throws IOException {
        byte[] jpeg = toJpeg(image);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            PDImageXObject pdfImage = PDImageXObject.createFromByteArray(document, jpeg, "scan.jpg");
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdfImage, 0, 0, image.getWidth(), image.getHeight());
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private String normalizeFormat(String format) {
        String value = format == null ? "jpg" : format.toLowerCase(Locale.ROOT);
        return "pdf".equals(value) ? "pdf" : "jpg";
    }

    private String safeFileName(String documentName, String sourceFileName) {
        String baseName = StringUtils.hasText(documentName)
                ? documentName
                : sourceFileName.replaceFirst("\\.[^.]+$", "");
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.hasText(normalized) ? normalized : "documento";
    }

    private static final class MediaTypes {
        static final String JPEG = "image/jpeg";
        static final String PDF = "application/pdf";
    }
}
