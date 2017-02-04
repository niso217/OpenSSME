package com.example.root.openssme.Helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.example.root.openssme.Objects.GoogleMatrixResponse;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class GoogleMatrixRequest extends AsyncTask<String, Void, GoogleMatrixResponse> {
    Context mContext;
    Geo geo1;
    //constructor is used to get the context.
    public GoogleMatrixRequest(Context mContext) {
        this.mContext = mContext;
        geo1= (Geo) mContext;
    }

    //This function is executed after the execution of "doInBackground(String...params)" to dismiss the dispalyed progress dialog and call "setDouble(Double)" defined in "MainActivity.java"
    @Override
    protected void onPostExecute(GoogleMatrixResponse response) {
        super.onPostExecute(response);
        if(response!=null)
        {
            geo1.setGoogleMatrixResponse(response);
        }
    }

    @Override
    protected GoogleMatrixResponse doInBackground(String... params) {
        try {
            URL url=new URL(params[0]);
            HttpURLConnection con= (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.connect();
            int statuscode=con.getResponseCode();
            if(statuscode==HttpURLConnection.HTTP_OK)
            {
                BufferedReader br=new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb=new StringBuilder();
                String line=br.readLine();
                while(line!=null)
                {
                    sb.append(line);
                    line=br.readLine();
                }
                String json=sb.toString();

                GoogleMatrixResponse response = (new Gson().fromJson(json, GoogleMatrixResponse.class));
                Log.d("JSON",json);
                return response;

            }
        } catch (MalformedURLException e) {
            Log.d("error", "error1");
        } catch (IOException e) {
            Log.d("error", "error2");
        } 



        return null;
    }

    public interface Geo{
        public void setGoogleMatrixResponse(GoogleMatrixResponse response);
    }


}
