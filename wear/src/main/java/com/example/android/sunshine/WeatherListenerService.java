package com.example.android.sunshine;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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

    private static final String WEATHER_URI = "/weather";
    private static final String TEMP_HIGH = "high";
    private static final String TEMP_LOW = "low";
    private static final String IMAGE_ASSET = "image";

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
        mGoogleApiClient.connect();

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
                if(path.equals(WEATHER_URI)) {
                    MyWatchFace.HIGH = dataMap.getString(TEMP_HIGH);
                    MyWatchFace.LOW  = dataMap.getString(TEMP_LOW);

                    // Load image asset in background thread.
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());

                    Asset imageAsset = dataMapItem.getDataMap()
                            .getAsset(IMAGE_ASSET);
                    // Loads image on background thread.
                    new LoadBitmapAsyncTask().execute(imageAsset);

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


        @Override
        protected Bitmap doInBackground(Asset... params) {

            if (params.length > 0) {

                Asset asset = params[0];

                if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                    ConnectionResult connectionResult = mGoogleApiClient
                            .blockingConnect(30, TimeUnit.SECONDS);
                    if (!connectionResult.isSuccess()) {
                        Log.e(TAG, TAG + " failed to connect to GoogleApiClient, "
                                + "error code: " + connectionResult.getErrorCode());
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

            } else {
                Log.e(TAG, "Weather image load failed.");
            }
        }
    }

}
