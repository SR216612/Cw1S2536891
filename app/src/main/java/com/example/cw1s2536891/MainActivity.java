/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Saif Raseed
// Student ID           S2536891
// Programme of Study   Software Development
//

// UPDATE THE PACKAGE NAME to include your Student Identifier
package com.example.cw1s2536891;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.CompoundButton;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity implements OnClickListener, OnMapReadyCallback {
    private TextView rawDataDisplay;
    private Button startButton;
    private ArrayList<Situation> situations;
    private TrafficViewModel viewModel;
    private HashSet<String> selectedSituationIds = new HashSet<>();
    private ListView situationList;
    private SituationAdapter adapter;

    private EditText roadSearchInput;
    private Button roadSearchButton;
    private Button clearRoadSearchButton;

    private EditText distanceSearchInput;
    private Button distanceSearchButton;
    private Button clearDistanceSearchButton;

    private Button typeAllButton;
    private Button typeIncidentsButton;
    private Button typeRoadworksButton;
    private String currentTypeFilter = "ALL";

    private ArrayList<Situation> displayedSituations = new ArrayList<>();
    private String currentRoadQuery = "";
    private String currentDistanceQuery = "";

    private Button showMapButton;
    private Button backToListButton;
    private LinearLayout scrollViewContainer;
    private LinearLayout mapContainerLayout;
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;

    // Periodic Update scheduling
    private static final long UPDATE_INTERVAL_MS = 60 * 60 * 1000L; // 1 hour
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("TrafficScotland", "Automatic hourly data update triggered.");
            startProgress();
            updateHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    //DOWNLOADING FROM TRAFFIC SCOTLAND
    private String clientId = "";
    private String clientKey = "";
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    final String roadworksEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        situations = viewModel.getSituations();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        startButton = (Button)findViewById(R.id.startButton);

        startButton.setOnClickListener(this);
        situationList = (ListView)findViewById(R.id.situationList);

        roadSearchInput = findViewById(R.id.roadSearchInput);
        roadSearchButton = findViewById(R.id.roadSearchButton);
        clearRoadSearchButton = findViewById(R.id.clearRoadSearchButton);

        distanceSearchInput = findViewById(R.id.distanceSearchInput);
        distanceSearchButton = findViewById(R.id.distanceSearchButton);
        clearDistanceSearchButton = findViewById(R.id.clearDistanceSearchButton);

        typeAllButton = findViewById(R.id.typeAllButton);
        typeIncidentsButton = findViewById(R.id.typeIncidentsButton);
        typeRoadworksButton = findViewById(R.id.typeRoadworksButton);

        typeAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTypeFilter = "ALL";
                filterSituations();
                updateTypeButtons();
            }
        });

        typeIncidentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTypeFilter = "INCIDENTS";
                filterSituations();
                updateTypeButtons();
            }
        });

        typeRoadworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTypeFilter = "ROADWORKS";
                filterSituations();
                updateTypeButtons();
            }
        });

        updateTypeButtons();

        roadSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRoadQuery = roadSearchInput.getText().toString().trim();
                filterSituations();
            }
        });

        clearRoadSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRoadQuery = "";
                roadSearchInput.setText("");
                filterSituations();
            }
        });

        distanceSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = distanceSearchInput.getText().toString().trim();

                try {
                    double distanceKm = Double.parseDouble(input);

                    if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
                        Toast.makeText(MainActivity.this,
                                "Enter a valid distance greater than zero",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentDistanceQuery = input;
                    filterSituations();

                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this,
                            "Enter a valid distance in miles",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        clearDistanceSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDistanceQuery = "";
                distanceSearchInput.setText("");
                filterSituations();
            }
        });

        showMapButton = (Button)findViewById(R.id.showMapButton);
        backToListButton = (Button)findViewById(R.id.backToListButton);
        scrollViewContainer = findViewById(R.id.scrollViewContainer);
        mapContainerLayout = findViewById(R.id.mapContainerLayout);

        showMapButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMapView();
            }
        });

        backToListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showListView();
            }
        });

        situationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Situation selectedSituation = adapter.getItem(position);

                if (selectedSituation != null) {
                    showSituationDetails(selectedSituation);
                }
            }
        });

        // If situations were already loaded before rotation, filter and set adapter immediately
        if (!situations.isEmpty()) {
            rawDataDisplay.setText("Total live situations: " + situations.size());
            filterSituations();
            adapter = new SituationAdapter(
                    MainActivity.this, displayedSituations, selectedSituationIds);
            situationList.setAdapter(adapter);
        }

        // Initialize SupportMapFragment and add it to the mapContainer frame layout once
        FragmentManager fragmentManager = getSupportFragmentManager();
        mapFragment = (SupportMapFragment) fragmentManager.findFragmentByTag("MAP_FRAGMENT");
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.mapContainer, mapFragment, "MAP_FRAGMENT");
            transaction.commit();
        }
        mapFragment.getMapAsync(this);


        if (savedInstanceState != null) {
            ArrayList<String> savedIds = savedInstanceState.getStringArrayList("SELECTED_IDS");
            if (savedIds != null) {
                selectedSituationIds.clear();
                selectedSituationIds.addAll(savedIds);
            }
            currentRoadQuery = savedInstanceState.getString("ROAD_SEARCH_QUERY", "");
            roadSearchInput.setText(currentRoadQuery);

            currentDistanceQuery = savedInstanceState.getString("DISTANCE_SEARCH_QUERY", "");
            distanceSearchInput.setText(currentDistanceQuery);

            currentTypeFilter = savedInstanceState.getString("TYPE_FILTER", "ALL");
            updateTypeButtons();

            filterSituations();

            boolean showingMap = savedInstanceState.getBoolean("SHOWING_MAP", false);
            if (showingMap) {
                showMapView();
            } else {
                showListView();
            }
        }


        viewModel.getDataVersion().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer version) {
                if (version == null) {
                    return;
                }

                HashSet<String> currentIds = new HashSet<>();
                for (Situation s : situations) {
                    currentIds.add(s.getId());
                }
                selectedSituationIds.retainAll(currentIds);

                rawDataDisplay.setText("Total live situations: " + situations.size());
                filterSituations();
                if (adapter == null) {
                    adapter = new SituationAdapter(
                            MainActivity.this, displayedSituations, selectedSituationIds);
                    situationList.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }

                // Refresh map markers if map view is currently visible
                if (mapContainerLayout.getVisibility() == View.VISIBLE) {
                    plotMarkersOnMap();
                }
            }
        });

        viewModel.getDownloadError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null && !message.isEmpty()) {
                    rawDataDisplay.setText(message);
                }
            }
        });

        try {
            Bundle metaData = getPackageManager()
                    .getApplicationInfo(
                            getPackageName(),
                            PackageManager.GET_META_DATA
                    ).metaData;

            if (metaData != null) {
                clientId = metaData.getString(
                        "com.example.cw1s2536891.TRAFFIC_CLIENT_ID", "");

                clientKey = metaData.getString(
                        "com.example.cw1s2536891.TRAFFIC_CLIENT_KEY", "");
            }

            Log.d("TrafficScotland",
                    "Credentials configured: "
                            + (!clientId.isEmpty() && !clientKey.isEmpty()));

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("TrafficScotland",
                    "Could not read app metadata", e);
        }

        // Automatic download on fresh startup if data is not yet loaded
        if (situations.isEmpty() && !viewModel.isDownloading()) {
            startProgress();
        }

        // More Code goes here

    }

    private void updateTypeButtons() {
        if (typeAllButton != null && typeIncidentsButton != null && typeRoadworksButton != null) {
            typeAllButton.setEnabled(!"ALL".equals(currentTypeFilter));
            typeIncidentsButton.setEnabled(!"INCIDENTS".equals(currentTypeFilter));
            typeRoadworksButton.setEnabled(!"ROADWORKS".equals(currentTypeFilter));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        updateHandler.removeCallbacks(updateRunnable);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("SELECTED_IDS", new ArrayList<>(selectedSituationIds));
        outState.putBoolean("SHOWING_MAP", mapContainerLayout.getVisibility() == View.VISIBLE);
        outState.putString("ROAD_SEARCH_QUERY", currentRoadQuery);
        outState.putString("DISTANCE_SEARCH_QUERY", currentDistanceQuery);
        outState.putString("TYPE_FILTER", currentTypeFilter);
    }

    public void onClick(View aview)
    {
        startProgress();
    }

    private void showMapView() {
        scrollViewContainer.setVisibility(View.GONE);
        mapContainerLayout.setVisibility(View.VISIBLE);

        if (selectedSituationIds.isEmpty()) {
            Toast.makeText(this, "Please select situations from the list", Toast.LENGTH_SHORT).show();
        }

        plotMarkersOnMap();
    }

    private void showListView() {
        mapContainerLayout.setVisibility(View.GONE);
        scrollViewContainer.setVisibility(View.VISIBLE);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void filterSituations() {
        displayedSituations.clear();

        displayedSituations.addAll(
                viewModel.getFilteredSituations(
                        currentRoadQuery,
                        currentDistanceQuery,
                        currentTypeFilter));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        String roadQuery = currentRoadQuery.trim()
                .toLowerCase(java.util.Locale.ROOT);

        double maxDistanceKm = -1.0;

        if (!currentDistanceQuery.isEmpty()) {
            try {
                maxDistanceKm =
                        Double.parseDouble(currentDistanceQuery);
            } catch (NumberFormatException e) {
                maxDistanceKm = -1.0;
            }
        }

        if (!situations.isEmpty() && (!roadQuery.isEmpty() || maxDistanceKm > 0 || !"ALL".equals(currentTypeFilter))
                && displayedSituations.isEmpty()) {
            Toast.makeText(this,
                    "No matching situations found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // Centre the map on Scotland (latitude 56.5, longitude -4.2) with zoom level 6
        LatLng scotland = new LatLng(56.5, -4.2);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotland, 6f));

        // If user requested map view before map was ready, plot markers now
        if (mapContainerLayout.getVisibility() == View.VISIBLE) {
            plotMarkersOnMap();
        }
    }

    private void plotMarkersOnMap() {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();

        boolean plottedAny = false;
        for (Situation sit : situations) {
            if (selectedSituationIds.contains(sit.getId())) {
                double lat = 0.0;
                double lng = 0.0;
                boolean validCoord = false;

                // Prefer fromLatitude / fromLongitude
                if (hasValue(sit.getFromLatitude()) && hasValue(sit.getFromLongitude())) {
                    try {
                        lat = Double.parseDouble(sit.getFromLatitude());
                        lng = Double.parseDouble(sit.getFromLongitude());
                        validCoord = true;
                    } catch (NumberFormatException e) {
                        Log.e("TrafficScotland", "Invalid from coordinates", e);
                    }
                }

                // Fallback to toLatitude / toLongitude if from coordinates are missing or invalid
                if (!validCoord && hasValue(sit.getToLatitude()) && hasValue(sit.getToLongitude())) {
                    try {
                        lat = Double.parseDouble(sit.getToLatitude());
                        lng = Double.parseDouble(sit.getToLongitude());
                        validCoord = true;
                    } catch (NumberFormatException e) {
                        Log.e("TrafficScotland", "Invalid to coordinates", e);
                    }
                }

                if (validCoord) {
                    LatLng latLng = new LatLng(lat, lng);
                    String title = (hasValue(sit.getRoadName()) ? sit.getRoadName() : sit.getType());
                    String snippet = sit.getDescription();
                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(title)
                            .snippet(snippet));
                    plottedAny = true;
                }
            }
        }

        if (!plottedAny && !selectedSituationIds.isEmpty()) {
            Toast.makeText(this, "Selected situations do not have valid coordinates", Toast.LENGTH_SHORT).show();
        }
    }



    private void showSituationDetails(Situation situation) {

        StringBuilder details = new StringBuilder();

        details.append("Description: ")
                .append(situation.getDescription())
                .append("\n\n");

        if (hasValue(situation.getRoadName())) {
            details.append("Road: ")
                    .append(situation.getRoadName())
                    .append("\n");
        }

        if (hasValue(situation.getDirection())) {
            details.append("Direction: ")
                    .append(situation.getDirection())
                    .append("\n");
        }

        if (hasValue(situation.getStartTime())) {
            details.append("Start: ")
                    .append(situation.getStartTime())
                    .append("\n");
        }

        if (hasValue(situation.getEndTime())) {
            details.append("End: ")
                    .append(situation.getEndTime())
                    .append("\n");
        }

        if (hasValue(situation.getDelayType())) {
            details.append("Delay: ")
                    .append(situation.getDelayType())
                    .append("\n");
        }

        if (hasValue(situation.getOperationalLanes())) {
            details.append("Operational lanes: ")
                    .append(situation.getOperationalLanes())
                    .append("\n");
        }

        if (hasValue(situation.getRestrictedLanes())) {
            details.append("Restricted lanes: ")
                    .append(situation.getRestrictedLanes())
                    .append("\n");
        }

        if (hasValue(situation.getFromLocation())) {
            details.append("From: ")
                    .append(situation.getFromLocation())
                    .append("\n");
        }

        if (hasValue(situation.getToLocation())) {
            details.append("To: ")
                    .append(situation.getToLocation())
                    .append("\n");
        }

        if (hasValue(situation.getFromLatitude())
                && hasValue(situation.getFromLongitude())) {
            details.append("From coordinates: ")
                    .append(situation.getFromLatitude())
                    .append(", ")
                    .append(situation.getFromLongitude())
                    .append("\n");
        }

        if (hasValue(situation.getToLatitude())
                && hasValue(situation.getToLongitude())) {
            details.append("To coordinates: ")
                    .append(situation.getToLatitude())
                    .append(", ")
                    .append(situation.getToLongitude())
                    .append("\n");
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(situation.getType());
        builder.setMessage(details.toString());
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }



    private String downloadXML(String urlString) throws IOException {
        BufferedReader in = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (!clientId.isEmpty() && !clientKey.isEmpty()) {
                String auth = clientId + ":" + clientKey;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }

            int responseCode = conn.getResponseCode();
            Log.d("TrafficScotland", "URL: " + urlString + " Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine).append("\n");
                }
                String xml = result.toString();
                int i = xml.indexOf("<?");
                if (i >= 0) {
                    xml = xml.substring(i);
                }
                return xml;
            } else {
                Log.e("TrafficScotland", "HTTP error status " + responseCode + " for URL: " + urlString);
                return null;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("TrafficScotland", "Error closing reader", e);
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    public void startProgress()
    {
        if (viewModel.isDownloading()) {
            Log.d("TrafficScotland",
                    "Download already in progress. Skipping.");
            return;
        }

        viewModel.setDownloading(true);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<Situation> tempSituations = new ArrayList<>();


                    String unplannedXml = downloadXML(unplannedEventsURL);
                    int unplannedStatus = (unplannedXml != null) ? HttpURLConnection.HTTP_OK : -1;

                    if (unplannedXml != null) {
                        parseXmlStringInto(unplannedXml, tempSituations);
                    }


                    String roadworksXml = downloadXML(roadworksEventsURL);
                    int roadworksStatus = (roadworksXml != null) ? HttpURLConnection.HTTP_OK : -1;

                    if (roadworksXml != null) {
                        parseXmlStringInto(roadworksXml, tempSituations);
                    }

                    final int finalUnplannedStatus = unplannedStatus;
                    final int finalRoadworksStatus = roadworksStatus;

                    if (unplannedStatus == HttpURLConnection.HTTP_OK && roadworksStatus == HttpURLConnection.HTTP_OK) {
                        Log.d("TrafficScotland", "Total live situations downloaded: " + tempSituations.size());


                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                viewModel.finishSuccessfulDownload(tempSituations);
                            }
                        });
                    } else {
                        Log.e("TrafficScotland", "Failed to load live feeds. Status: " + unplannedStatus + ", " + roadworksStatus);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                viewModel.finishFailedDownload("Error: Could not load live data (HTTP Unplanned: " + finalUnplannedStatus + ", Roadworks: " + finalRoadworksStatus + ")");
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("TrafficScotland", "Error downloading live XML", e);
                    final String errorMsg = e.getMessage();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            viewModel.finishFailedDownload("Error loading live data: " + errorMsg);
                        }
                    });
                }
            }
        }).start();
    }



    private class SituationAdapter extends ArrayAdapter<Situation> {
        private final ArrayList<Situation> items;
        private final HashSet<String> selection;

        public SituationAdapter(MainActivity context, ArrayList<Situation> items, HashSet<String> selection) {
            super(context, R.layout.row_situation, items);
            this.items = items;
            this.selection = selection;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = getLayoutInflater().inflate(R.layout.row_situation, parent, false);
            }

            Situation situation = items.get(position);

            CheckBox checkBox = row.findViewById(R.id.situationCheckBox);
            TextView textView = row.findViewById(R.id.situationTextView);
            ImageView iconView = row.findViewById(R.id.situationIcon);

            int iconResource = R.drawable.ic_situation_default;

            String type = situation.getType();

            if (type != null) {

                if (type.contains("MaintenanceWorks")) {
                    iconResource = R.drawable.ic_roadworks;

                } else if (type.contains("Accident")) {
                    iconResource = R.drawable.ic_accident;

                } else if (type.contains("RoadOrCarriagewayOrLaneManagement")) {
                    iconResource = R.drawable.ic_closure;

                } else if (type.contains("AbnormalTraffic")) {
                    iconResource = R.drawable.ic_congestion;

                } else if (type.contains("Obstruction")) {
                    iconResource = R.drawable.ic_obstruction;

                } else if (type.contains("RoadHazard") || type.contains("Hazard")) {
                    iconResource = R.drawable.ic_hazard;
                }
            }

            iconView.setImageResource(iconResource);

            String label = (hasValue(situation.getRoadName()) ? situation.getRoadName() + " - " : "")
                    + situation.getType() + "\n" + situation.getDescription();
            textView.setText(label);

            int colour = android.graphics.Color.BLACK;

            String delay = situation.getDelayType();

            if (type != null
                    && type.contains("MaintenanceWorks")
                    && delay != null) {

                if (delay.equalsIgnoreCase("longDelays")
                        || delay.equalsIgnoreCase("veryLongDelays")) {

                    colour = android.graphics.Color.RED;

                } else if (delay.equalsIgnoreCase("delays")) {

                    colour = android.graphics.Color.rgb(0, 128, 0);
                }
            }

            textView.setTextColor(colour);


            checkBox.setOnCheckedChangeListener(null);

            checkBox.setChecked(selection.contains(situation.getId()));

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (selection != null) {
                        if (isChecked) {
                            selection.add(situation.getId());
                        } else {
                            selection.remove(situation.getId());
                        }
                    }
                }
            });

            return row;
        }
    }


    @SuppressWarnings("unused")
    private void readSampleXML(String fileName)
            throws IOException, XmlPullParserException
    {
        InputStream inputStream = getAssets().open(fileName);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream));

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        reader.close();

        parseXmlStringInto(result.toString(), situations);
    }


    private void parseXmlStringInto(String xmlContent, ArrayList<Situation> targetList)
            throws IOException, XmlPullParserException
    {
        XmlPullParserFactory factory =
                XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xmlContent));

        int eventType = xpp.getEventType();

        String id = "";
        String type = "";
        String description = "";
        boolean inComment = false;
        String startTime = "";
        String endTime = "";

        String delayType = "";
        String operationalLanes = "";
        String restrictedLanes = "";
        String direction = "";
        String roadName = "";
        String fromLocation = "";
        String toLocation = "";
        String fromLatitude = "";
        String fromLongitude = "";
        String toLatitude = "";
        String toLongitude = "";

        boolean inFrom = false;
        boolean inTo = false;
        boolean inNameBlock = false;
        String blockValue = "";

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {

                String tagName = xpp.getName();


                if (tagName.equals("situation")) {
                    id = xpp.getAttributeValue(null, "id");
                    type = "";
                    description = "";
                    startTime = "";
                    endTime = "";
                    delayType = "";
                    operationalLanes = "";
                    restrictedLanes = "";
                    direction = "";
                    roadName = "";
                    fromLocation = "";
                    toLocation = "";
                    fromLatitude = "";
                    fromLongitude = "";
                    toLatitude = "";
                    toLongitude = "";
                    inFrom = false;
                    inTo = false;
                    inComment = false;
                    inNameBlock = false;
                    blockValue = "";
                }


                if (tagName.equals("situationRecord")) {
                    type = xpp.getAttributeValue(
                            "http://www.w3.org/2001/XMLSchema-instance",
                            "type");
                    if (type != null) {
                        int idx = type.indexOf(':');
                        if (idx >= 0) {
                            type = type.substring(idx + 1);
                        }
                    }
                }

                if (tagName.equals("generalPublicComment")) {
                    inComment = true;
                }

                if (tagName.equals("delaysType")) {
                    delayType = xpp.nextText();
                }

                if (tagName.equals("numberOfOperationalLanes")) {
                    operationalLanes = xpp.nextText();
                }

                if (tagName.equals("numberOfLanesRestricted")) {
                    restrictedLanes = xpp.nextText();
                }

                if (tagName.equals("tpegDirection")) {
                    direction = xpp.nextText();
                }

                if (tagName.equals("overallStartTime")) {
                    startTime = xpp.nextText();
                }

                if (tagName.equals("overallEndTime")) {
                    endTime = xpp.nextText();
                }

                if (tagName.equals("from")) {
                    inFrom = true;
                }

                if (tagName.equals("to")) {
                    inTo = true;
                }

                if (tagName.equals("ilc") || tagName.equals("name") || tagName.equals("otherName")) {
                    inNameBlock = true;
                    blockValue = "";
                }

                if (tagName.equals("latitude")) {
                    String latitude = xpp.nextText();

                    if (inFrom) {
                        fromLatitude = latitude;
                    } else if (inTo) {
                        toLatitude = latitude;
                    }
                }

                if (tagName.equals("longitude")) {
                    String longitude = xpp.nextText();

                    if (inFrom) {
                        fromLongitude = longitude;
                    } else if (inTo) {
                        toLongitude = longitude;
                    }
                }

                if (tagName.equals("value")) {
                    String valText = xpp.nextText();
                    if (inComment) {
                        description = valText;
                    } else if (inNameBlock) {
                        blockValue = valText;
                    }
                }

                if (tagName.equals("tpegOtherPointDescriptorType") || tagName.equals("tpegJunctionPointDescriptorType") || tagName.equals("tpegIlcPointDescriptorType")) {
                    String dtype = xpp.nextText();
                    if (dtype.equals("linkName") || dtype.equals("tpegIlcName1")) {
                        if (roadName.isEmpty() && !blockValue.isEmpty()) {
                            roadName = blockValue;
                            blockValue = "";
                        }
                    } else if (dtype.equals("nonLinkedPointName") || dtype.equals("junctionName") || dtype.equals("intersectionName") || dtype.equals("tpegIlcName2") || dtype.equals("otherPointName")) {
                        if (inFrom && fromLocation.isEmpty() && !blockValue.isEmpty()) {
                            fromLocation = blockValue;
                            blockValue = "";
                        } else if (inTo && toLocation.isEmpty() && !blockValue.isEmpty()) {
                            toLocation = blockValue;
                            blockValue = "";
                        }
                    }
                }


            } else if (eventType == XmlPullParser.END_TAG) {

                String tagName = xpp.getName();

                if (tagName.equals("generalPublicComment")) {
                    inComment = false;
                }

                if (tagName.equals("ilc") || tagName.equals("name") || tagName.equals("otherName")) {
                    inNameBlock = false;
                    blockValue = "";
                }

                if (tagName.equals("from")) {
                    inFrom = false;
                }

                if (tagName.equals("to")) {
                    inTo = false;
                }


                if (tagName.equals("situation")) {
                    Situation situation =
                            new Situation(id, type, description);

                    situation.setStartTime(startTime);
                    situation.setEndTime(endTime);
                    situation.setDelayType(delayType);
                    situation.setOperationalLanes(operationalLanes);
                    situation.setRestrictedLanes(restrictedLanes);
                    situation.setDirection(direction);
                    situation.setRoadName(roadName);
                    situation.setFromLocation(fromLocation);
                    situation.setToLocation(toLocation);
                    situation.setFromLatitude(fromLatitude);
                    situation.setFromLongitude(fromLongitude);
                    situation.setToLatitude(toLatitude);
                    situation.setToLongitude(toLongitude);

                    targetList.add(situation);
                }

            }

            eventType = xpp.next();
        }
    }
}
