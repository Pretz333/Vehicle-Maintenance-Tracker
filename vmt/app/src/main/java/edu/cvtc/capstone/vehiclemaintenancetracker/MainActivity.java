package edu.cvtc.capstone.vehiclemaintenancetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity{
    public final static String TAG = "MAINACTIVITY_CLASS";

    // An array containing each vehicle object.
    // This is used by the RecyclerView.
    private ArrayList<Vehicle> vehicleArrayList;

    // A custom adapter used by the RecyclerView.
    // Whenever removing, modifying, or updating data
    // within the vehicleArrayList, make sure to let
    // this adapter know! No one likes being left out.
    // not even adapters.
    private VehicleRecyclerAdapter vehicleRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        populateRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();

        populateRecyclerView();
    }

    private void populateRecyclerView() {
        //Get all saved vehicles
        DBHelper dbHelper = new DBHelper(MainActivity.this);
        vehicleArrayList = dbHelper.getAllVehicles();

        // Fill the RecyclerView and its Adapter with some data.
        prepRecyclerView();

        if (vehicleArrayList.isEmpty()) {
            //Display the "You have no vehicles" message
            findViewById(R.id.noVehiclesTextView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.noVehiclesTextView).setVisibility(View.INVISIBLE);
        }
    }

    // Initializes the RecyclerView and its Adapter using
    // the data within the vehicleArrayList DataSet.
    private void prepRecyclerView() {

        // Find the RecyclerView view
        RecyclerView recyclerView = findViewById(R.id.recyclerView_mainActivity);

        // Create a LayoutManager for the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create the adapter and make the RecyclerView use it
        vehicleRecyclerAdapter = new VehicleRecyclerAdapter(vehicleArrayList);
        recyclerView.setAdapter(vehicleRecyclerAdapter);

    }

    //Add in night mode toggle
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vehicle_menu, menu);
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            menu.findItem(R.id.night_mode).setTitle(R.string.day_mode);
        } else {
            menu.findItem(R.id.night_mode).setTitle(R.string.night_mode);
        }
        return true;
    }

    //Has the night mode/day mode toggle and will have the add a vehicle button
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.night_mode) {
            int nightMode = AppCompatDelegate.getDefaultNightMode();
            if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            recreate();
        } else if (item.getItemId() == R.id.addAVehicle){
            // Go to the vehicle settings activity and send an id
            // of -1 so it knows we're creating a new vehicle
            Intent intent = new Intent(MainActivity.this, VehicleSettingsActivity.class);
            intent.putExtra(VehicleOptionActivity.EXTRA_VEHICLE_ID, -1);
            startActivity(intent);
        }
        return true;
    }

}


/*
    A RecyclerView adapter for each vehicle displayed. Although we could use a more simple
    approach such as ArrayAdapter using a ListView, the layout for displaying the vehicles
    is costly and contains lots of children. So for efficiency, we will use RecyclerView.
 */
class VehicleRecyclerAdapter extends RecyclerView.Adapter<VehicleRecyclerAdapter.ViewHolder> {

    // The DataSet that contains the vehicle objects
    public ArrayList<Vehicle> vehicleDataSet;

    // Default constructor to receive vehicle data
    public VehicleRecyclerAdapter(ArrayList<Vehicle> vehicleDataSet){
        this.vehicleDataSet = vehicleDataSet;
    }

    // Provide a reference to the type of views that the
    // RecyclerView adapter will interact with.
    // Heads up, it's quite a bit!
    // NOTE: ViewHolder is a child class, not a function!
    // hopefully that saves some confusion.
    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        // Context of super class
        Context context;

        // A custom preference util used to set/get
        // a key-value pair. In this case, the amount
        // of issues a given vehicle has.
        private PreferenceUtil preferenceUtil;

        // VehicleID
        private int vehicleID;

        // TextView members of the card_vehicle.xml layout file.
        private final TextView nickname;
        private final TextView makeAndModel;
        private final TextView colorAndYear;
        private final TextView plateNumber;

        private final TextView issueDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Context used mainly for OnClickListener
            context = itemView.getContext();

            // TODO: Maybe this shouldn't be re-created multiple times.
            //  Just sayin' - Alex
            // Initialize the preference util
            preferenceUtil = new PreferenceUtil(context);

            // Find the views within the itemView (cardview)
            // layout and set them to the member variables above.
            nickname = itemView.findViewById(R.id.card_mainActivity_nickname);
            makeAndModel = itemView.findViewById(R.id.card_mainActivity_vehicleMakeAndModel);
            colorAndYear = itemView.findViewById(R.id.card_mainActivity_vehicleColorAndYear);
            plateNumber = itemView.findViewById(R.id.card_mainActivity_vehiclePlateNumber);
            issueDescription = itemView.findViewById(R.id.card_mainActivity_issueDescription);

            Button dummyButton = itemView.findViewById(R.id.card_mainActivity_dummyButton);

            // Onclick Listener for card, err, button
            dummyButton.setOnClickListener(this);
        }

        // Click listener for the card
        @Override
        public void onClick(View v) {
            // Create the target intent
            Intent intent = new Intent(context, VehicleOptionActivity.class);

            // Pass the vehicle ID
            intent.putExtra(VehicleOptionActivity.EXTRA_VEHICLE_ID, vehicleID);

            // Start the activity
            context.startActivity(intent);
        }

        // One-hitter method for setting the values for all views
        // which is called by the parent class's onBindViewHolder method.
        public void setData(int vehicleID,
                            String nickname,
                            String makeAndModel,
                            String colorAndYear,
                            String plateNumber) {

            this.vehicleID = vehicleID;
            this.nickname.setText(nickname);
            this.makeAndModel.setText(makeAndModel);
            this.colorAndYear.setText(colorAndYear);
            this.plateNumber.setText(plateNumber);


            // Default issue string
            String issueString = context.getResources().getString(R.string.card_mainActivity_recyclerView_noIssues);

            // Get the amount of issues this specific vehicle has
            int issueCount = preferenceUtil.getIssueCountByVehicleId(vehicleID);

            // If this vehicle has any issues, set it. If not, display the default issue string.
            if(issueCount == 1) {
                this.issueDescription.setText(String.format("%s %s", issueCount, context.getResources().getString(R.string.card_mainActivity_recyclerView_withIssue)));
            } else if (issueCount > 0) {
                this.issueDescription.setText(String.format("%s %s", issueCount, context.getResources().getString(R.string.card_mainActivity_recyclerView_withIssues)));
            } else {
                this.issueDescription.setText(issueString);
            }
        }

    }

    // Inflate the view layout and attaching a ViewHolder to it as well
    @NonNull
    @Override
    public VehicleRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_vehicle, parent, false);
        return new ViewHolder(view);
    }

    // Set each view with it's correct data
    @Override
    public void onBindViewHolder(@NonNull VehicleRecyclerAdapter.ViewHolder holder, int position) {
        // Grab the vehicle at the specified position
        Vehicle v = vehicleDataSet.get(position);

        // Set the view holder data based on the vehicle above
        // Note: Make & Model, along with Color & Year are a SINGLE
        // TextView view if you checkout card_vehicle.xml.
        // This was done to save stress on the RecyclerView when
        // populating, and, because we already have a heck of a lot
        // of views for the vehicle card.
        holder.setData(v.getId(),
                v.getName(),
                v.getMake() + " " + v.getModel(),
                v.getColor() + ", " + v.getYear(),
                v.getLicensePlate());
    }

    @Override
    public int getItemCount() {
        return vehicleDataSet.size();
    }
}