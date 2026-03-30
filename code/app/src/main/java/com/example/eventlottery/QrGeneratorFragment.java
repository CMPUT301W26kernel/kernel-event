/**
 * QR Generator Fragment
 * Displays a QR code for an event which admin/organizers can download to use as they please.
 * Last Modified: 2026-03-29 by Grace MacKenzie
 *
 *<p>
 *     Based on Tutorial "How to Generate QR Code in Android?" from Geeks For Geeks at
 *     https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
 *</p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-25
 */
package com.example.eventlottery;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.IOException;
import java.io.OutputStream;

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
        Bitmap qrCode;

        // Generate QR code from eventId
        if (eventId != null) {
            qrCode = generateQrCode(eventId);
        } else {
            qrCode = null;
            view.findViewById(R.id.error_mssg).setVisibility(View.VISIBLE);
        }

        // Display QR code
        if (qrCode != null) {
            qrCodeView.setImageBitmap(qrCode);
        } else {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_LONG).show();
        }

        // Set on click listeners

        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener( v -> {
            // Navigate back to EventOverviewFragment
            EventOverviewFragment fragment = new EventOverviewFragment();
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            fragment.setArguments(bundle);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        if (qrCode!= null) {
            Button saveButton = view.findViewById(R.id.save_button);
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setOnClickListener( v -> {
                try {
                    saveBitmapToGallery(qrCode);
                } catch (IOException e) {
                    Toast.makeText(getContext(), R.string.error_failed_to_save_image, Toast.LENGTH_SHORT).show();
                    Log.e("SaveImage", "Failed to save image:" + e.getMessage());
                }
            });
        }
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
     *
     * @param eventId The eventId of an event this QR code should reference
     * @return On success, returns a bitmap encoding the given eventId
     */
    private Bitmap generateQrCode(String eventId) {
        Bitmap bitmap;
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try{
            bitmap = barcodeEncoder.encodeBitmap(eventId, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            Log.e("QRGenerator", "Failed to encode QR: " + e.getMessage());
            return null;
        }
        return bitmap;
    }

    /**
     * Saves a QR code bitmap to the gallery as a png.
     * This function was written with assistance from Microsoft, Copilot
     *
     * @param qrCode the QR code bitmap to save to the gallery.
     * @throws IOException if the MediaStore entry cannot be created or the output stream cannot be opened
     */
    private void saveBitmapToGallery(Bitmap qrCode) throws IOException {
        // Setup metadata for image file
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "qrCode_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EventLottery");

        // Create a file entry to insert the qrCode into
        Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        // Write the qrCode to the gallery
        // (The try/catch block automatically closes the OutputStream)
        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("Failed to open output stream");
            }
            qrCode.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}