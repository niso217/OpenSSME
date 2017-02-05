package com.open.ssme.Utils;

/**
 * Created by nir on 19/03/2016.
 */
public class Constants {


    public static final int REQ_SELECT_PHOTO = 11;


    public static final long UPDATE_INTERVAL = 3000;
    public static final long FASTEST_INTERVAL = 3000;
    public static final int DRIVING_SPEED = 100;
    public static final long DEFAULT_LOCATION_INTERVAL = 30000;
    public static final long DEFAULT_CHECK_WIFI_TASK = 20000;
    public static final long DEFAULT_RUN_SERVICE_TASK = 2000;
    public static final int DEFAULT_ACTIVE_COEFFICIENT = 2;
    public static final int GOOGLE_MATRIX_API_REQ_TIME = 60 * 5;


    //facebook
    public static final String FACEBOOK_ID = "id";
    public static final String FACEBOOK_EMAIL = "email";
    public static final String FACEBOOK_NAME = "name";
    public static final String FACEBOOK_GENDER = "gender";
    public static final String FACEBOOK_IMAGE_PREFIX = "https://graph.facebook.com/";
    public static final String FACEBOOK_IMAGE_SUFFIX = "/picture?type=large";
    public static final String FACEBOOK_FIELDS = "fields";
    public static final String FACEBOOK_FIELDS_DATA = "id,name,email,gender,birthday";
    public static final String FACEBOOK_PUBLIC_PROFILE = "public_profile";
    public static final String FACEBOOK_USER_LOCATION = "user_location";

    //GPlus
    public static final String GOOGLE_CONNECTION = "com.open.ssme.googleconnection";
    public static final int GOOGLE_REQUEST_CODE = 1234;
    public static final int GOOGLE_RC_SIGN_IN = 9001;
    public static final String GOOGLE_ERROR = "error";
    public static final String GOOGLE_RESOLUTION = "resolution";


    //MapFragment
    public static final String MAP_ZOOM = "zoom";
    public static final String MAP_LAT = "latitude";
    public static final String MAP_LNG = "longitude";
    public static final String MAP_TILT = "tilt";
    public static final String MAP_BEARING = "bearing";
    public static final String MAP_CURRENT_MARKER = "currentmarker";
    public static final String MAP_MARKERS = "markers";
    public static final String MAP_CLICKED_LAT_LNG = "onclicklatlang";
    public static final String MAP_DUMMY = "dummy";
    public static final int LOCATION_UPDATE_FLAG = 1;
    public final static int PICK_CONTACT = 8;

    //addPreferencesFromResource
    public static final String PREF_UPDATE_CATEGORY = "update_category";
    public static final String PREF_FIRST_RUN = "first_run";


    //SocialNetworkHelper
    public static final String PERMISSION = "publish_actions";
    public static final int REQ_START_SHARE = 2;

    //PrefUtils
    public static final String CURRENT_USER_VALUE = "current_user_value";
    public static final String USER_PREFS = "user_prefs";
    public static final String GATE_PREFS = "gate_prefs";
    public static final String GATE_LIST = "gate_list";

    public static final String DEFAULT_LOCATION_UPDATE = "4";
    public static final String DEFAULT_OPEN_DISTANCE = "400";
    public static final String DEFAULT_MAP_TYPE = "1";


    //PictUtil
    public static final String DEFAULT_FOLDER = "/OpenSSME/";

    //ComplexPreferences
    public static final String COMPLEX_PREFERENCES = "complex_preferences";

    //premissions
    public static final String CALL_PHONE = "android.permission.CALL_PHONE";
    public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public static final String WRITE_EXTERNAL_STORAGE = "an" + "droid.permission.WRITE_EXTERNAL_STORAGE";
    public static final String PROVIDERS_CHANGED = "android.location.PROVIDERS_CHANGED";

    //Service
    public static final String DISTANCE_MATRIX_SUFFIX = "https://maps.googleapis.com/maps/api/distancematrix/json?";



    public static final String MAP_TYPE = "map_type";
    public static final String FOLLOW_ME = "follow_me";
    public static final String SCREEN = "screen";
    public static final String FIRST_RUN = "first_run";
    public static final String GPS_STATUS = "gps_status";
    public static final String STRING_DIVIDER = "_OpenSSME_";


    public static final String MAP_FRAGMENT = "mMapFragment";
    public static final String SETTINGS_FRAGMENT = "mSettingsFragment";
    public static final String GATE_LIST_FRAGMENT = "mGateListFragment";

    public static final String RESTART_SERVICE = "OpenSSME.OpenSSMEService.RestartServer";
    public static final String LOCATION_SERVICE = "OpenSSME.OpenSSMEService.Location";
    public static final String DATA_CHANGED = "OpenSSME.OpenSSMEService.DATA_CHANGED";

    public static final String STARTFOREGROUND_ACTION = "OpenSSME.OpenSSMEService.StartForeground";


    public static final String START_LOCATAION_UPDATE = "gps_distance";
    public static final String OPEN_DISTANCE = "open_distance";


    public static final String FACEBOOK = "FacebookLoginFragment";
    public static final String GPLUS = "GPlusLoginFragment";
    public static final String LOCATION = "location";


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
