package com.example.root.openssme.Utils;

/**
 * Created by nir on 19/03/2016.
 */
public class Constants {

    public static final int LOCATION_UPDATE_FLAG= 1;

    public static final int NEXT_UPDATE_FLAG= 4;
    public static final int CHANGE_MAP = 2;
    public static final int CHANGE_RADIUS = 3;
    public static final int REQUEST_CHECK_SETTINGS = 5;
    public static final int REQ_SELECT_PHOTO = 11;
    public static final int PREMISSIONS = 6;
    public static final int VOLLEY = 9;

    public static final int PERMISSIONS_REQUEST_CALL_PHONE = 7;
    public final static int PICK_CONTACT = 8;
    public static final int PERMISSIONS_REQUEST_STORAGE = 10;
    public static final int NOTIFICATION = 11;
    public static final int DATA_UPDATE_FLAG= 12;


    public static final long UPDATE_INTERVAL= 3000;
    public static final long FASTEST_INTERVAL= 3000;
    public static final int DRIVING_SPEED = 100;
    public static final long DEFAULT_LOCATION_INTERVAL = 30000;
    public static final long DEFAULT_CHECK_WIFI_TASK = 20000;
    public static final long DEFAULT_RUN_SERVICE_TASK = 2000;
    public static final int DEFAULT_ACTIVE_COEFFICIENT = 2;
    public static final int GOOGLE_MATRIX_API_REQ_TIME = 60 * 5;







    //premissions

    public static final String CALL_PHONE = "android.permission.CALL_PHONE";
    public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String PROVIDERS_CHANGED = "android.location.PROVIDERS_CHANGED";


    public static final String GPS = "location_update";
    public static final String GOOGLE_API = "google_api";
    public static final String GPS_PROVIDER = "gps_provider";
    public static final String NEXT_UPDATE = "next_update";
    public static final String SPEED = "speed";
    public static final String GATE_NAME = "gate_name";
    public static final String LAST_UPDATE = "last_update";
    public static final String MAP_TYPE = "map_type";
    public static final String SERVICE_PROVIDER = "service_provider";
    public static final String FOLLOW_ME = "follow_me";
    public static final String SCREEN = "screen";
    public static final String FIRST_RUN = "first_run";
    public static final String GPS_STATUS = "gps_status";
    public static final String ETA = "eta";
    public static final String GATE_RADIUS = "gate_radius";
    public static final String STRING_DIVIDER = "_OpenSSME_";




    public static final String RESTART_SERVICE = "OpenSSME.OpenSSMEService.RestartServer";
    public static final String STOP_SERVICE = "OpenSSME.OpenSSMEService.StopService";
    public static final String LOCATION_SERVICE = "OpenSSME.OpenSSMEService.Location";
    public static final String LOCATION_SERVICE_DATA = "OpenSSME.OpenSSMEService.LocationServiceData";
    public static final String DATA_CHANGED = "OpenSSME.OpenSSMEService.DATA_CHANGED";

    public static final String STARTFOREGROUND_ACTION = "OpenSSME.OpenSSMEService.StartForeground";
    public static final String MAIN_ACTION = "OpenSSME.Action.Main";


    public static final String PROFILE = "profile";
    public static final String START_LOCATAION_UPDATE = "gps_distance";
    public static final String OPEN_DISTANCE = "open_distance";


    public static final String FACEBOOK = "FacebookLoginFragment";
    public static final String GPLUS = "GPlusLoginFragment";
    public static final String LOCATION = "location";
    public static final String DISTANCE = "duration";


    public static final int FOREGROUND_SERVICE = 101;

    public enum GateStatus {
        HOME("Inside"),
        ONWAY("On The Way..."),
        ALMOST("Almost There..."),
        UNKNOWN("");

        private String status;

        GateStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }
    }
}
