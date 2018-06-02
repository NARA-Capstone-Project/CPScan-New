package com.example.avendano.cpscan_new.BackgroundServices;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.avendano.cpscan_new.Activities.RequestListsActivity;
import com.example.avendano.cpscan_new.Activities.ViewInventoryReport;
import com.example.avendano.cpscan_new.Activities.ViewRequestPeripheralsDetails;
import com.example.avendano.cpscan_new.Database.SQLiteHelper;
import com.example.avendano.cpscan_new.Network_Handler.AppConfig;
import com.example.avendano.cpscan_new.Network_Handler.HttpURLCon;
import com.example.avendano.cpscan_new.R;
import com.example.avendano.cpscan_new.SharedPref.SharedPrefManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Avendano on 31 May 2018.
 */

public class InventoryService extends Service {

    public static final String BROADCAST_ACTION = "com.example.avendano.InvNotif";
    private final Handler handler = new Handler();
    Intent intent;
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    int notifCounter = 0; //pag nagstop ng service ireturn sa 0 = wifi state
    SQLiteHelper db = new SQLiteHelper(this);


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        notificationBuilder.setAutoCancel(true);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        intent = new Intent(getApplicationContext(), RequestListsActivity.class);

        handler.removeCallbacks(myFunction);
        handler.postDelayed(myFunction, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPeripheralService();
    }

    public void stopPeripheralService() {
        stopSelf();
    }

    private Runnable myFunction = new Runnable() {
        @Override
        public void run() {
            new getDataFromOnline().execute();
            handler.postDelayed(this, 10000);
        }
    };

    class getDataFromOnline extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLCon con = new HttpURLCon();
            String response = con.sendGetRequest(AppConfig.GET_INVENTORY_REQ);
            db.updateAllToggle(1, 3);
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("RESPONSE SERVICE", s);
            try {
                JSONArray array = new JSONArray(s);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int req_id = obj.getInt("req_id");
                    String date_requested = obj.getString("date_requested");
                    String time_requested = obj.getString("time_requested");
                    String date = obj.getString("date");
                    String cust_name = obj.getString("cust_name");
                    String req_status = obj.getString("req_status");
                    String cancel_rem = obj.getString("cancel_remarks");
                    String cust_id = obj.getString("custodian");
                    int rep_id = 0;
                    if (!obj.isNull("rep_id"))
                        rep_id = obj.getInt("rep_id");
                    String tech_name = obj.getString("tech_name");
                    String tech_id = obj.getString("technician");
                    int room_id = obj.getInt("room_id");
                    String time = obj.getString("time");
                    String msg = obj.getString("msg");
                    String room_name = obj.getString("room_name");

                    checkDataFromLocal(req_id, rep_id, room_id, room_name, cust_id, tech_id
                            , cust_name, tech_name, date, time, msg, req_status, date_requested
                            , time_requested, cancel_rem);
                }

                //delete data where toggle = 1 from database
                deleteDataWithToggle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void deleteDataWithToggle() {
            db.deleteDataToggle(3);
        }
    }


    private void checkDataFromLocal(int req_id, int rep_id, int room_id,
                                    String room_name, String cust_id, String tech_id,
                                    String cust_name, String tech_name, String date,
                                    String time, String msg, String req_status, String date_req
            , String time_req, String cancel_rem) {
        Cursor c = db.getReqInventory(req_id);
        if (req_status.equalsIgnoreCase("done")) {
            intent = new Intent(getApplicationContext(), ViewInventoryReport.class);
            intent.putExtra("rep_id", rep_id);
        }else
            intent = new Intent(getApplicationContext(), RequestListsActivity.class);

        if (c.moveToFirst()) {

            db.updateSpecificToggle(req_id, 0, 3);
            //check kung may mga naupdate then notif
            if (!req_status.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_STATUS)))) {
                //status
                db.updateInventoryRequests(req_id, 1, req_status);
                //custodian notif
                if (SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("custodian")) {
                    if (cust_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())) {
                        showNotification("Request Status Update: Your inventory request last " + date_req + " for " + room_name + " is " + req_status
                                + ". Click here for more information.", "Inventory Request Update", req_id);
                    }
                }
            }
            if (!msg.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_MESSAGE)))) {
                //purpose
                db.updateInventoryRequests(req_id, 2, msg);
            }
            if (!date.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_DATE)))) {
                db.updateInventoryRequests(req_id, 3, date);
            }
            if (!time.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_TIME)))) {
                db.updateInventoryRequests(req_id, 4, time);
            }
            if (!date_req.equalsIgnoreCase(c.getString(c.getColumnIndex(db.DATE_OF_REQ)))) {
                //cancel remarks
                db.updateInventoryRequests(req_id, 5, date_req);
            }
            if (!time_req.equalsIgnoreCase(c.getString(c.getColumnIndex(db.TIME_OF_REQ)))) {
                //cancel remarks
                db.updateInventoryRequests(req_id, 6, time_req);
            }
            if (!cancel_rem.equalsIgnoreCase(c.getString(c.getColumnIndex(db.CANCEL_REM)))) {
                //cancel remarks
                db.updateInventoryRequests(req_id, 7, cancel_rem);
            }
        } else {
            //add
            //new request
            //technician notif
            if (SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(tech_id) ||
                    SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(cust_id))
                db.addReqInventory(req_id, rep_id, room_id, room_name, cust_name, tech_name, cust_id
                        , tech_id, date, time, msg, date_req, time_req, req_status, cancel_rem);

            if (SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("technician")) {
                if (tech_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())) {
                    if(req_status.equalsIgnoreCase("Pending"))
                        showNotification("Ms./Mr. " + cust_name + " of " + room_name + " requests for inventory/assessment. Click here for more information",
                                "New Inventory Request", req_id);
                }
            }
        }
    }


    private void showNotification(String contentText, String title, int id) {
        //set message
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText("Inventory Request");

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(notifCounter, notificationBuilder.build());
        notifCounter++;
    }
}
