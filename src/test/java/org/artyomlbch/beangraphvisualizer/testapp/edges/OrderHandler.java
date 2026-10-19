package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Поле с @Autowired объявлено в родителе
 * getDeclaredFields() класса OrderHandler его не видит, поэтому нужно проверять родителей
 */
@Component
public class OrderHandler extends BaseHandler {
}

abstract class BaseHandler {

    @Autowired
    protected AuditTrail auditTrail;
}

@Component
class AuditTrail {
}
