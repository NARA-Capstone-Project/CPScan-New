package com.example.avendano.cpscan_new.Network_Handler;

/**
 * Created by Avendano on 6 Mar 2018.
 */

public class AppConfig {

    public static String ROOT_URL = "https://cp-scan.000webhostapp.com/android_api/"; //online
    public static String ROOT = "https://cp-scan.000webhostapp.com/api/"; //online
//    public static String ROOT_URL = "http://192.168.43.21/android_api/";
//    public static String ROOT = "http://192.168.43.21/api/";

    public static String URL_REQUEST_ACCOUNT = ROOT_URL + "cict_request_account.php"; //request an account
    public static String URL_EDIT_PROFILE = ROOT + "cict_edit_user.php"; // edit profile
    public static String URL_REACTIVATE_ACC = ROOT_URL + "cict_reactivate_account.php";
    public static String URL_DEACTIVATE_ACC = ROOT_URL + "cict_deactivate_user.php";
    public static String URL_CANCEL_SCHEDULE = ROOT_URL + "cict_cancel_schedule.php";
    public static String URL_CHECK_LAST_REPAIR_REQUEST = ROOT_URL + "cict_repair_request_details.php";

    public static String LOGIN = ROOT + "login.php";
    public static String URL_UPDATE_SCHEDULE = ROOT + "cict_update_schedule.php";
    public static String URL_UPDATE_REPAIR_REQUEST = ROOT + "cict_update_repair.php";
    public static String GET_ROOMS = ROOT + "get_room.php";
    public static String SEARCH_ROOMS = ROOT + "search_room.php";
    public static String COUNT_REQ = ROOT + "count_requests.php";
    public static String GET_INVENTORY_REQ = ROOT + "get_inventory_requests.php";
    public static String GET_REPAIR_REQ = ROOT + "get_repair_requests.php";
    public static String GET_COMPUTERS = ROOT + "get_computers.php";
    public static String SAVE_INVENTORY = ROOT + "save_inventory.php";
    public static String SAVE_REPAIR = ROOT + "save_repair_report.php";
    public static String SAVE_REQ_INVENTORY = ROOT + "save_request_inventory.php";
    public static String SAVE_REQ_REPAIR = ROOT + "save_request_repair.php";
    public static String SAVE_REQ_PERIPHERALS = ROOT + "save_request_peripherals.php";
    public static String PENDING_INVENTORY = ROOT + "pending_request_inventory.php";
    public static String GET_INVENTORY_REPORTS = ROOT + "get_inventory_reports.php";
    public static String GET_INVENTORY_REPORTS_DETAILS = ROOT + "get_inventory_details.php";
    public static String GET_USER_INFO = ROOT + "get_user_info.php";
    public static String UPDATE_QUERY = ROOT + "update.php";
    public static String SAVE_SIGNATURE = ROOT + "save_signature.php";
    public static String SEND_SMS = ROOT + "send_alert_sms.php";
    public static String GET_PERIPHERALS = ROOT + "get_peripheral_requests.php";
    public static String GET_PERIPHERALS_DETAILS = ROOT + "get_peripheral_details.php";
    public static String EDIT_PERIPHERALS = ROOT + "update_request_peripherals.php";
    public static String ISSUE_PERIPHERALS = ROOT + "issue_peripherals.php";
    public static String SEND_SERIAL = ROOT + "sendQRSerial.php";
    public static String NOTIF_COUNTER = ROOT + "notif_counter.php";
    public static String SCAN = ROOT + "scanning.php";
}
