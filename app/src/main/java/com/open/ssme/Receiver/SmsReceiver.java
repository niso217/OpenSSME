package com.open.ssme.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.open.ssme.Helpers.CheckContact;
import com.open.ssme.Objects.ListGateComplexPref;
import com.open.ssme.Objects.Settings;
import com.open.ssme.Service.OpenSSMEService;
import com.open.ssme.Utils.Constants;
import java.util.concurrent.ExecutionException;

import static com.open.ssme.Utils.Constants.FORMAT;
import static com.open.ssme.Utils.Constants.PDUS;
import static com.open.ssme.Utils.Constants.SMS_RECEIVED;


/**
 * Created by nir on 09/02/17.
 */
public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = SMSReceiver.class.getSimpleName();
    private boolean isContactExsit;
    private Bundle bundle;
    private SmsMessage currentSMS;
    private String message;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (!Settings.getInstance().isSocial()) return;

        if (intent.getAction().equals(SMS_RECEIVED)) {
            bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdu_Objects = (Object[]) bundle.get(PDUS);
                if (pdu_Objects != null) {

                    for (Object aObject : pdu_Objects) {

                        currentSMS = getIncomingMessage(aObject, bundle);

                        String senderNo = currentSMS.getDisplayOriginatingAddress();

                        message = currentSMS.getDisplayMessageBody().replaceAll("\\s+","");

                        try {
                            isContactExsit = new CheckContact(context).execute(senderNo).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        // if the SMS is not from our contacts, ignore the message
                        if (isContactExsit && message.toLowerCase().equals(Constants.KEY_SMS_ORIGIN)
                                && ListGateComplexPref.getInstance().gates.size()>0) {
                            Log.d(TAG, "SMS Received! Open Gate");
                            OpenSSMEService.MakeTheCall(context,true);

                        }
                        else
                            Log.e(TAG, "SMS is not for our contact!");

                    }
                    this.abortBroadcast();
                    // End of loop
                }
            }
        }
    }

    private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
        SmsMessage currentSMS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String format = bundle.getString(FORMAT);
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject, format);
        } else {
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject);
        }
        return currentSMS;
    }
}
