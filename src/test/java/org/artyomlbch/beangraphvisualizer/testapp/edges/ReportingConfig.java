package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

/**
 * reportGenerator создаётся @Bean-методом. У него две зависимости
 * invoiceRepository передается через параметр метода (FACTORY_METHOD)
 * reportingConfig конфигурация, в которой объявлен метод (DECLARED_IN).
 *
 * Так как конструктор в методе вызывается вручную, тип внедрения CONSTRUCTOR
 * не является верным
 */
@Configuration
public class ReportingConfig {

    @Bean
    ReportGenerator reportGenerator(InvoiceRepository invoiceRepository) {
        return new ReportGenerator(invoiceRepository);
    }
}

class ReportGenerator {

    private final InvoiceRepository invoiceRepository;

    ReportGenerator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }
}

@Repository
class InvoiceRepository {
}
