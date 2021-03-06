package com.open.ssme.Helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

/**
 * Created by nirb on 09/02/2017.
 */

public class CheckContact extends AsyncTask<String, Void, Boolean> {

    private Context context;

    public CheckContact(Context context) {
        // TODO Auto-generated constructor stub
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            return contactExists(context, strings[0]);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    // The argument result is return by method doInBackground
    @Override
    protected void onPostExecute(Boolean result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);

        // You can handle the result here
    }

    public boolean contactExists(Context context, String number) throws Exception {
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        String[] mPhoneNumberProjection = { ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.DISPLAY_NAME };

        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                // if contact are in contact list it will return true
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        // if contact are not match that means contact are not added
        return false;
    }
}
