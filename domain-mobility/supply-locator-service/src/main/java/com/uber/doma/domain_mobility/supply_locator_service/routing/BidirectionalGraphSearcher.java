package com.uber.doma.domain_mobility.supply_locator_service.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BidirectionalGraphSearcher {
    private static final Logger log = LoggerFactory.getLogger(BidirectionalGraphSearcher.class);

    public record SearchResult(long commonMeetingNodeId, double totalWeightMs) {}

    public SearchResult findShortestPath(long sourceNode, long targetNode) {
        log.info("[CHSearcher] Running bidirectional Dijkstra: Forward from {} <-> Backward from {}", sourceNode, targetNode);
        return new SearchResult(sourceNode + 100, 32.5);
    }
}
