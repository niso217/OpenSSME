package com.example.root.openssme;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.common.State;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ResultCallback<LocationSettingsResult>,
        Observer

{
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private CircleImageView circleImageView;
    private TextView tvName,tvEmail;
    private boolean viewIsAtHome;
    private  String CurrentFragment;
    public  boolean mCallPremissionGranted;
    public  boolean mLocationPremissionGranted;
    public  boolean mContactPremissionGranted;
    public  boolean mStoragePremissionGranted;
    public LocationRequest mLocationRequest;
    public GoogleConnection mGoogleConnection;
    private final String MAP_FRAGMENT = "mMapFragment";
    private final String SETTINGS_FRAGMENT = "mSettingsFragment";
    private final String GATE_LIST_FRAGMENT = "mGateListFragment";
    private  final String MAIN_FRAGMENT ="MainFragment" ;
    private View mAutocompleteFragment;
    private Fragment fragment;





    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);

        setupLocationRequestBalanced();

        setContentView(R.layout.activity_main);

        Initialize();

        mAutocompleteFragment = findViewById(R.id.place_autocomplete_fragment);

        mAutocompleteFragment.setVisibility(View.GONE);

        if (savedInstanceState==null){
            displayView(R.id.main);

        }

    }

    private boolean CheckPremissions() {
        mCallPremissionGranted =  ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);
        mLocationPremissionGranted = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        mContactPremissionGranted =ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS);
        mStoragePremissionGranted = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (mCallPremissionGranted || mLocationPremissionGranted || mContactPremissionGranted || mStoragePremissionGranted) {
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        connectClient();
        super.onStart();
    }



    @Override
    public void onDestroy() {
        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }

        super.onDestroy();
    }


    public void connectClient() {

        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                // We are signed in!
                requestLocationSettings();
                Log.d(TAG, "Connected to Google Api Client");
                break;
            case CLOSED:
                Log.d(TAG, "Disconnected from Google Api Client");
                break;
        }
    }

    private void Initialize(){

        // Initializing Toolbar and setting it as the actionbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initializing NavigationView
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        //Initializing the header.xml data
        View headerLayout = navigationView.getHeaderView(0);

        //Initializing the profile image
        circleImageView = (CircleImageView) headerLayout.findViewById(R.id.profile_image);

        //Initializing the profile name
        tvName = (TextView) headerLayout.findViewById(R.id.username) ;

        //Initializing the profile email
        tvEmail = (TextView) headerLayout.findViewById(R.id.email) ;

        Picasso.with(this)
                .load(User.getInstance().image)
                .resize(400, 400)
                .centerCrop()
                .into(circleImageView);

        tvName.setText(User.getInstance().name);
        tvEmail.setText(User.getInstance().email);

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(this);

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.openDrawer, R.string.closeDrawer){

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };


        //Setting the actionbarToggle to drawer layout
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    private void GetAllPremissionNeeded(){
        //request phone call premission M+
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            requestCallPermission();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            requestReadContactsPermission();
        }
        //request location call premission M+
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        }

        //request storage  premission M+
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }

        requestLocationSettings();
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {


        //Checking if the item is in checked state or not, if not make it in checked state
        if(menuItem.isChecked()) menuItem.setChecked(false);
        else menuItem.setChecked(true);

        displayView(menuItem.getItemId());
        return true;
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Shut Down")
                .setMessage("Do you want to shut down OpenSSME?")
                .setPositiveButton("Shut Down", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopService(new Intent(getBaseContext(), LocationService.class));
                        finish();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void displayView(int viewId) {

        if(viewId==R.id.logout){
            signOut();
            return;
        }
        if (!CheckPremissions()) {
            GetAllPremissionNeeded();
            Toast.makeText(this,"Fix Premissions!",Toast.LENGTH_LONG).show();
            return;
        }

        String title = getString(R.string.app_name);

        mAutocompleteFragment.setVisibility(View.GONE);


        switch (viewId) {
            case R.id.main:
                    fragment = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT);
                    if (fragment == null) {
                        fragment = new MainFragment();
                    }
                    title = "Main";
                    viewIsAtHome = false;
                    CurrentFragment = MAIN_FRAGMENT;

                break;
            case R.id.gate_list:
                fragment = getSupportFragmentManager().findFragmentByTag(GATE_LIST_FRAGMENT);
                if (fragment==null){
                    fragment = new GateListFragment();
                }
                title  = "My Gates";
                viewIsAtHome = false;
                CurrentFragment = GATE_LIST_FRAGMENT;


                break;
            case R.id.map:
                fragment = getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT);
                if (fragment==null){
                    fragment = new MapFragment();
                }
                title = "Map";
                viewIsAtHome = true;
                CurrentFragment = MAP_FRAGMENT;
                break;

            case R.id.settings:
                fragment = getSupportFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT);
                if (fragment==null){
                    fragment = new SettingsFragment();
                }
                title = "Settings";
                viewIsAtHome = true;
                CurrentFragment = SETTINGS_FRAGMENT;
                break;

            case  R.id.share:
                postStatusUpdate();
                break;

        }

        if (fragment!=null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.frame, fragment, CurrentFragment);
                    ft.commit();
                    if (CurrentFragment.equals(MAP_FRAGMENT)){
                        mAutocompleteFragment.setVisibility(View.VISIBLE);
                    }
                }
            }, 300);

        }


        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (drawerLayout!=null)
            drawerLayout.closeDrawer(GravityCompat.START);



    }

    // [START signOut]
    private void signOut() {
        if (User.getInstance().source.equals(Constants.GPLUS)) {

            Auth.GoogleSignInApi.signOut(GoogleConnection.getInstance(this).getGoogleApiClient()).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(com.google.android.gms.common.api.Status status) {
                            PrefUtils.clearCurrentUser(getApplicationContext());
                            Log.e(TAG, "USER STATE:" + PrefUtils.getCurrentUser(getApplicationContext()));
                        }
                    });

        }
        if (User.getInstance().source.equals(Constants.FACEBOOK)) {
            PrefUtils.clearCurrentUser(this);
            Log.e(TAG, "USER STATE: " + PrefUtils.getCurrentUser(this));
            LoginManager.getInstance().logOut();
        }

        //go to login activity
        Intent intent = new Intent(this,LogInActivity.class);
        startActivity(intent);
        finish();
    }

    public void setupLocationRequestBalanced() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
    }

    /*
    =================PREMISSIONS=================
     */

    private void requestLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .setAlwaysShow(true)
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleConnection.getGoogleApiClient(), builder.build());
        result.setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {

        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can initialize location
                Log.d(TAG, "All location settings are satisfied. The client can initialize location requests here");
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                // Location settings are not satisfied. But could be fixed by showing the user
                // a dialog.
                ChangeLocationSettings(status);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e(TAG, "Settings change unavailable. We have no way to fix the settings so we won't show the dialog.");
                break;
        }

    }

    private void requestLocationPermission() {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.PREMISSIONS);
        }

    private void requestCallPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, Constants.PERMISSIONS_REQUEST_CALL_PHONE);
    }

    private void requestReadContactsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, Constants.PERMISSIONS_REQUEST_CALL_PHONE);
    }


    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_STORAGE);

    }


    private void ChangeLocationSettings(Status status) {
        // Location settings are not satisfied. But could be fixed by showing the user
        // a dialog.
        try {
            // Show the dialog by calling startResolutionForResult(),
            // and check the result in onActivityResult().
            status.startResolutionForResult(
                    this,
                    Constants.REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            // Ignore the error.
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case Constants.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
            case Constants.REQ_SELECT_PHOTO:

                    if (User.getInstance().source.equals(Constants.GPLUS)) {
                        MyApplication.getSocialNetworkHelper().GooglePostPhoto(this, data);
                    }
                    if (User.getInstance().source.equals(Constants.FACEBOOK)) {
                        MyApplication.getSocialNetworkHelper().FacebookPostPhoto(this, data);
                    }

        }
    }

    private void postStatusUpdate() {
        if (User.getInstance().source.equals(Constants.GPLUS)) {
            MyApplication.getSocialNetworkHelper().PostOnGoogle(this,
                    getResources().getString(R.string.subject),
                    getResources().getString(R.string.body),
                    getResources().getString(R.string.url));
        }
        if (User.getInstance().source.equals(Constants.FACEBOOK)) {
            MyApplication.getSocialNetworkHelper().PostOnFacebook(this,
                    getResources().getString(R.string.subject),
                    getResources().getString(R.string.body),
                    getResources().getString(R.string.url));
        }
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {


        if(requestCode == Constants.PERMISSIONS_REQUEST_CALL_PHONE){
            if(grantResults.length > 0){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //user accepted , make call
                    Log.d(TAG,"Permission granted");
                }
                else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean should = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);
                    if(should){
                        AlertDialog(getResources().getString(R.string.request_call));
                    }else{
                        //user has denied with `Never Ask Again`, go to settings
                        promptSettings();
                    }
                }
            }
        }
        if (requestCode == Constants.PREMISSIONS) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to

            } else {
                boolean fine = ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION);
                boolean coarse = ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION);

                if(fine || coarse){
                    AlertDialog(getResources().getString(R.string.request_location));
                }else{
                    //user has denied with `Never Ask Again`, go to settings
                    promptSettings();
                }
                // Permission was denied or request was cancelled
            }
        }


        if (requestCode == Constants.PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to

            } else {
                boolean write = ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
                boolean read = ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE);

                if(write || read){
                    AlertDialog(getResources().getString(R.string.request_location));
                }else{
                    //user has denied with `Never Ask Again`, go to settings
                    promptSettings();
                }
                // Permission was denied or request was cancelled
            }
        }
    }

    private void AlertDialog(String message){
        //user denied without Never ask again, just show rationale explanation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied");
        builder.setMessage(message);
        builder.setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("RE-TRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestCallPermission();
            }
        });
        builder.show();
    }

    private void promptSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Denied Never Ask");
        builder.setMessage("Denied Never Ask Msg");
        builder.setPositiveButton("go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToSettings();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(myAppSettings);
    }


}

