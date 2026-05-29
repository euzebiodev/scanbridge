package br.local.scanbridge.scanner;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record ScanDocument(
        String id,
        String fileName,
        Path path,
        long size,
        LocalDateTime createdAt
) {
}
