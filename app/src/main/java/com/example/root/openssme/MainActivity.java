package com.example.root.openssme;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener

{
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private CircleImageView circleImageView;
    private TextView tvName,tvEmail;
    private MapFragment mMapFragment;
    private GateListFragment mGateListFragment;
    private boolean viewIsAtHome;
    private  String CurrentFragment;
    private final String MAP_FRAGMENT = "mMapFragment";
    private final String GATE_LIST_FRAGMENT = "mGateListFragment";


    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //request phone call premission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }

        if (savedInstanceState != null) {
            // Let's first dynamically add a fragment into a frame container


        }
        else
        {
            displayView(R.id.map);

        }


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

    public void ReplaceFragment(Fragment fragment, String tag){
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame, fragment, tag)
                .commit();
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



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId())
        {

            case R.id.action_settings:
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivity(i);
                break;

//            case R.id.map_type_normal:
//                M.map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
//                break;
        }


        return super.onOptionsItemSelected(item);
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

                        finish();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void displayView(int viewId) {

        Fragment fragment = null;
        String title = getString(R.string.app_name);

        switch (viewId) {
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

            case R.id.logout:
                signOut();
                return;
        }



            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.frame, fragment,CurrentFragment);
            ft.commit();


        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (drawerLayout!=null)
        drawerLayout.closeDrawer(GravityCompat.START);

    }



    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, Constants.PERMISSIONS_REQUEST_CALL_PHONE);
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
                        //user denied without Never ask again, just show rationale explanation
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Permission Denied");
                        builder.setMessage("Without this permission the app is unable to make call.Are you sure you want to deny this permission?");
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
                                requestPermission();
                            }
                        });
                        builder.show();

                    }else{
                        //user has denied with `Never Ask Again`, go to settings
                        promptSettings();
                    }
                }
            }
        }
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

