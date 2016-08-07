package com.example.root.openssme.Utils;

/**
 * Created by niso2 on 22/07/2016.
 */
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.example.root.openssme.LogInActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ${dennis} on 5/31/16.
 */
public class PermissionsUtil {
    private final Activity mActivity;

    public PermissionsUtil(Activity activity) {
        this.mActivity = activity;
    }

    public static PermissionsUtil getInstance(Activity activity) {
        return new PermissionsUtil(activity);
    }

    /*
     * check permissions
     * Returns a List of unauthorized
     * */
    public List<String> checkPermissions(List<String> permissions) {
        List<String> unGranted = new ArrayList<>();
        for (String permission : permissions) {
            if (!checkPermission(permission)) {
                unGranted.add(permission);
            }
        }
        return unGranted;
    }

    /*
     * check single permission
     * */
    public boolean checkPermission(String permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mActivity.checkSelfPermission(permissions) == PackageManager.PERMISSION_GRANTED;
        } else {
            //if sdk <23,always return true,Do not carry out dynamic authorization
            return true;
        }
    }

    public void requestPermissions(List<String> permissions, int requestCode) {

        String[] permissionsArr = permissions.toArray((new String[permissions.size()]));
        request(permissionsArr, requestCode);
    }

    /*
    * request permission
    * */
    public void requsetPermission(String permission, int requestCode) {
        String[] permissionsArr = new String[]{permission};
        request(permissionsArr, requestCode);
    }

    private void request(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(mActivity, permissions, requestCode);
    }

    /*
    * check permissions
    * Returns a List of unauthorized
    * */
    public List<String> checkPermissionsRequest(String[] permissions, int[] grantResult) {
        List<String> unGranted = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResult[i] != PackageManager.PERMISSION_GRANTED) {
                unGranted.add(permissions[i]);
            }
        }
        return unGranted;
    }

    public boolean checkPermissionRequest(String[] permissions, int[] grantResult) {
        return grantResult[0] == PackageManager.PERMISSION_GRANTED;
    }

    public boolean IsAllPermissionsGranted() {
        List<String> unGranted = this.checkPermissions(addPermissionToList());
        if (unGranted.size() != 0) {
            return false;
        }
        return true;
    }

    private List<String> addPermissionToList() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissions;
    }
}
