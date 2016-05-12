package com.example.root.openssme;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.UserDictionary;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.root.openssme.SocialNetwork.Gate;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.Utils.PictUtil;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.provider.ContactsContract.CommonDataKinds.Phone;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import com.example.root.openssme.Adapter.CustomWindowAdapter;
import com.example.root.openssme.Utils.Constants;


public class MapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        ServiceConnection, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        ResultCallback<LocationSettingsResult>

{


    private static final String TAG = "MapFragment";
    public MapView mapView;
    private GoogleMap map;
    private Float zoom, tilt, bearing;
    private LatLng latlng;
    private ArrayList<MarkerOptions> mMarkers;
    private LocationService mLocationService;
    private Boolean isLocationUpdatesEnable = false;
    private boolean mServiceBound = false;
    private LatLng mOnClickLatLang;
    private LatLng mCurrentMarker;
    private Circle mCircle;
    private Bundle msavedInstanceState;


    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getFlags()) {


                case Constants.LOCATION_UPDATE_FLAG:
                    //recived location update
                    if (intent.hasExtra(Constants.LOCATION)) {
                        ZoomToCurrentLocation(intent);

                    }

                    break;
            }
        }
    };


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        if (savedInstanceState != null) {
            msavedInstanceState = savedInstanceState;
            isLocationUpdatesEnable = savedInstanceState.getBoolean("savedInstanceState");
            zoom = savedInstanceState.getFloat("zoom");
            latlng = new LatLng(savedInstanceState.getDouble("lat"), savedInstanceState.getDouble("lon"));
            tilt = savedInstanceState.getFloat("tilt");
            bearing = savedInstanceState.getFloat("bearing");
            mMarkers = savedInstanceState.getParcelableArrayList("markers");
            mCurrentMarker = savedInstanceState.getParcelable("currentmarker");


        } else {
            //get masseges from LocationService
            msavedInstanceState = null;
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(Constants.LOCATION_SERVICE));
        }

    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putBoolean("isLocationUpdatesEnable", isLocationUpdatesEnable);
            outState.putDouble("lat", map.getCameraPosition().target.latitude);
            outState.putDouble("lon", map.getCameraPosition().target.longitude);
            outState.putFloat("zoom", map.getCameraPosition().zoom);
            outState.putFloat("tilt", map.getCameraPosition().tilt);
            outState.putFloat("bearing", map.getCameraPosition().bearing);
            outState.putParcelable("currentmarker", mCurrentMarker);
            outState.putParcelableArrayList("markers", mMarkers);
        }
        super.onSaveInstanceState(outState);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater(getArguments())));
        map.setOnInfoWindowClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        SetUpCircle();


        getMyLocation();

        //restoring the map state
        if (latlng != null && zoom != null && tilt != null && bearing != null) {
            CameraPosition position = new CameraPosition(latlng, zoom, tilt, bearing);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            map.moveCamera(update);


        }
        restoreMarkerArrayToMap();

        for (int i = 0; i < mMarkers.size(); i++) {
            Marker temp = map.addMarker(mMarkers.get(i));
            if (mCurrentMarker != null && mLocationService.distance(mMarkers.get(i).getPosition().latitude, mMarkers.get(i).getPosition().longitude,
                    mCurrentMarker.latitude, mCurrentMarker.longitude) < 0.0001) {
                temp.showInfoWindow();
            }

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_map, container, false);
        try {
            MapsInitializer.initialize(this.getActivity());
            mapView = (MapView) rootView.findViewById(R.id.map);
            mapView.onCreate(savedInstanceState);

            if (mapView != null) {
                mapView.getMapAsync(this);


            } else {
                Toast.makeText(getContext(), "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
            }
        } catch (InflateException e) {
            //Log.e(TAG, "Inflate exception");
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //must have at least 1 gate to start the serive
        if (ListGateComplexPref.getInstance().gates.size() > 0)
        bindService();

    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mServiceBound) {
            unBindService();
            mServiceBound = false;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    void getMyLocation() {
        if (map != null) {
            // Now that map has loaded, let's get our location!
            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;

            } else {
                // permission has been granted, continue as usual
                map.setMyLocationEnabled(true);
                map.setOnMapLongClickListener(this);


            }
        }
    }




    public void ZoomToCurrentLocation(Intent intent){
        Bundle b = intent.getExtras();
        Location loc = (Location)b.get(LocationManager.KEY_LOCATION_CHANGED);
        LatLng latLng = new LatLng(loc.getLatitude(),loc.getLongitude());
        Double distance = intent.getDoubleExtra(Constants.DISTANCE, 0);
            if (map!=null && !isLocationUpdatesEnable) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                isLocationUpdatesEnable = true;
            }


        if(distance < Constants.DISTANCE_TO_OPEN){
            isLocationUpdatesEnable = false;
        }


        Log.d(TAG, "Location Update" + latLng.latitude + ","+ latLng.longitude);
        Log.d(TAG, "Distance" + intent.getDoubleExtra(Constants.DISTANCE,0));


    }


    private void askForPremissions() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.PREMISSIONS);
        }
    }

    private void SetUpCircle(){

        if (ListGateComplexPref.getInstance().gates.size()>0) {
            if (mCircle == null) {
                mCircle = map.addCircle(new CircleOptions()
                        .center(ListGateComplexPref.getInstance().gates.get(0).location)
                        .radius(Constants.DISTANCE_TO_OPEN)
                        .strokeColor(Color.RED));
            } else {
                mCircle.setRadius(Constants.DISTANCE_TO_OPEN);

            }
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == Constants.PREMISSIONS) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                    requestSettings();

            } else {
                // Permission was denied or request was cancelled
            }
        }
    }


    /*
     * Handle results returned to the FragmentActivity by Google Play services
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case Constants.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (msavedInstanceState==null){
                            mLocationService.askForLocation();
                        }                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
            case Constants.PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK){
                    retrieveContactNumber(data);
                }
                break;
        }
    }




    @Override
    public void onMapLongClick(LatLng latLng) {
        mOnClickLatLang = latLng;
        //showAlertDialogForPoint(latLng);
         SetContactPickerIntent();

    }






    private void restoreMarkerArrayToMap() {
        //first load add to mMarkers all the Gate from the ShredPrefernce
        if (mMarkers == null) {
            mMarkers = new ArrayList<MarkerOptions>();

            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {

                Bitmap photo = BitmapFactory.decodeFile(ListGateComplexPref.getInstance().gates.get(i).imagePath);

                //cant find user photo from external storage
                if (photo == null) {
                    photo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gate);
                }
                mMarkers.add(new MarkerOptions()
                        .position(ListGateComplexPref.getInstance().gates.get(i).location)
                        .snippet(ListGateComplexPref.getInstance().gates.get(i).gateName)
                        .title(ListGateComplexPref.getInstance().gates.get(i).phone).icon(BitmapDescriptorFactory.fromBitmap(photo))
                );
            }
        }


    }




    public void bindService() {
        Intent intent = new Intent(getContext(), LocationService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, this, getContext().BIND_AUTO_CREATE);
    }

    public void unBindService() {
        getActivity().unbindService(this);
        if (mLocationService != null) {
            mLocationService = null;
        }
    }

    //implements
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        LocationService.MyBinder myBinder = (LocationService.MyBinder) binder;
        mLocationService = myBinder.getService();
        mServiceBound = true;
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            askForPremissions();
        }
        requestSettings();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (mLocationService == null)
            return;
        mLocationService = null;
        mServiceBound = false;

    }

    private void SetContactPickerIntent(){
        Intent contact =  new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contact, Constants.PICK_CONTACT);
    }

    private void retrieveContactNumber(Intent intent) {
        String  contactID = null;
        String contactName = null;
        String contactNumber = null;
        Bitmap photo = null;


        // getting contacts ID
        Cursor cursorContact = getContext().getContentResolver().query(intent.getData(),
                new String[]{ContactsContract.Contacts._ID,ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null);

        if (cursorContact.moveToFirst()) {

             contactID = cursorContact.getString(cursorContact.getColumnIndex(ContactsContract.Contacts._ID));
            contactName = cursorContact.getString(cursorContact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

        }

        cursorContact.close();


        // Using the contact ID now we will get contact phone number
        Cursor cursorPhone = getContext().getContentResolver().query(Phone.CONTENT_URI,
                new String[]{Phone.NUMBER},

                Phone.CONTACT_ID + " = ? AND " +
                        Phone.TYPE + " = " +
                        Phone.TYPE_MOBILE,

                new String[]{contactID},
                null);

        if (cursorPhone.moveToFirst()) {
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(Phone.NUMBER));
        }

        cursorPhone.close();


        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContext().getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID)));

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                assert inputStream != null;
                inputStream.close();
            }

            PictUtil.saveToCacheFile(photo,contactID);

            if (photo==null){
                photo = BitmapFactory.decodeResource(getContext().getResources(),R.drawable.gate);
                PictUtil.saveToCacheFile(photo,contactID);

            }



        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Contact ID: " + contactID);
        Log.d(TAG, "Contact Name: " + contactName);
        Log.d(TAG, "Contact Phone Number: " + contactNumber);

        String imagePath = PictUtil.getCacheFilename(contactID);

        //add new fate to the ListGateComplexPref
        ListGateComplexPref.getInstance().gates.add(new Gate(contactName,contactNumber,mOnClickLatLang,imagePath));
        //add the new ListGateComplexPref to shared pref
        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), getActivity());

        BitmapDescriptor defaultMarker = BitmapDescriptorFactory.fromBitmap(photo);

        MarkerOptions markerOptions = new MarkerOptions().position(mOnClickLatLang)
                .title(contactName).snippet(contactNumber).icon(defaultMarker);

        // Creates and adds marker to the map
        Marker marker = map.addMarker(markerOptions);
        mMarkers.add(markerOptions);

        //fist gate just added, start the service
        if (ListGateComplexPref.getInstance().gates.size()==1){
            bindService();
        }

        dropPinEffect(marker);
    }


    private void dropPinEffect(final Marker marker) {
        // Handler allows us to repeat a code block after a specified delay
        final android.os.Handler handler = new android.os.Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        // Use the bounce interpolator
        final android.view.animation.Interpolator interpolator =
                new BounceInterpolator();

        // Animate marker with a bounce updating its position every 15ms
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                // Calculate t for bounce based on elapsed time
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed
                                / duration), 0);
                // Set the anchor
                marker.setAnchor(0.5f, 1.0f + 14 * t);

                if (t > 0.0) {
                    // Post this event again 15ms from now.
                    handler.postDelayed(this, 15);
                } else { // done elapsing, show window
                    marker.showInfoWindow();
                }
            }
        });
    }




    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mCurrentMarker = marker.getPosition();

        return false;
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {

        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                // All location settings are satisfied. The client can initialize location
                if (msavedInstanceState==null){
                    mLocationService.askForLocation();
                }
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
    private void ChangeLocationSettings(Status status) {
        // Location settings are not satisfied. But could be fixed by showing the user
        // a dialog.
        try {
            // Show the dialog by calling startResolutionForResult(),
            // and check the result in onActivityResult().
            status.startResolutionForResult(
                    getActivity(),
                    Constants.REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            // Ignore the error.
        }
    }





    private void requestSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .setAlwaysShow(true)
                .addLocationRequest(mLocationService.mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mLocationService.mGoogleConnection.getGoogleApiClient(), builder.build());
        result.setResultCallback(this);
    }
};












