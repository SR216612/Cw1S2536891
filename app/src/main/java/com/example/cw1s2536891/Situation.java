package com.example.cw1s2536891;

public class Situation {

    private String id;
    private String type;
    private String description;
    private String startTime;
    private String endTime;

    private String delayType;
    private String operationalLanes;
    private String restrictedLanes;
    private String direction;
    private String roadName;
    private String fromLocation;
    private String toLocation;
    private String fromLatitude;
    private String fromLongitude;
    private String toLatitude;
    private String toLongitude;


    public Situation(String id, String type, String description) {
        this.id = id;
        this.type = type;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return type + ": " + description;
    }


    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDelayType() {
        return delayType;
    }

    public void setDelayType(String delayType) {
        this.delayType = delayType;
    }

    public String getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(String operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public String getRestrictedLanes() {
        return restrictedLanes;
    }

    public void setRestrictedLanes(String restrictedLanes) {
        this.restrictedLanes = restrictedLanes;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getFromLatitude() {
        return fromLatitude;
    }

    public void setFromLatitude(String fromLatitude) {
        this.fromLatitude = fromLatitude;
    }

    public String getFromLongitude() {
        return fromLongitude;
    }

    public void setFromLongitude(String fromLongitude) {
        this.fromLongitude = fromLongitude;
    }

    public String getToLatitude() {
        return toLatitude;
    }

    public void setToLatitude(String toLatitude) {
        this.toLatitude = toLatitude;
    }

    public String getToLongitude() {
        return toLongitude;
    }

    public void setToLongitude(String toLongitude) {
        this.toLongitude = toLongitude;
    }

}
