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
import com.example.avendano.cpscan_new.Activities.ViewRepairReport;
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

public class RepairService extends Service {

    //check online database, compare then notify kung merong nabago
    public static final String BROADCAST_ACTION = "com.example.avendano.RepairNotif";
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

        //pass value with intent

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
            String response = con.sendGetRequest(AppConfig.GET_REPAIR_REQ);
            db.updateAllToggle(1, 5);
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
                    String date_requested = obj.getString("date_req");
                    String time_requested = obj.getString("time_req");
                    String set_date = obj.getString("set_date");
                    String cust_name = obj.getString("cust_name");
                    String req_status = obj.getString("req_status");
                    String cancel_rem = obj.getString("cancel_remarks");
                    String cust_id = obj.getString("cust_id");
                    int rep_id = 0;
                    if (!obj.isNull("rep_id"))
                        rep_id = obj.getInt("rep_id");
                    String tech_name = obj.getString("tech_name");
                    String tech_id = obj.getString("tech_id");
                    int comp_id = obj.getInt("room_id");
                    String set_time = obj.getString("set_time");
                    String msg = obj.getString("msg");
                    String room_name = obj.getString("room_name");
                    String req_details = obj.getString("req_details");//defectives
                    int pc_no = obj.getInt("pc_no");
                    int room_id = obj.getInt("room_id");


                    checkDataFromLocal(req_id, rep_id, comp_id, cust_id, tech_id, set_date, set_time,
                            msg, req_details, date_requested, time_requested, req_status, cancel_rem
                            , pc_no, cust_name, room_name, room_id, tech_name
                    );
                }
                deleteDataWithToggle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void deleteDataWithToggle() {
            db.deleteDataToggle(5);
        }
    }

    private void checkDataFromLocal(int req_id, int rep_id, int comp_id, String cust_id,
                                    String tech_id, String set_date, String set_time, String msg,
                                    String req_details, String date_requested,
                                    String time_requested, String req_status, String cancel_rem,
                                    int pc_no, String cust_name, String room_name, int room_id,
                                    String tech_name) {
        Cursor c = db.getReqRepair(req_id);
        if (req_status.equalsIgnoreCase("done")) {
            intent = new Intent(getApplicationContext(), ViewRepairReport.class);
            intent.putExtra("rep_id", rep_id);
        }else
            intent = new Intent(getApplicationContext(), RequestListsActivity.class);

        if (c.moveToFirst()) {
            db.updateSpecificToggle(req_id, 0, 5);
            //check kung may mga naupdate then notif

            if (!req_status.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_STATUS)))) {
                //status
                db.updateRepairRequests(req_id, 1, req_status);
                //custodian notif
                if (SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("custodian")) {
                    if (cust_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())) {
                        showNotification("Request Status Update: Your repair request last " + date_requested + " for PC " + pc_no + " of " + room_name + " is " + req_status
                                + ". Click here for more information.", "Repair Request Update", req_id);
                    }
                }
            }
            if (!msg.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_MESSAGE)))) {
                //purpose
                db.updateRepairRequests(req_id, 2, msg);
            }
            if (!set_date.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_DATE)))) {
                db.updateRepairRequests(req_id, 3, set_date);
            }
            if (!set_time.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_TIME)))) {
                db.updateRepairRequests(req_id, 4, set_date);
            }
            if (!date_requested.equalsIgnoreCase(c.getString(c.getColumnIndex(db.DATE_OF_REQ)))) {
                //cancel remarks
                db.updateRepairRequests(req_id, 5, date_requested);
            }
            if (!time_requested.equalsIgnoreCase(c.getString(c.getColumnIndex(db.TIME_OF_REQ)))) {
                //cancel remarks
                db.updateRepairRequests(req_id, 6, time_requested);
            }
            if (!cancel_rem.equalsIgnoreCase(c.getString(c.getColumnIndex(db.CANCEL_REM)))) {
                //cancel remarks
                db.updateRepairRequests(req_id, 8, cancel_rem);
            }
            if (!req_details.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_DETAILS)))) {
                //cancel remarks
                db.updateRepairRequests(req_id, 7, req_details);
            }
        } else {
            //add
            //new request
            //technician notif
            if (SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(tech_id) ||
                    SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(cust_id))
                db.addReqRepair(req_id, rep_id, comp_id, cust_id, tech_id, set_date
                        , set_time, msg, req_details, date_requested, time_requested, req_status, cancel_rem);

            if (SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("technician")) {
                if (tech_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())) {
                    if (req_status.equalsIgnoreCase("Pending"))
                        showNotification("Ms./Mr. " + cust_name + " of " + room_name + " requests for repair of PC " + pc_no + ". Click here for more information",
                                "New Repair Request", req_id);
                }
            }
        }
    }


    private void showNotification(String contentText, String title, int id) {
        //set message

        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText("Repair Request");
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        notificationManager.notify(notifCounter, notificationBuilder.build());

        notifCounter++;
    }
}

