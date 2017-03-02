package com.open.ssme.Activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.open.ssme.Fragments.GateListFragment;
import com.open.ssme.Fragments.MapFragment;
import com.open.ssme.Fragments.SettingsFragment;
import com.open.ssme.Helpers.ScheduleHelper;
import com.open.ssme.Helpers.SocialNetworkHelper;
import com.open.ssme.Objects.Settings;
import com.open.ssme.R;
import com.open.ssme.Receiver.AlarmBroadcastReceiver;
import com.open.ssme.Receiver.BootReceiver;
import com.open.ssme.Service.OpenSSMEService;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Common.State;
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
import com.open.ssme.Utils.PermissionsUtil;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import com.open.ssme.Objects.User;
import com.open.ssme.Utils.Constants;
import com.open.ssme.Utils.PrefUtils;
import com.open.ssme.Common.GoogleConnection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.open.ssme.Utils.Constants.CURRENT_VIEW_ID;
import static com.open.ssme.Utils.Constants.GATE_LIST_FRAGMENT;
import static com.open.ssme.Utils.Constants.MAP_FRAGMENT;
import static com.open.ssme.Utils.Constants.PROVIDERS_CHANGED;
import static com.open.ssme.Utils.Constants.REQUEST_LOCATION;
import static com.open.ssme.Utils.Constants.SETTINGS_FRAGMENT;
import static com.open.ssme.Utils.Constants.SETTINGS_REQ_SMS;
import static com.open.ssme.Utils.Constants.SOCIAL;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        Observer

{
    private final String TAG = MainActivity.class.getSimpleName();
    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private CircleImageView circleImageView;
    private TextView tvName, tvEmail;
    private String CurrentFragment;
    public LocationRequest mLocationRequest;
    public GoogleConnection mGoogleConnection;
    private Fragment fragment;
    private int mCurrentViewId = R.id.gate_list;
    private OpenSSMEService mOpenSSMEService;
    final public static int REQUEST_CODE = 123;
    private List<String> permissions;
    private Handler mFragmentHandler;
    private boolean mLocationSettingsResultInProcess;
    private Runnable mRunnable;


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        outState.putInt(CURRENT_VIEW_ID, mCurrentViewId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        mGoogleConnection = GoogleConnection.getInstance(this);
        mGoogleConnection.addObserver(this);

        if (savedInstanceState != null)
            mCurrentViewId = savedInstanceState.getInt(CURRENT_VIEW_ID);

        Init();

        AskForPermissions();

    }


    private void AskForPermissions() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                addPermissionToList();
                requestPermissions();
            }
        });
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        connectClient();
        ScreenSetup();
        StartOpenSSMEService();
        SetUpDisplayView();
        if (!mLocationSettingsResultInProcess)
            GPSResolver();

        super.onResume();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null && mOpenSSMEService != null && !isMyServiceRunning(mOpenSSMEService.getClass())) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }
        //unregisterReceiver(receiver);

        super.onDestroy();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        mFragmentHandler.removeCallbacks(mRunnable);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_share:
                postStatusUpdate();
                break;
            case R.id.main_logout:
                AskLogOut();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    public void connectClient() {
        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected())
            mGoogleConnection.connect();
    }


    private void SetUpDisplayView() {

        if (ListGateComplexPref.getInstance().gates.size() > 0 || mCurrentViewId == R.id.settings)
            displayView(mCurrentViewId);
        else
            displayView(mCurrentViewId = R.id.menu_map);
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "Connected to Google Api Client");
                break;
            case CLOSED:
                Log.d(TAG, "Disconnected from Google Api Client");
                //CloseApplication();
                break;
        }
    }


    private void Init() {

        mLocationSettingsResultInProcess = false;

        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (fragment.getTag() != CurrentFragment) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.frame, fragment, CurrentFragment);
                    ft.commit();
                }

                if (CurrentFragment.equals(MAP_FRAGMENT)) {
                    if (ListGateComplexPref.getInstance().gates.size() == 0 && !mLocationSettingsResultInProcess && Settings.getInstance().isFirst_run()) {
                        onCoachMark();
                    }
                }
            }
        };

        mFragmentHandler = new Handler();

        setupLocationRequestBalanced();

        setContentView(R.layout.activity_main);


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
        tvName = (TextView) headerLayout.findViewById(R.id.username);

        //Initializing the profile email
        tvEmail = (TextView) headerLayout.findViewById(R.id.email);

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
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer) {

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

        //RegisterReciver();


    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.shut_down))
                .setMessage(getResources().getString(R.string.do_shut_down))
                .setPositiveButton(getResources().getString(R.string.shut_down), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ExitApplication();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .show();
    }

    private void CloseApplication() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.error))
                .setMessage(getResources().getString(R.string.error_info))
                .setPositiveButton(getResources().getString(R.string.shut_down), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ExitApplication();
                    }

                })
                .show();
    }

    private void ExitApplication() {
        stopService(new Intent(getBaseContext(), OpenSSMEService.class));
        ExitActivity.exitApplication(getApplicationContext());
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        //Checking if the item is in checked state or not, if not make it in checked state
        if (menuItem.isChecked())
            menuItem.setChecked(false);
        else
            menuItem.setChecked(true);

        mCurrentViewId = menuItem.getItemId();
        Log.d(TAG, "displayView");
        displayView(mCurrentViewId);
        return true;
    }

    public void displayView(int ViewId) {


        String title = getString(R.string.app_name);

        switch (ViewId) {
            case R.id.gate_list:
                fragment = getSupportFragmentManager().findFragmentByTag(GATE_LIST_FRAGMENT);
                if (fragment == null) {
                    fragment = new GateListFragment();

                }
                title = getResources().getString(R.string.my_gates);
                CurrentFragment = GATE_LIST_FRAGMENT;


                break;
            case R.id.menu_map:
                fragment = getSupportFragmentManager().findFragmentByTag(MAP_FRAGMENT);
                if (fragment == null) {
                    fragment = new MapFragment();
                }
                title = getResources().getString(R.string.add_gate);
                CurrentFragment = MAP_FRAGMENT;
                break;

            case R.id.settings:
                fragment = getSupportFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT);
                if (fragment == null) {
                    fragment = new SettingsFragment();
                }
                title = getResources().getString(R.string.settings);
                CurrentFragment = SETTINGS_FRAGMENT;
                break;

        }

        if (fragment != null) {

            mFragmentHandler.postDelayed(mRunnable, 500);
        }

        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (drawerLayout != null)
            drawerLayout.closeDrawer(GravityCompat.START);


    }

    private void AskLogOut() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.log_out))
                .setMessage(getResources().getString(R.string.do_log_out))
                .setPositiveButton(getResources().getString(R.string.log_out), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signOut();

                    }

                })
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .show();

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
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    public void setupLocationRequestBalanced() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);
    }

    public void onCoachMark() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        masterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

            }
        });
        dialog.show();
        if (Settings.getInstance().isFirst_run())
            PrefUtils.setFirstRun(getApplicationContext());
    }

    public void StartOpenSSMEService() {
        if (ListGateComplexPref.getInstance().gates != null && ListGateComplexPref.getInstance().gates.size() > 0
                && !Settings.getInstance().isSchedule()) {

            mOpenSSMEService = new OpenSSMEService();
            Intent ServiceIntent = new Intent(this, mOpenSSMEService.getClass());
            if (!isMyServiceRunning(mOpenSSMEService.getClass())) {
                startService(ServiceIntent);
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG, "is OpenSSME Service Running: " + true);
                return true;
            }
        }
        Log.d(TAG, "is OpenSSME Service Running: " + false);
        return false;
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
                        mLocationSettingsResultInProcess = false;

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be
                        // fixed by showing the user
                        // a dialog.

                        try {
                            // Show the dialog by calling
                            // startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have
                        // no way to fix the
                        // settings so we won't show the dialog.
                        mLocationSettingsResultInProcess = false;

                        break;
                }
            }
        });
    }

    private void addPermissionToList() {
        if (permissions == null) {
            permissions = new ArrayList<>();
            permissions.add(Manifest.permission.CALL_PHONE);
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissions.add(Manifest.permission.READ_CONTACTS);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        }
    }

    private void PermissionResolver(String Permission) {
        boolean messege = false;
        String AlertMessege = "";
        switch (Permission) {

            case Constants.CALL_PHONE:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);
                AlertMessege = getResources().getString(R.string.request_call);
                break;
            case Constants.ACCESS_FINE_LOCATION:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                AlertMessege = getResources().getString(R.string.request_location);
                break;
            case Constants.ACCESS_COARSE_LOCATION:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                AlertMessege = getResources().getString(R.string.request_location);
                break;
            case Constants.READ_CONTACTS:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS);
                AlertMessege = getResources().getString(R.string.request_contacts);
                break;
            case Constants.READ_EXTERNAL_STORAGE:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                AlertMessege = getResources().getString(R.string.request_read_write);
                break;
            case Constants.WRITE_EXTERNAL_STORAGE:
                messege = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                AlertMessege = getResources().getString(R.string.request_read_write);
                break;
        }
        if (messege) {
            AlertDialog(AlertMessege);
        } else {
            //user has denied with `Never Ask Again`, go to settings
            promptAppSettings();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case Constants.REQ_SELECT_PHOTO:

                if (User.getInstance().source.equals(Constants.GPLUS)) {
                    SocialNetworkHelper.getInstance(this).GooglePostPhoto(this, data);
                }
                if (User.getInstance().source.equals(Constants.FACEBOOK)) {
                    SocialNetworkHelper.getInstance(this).FacebookPostPhoto(this, data);
                }
                break;
            case REQUEST_LOCATION:
                if (resultCode == 0) {
                    mLocationSettingsResultInProcess = true;
                    promptLocationSettings();
                } else {
                    mLocationSettingsResultInProcess = false;

                }
                break;

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        List<String> unGranted = PermissionsUtil.getInstance(this).checkPermissionsRequest(permissions, grantResults);
        switch (requestCode) {
            //Confirm the result of which request to return
            case REQUEST_CODE:
                if (unGranted.size() == 0) {
                    //All permissions have been granted
                } else {
                    Iterator<String> iterator = unGranted.iterator();
                    PermissionResolver(iterator.next());
                }
                break;
            case SETTINGS_REQ_SMS:
                if (unGranted.size() != 0)
                    SettingsFragment.mSettingsChangedListener.SMSChanged();
                else
                    PrefUtils.getSettings(this);
                break;

        }
    }

    private void requestPermissions() {
        List<String> unGranted = PermissionsUtil.getInstance(this).checkPermissions(permissions);
        if (unGranted.size() != 0)
            PermissionsUtil.getInstance(this).requestPermissions(unGranted, REQUEST_CODE);
        else {
            Log.d(TAG, "SetUpDisplayView requestPermissions");


        }
    }


    private void AlertDialog(String message) {
        //user denied without Never ask again, just show rationale explanation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.permission_denied));
        builder.setMessage(message);

        builder.setNegativeButton(getResources().getString(R.string.re_try), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestPermissions();
            }
        });
        builder.show();
    }

    private void promptAppSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.permission_denied));
        builder.setMessage(getResources().getString(R.string.please_fix));
        builder.setPositiveButton(getResources().getString(R.string.go_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToAppSettings();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    private void promptLocationSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.location_denied));
        builder.setMessage(getResources().getString(R.string.location_fix));
        builder.setPositiveButton(getResources().getString(R.string.go_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goLocationSettings();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    private void goToAppSettings() {
        Intent AppSettings = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
        AppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        AppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //myAppSettings.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(AppSettings);
        finish();
    }

    private void goLocationSettings() {
        Intent LocationSettings = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        LocationSettings.addCategory(Intent.CATEGORY_DEFAULT);
        LocationSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //myAppSettings.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(LocationSettings);
        finish();
    }

    private void postStatusUpdate() {
        if (User.getInstance().source.equals(Constants.GPLUS)) {
            SocialNetworkHelper.getInstance(this).PostOnGoogle(this,
                    getResources().getString(R.string.subject),
                    getResources().getString(R.string.body),
                    getResources().getString(R.string.url));
        }
        if (User.getInstance().source.equals(Constants.FACEBOOK)) {
            SocialNetworkHelper.getInstance(this).PostOnFacebook(this,
                    getResources().getString(R.string.subject),
                    getResources().getString(R.string.body),
                    getResources().getString(R.string.url));
        }
    }

    private void ScreenSetup() {
        if (Settings.getInstance().isScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }




}

