package com.open.ssme.Helpers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.open.ssme.R;

import java.io.IOException;

/**
 * Created by nirb on 15/03/2017.
 */

public class PlayNotification extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... contexts) {
        PlayNotificationSound(contexts[0]);
        return null;
    }


    private static void PlayNotificationSound(Context context) {

        Uri url = Uri.parse("android.resource://" + "com.open.ssme" + "/" + R.raw.gtae_open);

        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(context, url);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}