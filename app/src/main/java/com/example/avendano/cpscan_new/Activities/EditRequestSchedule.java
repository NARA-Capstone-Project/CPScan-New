package com.example.avendano.cpscan_new.Activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.avendano.cpscan_new.DatePicker;
import com.example.avendano.cpscan_new.Network_Handler.AppConfig;
import com.example.avendano.cpscan_new.Network_Handler.RequestQueueHandler;
import com.example.avendano.cpscan_new.Network_Handler.VolleyCallback;
import com.example.avendano.cpscan_new.Network_Handler.VolleyRequestSingleton;
import com.example.avendano.cpscan_new.R;
import com.example.avendano.cpscan_new.TimePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;

/**
 * Created by Avendano on 27 Mar 2018.
 */

public class EditRequestSchedule extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private int id, room_pc_id, r_id; // r_id ffor viewpc
    String type, image_path;
    //    SQLiteHandler db;
    EditText message;
    TextView date, time, peripherals, label;
    Spinner date_type;
    Toolbar toolbar;
    AlertDialog progress;
    ImageView photo1;
    String tech_id, status;
    private final int IMG_REQUEST = 1, CAMERA_REQUEST = 0;
    private Bitmap bitmap;
    boolean setImage = false, dateSet = false;
    VolleyRequestSingleton volley;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_date_time);

        volley = new VolleyRequestSingleton(this);

        id = getIntent().getIntExtra("id", 0);  //req_id
        room_pc_id = getIntent().getIntExtra("room_pc_id", 0);  //comp or room id
        type = getIntent().getStringExtra("type");
        status = getIntent().getStringExtra("status");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Edit Request");

        progress = new SpotsDialog(this, "Loading...");
        progress.setCancelable(false);
        if (progress != null) {
            progress.dismiss();
        }
        progress.show();
//        db = new SQLiteHandler(this);
        message = (EditText) findViewById(R.id.message);
        date = (TextView) findViewById(R.id.custom_date);
        time = (TextView) findViewById(R.id.custom_time);
        date_type = (Spinner) findViewById(R.id.date);
        String[] item = new String[]{"Anytime", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, item);
        date_type.setAdapter(adapter);
        date_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: {
                        dateSet = false;
                        date.setVisibility(View.GONE);
                        break;
                    }
                    case 1: {
                        date.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //date picker
                DialogFragment datePicker = new DatePicker();
                datePicker.show(getSupportFragmentManager(), "date picker");
            }
        });
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //time picker
                DialogFragment timePicker = new TimePicker();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        //FOR REPAIR

        photo1 = (ImageView) findViewById(R.id.photo1);
        peripherals = (TextView) findViewById(R.id.peripherals);

        if (type.equalsIgnoreCase("repair")) {

            label = (TextView) findViewById(R.id.label);
            label.setVisibility(View.VISIBLE);
            peripherals.setVisibility(View.VISIBLE);
        }

        peripherals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                peripheralsDialog();
            }
        });

        image_path = "";

        photo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //photo
                showDialog();
            }
        });

        setDetails();

    }

    private void showDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Select Action...");
        String[] items = {"Camera", "Gallery"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        //capture image
                        captureImage();
                        break;
                    }
                    case 1: {
                        selectImage();
                        break;
                    }
                }
            }
        });
        android.support.v7.app.AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri path = data.getData();
            if (requestCode == CAMERA_REQUEST) {
                Bundle bundle = data.getExtras();
                bitmap = (Bitmap) bundle.get("data");
                photo1.setImageBitmap(bitmap);
                photo1.setBackgroundResource(0);
                setImage = true;
            } else if (requestCode == IMG_REQUEST) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                    photo1.setImageBitmap(bitmap);
                    photo1.setBackgroundResource(0);
                    setImage = true;
                } catch (Exception e) {
                    Log.e("REPAIR123", "bitmap: " + e.getMessage());
                }
            }
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMG_REQUEST);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void peripheralsDialog() {
        final ArrayList<String> checked = new ArrayList<>();
        final String[] items = {"Monitor", "Keyboard", "Mouse", "System Unit"};
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Select Peripherals...")
                .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked)
                            checked.add(items[which]);
                        else if (checked.contains(items[which]))
                            checked.remove(items[which]);
                    }
                })
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setCancelable(false);
        final android.support.v7.app.AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (checked.size() > 0) {
                            String content = "";
                            for (int i = 0; i < checked.size(); i++) {
                                content = "" + content + " > " + checked.get(i).toString().trim();
                            }
                            peripherals.setText(content);
                            peripherals.setVisibility(View.VISIBLE);
                            alert.dismiss();
                        } else {
                            Toast.makeText(EditRequestSchedule.this, "No selected peripherals", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        alert.show();
    }

    private void setDetails() {
        //set time and date image(kung repair)
        //check kung repair or inventory
        ///if cant connect sa server babalik tas toast ng cant connect
        class loadDetails extends AsyncTask<Void, Void, Void> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (type.equalsIgnoreCase("repair")) {
                    peripherals.setVisibility(View.VISIBLE);
                    photo1.setVisibility(View.VISIBLE);
                    label.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected Void doInBackground(Void... voids) {
                if (type.equalsIgnoreCase("inventory"))
                    getReqInvDetails();
                else if (type.equalsIgnoreCase("repair")) {
                    getReqRepDetails();
                }
                return null;
            }
        }

        new loadDetails().execute();
    }

    private void getReqRepDetails() {
        StringRequest str = new StringRequest(Request.Method.GET
                , AppConfig.GET_REPAIR_REQ
                , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int req_id = obj.getInt("req_id");
                        String date = obj.getString("set_date");
                        String time = obj.getString("set_time");
                        String msg = obj.getString("msg");
                        String req_details = obj.getString("req_details");
                        String path = obj.getString("image");
                        String req_status = obj.getString("req_status");

                        if (req_id == id) {
                            if (!date.equalsIgnoreCase("anytime")) {
                                date_type.setSelection(1);
                                EditRequestSchedule.this.date.setText(date);
                                dateSet = true;
                            }
                            tech_id = obj.getString("tech_id");
                            message.setText(msg);
                            peripherals.setText(req_details);
                            if (obj.isNull("image"))
                                image_path = "";
                            else
                                image_path = AppConfig.ROOT + path;
                            EditRequestSchedule.this.time.setText(time);
                            Log.e("PATH", path + " IMAGE: " + image_path);
                            break;
                        }

                    }
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("IMAGE", image_path);
                            if (image_path.length() != 0)
                                getImage();
                            progress.dismiss();
                        }
                    }, 2000);

                } catch (JSONException e) {
                    progress.dismiss();
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();
                Toast.makeText(EditRequestSchedule.this, "Can't connect to the server", Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueueHandler.getInstance(EditRequestSchedule.this).addToRequestQueue(str);

    }

    private void getImage() {
        ImageRequest req = new ImageRequest(image_path, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                photo1.setImageBitmap(response);
                photo1.setBackgroundResource(0);
            }
        }, 0, 0, ImageView.ScaleType.CENTER_CROP, null,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(EditRequestSchedule.this, "Something went wrong " +
                                "in loading image", Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
                });

        RequestQueueHandler.getInstance(EditRequestSchedule.this).addToRequestQueue(req);
    }

    private void getReqInvDetails() {
        volley.sendStringRequestGet(AppConfig.GET_INVENTORY_REQ, new VolleyCallback() {
            @Override
            public void onSuccessResponse(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int req_id = obj.getInt("req_id");
                        String date = obj.getString("date");
                        String time = obj.getString("time");
                        String msg = obj.getString("msg");
                        String req_status = obj.getString("req_status");

                        if (req_id == id) {
                            if (!date.equalsIgnoreCase("anytime")) {
                                date_type.setSelection(1);
                                EditRequestSchedule.this.date.setText(date);
                                dateSet = true;
                            }
                            tech_id = obj.getString("technician");
                            EditRequestSchedule.this.time.setText(time);
                            message.setText(msg);
                            break;
                        }

                    }
                    progress.dismiss();

                } catch (JSONException e) {
                    progress.dismiss();
                    e.printStackTrace();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();
                error.printStackTrace();
            }
        });
    }

    private boolean checkSchedule() {
        if (date_type.getSelectedItem().toString().equalsIgnoreCase("custom") &&
                date.getText().toString().equalsIgnoreCase("yyyy-mm-dd")) {
            date.setError("Set date!");
            return false;
        } else if (date_type.getSelectedItem().toString().equalsIgnoreCase("custom")) {
            boolean r = false;
            try {
                Date today = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                Date set_date = new SimpleDateFormat("yyyy-MM-dd").parse(date.getText().toString());

                if (set_date.before(today)) {
                    date.setError("Set Date!");
                    r = false;
                } else {
                    r = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return r;
        } else if (time.getText().toString().equalsIgnoreCase("HH:mm:ss")) {
            time.setError("Set date!");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        Date getdate = c.getTime();
        dateSet = true;
        String dateString = new SimpleDateFormat("yyyy-MM-dd").format(getdate);
        date.setText(dateString);
    }

    @Override
    public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        Date getTime = c.getTime();
        String timeString = new SimpleDateFormat("HH:mm:ss").format(getTime);
        //check time
        Date pickedTime;
        try {
            pickedTime = new SimpleDateFormat("HH:mm:ss").parse(timeString);

            Date am = new SimpleDateFormat("HH:mm:ss").parse("07:59:00");
            Date pm = new SimpleDateFormat("HH:mm:ss").parse("17:01:00");
//            Log.e("TIME", "GETTIME: " + pickedTime);
//            Log.e("TIME", "AM: " + am);
//            Log.e("TIME", "PM: " + pm);
//            Log.e("TIME", "TIME B4 AM: " + pickedTime.before(am));
//            Log.e("TIME", "AM B4 TIME: " + am.before(pickedTime));
//            Log.e("TIME", "TIME after AM: " + pickedTime.after(am));
//            Log.e("TIME", "AM after TIME: " + am.after(pickedTime));
//            Log.e("TIME", "TIME B4 PM: " + pickedTime.before(pm));
//            Log.e("TIME", "PM B4 TIME: " + pm.before(pickedTime));
//            Log.e("TIME", "TIME after PM: " + pickedTime.after(pm));
//            Log.e("TIME", "PM after TIME: " + pm.after(pickedTime));
            if (pickedTime.after(am) && pickedTime.before(pm)) {
                if (dateSet)    //hindi anytime ung time
                {
                    checkTime(timeString, date.getText().toString());
                } else
                    time.setText(new SimpleDateFormat("HH:mm:ss").format(pickedTime));
            } else {
                Toast.makeText(this, "Pick time between 8AM and 5PM", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkTime(final String pickedTime, final String s) {
        if (type.equalsIgnoreCase("inventory")) {
            volley.sendStringRequestGet(AppConfig.GET_INVENTORY_REQ, new VolleyCallback() {
                @Override
                public void onSuccessResponse(String response) {
                    try {
                        JSONArray array = new JSONArray(response);
                        int count = 0;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String t_id = obj.getString("technician");
                            //check status kung ignored, cancel done
                            if (!obj.getString("req_status").equalsIgnoreCase("Ignored") ||
                                    obj.getString("req_status").equalsIgnoreCase("Cancel") ||
                                    obj.getString("req_status").equalsIgnoreCase("Done")) {
                                if (t_id.equals(tech_id) && obj.getInt("req_id") != id) {
                                    //check date
                                    if (s.equals(obj.getString("date")) || obj.getString("date").equalsIgnoreCase("anytime")) {

                                        //check time
                                        Date set_time = new SimpleDateFormat("HH:mm").parse(obj.getString("time"));
                                        Date picked = new SimpleDateFormat("HH:mm").parse(pickedTime);
                                        //plus one hour
                                        Calendar cal1 = Calendar.getInstance();//set time
                                        cal1.setTime(set_time);
                                        cal1.add(Calendar.HOUR_OF_DAY, 1);

                                        Calendar cal2 = Calendar.getInstance(); //pick time
                                        cal2.setTime(picked);
                                        cal2.add(Calendar.HOUR_OF_DAY, 1);
                                        if ((picked.after(set_time) && picked.before(cal1.getTime())) || (picked.equals(set_time) || picked.equals(cal1.getTime()))
                                                || (cal2.getTime().after(set_time) && cal2.getTime().before(cal1.getTime())) || (cal2.getTime().equals(set_time) || cal2.getTime().equals(cal1.getTime()))) {
                                            Toast.makeText(EditRequestSchedule.this, "Picked time is not available", Toast.LENGTH_SHORT).show();
                                            DialogFragment timePicker = new TimePicker();
                                            timePicker.show(getSupportFragmentManager(), "time picker");
                                            break;
                                        } else {
                                            count++;
                                        }
                                    }
                                }
                            }
                        }
                        if (count == array.length()) {
                            time.setText(pickedTime);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(EditRequestSchedule.this, "Can't connect to the server", Toast.LENGTH_SHORT).show();
                }
            });
        } else { // repair
            volley.sendStringRequestGet(AppConfig.GET_REPAIR_REQ, new VolleyCallback() {
                @Override
                public void onSuccessResponse(String response) {
                    try {
                        JSONArray array = new JSONArray(response);
                        int count = 0;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String t_id = obj.getString("tech_id");
                            if (!obj.getString("req_status").equalsIgnoreCase("Ignored") ||
                                    obj.getString("req_status").equalsIgnoreCase("Cancel") ||
                                    obj.getString("req_status").equalsIgnoreCase("Done")) {
                                if (t_id.equals(tech_id) && obj.getInt("req_id") != id) {
                                    //check date
                                    if (s.equals(obj.getString("set_date")) || obj.getString("set_date").equalsIgnoreCase("anytime")) {
                                        //check time
                                        Date set_time = new SimpleDateFormat("HH:mm").parse(obj.getString("set_time"));
                                        Date picked = new SimpleDateFormat("HH:mm").parse(pickedTime);
                                        //plus one hour
                                        Calendar cal1 = Calendar.getInstance();//set time
                                        cal1.setTime(set_time);
                                        cal1.add(Calendar.HOUR_OF_DAY, 1);

                                        Calendar cal2 = Calendar.getInstance(); //pick time
                                        cal2.setTime(picked);
                                        cal2.add(Calendar.HOUR_OF_DAY, 1);
                                        if ((picked.after(set_time) && picked.before(cal1.getTime())) || (picked.equals(set_time) || picked.equals(cal1.getTime()))
                                                || (cal2.getTime().after(set_time) && cal2.getTime().before(cal1.getTime())) || (cal2.getTime().equals(set_time) || cal2.getTime().equals(cal1.getTime()))) {
                                            Toast.makeText(EditRequestSchedule.this, "Picked time is not available", Toast.LENGTH_SHORT).show();
                                            DialogFragment timePicker = new TimePicker();
                                            timePicker.show(getSupportFragmentManager(), "time picker");
                                            break;
                                        } else {
                                            count++;
                                        }
                                    }
                                }
                            }

                        }
                        if (count == array.length()) {
                            time.setText(pickedTime);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(EditRequestSchedule.this, "Can't connect to the server", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.assess_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save: {
                if (progress != null) {
                    progress.dismiss();
                }
                progress.show();
                if (checkSchedule()) {
                    Log.e("STATUS", "jkl " + status);
                    new editRequests().execute();
                } else {
                    progress.dismiss();
                }
                Log.w("SEND REQUEST", "User request for inventory");
                break;
            }
            case R.id.cancel: {
                finish();
                break;
            }
        }

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
//        db.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private class editRequests extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (type.equalsIgnoreCase("inventory"))
                editRequestInventory();
            else if (type.equalsIgnoreCase("repair")) {
                //if resched missed or ignored
                editRequestRepair();
            }
            return null;
        }
    }

    private void editRequestRepair() {

        String setDate = "";
        String setTime = "";
        final String getMsg = message.getText().toString().trim();
        if (date_type.getSelectedItem().toString().equalsIgnoreCase("anytime"))
            setDate = "Anytime";
        else
            setDate = date.getText().toString();

        setTime = time.getText().toString();

        final String finalSetDate = setDate;
        final String finalSetTime = setTime;

        String rep_msg = peripherals.getText().toString().trim();

        String image = "";
        if (setImage)
            image = imageToString();

        final String finalImage = image;
        final String finalRep_msg = rep_msg;
        StringRequest str = new StringRequest(Request.Method.POST
                , AppConfig.URL_UPDATE_REPAIR_REQUEST
                , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("ERROR", response);
                progress.dismiss();
                try {
                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        //update sqlite
                        Toast.makeText(EditRequestSchedule.this, "Request Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditRequestSchedule.this, "An error occured, please try again later", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(EditRequestSchedule.this, "An error occured, please try again later", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();
                Toast.makeText(EditRequestSchedule.this, "Can't Connect to the server", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                //        missed(accepted ung stat pero hindi nagawa ayon s sched) need to resched(pending pa rin kaya lng hindi napansin or walang action na ginawa) pending ignored
                if (status.equalsIgnoreCase("ignored") ||
                        status.equalsIgnoreCase("missed") ||
                        status.equalsIgnoreCase("need to resched"))
                    params.put("status", "pending");
                else
                    params.put("status", "");
                params.put("msg", getMsg);
                params.put("date", finalSetDate);
                params.put("time", finalSetTime);
                params.put("rep_details", finalRep_msg);
                params.put("old_path", image_path);
                params.put("image", finalImage);
                params.put("req_id", String.valueOf(id));
                return params;
            }
        };
        RequestQueueHandler.getInstance(EditRequestSchedule.this).addToRequestQueue(str);
    }

//    private void updateReqRepDetails(int id, String finalSetDate, String finalSetTime, String getMsg, String finalRep_msg) {
//        db.updateReqRepairDetails(id, finalSetDate, finalSetTime, getMsg, finalRep_msg);
//    }


    private String imageToString() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] imgBytes = outputStream.toByteArray();

        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private void editRequestInventory() {
        String setDate = "";
        String setTime = "";
        final String getMsg = message.getText().toString().trim();
        if (date_type.getSelectedItem().toString().equalsIgnoreCase("anytime"))
            setDate = "Anytime";
        else
            setDate = date.getText().toString();

        setTime = time.getText().toString();
        final String query;
//        missed(accepted ung stat pero hindi nagawa ayon s sched) need to resched(pending pa rin kaya lng hindi napansin or walang action na ginawa) pending ignored
        if (status.equalsIgnoreCase("pending")) {
            query = "UPDATE request_inventory SET date = '" + setDate + "', time ='"
                    + setTime + "', message = '" + getMsg + "' WHERE req_id = ?";
        } else { /// missed and ignored, need to resched
            query = "UPDATE request_inventory SET date = '" + setDate + "', time ='"
                    + setTime + "', message = '" + getMsg + "', req_status = 'Pending' WHERE req_id = ?";
        }

        Log.e("query", query);
        StringRequest str = new StringRequest(Request.Method.POST
                , AppConfig.URL_UPDATE_SCHEDULE
                , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("ERROR", response);
                progress.dismiss();
                try {

                    JSONObject obj = new JSONObject(response);
                    if (!obj.getBoolean("error")) {
                        //update sqlite
//                        updateSqliteInventory(finalSetDate, finalSetTime, getMsg);
                        Toast.makeText(EditRequestSchedule.this, "Request Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditRequestSchedule.this, "An error occured, please try again later", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();
                Toast.makeText(EditRequestSchedule.this, "Can't Connect to the server", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("query", query);
                params.put("id", String.valueOf(id));
                return params;
            }
        };
        RequestQueueHandler.getInstance(EditRequestSchedule.this).addToRequestQueue(str);
    }

//    private void updateSqliteInventory(String finalSetDate, String finalSetTime, String getMsg) {
//        db.updateReqInventoryDetails(id, finalSetDate, finalSetTime, getMsg);
//    }
}
