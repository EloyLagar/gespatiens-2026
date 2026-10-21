package com.viller.gespatiens.reports.application;

// Un PDF listo para descargar y el nombre con el que se guarda
public record PdfFile(String fileName, byte[] content) {
}
