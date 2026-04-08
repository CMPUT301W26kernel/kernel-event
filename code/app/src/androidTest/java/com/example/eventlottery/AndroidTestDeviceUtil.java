package com.example.eventlottery;

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;

/**
 * Small device helpers for stabilizing instrumentation runs on shared emulators.
 */
final class AndroidTestDeviceUtil {

    private AndroidTestDeviceUtil() {
    }

    static void disableSystemAnimations() {
        executeShellCommand("settings put global window_animation_scale 0");
        executeShellCommand("settings put global transition_animation_scale 0");
        executeShellCommand("settings put global animator_duration_scale 0");
    }

    private static void executeShellCommand(String command) {
        ParcelFileDescriptor descriptor = null;
        try {
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            descriptor = automation.executeShellCommand(command);
        } catch (RuntimeException ignored) {
            // Shared CI/emulator environments may reject shell access; tests can still proceed.
        } finally {
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException ignored) {
                    // Nothing useful to do during test setup.
                }
            }
        }
    }
}
