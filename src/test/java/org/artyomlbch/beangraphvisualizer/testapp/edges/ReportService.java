package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * pdfExporter подходит как в @Autowired поле, так и в конструктор
 * Так как в поле явный класс, а в конструкторе список интерфейсов,
 * то ребро будет FIELD
 *
 * csvExporter подходит только через конструктор, поэтому ребро CONSTRUCTOR
 */
@Service
public class ReportService {

    private final List<Exporter> exporters;

    @Autowired
    private PdfExporter pdfExporter;

    public ReportService(List<Exporter> exporters) {
        this.exporters = exporters;
    }
}

/**
 * Точный тип в конструкторе, список в поле
 * archiveService -> pdfExporter CONSTRUCTOR
 * archiveService -> csvExporter — FIELD.
 */
@Service
class ArchiveService {

    private final PdfExporter pdfExporter;

    @Autowired
    private List<Exporter> allExporters;

    ArchiveService(PdfExporter pdfExporter) {
        this.pdfExporter = pdfExporter;
    }
}

interface Exporter {
}

@Component
class PdfExporter implements Exporter {
}

@Component
class CsvExporter implements Exporter {
}
