package com.example.eventlottery.map;

import com.example.eventlottery.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups events into map markers based on zoom level (wider spacing when zoomed out).
 */
public final class MapMarkerGrouper {

    public static final class Group {
        public final double centroidLat;
        public final double centroidLon;
        public final List<Event> events;

        Group(double centroidLat, double centroidLon, List<Event> events) {
            this.centroidLat = centroidLat;
            this.centroidLon = centroidLon;
            this.events = events;
        }
    }

    private MapMarkerGrouper() {}

    /**
     * Cell size in degrees scales down as zoom increases (finer groups when zoomed in).
     */
    public static double cellSizeDegreesForZoom(double zoomLevel) {
        double z = Math.max(3.0, Math.min(zoomLevel, 19.0));
        return 0.45 / Math.pow(2.0, z - 3.0);
    }

    public static List<Group> groupEvents(List<Event> events, double zoomLevel) {
        double cell = cellSizeDegreesForZoom(zoomLevel);
        Map<String, List<Event>> buckets = new HashMap<>();
        for (Event e : events) {
            Double lat = e.getVenueLatitude();
            Double lon = e.getVenueLongitude();
            if (lat == null || lon == null) {
                continue;
            }
            int gi = (int) Math.floor(lat / cell);
            int gj = (int) Math.floor(lon / cell);
            String key = gi + ":" + gj;
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        List<Group> out = new ArrayList<>();
        for (List<Event> bucket : buckets.values()) {
            if (bucket.isEmpty()) {
                continue;
            }
            double sumLat = 0;
            double sumLon = 0;
            for (Event e : bucket) {
                sumLat += e.getVenueLatitude();
                sumLon += e.getVenueLongitude();
            }
            int n = bucket.size();
            out.add(new Group(sumLat / n, sumLon / n, bucket));
        }
        return out;
    }

    public static String titleForGroup(Group g) {
        if (g.events.size() == 1) {
            Event e = g.events.get(0);
            return e.getTitle() != null ? e.getTitle() : "Event";
        }
        return String.format(Locale.getDefault(), "%d events", g.events.size());
    }
}
