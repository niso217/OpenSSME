package com.open.ssme.Fragments;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.open.ssme.Common.GoogleConnection;
import com.open.ssme.Service.OpenSSMEService;
import com.open.ssme.R;
import com.open.ssme.Objects.Gate;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Utils.PrefUtils;
import com.open.ssme.Utils.PictUtil;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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
import java.util.List;

import com.open.ssme.Adapter.CustomWindowAdapter;
import com.open.ssme.Utils.Constants;

import static com.open.ssme.Utils.Constants.MAP_BEARING;
import static com.open.ssme.Utils.Constants.MAP_CLICKED_LAT_LNG;
import static com.open.ssme.Utils.Constants.MAP_CURRENT_MARKER;
import static com.open.ssme.Utils.Constants.MAP_DUMMY;
import static com.open.ssme.Utils.Constants.MAP_LAT;
import static com.open.ssme.Utils.Constants.MAP_LNG;
import static com.open.ssme.Utils.Constants.MAP_MARKERS;
import static com.open.ssme.Utils.Constants.MAP_TILT;
import static com.open.ssme.Utils.Constants.MAP_ZOOM;
import static java.lang.Math.abs;


public class MapFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        PlaceSelectionListener, View.OnClickListener, GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapClickListener


{


    private final String TAG = MapFragment.class.getSimpleName();
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    public MapView mapView;
    public GoogleMap map;
    private Float zoom, tilt, bearing;
    private LatLng latlng;
    private ArrayList<MarkerOptions> mMarkerOptions;
    private ArrayList<Marker> mMarker;
    private LatLng mOnClickLatLang;
    private Location mCurrentMarker;
    private PlaceAutocompleteFragment autocompleteFragment;
    private List<Circle> mCircleList;
    private boolean mShowCircles;
    private int mCurrentListIndex;

    private int light_blue, blue, light_red, red;

    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            zoom = savedInstanceState.getFloat(MAP_ZOOM);
            latlng = new LatLng(savedInstanceState.getDouble(MAP_LAT), savedInstanceState.getDouble(MAP_LNG));
            tilt = savedInstanceState.getFloat(MAP_TILT);
            bearing = savedInstanceState.getFloat(MAP_BEARING);
            mMarkerOptions = savedInstanceState.getParcelableArrayList(MAP_BEARING);
            mCurrentMarker = savedInstanceState.getParcelable(MAP_CURRENT_MARKER);
            mOnClickLatLang = savedInstanceState.getParcelable(MAP_CLICKED_LAT_LNG);


        } else {
            //get masseges from OpenSSMEService
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(Constants.STATUS_CHANGED));
        }

    }

    private void UnRegisterReceiver() {
        if (mMessageReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
            mMessageReceiver = null;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putDouble(MAP_LAT, map.getCameraPosition().target.latitude);
            outState.putDouble(MAP_LNG, map.getCameraPosition().target.longitude);
            outState.putFloat(MAP_ZOOM, map.getCameraPosition().zoom);
            outState.putFloat(MAP_TILT, map.getCameraPosition().tilt);
            outState.putFloat(MAP_BEARING, map.getCameraPosition().bearing);
            outState.putParcelable(MAP_CURRENT_MARKER, mCurrentMarker);
            outState.putParcelableArrayList(MAP_MARKERS, mMarkerOptions);
            outState.putParcelable(MAP_CLICKED_LAT_LNG, mOnClickLatLang);


        }
        super.onSaveInstanceState(outState);


    }


    @Override
    public void onResume() {
        mapView.onResume();
        getMyLocation(true);
        super.onResume();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        UnRegisterReceiver();

    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        android.app.Fragment mapFragment = getActivity().getFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            getActivity().getFragmentManager().beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }
        android.app.Fragment autoComplete = getActivity().getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (autoComplete != null) {
            getActivity().getFragmentManager().beginTransaction()
                    .remove(autoComplete)
                    .commit();
        }

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (map != null && mShowCircles) {
                if (mShowCircles) {
                    ClearCircles();
                    SetUpCircle();
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mCircleList = new ArrayList<Circle>();
        light_blue = ContextCompat.getColor(getContext(), R.color.shadecolor);
        blue = ContextCompat.getColor(getContext(), R.color.strokecolor);
        light_red = ContextCompat.getColor(getContext(), R.color.shaderedcolor);
        red = ContextCompat.getColor(getContext(), R.color.strokeredcolor);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater(getArguments())));
        map.setOnInfoWindowClickListener(this);
        map.setOnMarkerClickListener(this);


        ChangeMapType();

        SetUpZoom();

        //SetUpCircle();

        getMyLocation(true);


        //restoring the map state
        if (latlng != null && zoom != null && tilt != null && bearing != null) {
            CameraPosition position = new CameraPosition(latlng, zoom, tilt, bearing);
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            map.moveCamera(update);


        }
        restoreMarkerArrayToMap();

        for (int i = 0; i < mMarkerOptions.size(); i++) {
            if (mMarkerOptions.get(i) != null && mMarkerOptions.get(i).getPosition() != null) {
                Marker temp = map.addMarker(mMarkerOptions.get(i));
                mMarker.add(temp);
                Location marker = LocationToLatLng(temp.getPosition());
                if (mCurrentMarker != null && temp != null) {
                    if (mCurrentMarker.distanceTo(marker) < 0.0001)
                        temp.showInfoWindow();
                }
            }


        }
    }


    private void ChangeMapType() {
        int maptype = Settings.getInstance().getMap_type();
        switch (maptype) {
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
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.activity_map, container, false);
        try {
            rootView.findViewById(R.id.help).setOnClickListener(this);
            rootView.findViewById(R.id.current_location).setOnClickListener(this);
            rootView.findViewById(R.id.draw).setOnClickListener(this);
            rootView.findViewById(R.id.left).setOnClickListener(this);
            rootView.findViewById(R.id.right).setOnClickListener(this);
            MapsInitializer.initialize(this.getActivity());
            mapView = (MapView) rootView.findViewById(R.id.map);
            mapView.onCreate(savedInstanceState);


            if (mapView != null) {
                mapView.getMapAsync(this);
            } else {
                Toast.makeText(getContext(), getContext().getResources().getString(R.string.map_error), Toast.LENGTH_SHORT).show();
            }
        } catch (InflateException e) {
            //Log.e(TAG, "Inflate exception");
        }
        if (autocompleteFragment == null) {
            autocompleteFragment = (PlaceAutocompleteFragment)
                    getActivity().getFragmentManager().findFragmentById(R.id.autocomplete_fragment);

            autocompleteFragment.setOnPlaceSelectedListener(this);
        }

        return rootView;
    }

    @Override
    public void onPause() {
        getMyLocation(false);
        super.onPause();
    }

    @Override
    public void onPlaceSelected(Place place) {

        if (map != null) {
            Toast.makeText(getContext(), place.getAddress(), Toast.LENGTH_LONG).show();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(place.getLatLng()).zoom(15).build();

            Marker PlaceMarker = map.addMarker(new MarkerOptions()
                    .position(place.getLatLng())
                    .draggable(true)
                    .snippet(place.getAddress() + "")
                    .title(getString(R.string.click_to_add))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            mMarker.add(PlaceMarker);


            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));

            PlaceMarker.showInfoWindow();

        }

    }


    @Override
    public void onError(Status status) {

    }


    void getMyLocation(boolean location_updates) {
        if (map != null) {
            // Now that map has loaded, let's get our location!
            if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;

            } else {
                // permission has been granted, continue as usual
                map.setMyLocationEnabled(location_updates);
                map.setOnMapLongClickListener(this);
                map.setOnMarkerDragListener(this);
                map.getUiSettings().setZoomControlsEnabled(true);

            }
        }
    }


    public void onLocationChanged(Intent Intent) {

        Bundle b = Intent.getExtras();
        Location location = (Location) b.get(android.location.LocationManager.KEY_LOCATION_CHANGED);
        //zoom to current position:
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(map.getCameraPosition().zoom).build();

        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

    }

    private void SetUpZoom() {
        double latitude, longitude;
        GoogleConnection mGoogleConnection = GoogleConnection.getInstance(getContext());
        if (mGoogleConnection.getGoogleApiClient().isConnected()) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12.0f));

            }
        }
    }


    private void SetUpCircle() {
        if (ListGateComplexPref.getInstance().gates.size() > 0 && map != null) {
            Circle temp;
            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {
                if (ListGateComplexPref.getInstance().gates.get(i).active)
                    temp = map.addCircle(setCircleOptions(
                            light_blue,
                            blue,
                            ListGateComplexPref.getInstance().gates.get(i).location));

                else
                    temp = map.addCircle(setCircleOptions(
                            light_red,
                            red,
                            ListGateComplexPref.getInstance().gates.get(i).location));

                mCircleList.add(temp);
            }

        }
    }


    private CircleOptions setCircleOptions(int fillColor, int strokeColor, LatLng latLng) {
        return new CircleOptions()
                .center(latLng)
                .radius(Settings.getInstance().getOpen_distance())
                .fillColor(fillColor)
                .strokeWidth(4)
                .strokeColor(strokeColor);

    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        mOnClickLatLang = latLng;
        SetContactPickerIntent();

    }

    @Override
    public void onClick(View v) {
        double latitude, longitude;
        switch (v.getId()) {
            case R.id.current_location:
                if (map != null) {
                    GoogleConnection mGoogleConnection = GoogleConnection.getInstance(getContext());
                    if (mGoogleConnection.getGoogleApiClient().isConnected()) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleConnection.getGoogleApiClient());
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            mOnClickLatLang = new LatLng(latitude, longitude);
                            SetContactPickerIntent();

                        }

                    }
                }
                break;
            case R.id.help:
                onCoachMark();
                break;
            case R.id.left:
                ChangeCameraPosition(--mCurrentListIndex);
                break;
            case R.id.right:
                ChangeCameraPosition(++mCurrentListIndex);
                break;
            case R.id.draw:
                if (mCircleList.size() == 0) {
                    mShowCircles = true;
                    SetUpCircle();
                } else {
                    mShowCircles = false;
                    ClearCircles();

                }
                break;
        }

    }

    private void ClearCircles() {
        for (Circle myCircle : mCircleList) {
            myCircle.remove();
        }
        mCircleList.clear();
    }

    private void ChangeCameraPosition(int position) {
        if (mMarker.size() > 0) {
            int index = abs(position % mMarker.size());
            Marker currentMarker = mMarker.get(index);
            LatLng latLng = currentMarker.getPosition();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            currentMarker.showInfoWindow();
            Log.d(TAG,currentMarker.getTitle()+ "  "+ currentMarker.getSnippet());
        }
    }


    private void restoreMarkerArrayToMap() {
        //first load add to mMarkerOptions all the Gate from the ShredPrefernce
        if (mMarkerOptions == null) {
            mMarkerOptions = new ArrayList<>();
            mMarker = new ArrayList<>();
            for (int i = 0; i < ListGateComplexPref.getInstance().gates.size(); i++) {

                Bitmap photo = BitmapFactory.decodeFile(ListGateComplexPref.getInstance().gates.get(i).imagePath);

                //cant find user photo from external storage
                if (photo == null) {
                    photo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gate);
                }
                mMarkerOptions.add(new MarkerOptions()
                        .position(ListGateComplexPref.getInstance().gates.get(i).location)
                        .snippet(ListGateComplexPref.getInstance().gates.get(i).gateName)
                        .draggable(true)
                        .title(ListGateComplexPref.getInstance().gates.get(i).phone).icon(BitmapDescriptorFactory.fromBitmap(photo))
                );
            }
        }


    }

    private void SetContactPickerIntent() {
        Intent contact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contact, Constants.PICK_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Decide what to do based on the original request code
        switch (requestCode) {
            case Constants.PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK) {
                    retrieveContactNumber(data);
                }
                break;
            case PLACE_AUTOCOMPLETE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(getContext(), data);
                    Log.i(TAG, "Place: " + place.getName());
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(getContext(), data);
                    // TODO: Handle the error.
                    Log.i(TAG, status.getStatusMessage());

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // The user canceled the operation.
                }

        }
    }

    private void retrieveContactNumber(Intent intent) {
        String contactID = "";
        String contactName = "";
        String contactNumber = "";
        Bitmap photo = null;


        // getting contacts ID
        Cursor cursorContact = getContext().getContentResolver().query(intent.getData(),
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null);

        if (cursorContact.moveToFirst()) {

            contactID = cursorContact.getString(cursorContact.getColumnIndex(ContactsContract.Contacts._ID));
            contactName = cursorContact.getString(cursorContact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

        }

        cursorContact.close();

        try {
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

            if (contactNumber.equals("")) {
                Log.d(TAG, "Phone Number Is Invalid");
                Toast.makeText(getContext(), getString(R.string.phone_invalid), Toast.LENGTH_LONG).show();
                return;
            }

            if (ListGateComplexPref.getInstance().isGateExist(contactNumber)) {
                Log.d(TAG, "Phone Number Exist");
                Toast.makeText(getContext(), getString(R.string.phone_exist), Toast.LENGTH_LONG).show();
                return;
            }


            cursorPhone.close();
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.no_phone), Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContext().getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID)));

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                assert inputStream != null;
                inputStream.close();
            }

            PictUtil.saveToCacheFile(photo, contactID);

            if (photo == null) {
                photo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gate);
                PictUtil.saveToCacheFile(photo, contactID);

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Contact ID: " + contactID);
        Log.d(TAG, "Contact Name: " + contactName);
        Log.d(TAG, "Contact Phone Number: " + contactNumber);

        String imagePath = PictUtil.getCacheFilename(contactID);

        //add new fate to the ListGateComplexPref
        ListGateComplexPref.getInstance().gates.add(new Gate(contactName, contactNumber, mOnClickLatLang, imagePath));
        //add the new ListGateComplexPref to shared pref
        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), getActivity());

        BitmapDescriptor defaultMarker = BitmapDescriptorFactory.fromBitmap(photo);

        MarkerOptions markerOptions = new MarkerOptions().position(mOnClickLatLang)
                .snippet(contactName)
                .title(contactNumber)
                .draggable(true)
                .icon(defaultMarker);

        // Creates and adds marker to the map
        if (map != null) {
            Marker marker = map.addMarker(markerOptions);
            mMarker.add(marker);
            dropPinEffect(marker);
            if (mShowCircles)
                SetUpCircle();


        }
        mMarkerOptions.add(markerOptions);

        //fist gate just added, start the service
        if (ListGateComplexPref.getInstance().gates.size() == 1 && !Settings.getInstance().isSchedule()) {
            Intent startIntent = new Intent(getActivity(), OpenSSMEService.class);
            startIntent.setAction(Constants.STARTFOREGROUND_ACTION);
            getActivity().startService(startIntent);
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
        if (!ListGateComplexPref.getInstance().isGateExist(marker.getTitle())) {

            mOnClickLatLang = marker.getPosition();
            marker.remove();
            SetContactPickerIntent();
        }

    }

    private Location LocationToLatLng(LatLng latlng) {
        Location MarkerLocation = new Location(MAP_DUMMY);
        MarkerLocation.setLatitude(latlng.latitude);
        MarkerLocation.setLatitude(latlng.longitude);
        return MarkerLocation;

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mCurrentMarker = LocationToLatLng(marker.getPosition());

        return false;
    }

    public void onCoachMark() {

        final Dialog dialog = new Dialog(getContext());
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
    }


    @Override
    public void onMarkerDragStart(Marker marker) {
        ClearCircles();
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        ListGateComplexPref.getInstance().ChangeGatePosition(marker);
        PrefUtils.setCurrentGate(ListGateComplexPref.getInstance(), getActivity());
        if (mShowCircles)
            SetUpCircle();

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }
};












