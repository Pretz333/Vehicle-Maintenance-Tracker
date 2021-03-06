package edu.cvtc.capstone.vehiclemaintenancetracker;

import android.util.Log;

public class Issue {
    public static final String TAG = "ISSUE_CLASS";

    private int id;
    private String title;
    private String description;
    private int priority;
    private int vehicleId;
    private int statusId;

    // Constructors, using sets to keep DRY
    // Minimum
    public Issue(String title, int vehicleId, int statusId) {
        this.id = -1;
        setTitle(title);
        this.priority = 0;
        setVehicleId(vehicleId);
        setStatusId(statusId);
    }

    // Everything but the id
    public Issue(String title, String description, int priority, int vehicleId, int statusId) {
        this.id = -1;
        setTitle(title);
        setDescription(description);
        setPriority(priority);
        setVehicleId(vehicleId);
        setStatusId(statusId);
    }

    // Everything, for use when reading from the database. Since it's in the database, it's already passed verification
    public Issue(int id, String title, String description, int priority, int vehicleId, int statusId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.vehicleId = vehicleId;
        this.statusId = statusId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (VerifyUtil.isStringSafe(title)) {
            this.title = title;
        } else {
            Log.w(TAG, "Issue title unsafe");
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (VerifyUtil.isTextSafe(description)) {
            this.description = description;
        } else {
            Log.w(TAG, "Issue description unsafe");
        }
    }

    public int getPriority() {
        return priority;
    }

    public String getPriorityAsString() {
        switch (priority) {
            case 0:
                return "High Priority";
            case 1:
                return "Medium Priority";
            case 2:
                return "Low Priority";
            default:
                return "No Priority";
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    @Override
    public String toString() {
        return "Issue ID: " + id + ", Title: " + title + ", Description: " + description;
    }
}
