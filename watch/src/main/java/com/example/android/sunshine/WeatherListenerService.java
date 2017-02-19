package com.example.android.sunshine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Watch listening for updates from phone app on weather data.
 * Want (3) pieces of data:
 *   High
 *   Low
 *   image representing forcast
 */
public class WeatherListenerService extends WearableListenerService {


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent dataEvent : dataEvents) {
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals("/weather")) {
                    MyWatchFace.HIGH = dataMap.getInt("high");
                    MyWatchFace.LOW  = dataMap.getInt("low");
                    // TODO: get asset
                }
            }
        }
    }
}
