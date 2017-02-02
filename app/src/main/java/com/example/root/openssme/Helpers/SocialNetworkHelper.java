package com.example.root.openssme.Helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.plus.PlusShare;

import java.util.ArrayList;
import java.util.Arrays;
import com.example.root.openssme.R;
import com.example.root.openssme.Common.GoogleConnection;

/**
 * Created by nir on 19/03/2016.
 */
public class SocialNetworkHelper

{
    private static final String TAG = "SocialNetworkHelper";
    private static final int REQ_START_SHARE = 2;
    private static final String PERMISSION = "publish_actions";

    private FacebookCallback<Sharer.Result> shareCallback;
    private CallbackManager callbackManager;
    private Context mContext;


    public SocialNetworkHelper(Context context){
        mContext = context;
        FacebookSdk.sdkInitialize(mContext);
        callbackManager = CallbackManager.Factory.create();
        setShareCallback();
    }



    public FacebookCallback<Sharer.Result> getShareCallback() {
        return shareCallback;
    }


    public boolean hasPublishPermission() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && accessToken.getPermissions().contains("publish_actions");
    }

    public boolean IsGoogleCached(){
        return Auth.GoogleSignInApi.silentSignIn(GoogleConnection.getInstance(mContext).getGoogleApiClient()).isDone();
    }

    public boolean IsFacebookCached(){
        return Profile.getCurrentProfile()!=null;
    }



    public void setShareCallback() {
        shareCallback = new FacebookCallback<Sharer.Result>() {
            @Override
            public void onCancel() {
                Log.d(TAG, "Canceled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, String.format("Error: %s", error.toString()));
                String title = mContext.getString(R.string.error);
                String alertMessage = error.getMessage();
                showResult(title, alertMessage);
            }

            @Override
            public void onSuccess(Sharer.Result result) {
                Log.d(TAG, "Success!");
                if (result.getPostId() != null) {
                    String title = mContext.getString(R.string.success);
                    String id = result.getPostId();
                    String alertMessage = mContext.getString(R.string.successfully_posted_post, id);
                    showResult(title, alertMessage);
                }
            }

            private void showResult(String title, String alertMessage) {
                new AlertDialog.Builder(mContext)
                        .setTitle(title)
                        .setMessage(alertMessage)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        };
    }


    /**
     *
     * @param activity - current activity
     * @param subject - the subject of the post
     * @param body - the body of the post
     * @param url - link inside the post
     */
    public void PostOnGoogle(Activity activity,String subject,String body, String url) {
        Intent shareIntent = new PlusShare.Builder(activity)
                .setType("text/plain")
                .setText(subject + "" + body)
                .setContentUrl(Uri.parse(url))
                .getIntent();
        activity.startActivityForResult(shareIntent, 0);
    }

    /**
     *
     * @param activity - current activity
     * @param subject - the subject of the post
     * @param body - the body of the post
     * @param url - link inside the post
     */
    public void PostOnFacebook(Activity activity,String subject,String body, String url) {


        ShareDialog shareDialog = new ShareDialog(activity);
        shareDialog.registerCallback(
                callbackManager,
                shareCallback);

        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentTitle(subject)
                .setContentDescription(body)
                .setContentUrl(Uri.parse(url))
                .build();
        // Can we present the share dialog for regular links?
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            shareDialog.show(linkContent);
        } else if (Profile.getCurrentProfile() != null && hasPublishPermission()) {
            ShareApi.share(linkContent, shareCallback);
        }
    }





    public void FacebookGetUserLocation(Activity activity){
        LoginManager.getInstance().logInWithReadPermissions(
                activity,
                Arrays.asList("user_location"));
    }


    public void GooglePostPhoto(Activity activity, Intent intent){
        try {
            Uri selectedImage = intent.getData();
            ContentResolver cr = activity.getContentResolver();
            String mime = cr.getType(selectedImage);

            PlusShare.Builder share = new PlusShare.Builder(activity);
            share.setText("OpenSSME Project!");
            share.addStream(selectedImage);
            share.setType(mime);
            activity.startActivityForResult(share.getIntent(), REQ_START_SHARE);
        }
        catch (NullPointerException e){
            Log.e(TAG,e.getMessage());
        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


    }

    public void FacebookPostPhoto(Activity activity, Intent intent) {

        ShareDialog shareDialog = new ShareDialog(activity);
        shareDialog.registerCallback(
                callbackManager,
                shareCallback);

        try {
            Uri selectedImage = intent.getData();
            SharePhoto sharePhoto = new SharePhoto.Builder().setImageUrl(selectedImage).build();
            ArrayList<SharePhoto> photos = new ArrayList<>();
            photos.add(sharePhoto);

            SharePhotoContent sharePhotoContent =
                    new SharePhotoContent.Builder().setPhotos(photos).build();
            if (ShareDialog.canShow(SharePhotoContent.class)) {
                shareDialog.show(sharePhotoContent);
            } else if (hasPublishPermission()) {
                ShareApi.share(sharePhotoContent, shareCallback);
            } else {
                // We need to get new permissions, then complete the action when we get called back.
                LoginManager.getInstance().logInWithPublishPermissions(
                        activity,
                        Arrays.asList(PERMISSION));
            }
        }
        catch (NullPointerException e){
            Log.e(TAG,e.getMessage());
        }catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }








}

