package com.example.root.openssme;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.example.root.openssme.Fragments.GateListFragment;
import com.example.root.openssme.Fragments.MainFragment;
import com.example.root.openssme.Fragments.MapFragment;
import com.example.root.openssme.Fragments.SettingsFragment;
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
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,

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
    public  boolean mAllPremissionsGranted;
    public LocationRequest mLocationRequest;
    public GoogleConnection mGoogleConnection;
    private final String MAP_FRAGMENT = "mMapFragment";
    private final String SETTINGS_FRAGMENT = "mSettingsFragment";
    private final String GATE_LIST_FRAGMENT = "mGateListFragment";
    private  final String MAIN_FRAGMENT ="MainFragment" ;
    private View mAutocompleteFragment;
    private Fragment fragment;
    private int mCurrentViewId;
    final public static int REQUEST_CODE = 123;





    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG,"onCreate");


        super.onCreate(savedInstanceState);


        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);

        setupLocationRequestBalanced();

        setContentView(R.layout.activity_main);

        Initialize();

        mAutocompleteFragment = findViewById(R.id.place_autocomplete_fragment);

        if (savedInstanceState==null){
            displayView(R.id.main);
            mCurrentViewId = R.id.main;

        }
        else
        {
            mCurrentViewId = savedInstanceState.getInt("mCurrentViewId");
            displayView(mCurrentViewId);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("mCurrentViewId",mCurrentViewId);


        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");

        super.onResume();
    }

    @Override
    protected void onStart() {



        Log.d(TAG,"onStart");

        if (mGoogleConnection.getGoogleApiClient().isConnected())
        {
            GPSResolver();
        }
        else {
            connectClient();
        }


        super.onStart();
    }



    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");

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
                GPSResolver();
               // requestLocationSettings();
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




    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {


        //Checking if the item is in checked state or not, if not make it in checked state
        if(menuItem.isChecked()) menuItem.setChecked(false);
        else menuItem.setChecked(true);
        mCurrentViewId = menuItem.getItemId();
        displayView(mCurrentViewId);
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

    public void displayView(int ViewId) {

        if(ViewId==R.id.logout){
            signOut();
            return;
        }

        String title = getString(R.string.app_name);

        mAutocompleteFragment.setVisibility(View.GONE);

        switch (ViewId) {
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
                    if (fragment.getTag()!=CurrentFragment){
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.frame, fragment, CurrentFragment);
                        ft.commit();
                    }

                    if (CurrentFragment.equals(MAP_FRAGMENT)){
                        mAutocompleteFragment.setVisibility(View.VISIBLE);
                        if (com.example.root.openssme.SocialNetwork.Settings.getInstance().isFirst_run())
                        {
                            onCoachMark();
                            PrefUtils.setSettings(getApplicationContext());
                            com.example.root.openssme.SocialNetwork.Settings.getInstance().setFirst_run(false);

                        }

                    }
                }
            }, 500);

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
                            ListGateComplexPref.getInstance().clear();
                        }
                    });

        }
        if (User.getInstance().source.equals(Constants.FACEBOOK)) {
            PrefUtils.clearCurrentUser(this);
            Log.e(TAG, "USER STATE: " + PrefUtils.getCurrentUser(this));
            LoginManager.getInstance().logOut();
            ListGateComplexPref.getInstance().clear();
        }

        //go to login activity
        Intent intent = new Intent(this,LogInActivity.class);
        startActivity(intent);
        finish();
    }

    public void setupLocationRequestBalanced() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
    }

    public void onCoachMark(){

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        masterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        });
        dialog.show();
    }

    /*
    =================PREMISSIONS=================
     */


    private void GPSResolver() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        // **************************
        builder.setAlwaysShow(true); // this is the key ingredient
        // **************************

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(mGoogleConnection.getGoogleApiClient(), builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result
                        .getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be
                        // fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling
                            // startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have
                        // no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
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






}

