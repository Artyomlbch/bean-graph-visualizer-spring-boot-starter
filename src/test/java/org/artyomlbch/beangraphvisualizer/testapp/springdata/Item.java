package org.artyomlbch.beangraphvisualizer.testapp.springdata;

import org.springframework.data.annotation.Id;

public class Item {

    @Id
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
