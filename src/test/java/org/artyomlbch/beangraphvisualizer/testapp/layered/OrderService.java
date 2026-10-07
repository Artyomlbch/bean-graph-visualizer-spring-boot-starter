package org.artyomlbch.beangraphvisualizer.testapp.layered;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository  = orderRepository;
    }

}
