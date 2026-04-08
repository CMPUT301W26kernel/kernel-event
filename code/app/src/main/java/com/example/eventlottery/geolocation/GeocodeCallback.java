package com.example.eventlottery.geolocation;

import androidx.annotation.Nullable;

public interface GeocodeCallback {
    void onResult(@Nullable GeocodeResult result);
}
