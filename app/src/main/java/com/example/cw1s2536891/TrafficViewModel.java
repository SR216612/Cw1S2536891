package com.example.cw1s2536891;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;

public class TrafficViewModel extends ViewModel {
    private final ArrayList<Situation> situations = new ArrayList<>();
    private boolean isDownloading = false;
    private final MutableLiveData<Integer> dataVersion = new MutableLiveData<>();
    private final MutableLiveData<String> downloadError = new MutableLiveData<>();

    private static final double FIXED_LATITUDE = 55.8642;
    private static final double FIXED_LONGITUDE = -4.2518;

    public ArrayList<Situation> getSituations() {
        return situations;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public LiveData<Integer> getDataVersion() {
        return dataVersion;
    }

    public LiveData<String> getDownloadError() {
        return downloadError;
    }

    public void finishSuccessfulDownload(ArrayList<Situation> newSituations) {
        situations.clear();
        situations.addAll(newSituations);
        isDownloading = false;
        downloadError.setValue(null);
        Integer version = dataVersion.getValue();
        dataVersion.setValue(version == null ? 1 : version + 1);
    }

    public void finishFailedDownload(String message) {
        isDownloading = false;
        downloadError.setValue(message);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double calculateDistanceKm(double lat1, double lon1,
                                       double lat2, double lon2) {
        double earthRadiusKm = 6371.0;

        double latitudeDifference = Math.toRadians(lat2 - lat1);
        double longitudeDifference = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latitudeDifference / 2) *
                Math.sin(latitudeDifference / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(longitudeDifference / 2) *
                Math.sin(longitudeDifference / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    public ArrayList<Situation> getFilteredSituations(String roadQuery, String distanceQuery, String typeFilter) {
        ArrayList<Situation> results = new ArrayList<>();

        String road = roadQuery == null ? "" : roadQuery.trim().toLowerCase(java.util.Locale.ROOT);
        double maxDistanceKm = -1.0;

        if (distanceQuery != null && !distanceQuery.trim().isEmpty()) {
            try {
                double maxDistanceMiles = Double.parseDouble(distanceQuery.trim());
                if (Double.isFinite(maxDistanceMiles) && maxDistanceMiles > 0) {
                    maxDistanceKm = maxDistanceMiles * 1.609344;
                }
            } catch (NumberFormatException e) {
                maxDistanceKm = -1.0;
            }
        }

        for (Situation situation : situations) {
            boolean matchesRoad = true;
            boolean matchesDistance = true;
            boolean matchesType = true;


            if (!road.isEmpty()) {
                String roadName = situation.getRoadName();
                matchesRoad = (roadName != null && roadName.toLowerCase(java.util.Locale.ROOT).contains(road));
            }


            if (maxDistanceKm > 0) {
                double lat = 0.0;
                double lng = 0.0;
                boolean validCoord = false;

                // Prefer fromLatitude / fromLongitude
                if (hasValue(situation.getFromLatitude()) && hasValue(situation.getFromLongitude())) {
                    try {
                        double tempLat = Double.parseDouble(situation.getFromLatitude());
                        double tempLng = Double.parseDouble(situation.getFromLongitude());
                        if (tempLat >= -90 && tempLat <= 90 && tempLng >= -180 && tempLng <= 180) {
                            lat = tempLat;
                            lng = tempLng;
                            validCoord = true;
                        }
                    } catch (NumberFormatException e) {

                    }
                }


                if (!validCoord && hasValue(situation.getToLatitude()) && hasValue(situation.getToLongitude())) {
                    try {
                        double tempLat = Double.parseDouble(situation.getToLatitude());
                        double tempLng = Double.parseDouble(situation.getToLongitude());
                        if (tempLat >= -90 && tempLat <= 90 && tempLng >= -180 && tempLng <= 180) {
                            lat = tempLat;
                            lng = tempLng;
                            validCoord = true;
                        }
                    } catch (NumberFormatException e) {

                    }
                }

                if (validCoord) {
                    double distance = calculateDistanceKm(FIXED_LATITUDE, FIXED_LONGITUDE, lat, lng);
                    matchesDistance = (distance <= maxDistanceKm);
                } else {
                    matchesDistance = false;
                }
            }


            String situationType = situation.getType();
            boolean isRoadworks = situationType != null && situationType.contains("MaintenanceWorks");

            if ("ROADWORKS".equals(typeFilter)) {
                matchesType = isRoadworks;
            } else if ("INCIDENTS".equals(typeFilter)) {
                matchesType = !isRoadworks;
            }

            if (matchesRoad && matchesDistance && matchesType) {
                results.add(situation);
            }
        }

        return results;
    }
}
