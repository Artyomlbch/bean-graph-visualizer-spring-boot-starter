package org.artyomlbch.beangraphvisualizer.testapp.proxy;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Сервис с интерфейсом и методом с @Async. В зависимости от настроек Spring
 * обернёт его JDK-прокси (реализует только интерфейс Notifier)
 * или CGLIB-прокси
 */
@Service
public class AsyncNotifier implements Notifier {

    @Async
    @Override
    public void notifyUser(String message) {
    }
}
