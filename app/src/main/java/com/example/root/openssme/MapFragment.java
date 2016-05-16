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
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import com.example.root.openssme.SocialNetwork.Gate;
import com.example.root.openssme.SocialNetwork.ListGateComplexPref;
import com.example.root.openssme.Utils.ComplexPreferences;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.Utils.PictUtil;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.example.root.openssme.Adapter.CustomWindowAdapter;
import com.example.root.openssme.Utils.Constants;


public class MapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener


{


    private static final String TAG = "MapFragment";
    public MapView mapView;
    public GoogleMap map;
    private Float zoom, tilt, bearing;
    private LatLng latlng;
    private ArrayList<MarkerOptions> mMarkers;
    private LatLng mOnClickLatLang;
    private LatLng mCurrentMarker;
    private Circle mCircle;
    private Bundle msavedInstanceState;



    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        if (savedInstanceState != null) {
            msavedInstanceState = savedInstanceState;
            zoom = savedInstanceState.getFloat("zoom");
            latlng = new LatLng(savedInstanceState.getDouble("lat"), savedInstanceState.getDouble("lon"));
            tilt = savedInstanceState.getFloat("tilt");
            bearing = savedInstanceState.getFloat("bearing");
            mMarkers = savedInstanceState.getParcelableArrayList("markers");
            mCurrentMarker = savedInstanceState.getParcelable("currentmarker");
            mOnClickLatLang = savedInstanceState.getParcelable("onclicklatlang");


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
            outState.putDouble("lat", map.getCameraPosition().target.latitude);
            outState.putDouble("lon", map.getCameraPosition().target.longitude);
            outState.putFloat("zoom", map.getCameraPosition().zoom);
            outState.putFloat("tilt", map.getCameraPosition().tilt);
            outState.putFloat("bearing", map.getCameraPosition().bearing);
            outState.putParcelable("currentmarker", mCurrentMarker);
            outState.putParcelableArrayList("markers", mMarkers);
            outState.putParcelable("onclicklatlang", mOnClickLatLang);


        }
        super.onSaveInstanceState(outState);


    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getFlags()) {


                case Constants.LOCATION_UPDATE_FLAG:
                    //recived location update
                    if (intent.hasExtra(Constants.LOCATION)) {
                        onLocationChanged(intent);
                    }

                    break;
            }
        }
    };



    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater(getArguments())));
        map.setOnInfoWindowClickListener(this);
        map.setOnMarkerClickListener(this);
        int maptype = PrefUtils.getSettings(getContext(),Constants.MAP_TYPE);
        switch(maptype){
            case 1:
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case 2:
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case 3:
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case 4:
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case 5:
                map.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;


        }
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
            if (mMarkers.get(i) != null && mMarkers.get(i).getPosition()!=null) {
                Marker temp = map.addMarker(mMarkers.get(i));
                if (temp != null) {
//                    if (mCurrentMarker != null && mLocationService.distance(mMarkers.get(i).getPosition().latitude, mMarkers.get(i).getPosition().longitude,
//                            mCurrentMarker.latitude, mCurrentMarker.longitude) < 0.0001) {
//                        temp.showInfoWindow();
//                    }
                }

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

    public void onLocationChanged(Intent Intent) {

        Bundle b = Intent.getExtras();
        Location location = (Location)b.get(android.location.LocationManager.KEY_LOCATION_CHANGED);
        //zoom to current position:
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(),location.getLongitude())).zoom(14).build();

        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

    }


    private void SetUpCircle(){

        if (ListGateComplexPref.getInstance().gates.size()>0) {
            if (mCircle == null) {
                mCircle = map.addCircle(new CircleOptions()
                        .center(ListGateComplexPref.getInstance().gates.get(0).location)
                        .radius(PrefUtils.getSettings(getContext(),Constants.OPEN_DISTANCE))
                        .strokeColor(Color.RED));
            } else {
                mCircle.setRadius(PrefUtils.getSettings(getContext(),Constants.OPEN_DISTANCE));

            }
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

    private void SetContactPickerIntent(){
        Intent contact =  new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contact, Constants.PICK_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case Constants.PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK){
                    retrieveContactNumber(data);
                }
                break;
        }
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
        if (map!=null){
            Marker marker = map.addMarker(markerOptions);
            dropPinEffect(marker);
            SetUpCircle();

        }
        mMarkers.add(markerOptions);

        //fist gate just added, start the service
        if (ListGateComplexPref.getInstance().gates.size()==1){
        }


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








};












