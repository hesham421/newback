package com.erp.testsupport;

import com.erp.events.DomainEvent;

/** A test-only domain event carrying a marker, to tell published events apart. */
public class ProbeEvent extends DomainEvent {

    private final String marker;

    public ProbeEvent(String marker) {
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }
}
