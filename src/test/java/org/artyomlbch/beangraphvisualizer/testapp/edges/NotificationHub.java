package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.stereotype.Component;

import java.util.List;

// в конструкторе тип с wildcard
@Component
public class NotificationHub {

    private final List<? extends Channel> channels;

    public NotificationHub(List<? extends Channel> channels) {
        this.channels = channels;
    }
}

interface Channel {
}

@Component
class EmailChannel implements Channel {
}
