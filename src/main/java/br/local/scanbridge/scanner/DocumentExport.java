package br.local.scanbridge.scanner;

public record DocumentExport(
        byte[] content,
        String fileName,
        String mediaType
) {
}
