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

import com.example.avendano.cpscan_new.Activities.Main_Page;
import com.example.avendano.cpscan_new.Activities.RequestListsActivity;
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

public class PeripheralsService extends Service {

    //check online database, compare then notify kung merong nabago
    public static final String BROADCAST_ACTION = "com.example.avendano.ReqNotif";
    private final Handler handler = new Handler();
    Intent intent;
    NotificationManager notificationManager;
    NotificationCompat.Builder notificationBuilder;
    int notifCounter = 0; //pag nagstop ng service ireturn sa 0 = wifi state

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setContentTitle("Requests Notification");
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        notificationBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        notificationBuilder.setAutoCancel(true);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        intent = new Intent(getApplicationContext(), RequestListsActivity.class);
        //pass value with intent

        handler.removeCallbacks(myFunction);
        handler.postDelayed(myFunction, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPeripheralService();
    }

    public void stopPeripheralService(){
        stopSelf();
    }

    private Runnable myFunction = new Runnable() {
        @Override
        public void run() {
            new getDataFromOnline().execute();
            handler.postDelayed(this, 10000);
        }
    };

    class getDataFromOnline extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLCon con = new HttpURLCon();
            String response = con.sendGetRequest(AppConfig.GET_PERIPHERALS);

            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("RESPONSE SERVICE", s);
            try{
                JSONArray array = new JSONArray(s);
                for (int i = 0; i < array.length(); i++){
                    JSONObject obj = array.getJSONObject(i);
                    int req_id = obj.getInt("req_id");
                    String purpose = obj.getString("purpose");
                    String date_req = obj.getString("date_req");
                    String date_approved = obj.getString("date_approved");
                    String req_status = obj.getString("req_status");
                    String cancel_rem = obj.getString("cancel_remarks");
                    String cust_id = obj.getString("cust_id");
                    int dept_id = obj.getInt("dept_id");
                    String designation = obj.getString("designation");
                    String tech_id = obj.getString("tech_id");
                    int room_id = obj.getInt("room_id");
                    String room_name = obj.getString("room_name");
                    String cust_name = obj.getString("cust_name");

                    checkDataFromLocal(req_id, purpose, date_req, date_approved,
                            req_status,cancel_rem, cust_id, dept_id, designation,
                            tech_id, room_id, cust_name, room_name);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void checkDataFromLocal(int req_id, String purpose, String date_req,
                                    String date_approved, String req_status, String cancel_rem,
                                    String cust_id, int dept_id, String designation, String tech_id, int room_id, String cust_name, String room_name) {
        SQLiteHelper db = new SQLiteHelper(this);
        Cursor c = db.getPeripheralRequests();
        int count = 0;
        if(c.moveToFirst()){
            do {
                if(c.getInt(c.getColumnIndex(db.REQ_ID)) == req_id){
                    //check kung may mga naupdate then notif
                    if(!req_status.equalsIgnoreCase(c.getString(c.getColumnIndex(db.REQ_STATUS)))){
                        //status
                        db.updatePeripheralRequests(req_id, 1, req_status);
                        //custodian notif
                        Log.e("STATUS Updated!", "UPDATED!");
                        if(SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("custodian")){
                            if(cust_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())){
                                showNotification("New Request Status Update: Your peripheral request last " + date_req + " for " +room_name+ " is " + req_status
                                        + ". Click here for more information.", "Peripherals Request Update");
                            }
                        }
                        }
                    if(!purpose.equalsIgnoreCase(c.getString(c.getColumnIndex(db.PURPOSE)))){
                        //purpose
                        db.updatePeripheralRequests(req_id, 2, purpose);
                    }
                    if(!date_req.equalsIgnoreCase(c.getString(c.getColumnIndex(db.DATE_OF_REQ)))){
                        //date requested
                        db.updatePeripheralRequests(req_id, 3, date_req);
                    }
                    if(!date_approved.equalsIgnoreCase(c.getString(c.getColumnIndex(db.DATE_APPROVED)))){
                        //date approved
                        db.updatePeripheralRequests(req_id, 4, date_approved);
                    }
                    if(!cancel_rem.equalsIgnoreCase(c.getString(c.getColumnIndex(db.CANCEL_REM)))){
                        //cancel remarks
                        db.updatePeripheralRequests(req_id, 5, cancel_rem);
                    }
                    break;
                }else{
                    count++;
                }
            }while(c.moveToNext());
            if(count == db.getReqPeripheralsCount()){
                //add
                //new request
                //technician notif
                if(SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(tech_id) ||
                        SharedPrefManager.getInstance(getApplicationContext()).getUserId().equalsIgnoreCase(cust_id))
                    db.addPeripheralsRequests(req_id, dept_id, cust_id, tech_id,designation, purpose
                    , date_req, date_approved, req_status, cancel_rem, room_id);

                if(SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("technician")){
                    if(tech_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())){
                        showNotification("Ms./Mr. " + cust_name + " of " +room_name+ " requests for peripherals. Click here for more information",
                                "New Peripheral Request");
                    }
                }
            }
        }else{
            db.addPeripheralsRequests(req_id, dept_id, cust_id, tech_id,designation, purpose
                    , date_req, date_approved, req_status, cancel_rem, room_id);
            if(SharedPrefManager.getInstance(getApplicationContext()).getUserRole().equalsIgnoreCase("technician")){
                if(tech_id.equalsIgnoreCase(SharedPrefManager.getInstance(getApplicationContext()).getUserId())){
                    if(req_status.equalsIgnoreCase("Pending"))
                        showNotification("Ms./Mr. " + cust_name + " of " +room_name+ " requests for peripherals. Click here for more information",
                            "New Peripheral Request");
                }
            }
        }
    }

    private void showNotification(String contentText, String title) {
        //set message
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
        notificationBuilder.setContentText(title);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(notifCounter, notificationBuilder.build());
        notifCounter++;
    }
}
