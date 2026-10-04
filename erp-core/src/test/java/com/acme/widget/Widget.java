package com.acme.widget;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Test fixture: an entity of a consuming application outside the core packages (never persisted). */
@Entity
@Table(name = "ACME_WIDGET")
public class Widget {

    @Id
    private Long id;

    public Long getId() {
        return id;
    }
}
