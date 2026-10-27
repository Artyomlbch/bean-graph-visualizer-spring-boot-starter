package org.artyomlbch.beangraphvisualizer.testapp.cycles.billing;

import org.artyomlbch.beangraphvisualizer.testapp.cycles.orders.OrderRepository;
import org.springframework.stereotype.Service;

// Единственная связь billing → orders: из-за неё модули и образуют цикл
@Service
public class InvoiceService {

    private final OrderRepository orderRepository;

    public InvoiceService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
