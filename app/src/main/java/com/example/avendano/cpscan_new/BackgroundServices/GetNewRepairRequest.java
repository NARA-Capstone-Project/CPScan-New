package com.example.avendano.cpscan_new.BackgroundServices;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.avendano.cpscan_new.Network_Handler.AppConfig;
import com.example.avendano.cpscan_new.Network_Handler.HttpURLCon;
import com.example.avendano.cpscan_new.SharedPref.SharedPrefManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Avendano on 9 Apr 2018.
 */

public class GetNewRepairRequest extends Service {
    public static final String BROADCAST_ACTION = "com.example.avendano.GET_REQ_COUNT";
    private final Handler handler = new Handler();
    Intent intent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
    }


    private Runnable sendUpdatesToUI = new Runnable() {
        @Override
        public void run() {
            DisplayNotifNumber();
            handler.postDelayed(this, 10000);
        }
    };

    private void DisplayNotifNumber() {
        class NotifNumber extends AsyncTask<Void,Void,String>{

            @Override
            protected String doInBackground(Void... voids) {
                String response = "";
                Map<String, String> param = new HashMap<>();
                param.put("user_id", SharedPrefManager.getInstance(GetNewRepairRequest.this).getUserId());

                HttpURLCon con = new HttpURLCon();
                response = con.sendPostRequest(AppConfig.COUNT_REQ, param);

                return response;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Log.e("RESPONSe", s);
                try{
                    JSONObject obj = new JSONObject(s);
                    int inv = obj.getInt("inventory");
                    int rep = obj.getInt("repair");
                    int per = obj.getInt("peripherals");
                    int sum  = inv + rep + per;
                    intent.putExtra("number", sum);
                    sendBroadcast(intent);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        new NotifNumber().execute();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

}
