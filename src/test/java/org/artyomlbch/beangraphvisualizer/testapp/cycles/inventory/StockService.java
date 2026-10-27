package org.artyomlbch.beangraphvisualizer.testapp.cycles.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    @Autowired
    private StockReservations reservations;
}

@Component
class StockReservations {

    @Autowired
    private StockService stockService;
}
