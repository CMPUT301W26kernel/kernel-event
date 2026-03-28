/**
 * QR Generator Fragment
 * Displays a QR code for an event which admin/organizers can download to use as they please.
 * Last Modified: 2026-03-25 by Grace MacKenzie
 *
 *<p>
 *     TODO: cite this: https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
 *     TODO: navigation
 *     TODO: implement download/save for QR Code
 *           try this: https://www.javathinking.com/blog/android-save-image-into-gallery/
 *     TODO: implement back button
 *</p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-25
 */
package com.example.eventlottery;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * A fragment to display a QR code for an event which admin/organizers can download
 * to use as they please.
 */
public class QrGeneratorFragment extends Fragment {

    private String eventId;

    public QrGeneratorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView qrCodeView = view.findViewById(R.id.qr_code);
        Bitmap qrCode = null;

        // Generate QR code from eventId
        if (eventId != null) {
            qrCode = generateQrCode(eventId);
        } else {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_LONG).show();
            Log.w("QRGenerator", "Missing eventId");
        }

        // Display QR code
        if (qrCode != null) {
            qrCodeView.setImageBitmap(qrCode);
        } else {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_LONG).show();
        }

        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener( v -> {
            // TODO: navigate back to event overview
        });

        Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener( v -> {
            // TODO: save generated qrCode as JPEG or PNG or something to device
        });
    }

    // METHODS

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param eventId an eventId used to generate a QR code for this fragment
     * @return A new instance of fragment QrGeneratorFragment.
     */
    public static QrGeneratorFragment newInstance(String eventId) {
        QrGeneratorFragment fragment = new QrGeneratorFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    //  HELPER METHODS

    /**
     * Generates a QR code to encode an eventID
     * @param eventId The eventId of an event this QR code should reference
     * @return On success, returns a bitmap encoding the given eventId
     */
    private Bitmap generateQrCode(String eventId) {
        Bitmap bitmap;
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try{
            bitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            Log.w("QRGenerator", "Failed to encode QR: " + e.getMessage());
            return null;
        }
        return bitmap;
    }
}