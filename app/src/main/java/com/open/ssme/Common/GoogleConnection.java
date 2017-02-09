package com.open.ssme.Common;

/**
 * Created by nir on 22/04/2016.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.util.Observable;

import com.open.ssme.R;

import static com.open.ssme.Utils.Constants.GOOGLE_CONNECTION;

public class GoogleConnection extends Observable
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static GoogleConnection sGoogleConnection;
    private static final String TAG = "GoogleConnection";
    private WeakReference<Context> activityWeakReference;
    private GoogleApiClient.Builder googleApiClientBuilder;
    private GoogleApiClient googleApiClient;
    private ConnectionResult connectionResult;
    private  Context mContext;
    private State currentState;
    private GoogleSignInOptions mGoogleSignInOptions;

    public static final int REQUEST_CODE = 1234;


    public void connect() {
        currentState.connect(this);
    }

    public GoogleSignInOptions getmGoogleSignInOptions() {
        return mGoogleSignInOptions;
    }

    public void disconnect() {
        currentState.disconnect(this);
    }


    public static GoogleConnection getInstance(Context context) {
        if (null == sGoogleConnection) {
            sGoogleConnection = new GoogleConnection(context);
        }

        return sGoogleConnection;
    }


    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }


    /*
 * Called by Location Services if the connection to the location client
 * drops because of an error.
 */
    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason.
        // We call connect() to attempt to re-establish the connection or get a
        // ConnectionResult that we can attempt to resolve.
        changeState(State.CLOSED);
        connect();
    }

    /*
 * Called by Location Services when the request to connect the client
 * finishes successfully. At this point, you can request the current
 * location or start periodic updates
 */
    @Override
    public void onConnected(Bundle hint) {
        changeState(State.OPENED);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (currentState.equals(State.CLOSED) && connectionResult.hasResolution()) {
            //start resolution
            Intent intent = new Intent(GOOGLE_CONNECTION);
            intent.putExtra("error", connectionResult.getErrorCode());
            intent.putExtra("resolution", connectionResult.getResolution());
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

        }
        else
            Log.d(TAG,"Location services not available to you");
    }

    public void onActivityResult(int result) {
        if (result == Activity.RESULT_OK) {
            // If the error resolution was successful we should continue
            // processing errors.
            changeState(State.OPENED);
        } else {
            // If the error resolution was not successful or the user canceled,
            // we should stop processing errors.
            changeState(State.CLOSED);
        }

        // If Google Play services resolved the issue with a dialog then
        // onStart is not called so we need to re-attempt connection here.
        onSignIn();
    }


    protected void onSignIn() {
        if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    protected void onSignOut() {
        if (googleApiClient.isConnected()) {
            // We clear the default account on sign out so that Google Play
            // services will not return an onConnected callback without user
            // interaction.
            googleApiClient.disconnect();
            googleApiClient.connect();
            changeState(State.CLOSED);
        }
    }



    private GoogleConnection(Context context) {

        mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestId()
                .requestIdToken(context.getString(R.string.server_client_id))
                .requestEmail()
                .build();

        mContext = context;

        activityWeakReference = new WeakReference<>(context);

        googleApiClientBuilder =
                new GoogleApiClient.Builder(activityWeakReference.get().getApplicationContext())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .addApi(Auth.GOOGLE_SIGN_IN_API,mGoogleSignInOptions);

        googleApiClient = googleApiClientBuilder.build();

        currentState = State.CLOSED;
    }

    private void changeState(State state) {
        currentState = state;
        setChanged();
        notifyObservers(state);
    }





}

