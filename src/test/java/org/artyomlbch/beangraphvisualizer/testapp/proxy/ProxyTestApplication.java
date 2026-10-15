package org.artyomlbch.beangraphvisualizer.testapp.proxy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Приложение, в котором спринг заменяет бины прокс
 * бины с @Async оборачиваются прокси из-за @EnableAsync
 * классы с @Configuration (и сам этот класс) Spring заменяет
 * сгенерированным наследником
 */
@SpringBootApplication
@EnableAsync
public class ProxyTestApplication {
}
