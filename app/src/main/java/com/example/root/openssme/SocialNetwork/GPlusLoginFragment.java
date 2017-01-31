package com.example.root.openssme.SocialNetwork;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.root.openssme.LogInActivity;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Observable;
import java.util.Observer;

import com.example.root.openssme.R;
import com.example.root.openssme.Utils.PrefUtils;
import com.example.root.openssme.common.GoogleConnection;
import com.example.root.openssme.common.State;

public class GPlusLoginFragment extends Fragment implements
        Observer,
        View.OnClickListener {

    private static final String TAG = "GPlusLoginFragment";
    public static final String GOOGLE_CONNECTION = "com.example.root.openssme.googleconnection";
    public static final int REQUEST_CODE = 1234;


    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInOptions mGoogleSignInOptions;
    private SignInButton mSignInButton;
    private GoogleConnection mGoogleConnection;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleConnection = GoogleConnection.getInstance(getContext());

        mGoogleApiClient = mGoogleConnection.getGoogleApiClient();
        mGoogleSignInOptions = mGoogleConnection.getmGoogleSignInOptions();

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mMessageReceiver,
                new IntentFilter(GOOGLE_CONNECTION));

        mGoogleConnection.addObserver(this);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootFragment = inflater.inflate(R.layout.fragment_google, null);
        // [START customize_button]
        // Customize sign-in button. The sign-in button can be displayed in
        // multiple sizes and color schemes. It can also be contextually
        // rendered based on the requested scopes. For example. a red button may
        // be displayed when Google+ scopes are requested, but a white button
        // may be displayed when only basic fragment_profile_test is requested. Try adding the
        // Scopes.PLUS_LOGIN scope to the GoogleSignInOptions to see the
        // difference.
        mSignInButton = (SignInButton) rootFragment.findViewById(R.id.google_sign_in);
        mSignInButton.setOnClickListener(this);
        mSignInButton.setColorScheme(SignInButton.COLOR_DARK);
        mSignInButton.setSize(SignInButton.SIZE_STANDARD);
        mSignInButton.setScopes(mGoogleSignInOptions.getScopeArray());
        // [END customize_button]
        return rootFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
        mGoogleConnection.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        mGoogleConnection.disconnect();
        mGoogleConnection.deleteObserver(this);
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        //connection failed try reconnect
        if (GoogleConnection.REQUEST_CODE == requestCode) {
            mGoogleConnection.onActivityResult(resultCode);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            PrefUtils.clearCurrentUser(getActivity());
            try {
                User.getInstance().source = TAG;
                if (acct.getId()!=null)
                    User.getInstance().id = acct.getId();
                if (acct.getPhotoUrl()!=null)
                    User.getInstance().image = acct.getPhotoUrl().toString();
                if (acct.getDisplayName()!=null)
                    User.getInstance().name = acct.getDisplayName();
                if (acct.getEmail()!=null) {
                    User.getInstance().email = acct.getEmail();
                }
                if (acct.getIdToken()!=null) {
                    User.getInstance().accesstoken = acct.getIdToken();
                }
                Log.e(TAG, "id:" + acct.getId() + " image:" + acct.getPhotoUrl() +
                        " name:" + acct.getDisplayName() + " email:" + acct.getEmail() +
                        " ServerAuthCode:" + acct.getServerAuthCode() + " IDToken:" + acct.getIdToken());
                PrefUtils.setCurrentUser(User.getInstance(), getActivity());

            }catch (Exception e){
                e.printStackTrace();
            }

            //start MainActivity
            Intent intent = new Intent(getContext(), LogInActivity.class);
            getActivity().startActivity(intent);
            getActivity().finish();

        }
    }


    // [START signIn]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.google_sign_in:
                signIn();
                break;

        }
    }



    @Override
    public void update(Observable observable, Object data) {
        if (observable != mGoogleConnection) {
            return;
        }

        switch ((State) data) {
            case OPENED:
                // We are signed in to google services!
                Log.d(TAG, "OPENED");
                mSignInButton.setEnabled(true);
                break;
            case CLOSED:
                mSignInButton.setEnabled(false);
                Log.d(TAG, "CLOSED");


                break;
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(GOOGLE_CONNECTION)) {
                startResolutionForResult(intent);
            }
        }
    };

    //Connection Failed
    private void startResolutionForResult(Intent intent){
        //Recreate the connection result
        int statusCode = intent.getIntExtra("error", 0);
        PendingIntent pendingIntent = intent.getParcelableExtra("resolution");
        ConnectionResult connectionResult = new ConnectionResult(statusCode, pendingIntent);
        try {
            connectionResult.startResolutionForResult(getActivity(),REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }
}
