package org.artyomlbch.beangraphvisualizer.testapp.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Сервис без интерфейсов с методов @Acyns. Spring оборачивает его
 * CGLIB-прокси, который является наследником, сгенерированным в рантайме.
 */
@Service
public class AsyncWorker {

    @Autowired
    private AuditLog auditLog;

    @Async
    public void work() {
    }
}
