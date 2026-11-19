package org.artyomlbch.beangraphvisualizer.testapp.springdata;

import org.springframework.data.repository.CrudRepository;

public interface ItemRepository extends CrudRepository<Item, String> {
}
