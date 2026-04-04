package com.example.eventlottery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeoUtilsTest {

    @Test
    public void haversineSamePointIsZero() {
        assertEquals(0.0, GeoUtils.haversineMeters(53.5, -113.5, 53.5, -113.5), 1.0);
    }

    @Test
    public void haversineKnownShortDistance() {
        // ~1 km apart in latitude
        double m = GeoUtils.haversineMeters(0.0, 0.0, 0.009, 0.0);
        assertTrue(m > 900 && m < 1100);
    }

    @Test
    public void haversineNullReturnsNaN() {
        assertTrue(Double.isNaN(GeoUtils.haversineMeters(null, 0.0, 0.0, 0.0)));
    }
}
