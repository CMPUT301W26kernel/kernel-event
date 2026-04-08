package com.example.eventlottery.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.Event;
import com.example.eventlottery.EventOverviewFragment;
import com.example.eventlottery.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * OpenStreetMap (via Osmdroid) view of events that currently accept waitlist registration and have a venue.
 * Markers are grouped by zoom level; tags filter which events are shown.
 */
public class NearbyEventsMapFragment extends Fragment {

    /** Lowercase trimmed query; filters pins by tag match and event title. */
    private String mapSearchKeyword = "";

    private MapView mapView;
    private final ArrayList<Event> allEvents = new ArrayList<>();
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> {
                if (hasLocationPermission()) {
                    centerOnUserLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_events_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context appCtx = requireContext().getApplicationContext();
        Configuration.getInstance().load(appCtx, appCtx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(appCtx.getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(appCtx.getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(appCtx.getCacheDir(), "osmdroid/tiles"));

        mapView = view.findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(51.0447, -114.0719));

        view.findViewById(R.id.btn_map_back).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        TextInputEditText searchInput = view.findViewById(R.id.input_map_search);
        MaterialButton searchBtn = view.findViewById(R.id.btn_map_search);
        MaterialButton clearSearchBtn = view.findViewById(R.id.btn_map_search_clear);
        if (searchBtn != null) {
            searchBtn.setOnClickListener(v -> {
                if (searchInput != null && searchInput.getText() != null) {
                    mapSearchKeyword = searchInput.getText().toString().trim().toLowerCase(Locale.US);
                } else {
                    mapSearchKeyword = "";
                }
                hideKeyboard(v);
                refreshMarkers();
            });
        }
        if (clearSearchBtn != null) {
            clearSearchBtn.setOnClickListener(v -> {
                mapSearchKeyword = "";
                if (searchInput != null) {
                    searchInput.setText("");
                }
                hideKeyboard(v);
                refreshMarkers();
            });
        }
        if (searchInput != null) {
            searchInput.setOnEditorActionListener((TextView tv, int actionId, android.view.KeyEvent event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && searchBtn != null) {
                    searchBtn.performClick();
                    return true;
                }
                return false;
            });
        }

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                scheduleRefreshMarkers();
                return false;
            }
        });

        loadEventsFromFirestore();

        if (!hasLocationPermission()) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            centerOnUserLocation();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void scheduleRefreshMarkers() {
        if (refreshRunnable != null) {
            debounce.removeCallbacks(refreshRunnable);
        }
        refreshRunnable = this::refreshMarkers;
        debounce.postDelayed(refreshRunnable, 250);
    }

    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance().collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) {
                        return;
                    }
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        try {
                            Event e = doc.toObject(Event.class);
                            e.setEventId(doc.getId());
                            if (isDrawPoolEligible(e)) {
                                allEvents.add(e);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    buildTagChips(requireView());
                    refreshMarkers();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.failed_to_load_events, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static boolean isDrawPoolEligible(Event e) {
        if (e.getVenueLatitude() == null || e.getVenueLongitude() == null) {
            return false;
        }
        return !e.isPrivate();
    }

    private void buildTagChips(@NonNull View root) {
        ChipGroup group = root.findViewById(R.id.chip_group_tags);
        group.removeAllViews();
        Set<String> tags = new HashSet<>();
        for (Event e : allEvents) {
            if (e.getTags() != null) {
                for (String t : e.getTags()) {
                    if (t != null && !t.isEmpty()) {
                        tags.add(t.toLowerCase(Locale.US));
                    }
                }
            }
        }
        List<String> sorted = new ArrayList<>(tags);
        Collections.sort(sorted);
        for (String tag : sorted) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(false);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> refreshMarkers());
            group.addView(chip);
        }
    }

    private Set<String> getSelectedTags() {
        ChipGroup group = requireView().findViewById(R.id.chip_group_tags);
        Set<String> s = new HashSet<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            if (c instanceof Chip) {
                Chip chip = (Chip) c;
                if (chip.isChecked()) {
                    s.add(chip.getText().toString().toLowerCase(Locale.US));
                }
            }
        }
        return s;
    }

    private static boolean passesTagFilter(Event e, Set<String> selected) {
        if (selected.isEmpty()) {
            return true;
        }
        List<String> tags = e.getTags();
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        for (String t : tags) {
            if (t != null && selected.contains(t.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Text search: any tag contains the keyword (case-insensitive), or event title contains it.
     */
    private boolean passesMapSearchKeyword(Event e) {
        if (mapSearchKeyword.isEmpty()) {
            return true;
        }
        if (e.getTags() != null) {
            for (String t : e.getTags()) {
                if (t == null) {
                    continue;
                }
                String nt = t.toLowerCase(Locale.US).trim();
                if (!nt.isEmpty() && nt.contains(mapSearchKeyword)) {
                    return true;
                }
            }
        }
        String title = e.getTitle();
        return title != null && title.toLowerCase(Locale.US).contains(mapSearchKeyword);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && v.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void refreshMarkers() {
        if (mapView == null) {
            return;
        }
        mapView.getOverlays().clear();
        Set<String> selected = getSelectedTags();
        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            if (!passesTagFilter(e, selected)) {
                continue;
            }
            if (!passesMapSearchKeyword(e)) {
                continue;
            }
            filtered.add(e);
        }
        double zoom = mapView.getZoomLevelDouble();
        List<MapMarkerGrouper.Group> groups = MapMarkerGrouper.groupEvents(filtered, zoom);
        for (MapMarkerGrouper.Group g : groups) {
            Marker m = new Marker(mapView);
            m.setPosition(new GeoPoint(g.centroidLat, g.centroidLon));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(MapMarkerGrouper.titleForGroup(g));
            boolean singleEventMarker = g.events.size() == 1;
            if (g.events.size() == 1) {
                Event one = g.events.get(0);
                String addr = one.getVenueAddress();
                if (addr != null && !addr.trim().isEmpty()) {
                    m.setSnippet(addr.trim());
                } else {
                    String d = one.getDescription();
                    m.setSnippet(d != null && d.length() > 80 ? d.substring(0, 77) + "…" : d);
                }
            } else {
                m.setSnippet(getString(R.string.pick_event_from_cluster));
            }
            m.setOnMarkerClickListener((marker, mapView1) -> {
                onMarkerGroupClicked(g);
                return true;
            });
            mapView.getOverlays().add(m);
            if (singleEventMarker) {
                // Keep the event name visible above individual pins.
                m.showInfoWindow();
            }
        }
        mapView.invalidate();
    }

    private void onMarkerGroupClicked(MapMarkerGrouper.Group g) {
        if (g.events.size() == 1) {
            openEvent(g.events.get(0).getEventId());
            return;
        }
        String[] titles = new String[g.events.size()];
        for (int i = 0; i < g.events.size(); i++) {
            String t = g.events.get(i).getTitle();
            titles[i] = t != null ? t : "Event";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pick_event_from_cluster)
                .setItems(titles, (dialog, which) -> openEvent(g.events.get(which).getEventId()))
                .show();
    }

    private void openEvent(String eventId) {
        EventOverviewFragment f = new EventOverviewFragment();
        Bundle b = new Bundle();
        b.putString("eventId", eventId);
        f.setArguments(b);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    private void centerOnUserLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(requireActivity());
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null && mapView != null) {
                        GeoPoint p = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                        mapView.getController().animateTo(p);
                        mapView.getController().setZoom(13.0);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        debounce.removeCallbacksAndMessages(null);
        if (mapView != null) {
            mapView.onDetach();
        }
        mapView = null;
        super.onDestroyView();
    }
}
