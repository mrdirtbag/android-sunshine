package com.example.android.sunshine;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Listener for watch face to get data from phone when Sunshine phone
 * app gets new data.
 */
public class WeatherListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WeatherListenerService";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        // init Google API Client to send data to wearable
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent dataEvent : dataEvents) {
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals(Utils.WEATHER_URI)) {

                    Toast.makeText(this, "data received.", Toast.LENGTH_SHORT).show();

                    // Load image asset in background thread.
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());

                    Asset imageAsset = dataMapItem.getDataMap()
                            .getAsset(Utils.IMAGE_ASSET);
                    if(null != imageAsset) {
                        // Loads image on background thread.
                        new LoadBitmapAsyncTask(dataMap.getString(Utils.TEMP_HIGH), dataMap.getString(Utils.TEMP_LOW)).execute(imageAsset);
                    }
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    /*
     * From DataLayer Sample Android app.
     *
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        String mHigh;
        String mLow;

        LoadBitmapAsyncTask(String high, String low)  {
            mHigh = high;
            mLow = low;
        }

        @Override
        protected Bitmap doInBackground(Asset... params) {

            if (params.length > 0) {

                Asset asset = params[0];

                if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                    ConnectionResult connectionResult = mGoogleApiClient
                            .blockingConnect(30, TimeUnit.SECONDS);
                    if (!connectionResult.isSuccess()) {
                        Log.e(TAG, TAG + " failed to connect to GoogleApiClient, "
                                + "error code: " + connectionResult.getErrorCode()  + " : " +  connectionResult.getErrorMessage() + " : " + connectionResult.toString());
                        return null;
                    }
                }

                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    return null;
                }
                return BitmapFactory.decodeStream(assetInputStream);

            } else {
                Log.e(TAG, "Asset must be non-null");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                MyWatchFace.setWeatherImage(bitmap);
                // store data in shared preferences
                SharedPreferences prefs = getSharedPreferences(Utils.WEATHER_PREF_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Utils.TEMP_HIGH, mHigh);
                editor.putString(Utils.TEMP_LOW, mLow);
                editor.commit();


                // notify watch to update display
                synchronized (editor) {
                    editor.notify();
                }

            } else {
                Log.e(TAG, "Weather image load failed.");
            }
        }
    }

}
