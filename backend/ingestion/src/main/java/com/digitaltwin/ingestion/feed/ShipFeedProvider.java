package com.digitaltwin.ingestion.feed;

import com.digitaltwin.shared.event.ShipRawEvent;

import java.util.List;

public interface ShipFeedProvider {

    /**
     * Fetches the latest batch of vessel position events.
     *
     * @return list of raw ship events representing current positions
     */
    List<ShipRawEvent> fetch();
}
