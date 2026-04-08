package com.example.eventlottery.geolocation;

public class GeocodeResult {
    public final double latitude;
    public final double longitude;
    public final String formattedAddress;

    public GeocodeResult(double latitude, double longitude, String formattedAddress) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.formattedAddress = formattedAddress;
    }
}
