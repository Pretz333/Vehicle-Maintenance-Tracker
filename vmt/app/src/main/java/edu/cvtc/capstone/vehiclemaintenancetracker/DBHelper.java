package edu.cvtc.capstone.vehiclemaintenancetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    // The DBHelper class exists as our way of interacting with the database.
    // In order to get, insert (save), or delete information from the database,
    // you should use this class. All of the tables' have a create and drop statement
    // defined at the bottom in a class named tableSQL

    // Class level variables. We save the context so we can alert the user if something goes wrong.
    public static final String TAG = "DBHELPER_CLASS";
    public static final String DATABASE_NAME = "VehicleMaintenanceTracker.db";
    public static final int DATABASE_VERSION = 5;
    private final Context context;

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create all of our tables
        db.execSQL(VehicleSQL.SQL_CREATE_TABLE_VEHICLE);
        db.execSQL(MaintenanceLogSQL.SQL_CREATE_TABLE_MAINTENANCE_LOG);
        db.execSQL(IssueSQL.SQL_CREATE_TABLE_ISSUE);
        db.execSQL(IssueStatusSQL.SQL_CREATE_TABLE_ISSUE_STATUS);
        // db.execSQL(SystemSQL.SQL_CREATE_TABLE_SYSTEM);
        db.execSQL(IssueLogSQL.SQL_CREATE_TABLE_ISSUE_LOG);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) { // Upgrade to version 2
            // Version 1 to version 2's change was that some fields in the vehicle table and log table
            // were changed from NOT NULL to nullable, so we'll change both here. For more info, see
            // https://github.com/Pretz333/2021_Vehicle_Maintenance_Tracker/commit/196894d5e725cb78e4974baa15941eb43d8dfe36
            // NOTE: We no longer need to change the log table, as we have to do it again from v2 to v3

            if (updateVehicleTableToV2(db)) {
                oldVersion = 2; // We're now caught up to DB version 2.
            } else {
                // We failed to update the DB, so we'll start from scratch.
                // This means the user looses all data
                // TODO: Consider alternatives
                resetDB(db);
                oldVersion = newVersion; // We are using the newest table definitions
            }
        }

        if (oldVersion == 2) { // Upgrade to version 3
            // This was modifying one column in the logs table to be nullable. For more info, see
            // https://github.com/Pretz333/2021_Vehicle_Maintenance_Tracker/commit/d1afd991e8f4414ed518d481be3613e21626279a
            // We'll do the same process as above.

            if (updateLogTableToV3(db)) {
                oldVersion = 3; // We're now caught up to DB version 3.
            } else {
                // We failed to update the DB, so we'll start from scratch.
                // This means the user looses all data
                // TODO: Consider alternatives
                resetDB(db);
                oldVersion = newVersion; // We are using the newest table definitions
            }
        }

        if (oldVersion == 3) { // Upgrade to version 4
            // This was adding in issue priorities. We'll need to create the Issue Priority table
            // and rename the "issue_priority" to "issue_priority_id". For more info, see
            // https://github.com/Pretz333/2021_Vehicle_Maintenance_Tracker/commit/9ee1649a63f497457b238799c161a4eb9f6bc76c

            // NOTE: We never ended up using the issue priority table, so we're no longer going to create it
            db.execSQL("ALTER TABLE " + IssueSQL.TABLE_NAME_ISSUE + " RENAME issue_priority TO " + IssueSQL.COLUMN_ISSUE_PRIORITY_ID);
            oldVersion = 4;
        }

        if (oldVersion == 4) {
            // This one is written out as we're not planning on using issue priorities at all,
            // so the class that used to contain all of it's information is now gone
            db.execSQL("DROP TABLE IF EXISTS issue_priority");

            // This is being dropped for now as we're not planning on implementing systems anymore
            db.execSQL(SystemSQL.SQL_DROP_TABLE_SYSTEM);
            // oldVersion = 5; // Unneeded, but could save someone a sanity check later
        }
    }

    //This is called when the onUpgrade fails
    private void resetDB(SQLiteDatabase db) {
        // Drop all tables, functionally deleting the DB
        db.execSQL(VehicleSQL.SQL_DROP_TABLE_VEHICLE);
        db.execSQL(MaintenanceLogSQL.SQL_DROP_TABLE_MAINTENANCE_LOG);
        db.execSQL(IssueSQL.SQL_DROP_TABLE_ISSUE);
        db.execSQL(IssueStatusSQL.SQL_DROP_TABLE_ISSUE_STATUS);
        // db.execSQL(SystemSQL.SQL_DROP_TABLE_SYSTEM);
        db.execSQL(IssueLogSQL.SQL_DROP_TABLE_ISSUE_LOG);

        // Create all tables again
        db.execSQL(VehicleSQL.SQL_CREATE_TABLE_VEHICLE);
        db.execSQL(MaintenanceLogSQL.SQL_CREATE_TABLE_MAINTENANCE_LOG);
        db.execSQL(IssueSQL.SQL_CREATE_TABLE_ISSUE);
        db.execSQL(IssueStatusSQL.SQL_CREATE_TABLE_ISSUE_STATUS);
        // db.execSQL(SystemSQL.SQL_CREATE_TABLE_SYSTEM);
        db.execSQL(IssueLogSQL.SQL_CREATE_TABLE_ISSUE_LOG);
    }

    // Inserts

    public void insertVehicle(Vehicle vehicle) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if (vehicle.getId() != -1) {
            values.put(VehicleSQL._ID, vehicle.getId());
        }
        values.put(VehicleSQL.COLUMN_VEHICLE_MAKE, vehicle.getMake());
        values.put(VehicleSQL.COLUMN_VEHICLE_MODEL, vehicle.getModel());
        values.put(VehicleSQL.COLUMN_VEHICLE_YEAR, vehicle.getYear());
        values.put(VehicleSQL.COLUMN_VEHICLE_NICKNAME, vehicle.getName());
        values.put(VehicleSQL.COLUMN_VEHICLE_COLOR, vehicle.getColor());
        values.put(VehicleSQL.COLUMN_VEHICLE_MILEAGE, vehicle.getMileage());
        values.put(VehicleSQL.COLUMN_VEHICLE_VIN, vehicle.getVIN());
        values.put(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE, vehicle.getLicensePlate());
        if (vehicle.getPurchaseDate() != null) { // Calling .getTime() on a null date throws an error
            values.put(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED, vehicle.getPurchaseDate().getTime());
        }
        values.put(VehicleSQL.COLUMN_VEHICLE_VALUE, vehicle.getValue());

        long newRowId = db.insert(VehicleSQL.TABLE_NAME_VEHICLE, null, values);
        db.close();

        if (newRowId == -1) {
            Log.w(TAG, "DB Insert Failed!");
            VerifyUtil.alertUser(context, "Database Insert Failed", "Your data did not save, please try again");
        }

        if (newRowId != vehicle.getId()) {
            vehicle.setId((int) newRowId);
        }

    }

    public void insertMaintenanceLog(MaintenanceLog maintenanceLog) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if (maintenanceLog.getId() != -1) {
            values.put(MaintenanceLogSQL._ID, maintenanceLog.getId());
        }
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE, maintenanceLog.getTitle());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION, maintenanceLog.getDescription());
        if (maintenanceLog.getDate() == null) {
            // If they didn't set the day of the log, we'll set it to right now
            maintenanceLog.setDate(Calendar.getInstance().getTime());
        }
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE, maintenanceLog.getDate().getTime());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST, maintenanceLog.getCost());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME, maintenanceLog.getTime());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE, maintenanceLog.getMileage());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID, maintenanceLog.getVehicleId());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID, maintenanceLog.getSystemId());

        long newRowId = db.insert(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, null, values);
        db.close();

        if (newRowId == -1) {
            Log.w(TAG, "DB Insert Failed!");
            VerifyUtil.alertUser(context, "Database Insert Failed", "Your data did not save, please try again");
        }

        if (newRowId != maintenanceLog.getId()) {
            maintenanceLog.setId((int) newRowId);
        }

    }

    /* This is unused

    public void insertSystem(System system) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if(system.getId() != -1) {
            values.put(SystemSQL._ID, system.getId());
        }
        values.put(SystemSQL.COLUMN_SYSTEM_DESCRIPTION, system.getDescription());

        long newRowId = db.insert(SystemSQL.TABLE_NAME_SYSTEM, null, values);
        db.close();

        if(newRowId == -1){
            Log.w(TAG, "DB Insert Failed!");
            VerifyUtil.alertUser(context, "Database Insert Failed", "Your data did not save, please try again");
        }

        if(newRowId != system.getId()){
            system.setId((int) newRowId);
        }

    }

     */

    public void insertIssue(Issue issue) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if (issue.getId() != -1) {
            values.put(IssueSQL._ID, issue.getId());
        }
        values.put(IssueSQL.COLUMN_ISSUE_TITLE, issue.getTitle());
        values.put(IssueSQL.COLUMN_ISSUE_DESCRIPTION, issue.getDescription());
        values.put(IssueSQL.COLUMN_ISSUE_PRIORITY_ID, issue.getPriority());
        values.put(IssueSQL.COLUMN_ISSUE_VEHICLE_ID, issue.getVehicleId());
        values.put(IssueSQL.COLUMN_ISSUE_STATUS_ID, issue.getStatusId());

        long newRowId = db.insert(IssueSQL.TABLE_NAME_ISSUE, null, values);
        db.close();

        if (newRowId == -1) {
            Log.w(TAG, "DB Insert Failed!");
            VerifyUtil.alertUser(context, "Database Insert Failed", "Your data did not save, please try again");
        }

        if (newRowId != issue.getId()) {
            issue.setId((int) newRowId);
        }

    }

    private void insertIssueStatus(IssueStatus issueStatus) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        if (issueStatus.getId() != -1) {
            values.put(IssueStatusSQL._ID, issueStatus.getId());
        }
        values.put(IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION, issueStatus.getDescription());

        long newRowId = db.insert(IssueStatusSQL.TABLE_NAME_ISSUE_STATUS, null, values);
        db.close();

        if (newRowId == -1) {
            Log.w(TAG, "DB Insert Failed!");
            VerifyUtil.alertUser(context, "Database Insert Failed", "Your data did not save, please try again");
        }

        if (newRowId != issueStatus.getId()) {
            issueStatus.setId((int) newRowId);
        }

    }

    // Updates

    public void updateVehicle(Vehicle vehicle) {
        // Get a writable db
        SQLiteDatabase db = getWritableDatabase();

        // Set all columns
        ContentValues values = new ContentValues();
        values.put(VehicleSQL._ID, vehicle.getId());
        values.put(VehicleSQL.COLUMN_VEHICLE_MAKE, vehicle.getMake());
        values.put(VehicleSQL.COLUMN_VEHICLE_MODEL, vehicle.getModel());
        values.put(VehicleSQL.COLUMN_VEHICLE_YEAR, vehicle.getYear());
        values.put(VehicleSQL.COLUMN_VEHICLE_NICKNAME, vehicle.getName());
        values.put(VehicleSQL.COLUMN_VEHICLE_COLOR, vehicle.getColor());
        values.put(VehicleSQL.COLUMN_VEHICLE_MILEAGE, vehicle.getMileage());
        values.put(VehicleSQL.COLUMN_VEHICLE_VIN, vehicle.getVIN());
        values.put(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE, vehicle.getLicensePlate());
        if (vehicle.getPurchaseDate() != null) { // Calling .getTime() on a null date throws an error
            values.put(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED, vehicle.getPurchaseDate().getTime());
        }
        values.put(VehicleSQL.COLUMN_VEHICLE_VALUE, vehicle.getValue());

        // Grab the vehicle ID and put it into a String[] for the where clause
        String[] args = {String.valueOf(vehicle.getId())};

        db.update(VehicleSQL.TABLE_NAME_VEHICLE, values, VehicleSQL._ID + "=?", args);
    }

    public void updateLog(MaintenanceLog maintenanceLog) {
        // Get a writable db
        SQLiteDatabase db = getWritableDatabase();

        // Set all values
        ContentValues values = new ContentValues();
        values.put(MaintenanceLogSQL._ID, maintenanceLog.getId());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE, maintenanceLog.getTitle());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION, maintenanceLog.getDescription());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE, maintenanceLog.getDate().getTime());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST, maintenanceLog.getCost());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME, maintenanceLog.getTime());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE, maintenanceLog.getMileage());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID, maintenanceLog.getVehicleId());
        values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID, maintenanceLog.getSystemId());

        // Grab the ID and put it into a String[] for the where clause
        String[] args = {String.valueOf(maintenanceLog.getId())};

        db.update(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, values, MaintenanceLogSQL._ID + "=?", args);
    }

    public void updateIssue(Issue issue) {
        // Get a writable database
        SQLiteDatabase db = getWritableDatabase();

        // Set all of the values
        ContentValues values = new ContentValues();
        values.put(IssueSQL._ID, issue.getId());
        values.put(IssueSQL.COLUMN_ISSUE_TITLE, issue.getTitle());
        values.put(IssueSQL.COLUMN_ISSUE_DESCRIPTION, issue.getDescription());
        values.put(IssueSQL.COLUMN_ISSUE_PRIORITY_ID, issue.getPriority());
        values.put(IssueSQL.COLUMN_ISSUE_VEHICLE_ID, issue.getVehicleId());
        values.put(IssueSQL.COLUMN_ISSUE_STATUS_ID, issue.getStatusId());

        // Get the id of the issue to use for the where clause
        String[] args = {String.valueOf(issue.getId())};

        db.update(IssueSQL.TABLE_NAME_ISSUE, values, IssueSQL._ID + "=?", args);
    }

    private boolean updateVehicleTableToV2(SQLiteDatabase db) {
        try {
            // Start a transaction, as this would be really messy if it became messed up
            db.execSQL("BEGIN TRANSACTION");

            // Get all of the vehicle data from the vehicles table
            String[] vehicleColumns = {
                    VehicleSQL._ID,
                    VehicleSQL.COLUMN_VEHICLE_MAKE,
                    VehicleSQL.COLUMN_VEHICLE_MODEL,
                    VehicleSQL.COLUMN_VEHICLE_YEAR,
                    VehicleSQL.COLUMN_VEHICLE_NICKNAME,
                    VehicleSQL.COLUMN_VEHICLE_COLOR,
                    VehicleSQL.COLUMN_VEHICLE_MILEAGE,
                    VehicleSQL.COLUMN_VEHICLE_VIN,
                    VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE,
                    VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED,
                    VehicleSQL.COLUMN_VEHICLE_VALUE};
            Cursor cursor = db.query(VehicleSQL.TABLE_NAME_VEHICLE, vehicleColumns, null,
                    null, null, null, null);

            int vehicleIdPosition = cursor.getColumnIndex(VehicleSQL._ID);
            int vehicleMakePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MAKE);
            int vehicleModelPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MODEL);
            int vehicleYearPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_YEAR);
            int vehicleNicknamePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_NICKNAME);
            int vehicleColorPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_COLOR);
            int vehicleMileagePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MILEAGE);
            int vehicleVINPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VIN);
            int vehicleLicensePlatePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE);
            int vehicleDatePurchasedPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED);
            int vehicleValuePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VALUE);

            ArrayList<Vehicle> vehicles = new ArrayList<>();

            while (cursor.moveToNext()) {
                // We don't want to set the date to epoch, and since longs can't be null, we'll flip
                // which constructor we're using based on if the time is 0, meaning it wasn't set
                if (cursor.getLong(vehicleDatePurchasedPosition) == 0L) {
                    vehicles.add(new Vehicle(cursor.getInt(vehicleIdPosition), cursor.getString(vehicleNicknamePosition),
                            cursor.getString(vehicleMakePosition), cursor.getString(vehicleModelPosition),
                            cursor.getString(vehicleYearPosition), cursor.getString(vehicleColorPosition),
                            cursor.getInt(vehicleMileagePosition), cursor.getString(vehicleVINPosition),
                            cursor.getString(vehicleLicensePlatePosition), cursor.getInt(vehicleValuePosition)));
                } else {
                    vehicles.add(new Vehicle(cursor.getInt(vehicleIdPosition), cursor.getString(vehicleNicknamePosition),
                            cursor.getString(vehicleMakePosition), cursor.getString(vehicleModelPosition),
                            cursor.getString(vehicleYearPosition), cursor.getString(vehicleColorPosition),
                            cursor.getInt(vehicleMileagePosition), cursor.getString(vehicleVINPosition),
                            cursor.getString(vehicleLicensePlatePosition), new Date(cursor.getLong(vehicleDatePurchasedPosition)),
                            cursor.getInt(vehicleValuePosition)));
                }
            }

            cursor.close();

            // Drop the old table and recreate the table with the new fields as currently defined.
            // We wouldn't need to do this if we could MODIFY, but Android only allows ADD and RENAME
            db.execSQL(VehicleSQL.SQL_DROP_TABLE_VEHICLE);
            db.execSQL(VehicleSQL.SQL_CREATE_TABLE_VEHICLE);

            // Insert in all of the old data
            for (Vehicle vehicle : vehicles) {
                ContentValues vehicleValues = new ContentValues();

                vehicleValues.put(VehicleSQL._ID, vehicle.getId());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_MAKE, vehicle.getMake());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_MODEL, vehicle.getModel());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_YEAR, vehicle.getYear());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_NICKNAME, vehicle.getName());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_COLOR, vehicle.getColor());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_MILEAGE, vehicle.getMileage());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_VIN, vehicle.getVIN());
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE, vehicle.getLicensePlate());
                if (vehicle.getPurchaseDate() != null) {
                    vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED, vehicle.getPurchaseDate().getTime());
                }
                vehicleValues.put(VehicleSQL.COLUMN_VEHICLE_VALUE, vehicle.getValue());

                int newRowId = (int) db.insert(VehicleSQL.TABLE_NAME_VEHICLE, null, vehicleValues);

                // If the insert failed, we want to rollback as this means the
                // user's data isn't going to properly transfer to the new table
                if (newRowId == -1) {
                    throw new Exception("DB Insert Failed!");
                }

                if (newRowId != vehicle.getId()) {
                    // Update issues and logs vehicleID FK to the new ID
                    try {
                        // Logs
                        List<MaintenanceLog> logs = getAllLogsByVehicleId(vehicle.getId());
                        for (MaintenanceLog maintenanceLog : logs) {
                            maintenanceLog.setVehicleId(newRowId);

                            // Set all values
                            ContentValues logValues = new ContentValues();
                            logValues.put(MaintenanceLogSQL._ID, maintenanceLog.getId());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE, maintenanceLog.getTitle());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION, maintenanceLog.getDescription());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE, maintenanceLog.getDate().getTime());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST, maintenanceLog.getCost());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME, maintenanceLog.getTime());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE, maintenanceLog.getMileage());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID, maintenanceLog.getVehicleId());
                            logValues.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID, maintenanceLog.getSystemId());

                            // Grab the ID and put it into a String[] for the where clause
                            String[] args = {String.valueOf(maintenanceLog.getId())};

                            db.update(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, logValues, MaintenanceLogSQL._ID + "=?", args);
                        }

                        // Open issues
                        List<Issue> issues = getAllIssuesByVehicleId(vehicle.getId(), false);
                        for (Issue issue : issues) {
                            issue.setVehicleId(newRowId);

                            // Set all of the values
                            ContentValues issueValues = new ContentValues();
                            issueValues.put(IssueSQL._ID, issue.getId());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_TITLE, issue.getTitle());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_DESCRIPTION, issue.getDescription());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_PRIORITY_ID, issue.getPriority());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_VEHICLE_ID, issue.getVehicleId());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_STATUS_ID, issue.getStatusId());

                            // Get the id of the issue to use for the where clause
                            String[] args = {String.valueOf(issue.getId())};

                            db.update(IssueSQL.TABLE_NAME_ISSUE, issueValues, IssueSQL._ID + "=?", args);
                        }

                        // Closed issues
                        issues = getAllIssuesByVehicleId(vehicle.getId(), true);
                        for (Issue issue : issues) {
                            issue.setVehicleId(newRowId);

                            // Set all of the values
                            ContentValues issueValues = new ContentValues();
                            issueValues.put(IssueSQL._ID, issue.getId());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_TITLE, issue.getTitle());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_DESCRIPTION, issue.getDescription());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_PRIORITY_ID, issue.getPriority());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_VEHICLE_ID, issue.getVehicleId());
                            issueValues.put(IssueSQL.COLUMN_ISSUE_STATUS_ID, issue.getStatusId());

                            // Get the id of the issue to use for the where clause
                            String[] args = {String.valueOf(issue.getId())};

                            db.update(IssueSQL.TABLE_NAME_ISSUE, issueValues, IssueSQL._ID + "=?", args);
                        }
                    } catch (Exception ex) {
                        // If it failed, no worries
                        // The only thing that could fail at this point is a retrieval of data
                        // that doesn't exist, which means it doesn't need to be updated
                    }

                    vehicle.setId(newRowId);
                }
            }

            db.execSQL("COMMIT");
            return true;
        } catch (Exception ex) {
            // Something went wrong. Log it for debug purposes
            // then rollback so we don't lose any user data
            Log.e(TAG, "onUpgrade: " + ex.getMessage());
            Log.e(TAG, "onUpgrade: " + ex.toString());
            db.execSQL("ROLLBACK");
            return false;
        }
    }

    private boolean updateLogTableToV3(SQLiteDatabase db) {
        try {
            // Start a transaction, as this would be really messy if it became messed up
            db.execSQL("BEGIN TRANSACTION");

            // Get all of the data
            String[] categoryColumns = {
                    MaintenanceLogSQL._ID,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID,
                    MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID
            };

            ArrayList<MaintenanceLog> logs = new ArrayList<>();

            Cursor cursor = db.query(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, categoryColumns, null,
                    null, null, null, null);


            int idPosition = cursor.getColumnIndex(MaintenanceLogSQL._ID);
            int titlePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE);
            int descriptionPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION);
            int datePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE);
            int costPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST);
            int timePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME);
            int mileagePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE);
            int vehicleIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID);
            int systemIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID);

            while (cursor.moveToNext()) {
                logs.add(new MaintenanceLog(
                        cursor.getInt(idPosition),
                        cursor.getString(titlePosition),
                        cursor.getString(descriptionPosition),
                        new Date(cursor.getLong(datePosition)),
                        cursor.getInt(costPosition),
                        cursor.getInt(timePosition),
                        cursor.getInt(mileagePosition),
                        cursor.getInt(vehicleIdPosition),
                        cursor.getInt(systemIdPosition)
                ));
            }

            cursor.close();

            // Drop the old table and recreate the table with the new fields as we want them.
            // We wouldn't need to do this if we could MODIFY, but Android only allows ADD and RENAME
            db.execSQL(MaintenanceLogSQL.SQL_DROP_TABLE_MAINTENANCE_LOG);
            db.execSQL(MaintenanceLogSQL.SQL_CREATE_TABLE_MAINTENANCE_LOG);

            // Insert in all of the old data
            for (MaintenanceLog maintenanceLog : logs) {
                ContentValues values = new ContentValues();

                values.put(MaintenanceLogSQL._ID, maintenanceLog.getId());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE, maintenanceLog.getTitle());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION, maintenanceLog.getDescription());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE, maintenanceLog.getDate().getTime());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST, maintenanceLog.getCost());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME, maintenanceLog.getTime());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE, maintenanceLog.getMileage());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID, maintenanceLog.getVehicleId());
                values.put(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID, maintenanceLog.getSystemId());

                long newRowId = db.insert(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, null, values);

                // If the insert failed, we want to rollback as this means the
                // user's data isn't going to properly transfer to the new table
                if (newRowId == -1) {
                    throw new Exception("DB Insert Failed!");
                }

                if (newRowId != maintenanceLog.getId()) {
                    maintenanceLog.setId((int) newRowId);
                }
            }

            db.execSQL("COMMIT");
            return true;
        } catch (Exception ex) {
            // Something went wrong. Log it for debug purposes
            // then rollback so we don't lose any user data
            Log.e(TAG, "onUpgrade: " + ex.getMessage());
            Log.e(TAG, "onUpgrade: " + ex.toString());
            db.execSQL("ROLLBACK");
            return false;
        }
    }

    // Retrievals

    public ArrayList<Vehicle> getAllVehicles() {
        SQLiteDatabase db = getReadableDatabase();

        String[] vehicleColumns = {
                VehicleSQL._ID,
                VehicleSQL.COLUMN_VEHICLE_MAKE,
                VehicleSQL.COLUMN_VEHICLE_MODEL,
                VehicleSQL.COLUMN_VEHICLE_YEAR,
                VehicleSQL.COLUMN_VEHICLE_NICKNAME,
                VehicleSQL.COLUMN_VEHICLE_COLOR,
                VehicleSQL.COLUMN_VEHICLE_MILEAGE,
                VehicleSQL.COLUMN_VEHICLE_VIN,
                VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE,
                VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED,
                VehicleSQL.COLUMN_VEHICLE_VALUE};

        String vehicleOrderBy = VehicleSQL._ID;
        Cursor cursor = db.query(VehicleSQL.TABLE_NAME_VEHICLE, vehicleColumns, null,
                null, null, null, vehicleOrderBy);

        int vehicleIdPosition = cursor.getColumnIndex(VehicleSQL._ID);
        int vehicleMakePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MAKE);
        int vehicleModelPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MODEL);
        int vehicleYearPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_YEAR);
        int vehicleNicknamePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_NICKNAME);
        int vehicleColorPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_COLOR);
        int vehicleMileagePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MILEAGE);
        int vehicleVINPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VIN);
        int vehicleLicensePlatePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE);
        int vehicleDatePurchasedPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED);
        int vehicleValuePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VALUE);

        ArrayList<Vehicle> vehicles = new ArrayList<>();

        while (cursor.moveToNext()) {
            // We don't want to set the date to epoch, and since longs can't be null, we'll flip
            // which constructor we're using based on if the time is 0, meaning it wasn't set
            if (cursor.getLong(vehicleDatePurchasedPosition) == 0L) {
                vehicles.add(new Vehicle(cursor.getInt(vehicleIdPosition), cursor.getString(vehicleNicknamePosition),
                        cursor.getString(vehicleMakePosition), cursor.getString(vehicleModelPosition),
                        cursor.getString(vehicleYearPosition), cursor.getString(vehicleColorPosition),
                        cursor.getInt(vehicleMileagePosition), cursor.getString(vehicleVINPosition),
                        cursor.getString(vehicleLicensePlatePosition), cursor.getInt(vehicleValuePosition)));
            } else {
                vehicles.add(new Vehicle(cursor.getInt(vehicleIdPosition), cursor.getString(vehicleNicknamePosition),
                        cursor.getString(vehicleMakePosition), cursor.getString(vehicleModelPosition),
                        cursor.getString(vehicleYearPosition), cursor.getString(vehicleColorPosition),
                        cursor.getInt(vehicleMileagePosition), cursor.getString(vehicleVINPosition),
                        cursor.getString(vehicleLicensePlatePosition), new Date(cursor.getLong(vehicleDatePurchasedPosition)),
                        cursor.getInt(vehicleValuePosition)));
            }
        }
        cursor.close();
        db.close();

        return vehicles;
    }

    public ArrayList<MaintenanceLog> getAllLogsByVehicleId(int id) {
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] categoryColumns = {
                MaintenanceLogSQL._ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID
        };

        String orderBy = MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE + " DESC";
        String filter = MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID + " = ?";
        String[] filterArgs = {String.valueOf(id)};

        ArrayList<MaintenanceLog> logs = new ArrayList<>();

        Cursor cursor = db.query(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, categoryColumns, filter,
                filterArgs, null, null, orderBy);


        int idPosition = cursor.getColumnIndex(MaintenanceLogSQL._ID);
        int titlePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE);
        int descriptionPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION);
        int datePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE);
        int costPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST);
        int timePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME);
        int mileagePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE);
        int vehicleIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID);
        int systemIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID);

        while (cursor.moveToNext()) {
            logs.add(new MaintenanceLog(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    new Date(cursor.getLong(datePosition)),
                    cursor.getInt(costPosition),
                    cursor.getInt(timePosition),
                    cursor.getInt(mileagePosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(systemIdPosition)
            ));
        }

        cursor.close();
        db.close();

        return logs;
    }

    public ArrayList<MaintenanceLog> getAllLogsBySearchTerm(String searchTerm, int vehicleId) {
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] categoryColumns = {
                MaintenanceLogSQL._ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID
        };

        // Only search logs where the vehicleID is the vehicle the user is looking at
        String orderBy = MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE + " DESC";
        String filter = MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE + " LIKE ? AND " +
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID + " = ?";
        String[] filterArgs = {"%" + searchTerm + "%", String.valueOf(vehicleId)};

        ArrayList<MaintenanceLog> logs = new ArrayList<>();

        Cursor cursor = db.query(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, categoryColumns, filter,
                filterArgs, null, null, orderBy);


        int idPosition = cursor.getColumnIndex(MaintenanceLogSQL._ID);
        int titlePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE);
        int descriptionPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION);
        int datePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE);
        int costPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST);
        int timePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME);
        int mileagePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE);
        int vehicleIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID);
        int systemIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID);

        while (cursor.moveToNext()) {
            logs.add(new MaintenanceLog(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    new Date(cursor.getLong(datePosition)),
                    cursor.getInt(costPosition),
                    cursor.getInt(timePosition),
                    cursor.getInt(mileagePosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(systemIdPosition)
            ));
        }

        cursor.close();
        db.close();

        return logs;
    }

    public ArrayList<Issue> getAllIssuesByVehicleId(int id, boolean viewClosed) {
        // Above so we don't have two open databases
        int closedIssueId = getClosedIssueStatusId();
        int openIssueId = getOpenIssueStatusId();

        // Get a readable database
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] issueColumns = {
                IssueSQL._ID,
                IssueSQL.COLUMN_ISSUE_TITLE,
                IssueSQL.COLUMN_ISSUE_DESCRIPTION,
                IssueSQL.COLUMN_ISSUE_PRIORITY_ID,
                IssueSQL.COLUMN_ISSUE_VEHICLE_ID,
                IssueSQL.COLUMN_ISSUE_STATUS_ID
        };

        String orderBy = IssueSQL.COLUMN_ISSUE_PRIORITY_ID;
        String filter = IssueSQL.COLUMN_ISSUE_VEHICLE_ID + " = ?";
        String[] filterArgs = {String.valueOf(id)};

        // Don't include closed if the user doesn't want them
        if (viewClosed) {
            filter += " AND " + IssueSQL.COLUMN_ISSUE_STATUS_ID + " = " + closedIssueId;
        } else {
            filter += " AND " + IssueSQL.COLUMN_ISSUE_STATUS_ID + " = " + openIssueId;
        }

        ArrayList<Issue> issues = new ArrayList<>();

        Cursor cursor = db.query(IssueSQL.TABLE_NAME_ISSUE, issueColumns, filter,
                filterArgs, null, null, orderBy);

        int idPosition = cursor.getColumnIndex(IssueSQL._ID);
        int titlePosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_TITLE);
        int descriptionPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_DESCRIPTION);
        int priorityIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_PRIORITY_ID);
        int vehicleIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_VEHICLE_ID);
        int statusIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_STATUS_ID);

        while (cursor.moveToNext()) {
            issues.add(new Issue(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    cursor.getInt(priorityIdPosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(statusIdPosition)
            ));
        }

        cursor.close();
        db.close();

        return issues;
    }

    public ArrayList<Issue> getAllIssuesBySearchTerm(String searchTerm, int vehicleId, boolean viewingClosed) {
        // Above so we only have one open copy of the DB at a time
        int status = viewingClosed ? getClosedIssueStatusId() : getOpenIssueStatusId();

        // Get a readable database
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] issueColumns = {
                IssueSQL._ID,
                IssueSQL.COLUMN_ISSUE_TITLE,
                IssueSQL.COLUMN_ISSUE_DESCRIPTION,
                IssueSQL.COLUMN_ISSUE_PRIORITY_ID,
                IssueSQL.COLUMN_ISSUE_VEHICLE_ID,
                IssueSQL.COLUMN_ISSUE_STATUS_ID
        };

        // Only return results from the current vehicle and from the status (open/closed) they are viewing
        String orderBy = IssueSQL.COLUMN_ISSUE_PRIORITY_ID;
        String filter = IssueSQL.COLUMN_ISSUE_TITLE + " LIKE ? AND " +
                IssueSQL.COLUMN_ISSUE_VEHICLE_ID + " = ? AND " +
                IssueSQL.COLUMN_ISSUE_STATUS_ID + "= ?";
        String[] filterArgs = {"%" + searchTerm + "%", String.valueOf(vehicleId), String.valueOf(status)};

        ArrayList<Issue> issues = new ArrayList<>();

        Cursor cursor = db.query(IssueSQL.TABLE_NAME_ISSUE, issueColumns, filter,
                filterArgs, null, null, orderBy);

        int idPosition = cursor.getColumnIndex(IssueSQL._ID);
        int titlePosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_TITLE);
        int descriptionPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_DESCRIPTION);
        int priorityIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_PRIORITY_ID);
        int vehicleIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_VEHICLE_ID);
        int statusIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_STATUS_ID);

        while (cursor.moveToNext()) {
            issues.add(new Issue(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    cursor.getInt(priorityIdPosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(statusIdPosition)
            ));
        }

        cursor.close();
        db.close();

        return issues;
    }

    /* This is unused

    public List<System> getAllSystems() {
        SQLiteDatabase db = getReadableDatabase();

        String[] systemColumns = {
                SystemSQL._ID,
                SystemSQL.COLUMN_SYSTEM_DESCRIPTION};

        String systemOrderBy = SystemSQL._ID;
        Cursor cursor = db.query(SystemSQL.TABLE_NAME_SYSTEM, systemColumns, null,
                null, null, null, systemOrderBy);

        int systemIdPosition = cursor.getColumnIndex(SystemSQL._ID);
        int systemDescriptionPosition = cursor.getColumnIndex(SystemSQL.COLUMN_SYSTEM_DESCRIPTION);

        List<System> systems = new ArrayList<>();

        while (cursor.moveToNext()) {
            systems.add(new System(cursor.getInt(systemIdPosition), cursor.getString(systemDescriptionPosition)));
        }
        cursor.close();
        db.close();

        return systems;
    }

     */

    public Vehicle getVehicleById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Vehicle v = null;

        // Get all of the fields
        String[] categoryColumns = {
                VehicleSQL._ID,
                VehicleSQL.COLUMN_VEHICLE_MAKE,
                VehicleSQL.COLUMN_VEHICLE_MODEL,
                VehicleSQL.COLUMN_VEHICLE_YEAR,
                VehicleSQL.COLUMN_VEHICLE_NICKNAME,
                VehicleSQL.COLUMN_VEHICLE_COLOR,
                VehicleSQL.COLUMN_VEHICLE_MILEAGE,
                VehicleSQL.COLUMN_VEHICLE_VIN,
                VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE,
                VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED,
                VehicleSQL.COLUMN_VEHICLE_VALUE
        };

        // Write the query in a SQL injection-proof way
        String filter = VehicleSQL._ID + " = ?";
        String[] filterArgs = {String.valueOf(id)};

        try {
            Cursor cursor = db.query(VehicleSQL.TABLE_NAME_VEHICLE, categoryColumns, filter,
                    filterArgs, null, null, null);

            // Get the places where all of the information is stored
            int idPosition = cursor.getColumnIndex(VehicleSQL._ID);
            int makePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MAKE);
            int modelPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MODEL);
            int yearPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_YEAR);
            int namePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_NICKNAME);
            int colorPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_COLOR);
            int mileagePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_MILEAGE);
            int VINPosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VIN);
            int licensePlatePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_LICENSE_PLATE);
            int datePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_DATE_PURCHASED);
            int valuePosition = cursor.getColumnIndex(VehicleSQL.COLUMN_VEHICLE_VALUE);

            // Get the information of the first matching vehicle (so be as specific as possible!)
            cursor.moveToFirst();

            // We don't want to set the date to epoch, and since longs can't be null, we'll flip
            // which constructor we're using based on if the time is 0, meaning it wasn't set
            if (cursor.getLong(datePosition) == 0L) {
                v = new Vehicle(
                        cursor.getInt(idPosition), cursor.getString(namePosition),
                        cursor.getString(makePosition), cursor.getString(modelPosition),
                        cursor.getString(yearPosition), cursor.getString(colorPosition),
                        cursor.getInt(mileagePosition), cursor.getString(VINPosition),
                        cursor.getString(licensePlatePosition), cursor.getInt(valuePosition)
                );
            } else {
                v = new Vehicle(
                        cursor.getInt(idPosition), cursor.getString(namePosition),
                        cursor.getString(makePosition), cursor.getString(modelPosition),
                        cursor.getString(yearPosition), cursor.getString(colorPosition),
                        cursor.getInt(mileagePosition), cursor.getString(VINPosition),
                        cursor.getString(licensePlatePosition), new Date(cursor.getLong(datePosition)),
                        cursor.getInt(valuePosition)
                );
            }
            cursor.close();
        } catch (Exception ex) {
            Log.e(TAG, "getVehicleById: " + ex.getMessage());
            VerifyUtil.alertUser(context, "Database Retrieval Failed", "Unable to fetch vehicle information, please try again");
        }

        db.close();

        return v;
    }

    public MaintenanceLog getLogByLogId(int id) {
        SQLiteDatabase db = getReadableDatabase();
        MaintenanceLog log = null;

        // Get all of the fields
        String[] categoryColumns = {
                MaintenanceLogSQL._ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID,
                MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID
        };

        String filter = MaintenanceLogSQL._ID + " = ?";
        String[] filterArgs = {String.valueOf(id)};

        try {
            Cursor cursor = db.query(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, categoryColumns, filter,
                    filterArgs, null, null, null);


            int idPosition = cursor.getColumnIndex(MaintenanceLogSQL._ID);
            int titlePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TITLE);
            int descriptionPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DESCRIPTION);
            int datePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_DATE);
            int costPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_COST);
            int timePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_TOTAL_TIME);
            int mileagePosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_MILEAGE);
            int vehicleIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID);
            int systemIdPosition = cursor.getColumnIndex(MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_SYSTEM_ID);

            // Get the information of the first matching log (so be as specific as possible!)
            cursor.moveToFirst();
            log = new MaintenanceLog(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    new Date(cursor.getLong(datePosition)),
                    cursor.getInt(costPosition),
                    cursor.getInt(timePosition),
                    cursor.getInt(mileagePosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(systemIdPosition)
            );
            cursor.close();
        } catch (Exception ex) {
            Log.e(TAG, "getLogByLogId: " + ex.getMessage());
            VerifyUtil.alertUser(context, "Database Retrieval Failed", "Unable to fetch log information, please try again");
        }

        db.close();

        return log;
    }

    public Issue getIssueByIssueId(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Issue issue = null;

        // Get all of the fields
        String[] issueColumns = {
                IssueSQL._ID,
                IssueSQL.COLUMN_ISSUE_TITLE,
                IssueSQL.COLUMN_ISSUE_DESCRIPTION,
                IssueSQL.COLUMN_ISSUE_PRIORITY_ID,
                IssueSQL.COLUMN_ISSUE_VEHICLE_ID,
                IssueSQL.COLUMN_ISSUE_STATUS_ID
        };

        String filter = IssueSQL._ID + " = ?";
        String[] filterArgs = {String.valueOf(id)};

        try {
            Cursor cursor = db.query(IssueSQL.TABLE_NAME_ISSUE, issueColumns, filter,
                    filterArgs, null, null, null);

            int idPosition = cursor.getColumnIndex(IssueSQL._ID);
            int titlePosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_TITLE);
            int descriptionPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_DESCRIPTION);
            int priorityIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_PRIORITY_ID);
            int vehicleIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_VEHICLE_ID);
            int statusIdPosition = cursor.getColumnIndex(IssueSQL.COLUMN_ISSUE_STATUS_ID);

            cursor.moveToFirst();
            issue = new Issue(
                    cursor.getInt(idPosition),
                    cursor.getString(titlePosition),
                    cursor.getString(descriptionPosition),
                    cursor.getInt(priorityIdPosition),
                    cursor.getInt(vehicleIdPosition),
                    cursor.getInt(statusIdPosition)
            );

            cursor.close();
        } catch (Exception ex) {
            Log.e(TAG, "getIssueByIssueId: " + ex.getMessage());
            VerifyUtil.alertUser(context, "Database Retrieval Failed", "Unable to fetch issue information, please try again");
        }

        db.close();

        return issue;
    }

    public List<IssueStatus> getPossibleIssueStatuses() {
        SQLiteDatabase db = getReadableDatabase();

        String[] values = {
                IssueStatusSQL._ID,
                IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION};

        String orderBy = IssueStatusSQL._ID;
        Cursor cursor = db.query(IssueStatusSQL.TABLE_NAME_ISSUE_STATUS, values, null,
                null, null, null, orderBy);

        int idPosition = cursor.getColumnIndex(IssueStatusSQL._ID);
        int descriptionPosition = cursor.getColumnIndex(IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION);

        List<IssueStatus> possibleStatuses = new ArrayList<>();

        while (cursor.moveToNext()) {
            possibleStatuses.add(new IssueStatus(cursor.getInt(idPosition), cursor.getString(descriptionPosition)));
        }

        if (possibleStatuses.size() < 1) {
            // This gets called if there are no issue statuses in the DB. We'll add our statuses here
            IssueStatus issueStatus = new IssueStatus("Open"); // New issues should use this status
            insertIssueStatus(issueStatus);
            possibleStatuses.add(issueStatus);

            issueStatus = new IssueStatus("Fixed"); // Finished/closed issues should use this one
            insertIssueStatus(issueStatus);
            possibleStatuses.add(issueStatus);
        }

        cursor.close();
        db.close();

        return possibleStatuses;
    }

    public int getOpenIssueStatusId() {
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] values = {
                IssueStatusSQL._ID,
                IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION
        };

        // No need for args as it's hardcoded here. It can't change
        String filter = IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION + " = 'Open'";

        try {
            Cursor cursor = db.query(IssueStatusSQL.TABLE_NAME_ISSUE_STATUS, values, filter,
                    null, null, null, null);

            int idPosition = cursor.getColumnIndex(IssueStatusSQL._ID);
            int id = -1;

            // We ignore the Exception as our error handling code is right below this
            try {
                cursor.moveToFirst();
                id = cursor.getInt(idPosition);
            } catch (Exception ignored) {
            }

            cursor.close();
            db.close();

            if (id == -1) {
                // Check if there are any issue statuses in the DB
                List<IssueStatus> issueStatuses = getPossibleIssueStatuses();
                if (!issueStatuses.isEmpty()) {
                    // Check if it's in the list returned
                    for (int i = 0; i < issueStatuses.size(); i++) {
                        if (issueStatuses.get(i).getDescription().equals("Open")) {
                            return issueStatuses.get(i).getId();
                        }
                    }
                }
            }

            return id;
        } catch (Exception ex) {
            Log.e(TAG, "getOpenIssueStatusId: " + ex.getMessage());
            db.close();
            VerifyUtil.alertUser(context, "Database Retrieval Failed", "Unable to fetch issue status, please try again");
            return -1;
        }
    }

    public int getClosedIssueStatusId() {
        SQLiteDatabase db = getReadableDatabase();

        // Get all of the fields
        String[] values = {
                IssueStatusSQL._ID,
                IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION
        };

        // No need for args as it's hardcoded here. It can't change
        String filter = IssueStatusSQL.COLUMN_ISSUE_STATUS_DESCRIPTION + " = 'Fixed'";

        try {
            Cursor cursor = db.query(IssueStatusSQL.TABLE_NAME_ISSUE_STATUS, values, filter,
                    null, null, null, null);

            int idPosition = cursor.getColumnIndex(IssueStatusSQL._ID);
            int id = -1;

            // We ignore the Exception as our error handling code is right below this
            try {
                cursor.moveToFirst();
                id = cursor.getInt(idPosition);
            } catch (Exception ignored) {
            }

            cursor.close();
            db.close();

            if (id == -1) {
                // Check if there are any issue statuses in the DB
                List<IssueStatus> issueStatuses = getPossibleIssueStatuses();
                if (!issueStatuses.isEmpty()) {
                    // Check if it's in the list returned
                    for (int i = 0; i < issueStatuses.size(); i++) {
                        if (issueStatuses.get(i).getDescription().equals("Fixed")) {
                            return issueStatuses.get(i).getId();
                        }
                    }
                }
            }

            return id;
        } catch (Exception ex) {
            Log.e(TAG, "getClosedIssueStatusId: " + ex.getMessage());
            db.close();
            VerifyUtil.alertUser(context, "Database Retrieval Failed", "Unable to fetch issue status, please try again");
            return -1;
        }
    }

    // Deletions

    public void deleteVehicle(Vehicle vehicle) {
        // Create the selection criteria
        final String selection = VehicleSQL._ID + " =?";
        final String[] selectionArgs = {Integer.toString(vehicle.getId())};

        // Get a writable database connection
        SQLiteDatabase db = getWritableDatabase();

        // Call the delete method.
        db.delete(VehicleSQL.TABLE_NAME_VEHICLE, selection, selectionArgs);
        db.delete(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, MaintenanceLogSQL.COLUMN_MAINTENANCE_LOG_VEHICLE_ID + "=?", selectionArgs);
        db.delete(IssueSQL.TABLE_NAME_ISSUE, IssueSQL.COLUMN_ISSUE_VEHICLE_ID + "=?", selectionArgs);
    }

    public void deleteMaintenanceLog(MaintenanceLog maintenanceLog) {
        // Create the selection criteria
        final String selection = MaintenanceLogSQL._ID + " =?";
        final String[] selectionArgs = {Integer.toString(maintenanceLog.getId())};

        // Get a writable database connection
        SQLiteDatabase db = getWritableDatabase();

        // Call the delete method.
        db.delete(MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG, selection, selectionArgs);
    }

    // Base SQL

    private static final class VehicleSQL implements BaseColumns {
        // Constants for vehicle table and fields
        private static final String TABLE_NAME_VEHICLE = "vehicle";
        private static final String COLUMN_VEHICLE_MAKE = "vehicle_make";
        private static final String COLUMN_VEHICLE_MODEL = "vehicle_model";
        private static final String COLUMN_VEHICLE_YEAR = "vehicle_year";
        private static final String COLUMN_VEHICLE_NICKNAME = "vehicle_nickname";
        private static final String COLUMN_VEHICLE_COLOR = "vehicle_color";
        private static final String COLUMN_VEHICLE_MILEAGE = "vehicle_mileage";
        private static final String COLUMN_VEHICLE_VIN = "vehicle_vin";
        private static final String COLUMN_VEHICLE_LICENSE_PLATE = "vehicle_license_plate";
        private static final String COLUMN_VEHICLE_DATE_PURCHASED = "vehicle_date_purchased";
        private static final String COLUMN_VEHICLE_VALUE = "vehicle_value";

        // Constant to create the vehicle table
        private static final String SQL_CREATE_TABLE_VEHICLE =
                "CREATE TABLE " + TABLE_NAME_VEHICLE + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_VEHICLE_MAKE + " TEXT, " +
                        COLUMN_VEHICLE_MODEL + " TEXT, " +
                        COLUMN_VEHICLE_YEAR + " INTEGER, " +
                        COLUMN_VEHICLE_NICKNAME + " TEXT NOT NULL, " +
                        COLUMN_VEHICLE_COLOR + " TEXT, " +
                        COLUMN_VEHICLE_MILEAGE + " INTEGER, " +
                        COLUMN_VEHICLE_VIN + " INTEGER, " +
                        COLUMN_VEHICLE_LICENSE_PLATE + " TEXT, " +
                        COLUMN_VEHICLE_DATE_PURCHASED + " INTEGER, " +
                        COLUMN_VEHICLE_VALUE + " INTEGER)";

        // Constant to drop the vehicle table
        private static final String SQL_DROP_TABLE_VEHICLE = "DROP TABLE IF EXISTS " + TABLE_NAME_VEHICLE;
    }

    private static final class MaintenanceLogSQL implements BaseColumns {
        // Constants for maintenance log table and fields
        private static final String TABLE_NAME_MAINTENANCE_LOG = "maintenance_log";
        private static final String COLUMN_MAINTENANCE_LOG_TITLE = "maintenance_log_title";
        private static final String COLUMN_MAINTENANCE_LOG_DESCRIPTION = "maintenance_log_description";
        private static final String COLUMN_MAINTENANCE_LOG_DATE = "maintenance_log_date";
        private static final String COLUMN_MAINTENANCE_LOG_COST = "maintenance_log_cost";
        private static final String COLUMN_MAINTENANCE_LOG_TOTAL_TIME = "maintenance_log_total_time";
        private static final String COLUMN_MAINTENANCE_LOG_MILEAGE = "maintenance_log_mileage";
        private static final String COLUMN_MAINTENANCE_LOG_VEHICLE_ID = "maintenance_log_vehicle_id";
        private static final String COLUMN_MAINTENANCE_LOG_SYSTEM_ID = "maintenance_log_system_id";

        // Constant to create the maintenance log table
        private static final String SQL_CREATE_TABLE_MAINTENANCE_LOG =
                "CREATE TABLE " + TABLE_NAME_MAINTENANCE_LOG + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_MAINTENANCE_LOG_TITLE + " TEXT NOT NULL, " +
                        COLUMN_MAINTENANCE_LOG_DESCRIPTION + " TEXT, " +
                        COLUMN_MAINTENANCE_LOG_DATE + " INTEGER, " +
                        COLUMN_MAINTENANCE_LOG_COST + " INTEGER, " +
                        COLUMN_MAINTENANCE_LOG_TOTAL_TIME + " INTEGER, " +
                        COLUMN_MAINTENANCE_LOG_MILEAGE + " INTEGER, " +
                        COLUMN_MAINTENANCE_LOG_VEHICLE_ID + " INTEGER NOT NULL, " +
                        COLUMN_MAINTENANCE_LOG_SYSTEM_ID + " INTEGER, " +
                        "FOREIGN KEY (" + COLUMN_MAINTENANCE_LOG_VEHICLE_ID + ") REFERENCES " +
                        VehicleSQL.TABLE_NAME_VEHICLE + " (" + VehicleSQL._ID + "), " +
                        "FOREIGN KEY (" + COLUMN_MAINTENANCE_LOG_SYSTEM_ID + ") REFERENCES " +
                        SystemSQL.TABLE_NAME_SYSTEM + " (" + SystemSQL._ID + "))";

        // Constant to drop the maintenance log table
        private static final String SQL_DROP_TABLE_MAINTENANCE_LOG = "DROP TABLE IF EXISTS " + TABLE_NAME_MAINTENANCE_LOG;
    }

    private static final class IssueSQL implements BaseColumns {
        // Constants for issue table and fields
        private static final String TABLE_NAME_ISSUE = "issue";
        private static final String COLUMN_ISSUE_TITLE = "issue_title";
        private static final String COLUMN_ISSUE_DESCRIPTION = "issue_description";
        private static final String COLUMN_ISSUE_PRIORITY_ID = "issue_priority_id";
        private static final String COLUMN_ISSUE_VEHICLE_ID = "issue_vehicle_id";
        private static final String COLUMN_ISSUE_STATUS_ID = "issue_status_id";

        // Constant to create the issue table
        private static final String SQL_CREATE_TABLE_ISSUE =
                "CREATE TABLE " + TABLE_NAME_ISSUE + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_ISSUE_TITLE + " TEXT NOT NULL, " +
                        COLUMN_ISSUE_DESCRIPTION + " TEXT, " +
                        COLUMN_ISSUE_PRIORITY_ID + " INTEGER, " +
                        COLUMN_ISSUE_VEHICLE_ID + " INTEGER NOT NULL, " +
                        COLUMN_ISSUE_STATUS_ID + " INTEGER NOT NULL, " +
                        //"FOREIGN KEY (" + COLUMN_ISSUE_PRIORITY_ID + ") REFERENCES " +
                        //IssuePrioritySQL.TABLE_NAME_ISSUE_PRIORITY + " (" + IssuePrioritySQL._ID + "), " +
                        "FOREIGN KEY (" + COLUMN_ISSUE_VEHICLE_ID + ") REFERENCES " +
                        VehicleSQL.TABLE_NAME_VEHICLE + " (" + VehicleSQL._ID + "))";
                        //"FOREIGN KEY (" + COLUMN_ISSUE_STATUS_ID + ") REFERENCES " +
                        //IssueStatusSQL.TABLE_NAME_ISSUE_STATUS + " (" + IssueStatusSQL._ID + "))";

        // Constant to drop the issue table
        private static final String SQL_DROP_TABLE_ISSUE = "DROP TABLE IF EXISTS " + TABLE_NAME_ISSUE;
    }

    private static final class SystemSQL implements BaseColumns {
        // Constants for system table and fields
        private static final String TABLE_NAME_SYSTEM = "system";
        private static final String COLUMN_SYSTEM_DESCRIPTION = "system_description";

        // Constant to create the system table
        private static final String SQL_CREATE_TABLE_SYSTEM =
                "CREATE TABLE " + TABLE_NAME_SYSTEM + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_SYSTEM_DESCRIPTION + " TEXT NOT NULL)";

        // Constant to drop the system table
        private static final String SQL_DROP_TABLE_SYSTEM = "DROP TABLE IF EXISTS " + TABLE_NAME_SYSTEM;
    }

    private static final class IssueStatusSQL implements BaseColumns {
        // Constants for issue status table and fields
        private static final String TABLE_NAME_ISSUE_STATUS = "issue_status";
        private static final String COLUMN_ISSUE_STATUS_DESCRIPTION = "issue_status_description";

        // Constant to create the issue status table
        private static final String SQL_CREATE_TABLE_ISSUE_STATUS =
                "CREATE TABLE " + TABLE_NAME_ISSUE_STATUS + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_ISSUE_STATUS_DESCRIPTION + " TEXT NOT NULL)";

        // Constant to drop the status table
        private static final String SQL_DROP_TABLE_ISSUE_STATUS = "DROP TABLE IF EXISTS " + TABLE_NAME_ISSUE_STATUS;
    }

    private static final class IssueLogSQL implements BaseColumns {
        // Constants for the issue log table and fields
        private static final String TABLE_NAME_ISSUE_LOG = "issue_log";
        private static final String COLUMN_ISSUE_LOG_LOG_ID = "issue_log_log_id";
        private static final String COLUMN_ISSUE_LOG_ISSUE_ID = "issue_log_issue_id";

        // Constant to create the issue log table
        private static final String SQL_CREATE_TABLE_ISSUE_LOG =
                "CREATE TABLE " + TABLE_NAME_ISSUE_LOG + " (" +
                        COLUMN_ISSUE_LOG_LOG_ID + " INTEGER, " +
                        COLUMN_ISSUE_LOG_ISSUE_ID + " INTEGER, " +
                        "PRIMARY KEY (" + COLUMN_ISSUE_LOG_LOG_ID + ", " + COLUMN_ISSUE_LOG_ISSUE_ID + "), " +
                        "FOREIGN KEY (" + COLUMN_ISSUE_LOG_LOG_ID + ") REFERENCES " +
                        MaintenanceLogSQL.TABLE_NAME_MAINTENANCE_LOG + " (" + MaintenanceLogSQL._ID + "), " +
                        "FOREIGN KEY (" + COLUMN_ISSUE_LOG_ISSUE_ID + ") REFERENCES " +
                        IssueSQL.TABLE_NAME_ISSUE + " (" + IssueSQL._ID + "))";

        // Constant to drop the issue log table
        private static final String SQL_DROP_TABLE_ISSUE_LOG = "DROP TABLE IF EXISTS " + TABLE_NAME_ISSUE_LOG;
    }
}
