package org.artyomlbch.beangraphvisualizer.testapp.cycles.orders;

import org.artyomlbch.beangraphvisualizer.testapp.cycles.shipping.ShipmentListener;
import org.springframework.stereotype.Component;

// Реализация интерфейса из shipping: orders зависит от shipping, а не наоборот
@Component
public class OrderShipmentHandler implements ShipmentListener {
}
