package com.example.eventlottery;

/**
 * Haversine distance helpers for geolocation verification.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoUtils() {}

    /**
     * @return distance in meters between two WGS84 points, or NaN if any argument is null
     */
    public static double haversineMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.NaN;
        }
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }
}
