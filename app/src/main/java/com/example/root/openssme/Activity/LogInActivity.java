package com.example.root.openssme.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import com.example.root.openssme.R;
import com.example.root.openssme.Utils.Constants;
import com.example.root.openssme.Utils.PermissionsUtil;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.Common.GoogleConnection;
import com.example.root.openssme.Common.State;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class LogInActivity extends AppCompatActivity implements Observer {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "LogInActivity";
    public static final int STARTUP_DELAY = 300;
    public static final int ANIM_ITEM_DURATION = 1000;
    public static final int ITEM_DELAY = 300;
    List<String> permissions = new ArrayList<>();
    final public static int REQUEST_CODE = 123;
    public GoogleConnection mGoogleConnection;


    private boolean animationStarted = true;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        super.onCreate(savedInstanceState);

        checkPlayServices();

        mGoogleConnection = GoogleConnection.getInstance(this);

        if (PrefUtils.getCurrentUser(this) == null) {
            animationStarted = false;
            setContentView(R.layout.activity_login);
        } else {
            animationStarted = true;
            addPermissionToList();
            requestPermissions();

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            //Confirm the result of which request to return
            case REQUEST_CODE: {
                List<String> unGranted = PermissionsUtil.getInstance(this).checkPermissionsRequest(permissions, grantResults);
                if (unGranted.size() == 0) {
                    //All permissions have been granted
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Iterator<String> iterator = unGranted.iterator();
                    PermissionResolver(iterator.next());
                }

                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectClient();

    }


    private void addPermissionToList() {
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
            promptSettings();
        }
    }
    public void connectClient() {

        // Connect the client.
        if (mGoogleConnection != null && !mGoogleConnection.getGoogleApiClient().isConnected()) {
            mGoogleConnection.connect();
        }
    }



    private void requestPermissions() {
        List<String> unGranted = PermissionsUtil.getInstance(this).checkPermissions(permissions);
        if (unGranted.size() != 0) {
            PermissionsUtil.getInstance(this).requestPermissions(unGranted, REQUEST_CODE);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();

        }
    }

    private void AlertDialog(String message) {
        //user denied without Never ask again, just show rationale explanation
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied");
        builder.setMessage(message);

        builder.setNegativeButton("RE-TRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestPermissions();
            }
        });
        builder.show();
    }

    private void promptSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Denied");
        builder.setMessage("Please fix Permissions manually");
        builder.setPositiveButton("go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(myAppSettings);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        if (!hasFocus || animationStarted) {
            return;
        }

        animate();

        super.onWindowFocusChanged(hasFocus);
    }

    private void animate() {
        ImageView logoImageView = (ImageView) findViewById(R.id.img_logo);
        ViewGroup container = (ViewGroup) findViewById(R.id.container);

        ViewCompat.animate(logoImageView)
                .translationY(-250)
                .setStartDelay(STARTUP_DELAY)
                .setDuration(ANIM_ITEM_DURATION).setInterpolator(
                new DecelerateInterpolator(1.2f)).start();

        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            ViewPropertyAnimatorCompat viewAnimator;

            if (!(v instanceof Button)) {
                viewAnimator = ViewCompat.animate(v)
                        .translationY(50).alpha(1)
                        .setStartDelay((ITEM_DELAY * i) + 500)
                        .setDuration(1000);
            } else {
                viewAnimator = ViewCompat.animate(v)
                        .scaleY(1).scaleX(1)
                        .setStartDelay((ITEM_DELAY * i) + 500)
                        .setDuration(500);
            }

            viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }

        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        // Disconnecting the client invalidates it.
        if (mGoogleConnection != null) {
            mGoogleConnection.disconnect();
            mGoogleConnection.deleteObserver(this);
        }

    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {

            case OPENED:
                Log.d(TAG, "OPENED");
                // We are signed in!
                // Retrieve some profile information to personalize our app for the user.
                break;
            case CLOSED:
                Log.d(TAG, "CLOSED");


                break;
        }
    }
}
