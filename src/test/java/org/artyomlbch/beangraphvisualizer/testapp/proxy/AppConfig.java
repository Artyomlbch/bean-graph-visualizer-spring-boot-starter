package org.artyomlbch.beangraphvisualizer.testapp.proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring создает прокси, чтобы при вызове clock множество раз
 * проверять контекст на наличие бина и возвращать его
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
