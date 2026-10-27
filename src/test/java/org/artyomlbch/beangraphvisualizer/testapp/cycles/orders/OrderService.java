package org.artyomlbch.beangraphvisualizer.testapp.cycles.orders;

import org.artyomlbch.beangraphvisualizer.testapp.cycles.billing.BillingService;
import org.artyomlbch.beangraphvisualizer.testapp.cycles.billing.InvoiceService;
import org.artyomlbch.beangraphvisualizer.testapp.cycles.shipping.ShippingService;
import org.springframework.stereotype.Service;

/**
 * Модули orders и billing зависят друг от друга, хотя цикла между бинами нет:
 * <ul>
 *     <li>orders → billing: orderService использует billingService и invoiceService (2 связи);</li>
 *     <li>billing → orders: invoiceService использует orderRepository (1 связь).</li>
 * </ul>
 * С shipping цикла нет: shippingService зависит от своего интерфейса ShipmentListener,
 * а его реализацию OrderShipmentHandler даёт orders
 */
@Service
public class OrderService {

    private final BillingService billingService;
    private final InvoiceService invoiceService;
    private final ShippingService shippingService;

    public OrderService(BillingService billingService, InvoiceService invoiceService,
                        ShippingService shippingService) {
        this.billingService = billingService;
        this.invoiceService = invoiceService;
        this.shippingService = shippingService;
    }
}
