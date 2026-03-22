/**
 * QR Code Generator
 * A utility class containing a function to generate a QR code which encodes an eventId
 * Last Modified: 2026-03-22 by Grace MacKenzie
 *
 *<p>
 *     TODO: cite these:
 *     https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
 *     https://reintech.io/blog/implementing-android-app-qr-code-scanner
 *</p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-22
 */
package com.example.eventlottery;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * A utility class containing a function to encode an eventId into a QR code
 */
public class QrCodeGenerator {

    /**
     * Generates a QR code to encode an eventID
     * @param eventId The eventId of an event this QR code should reference
     * @return On success, returns a bitmap encoding the given eventId
     */
    public static Bitmap generate(String eventId) {
        Bitmap bitmap;
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try{
            bitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            return null;
        }
        return bitmap;
    }
}
