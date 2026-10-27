package org.artyomlbch.beangraphvisualizer.testapp.cycles.shipping;

import org.springframework.stereotype.Service;

// В коде зависимость только от своего интерфейса, хотя внедряется бин из orders
@Service
public class ShippingService {

    private final ShipmentListener listener;

    public ShippingService(ShipmentListener listener) {
        this.listener = listener;
    }
}
