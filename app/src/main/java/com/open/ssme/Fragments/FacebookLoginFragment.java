package com.open.ssme.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.open.ssme.Activity.LogInActivity;
import com.open.ssme.Objects.User;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONObject;

import java.util.Arrays;

import com.open.ssme.R;
import com.open.ssme.Utils.PrefUtils;

import static com.open.ssme.Utils.Constants.FACEBOOK_EMAIL;
import static com.open.ssme.Utils.Constants.FACEBOOK_FIELDS;
import static com.open.ssme.Utils.Constants.FACEBOOK_FIELDS_DATA;
import static com.open.ssme.Utils.Constants.FACEBOOK_GENDER;
import static com.open.ssme.Utils.Constants.FACEBOOK_ID;
import static com.open.ssme.Utils.Constants.FACEBOOK_IMAGE_PREFIX;
import static com.open.ssme.Utils.Constants.FACEBOOK_IMAGE_SUFFIX;
import static com.open.ssme.Utils.Constants.FACEBOOK_NAME;
import static com.open.ssme.Utils.Constants.FACEBOOK_PUBLIC_PROFILE;

public class FacebookLoginFragment extends Fragment {
    private CallbackManager callbackManager;
    private FacebookCallback<LoginResult> mCallBack;
    private LoginButton loginButton;
    private final String TAG = FacebookLoginFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        callbackManager = CallbackManager.Factory.create();

         mCallBack = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Facebook just Connected");
                storeProfile(loginResult);

            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Facebook canceled signin");
            }

            @Override
            public void onError(FacebookException e) {
                Log.e(TAG, "Facebook canceled signin: " + e.getMessage());
            }
        };

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootFragment = inflater.inflate(R.layout.fragment_facebook, null);
        loginButton= (LoginButton)rootFragment.findViewById(R.id.facebook_login);
        loginButton.setFragment(this);
        loginButton.setReadPermissions(Arrays.asList(FACEBOOK_PUBLIC_PROFILE,FACEBOOK_EMAIL));
        return rootFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        loginButton.registerCallback(callbackManager, mCallBack);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

    }






    private void storeProfile(LoginResult loginResult) {

        // App code
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {

                        Log.e("response: ", response + "");
                        try {
                            PrefUtils.clearCurrentUser(getActivity());
                            Log.e(TAG, "USER state: "+ PrefUtils.getCurrentUser(getContext()));
                            User.getInstance().source = TAG;
                            User.getInstance().accesstoken = AccessToken.getCurrentAccessToken().getToken();
                            User.getInstance().id = object.getString(FACEBOOK_ID).toString();
                            if (User.getInstance().id != null) {
                                User.getInstance().image = FACEBOOK_IMAGE_PREFIX + User.getInstance().id + FACEBOOK_IMAGE_SUFFIX;
                            }
                            User.getInstance().email = object.getString(FACEBOOK_EMAIL).toString();
                            User.getInstance().name = object.getString(FACEBOOK_NAME).toString();
                            User.getInstance().gender = object.getString(FACEBOOK_GENDER).toString();
                            PrefUtils.setCurrentUser(User.getInstance(), getActivity());
                            Log.d(TAG, "USER loged in: " + PrefUtils.getCurrentUser(getContext()).name);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //start MainActivity
                        Intent intent = new Intent(getContext(), LogInActivity.class);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                    }

                });

        Bundle parameters = new Bundle();
        parameters.putString(FACEBOOK_FIELDS,FACEBOOK_FIELDS_DATA);
        request.setParameters(parameters);
        request.executeAsync();


    }




}
