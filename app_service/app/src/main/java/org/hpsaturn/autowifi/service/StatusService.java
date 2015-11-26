package org.hpsaturn.autowifi.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.hpsaturn.autowifi.Config;
import org.hpsaturn.autowifi.NetUtils;
import org.hpsaturn.autowifi.System.Storage;
import org.hpsaturn.autowifi.models.ConfigVars;
import org.hpsaturn.autowifi.models.SetConfigRequest;
import org.hpsaturn.autowifi.models.StatusResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StatusService extends Service {

    public static final String TAG = StatusService.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private static final String SSID_AP_NAME = "ConfigMeridianAP";
    public static final String REFRESH_DATA_INTENT = "refresh_data";

    private static final String REQ_SET_CONFIG = "/setConfig";
    private static final String REQ_GET_CONFIG = "/getConfig";
    private static final String REQ_ROOT = "/";
    private static final int MAX_AP_TRY = 3;

    private final IBinder mBinder = new StatusServiceBinder();

    private WifiManager wifiManager;
    private String lastData = "waiting for data..";
    private String current_ssid;
    private String current_ip;
    private boolean inNewConfig;
    private String new_ssid;
    private String new_passwd;
    private int AP_TRY=0;


    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.i(TAG, "[MainService] === onCreate ===");
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        startConfigServer();
        Storage.setStartService(this, true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.i(TAG, "[MainService] === onStartCommand ===");

        if (!isCurrentConnected()&&!inNewConfig&&!isApOn(this)) buildDefaultHostpot();
        else if(inNewConfig)setNewConfig(new_ssid,new_passwd);

        if(!isCurrentConnected()&&!isApOn(this))AP_TRY++;

        getNetworkStatusVars();
        Storage.setStartService(this, true);

        return Service.START_STICKY;
    }

    private void processRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

        if (DEBUG) Log.d(TAG, "ConfigServer: onRequest");
        if (DEBUG) Log.d(TAG, "request: " + request.toString());
        if (DEBUG) Log.d(TAG, "method: " + request.getMethod());
        if (DEBUG) Log.d(TAG, "path  : " + request.getPath());
        if (DEBUG) Log.d(TAG, "query : " + request.getQuery());
        if (DEBUG) Log.d(TAG, "headers : " + request.getHeaders());

        SetConfigRequest req = new Gson().fromJson("" + request.getQuery(), SetConfigRequest.class);
        if (request.getPath().equals(REQ_SET_CONFIG) && req.ssid != null && req.passwd != null) {

            String ssid = req.ssid.get(0);
            String passwd = req.passwd.get(0);

            if (DEBUG) Log.i(TAG, "new config=> ssid:" + ssid + " passwd:" + passwd);
            response.send(new Gson().toJson(StatusResponse.getSuccessResponse()));

            setNewConfig(ssid, passwd);
        } else if (request.getPath().equals(REQ_GET_CONFIG)) {
            ConfigVars respConfig = new ConfigVars();
            respConfig.ssid = current_ssid;
            respConfig.ip = current_ip;
            response.send(new Gson().toJson(respConfig));
        } else
            response.send(new Gson().toJson(StatusResponse.getBadResponse()));

    }

    private void setNewConfig(String ssid, String passwd) {

        this.inNewConfig=true;
        this.new_ssid=ssid;
        this.new_passwd=passwd;

        if (DEBUG) Log.d(TAG, "disable config AP..");
        configApState(this);
        if (DEBUG) Log.d(TAG, "setNewConfig..");

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";
        conf.preSharedKey = "\""+ passwd +"\"";

        int netId = wifiManager.addNetwork(conf);
        if (DEBUG) Log.d(TAG, "WiFi disconnect..");
        wifiManager.disconnect();
        if (DEBUG) Log.d(TAG, "WiFi enable network..");
        wifiManager.enableNetwork(netId, true);
        if (DEBUG) Log.d(TAG, "WiFi reconnect..");
        wifiManager.reconnect();
        wifiManager.setWifiEnabled(true);
//        this.inNewConfig=false;
        AP_TRY=0; //restet AP create delay
    }



    //check whether wifi hotspot on or off
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {
        }
        return false;
    }

    // toggle wifi hotspot on or off
    public static boolean configApState(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it off
            if (isApOn(context)) {
                wifimanager.setWifiEnabled(false);
            }
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wificonfiguration, !isApOn(context));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void buildDefaultHostpot() {

        if (DEBUG) Log.d(TAG, "enable wireless hostpot with current_ssid: " + SSID_AP_NAME);
        AP_TRY=0; //restet AP create delay

        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }

        WifiConfiguration netConfig = new WifiConfiguration();

        netConfig.SSID = SSID_AP_NAME;
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        try {
            Method setWifiApMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus = (Boolean) setWifiApMethod.invoke(wifiManager, netConfig, true);

            Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
            while (!(Boolean) isWifiApEnabledmethod.invoke(wifiManager)) {
            }
            Method getWifiApStateMethod = wifiManager.getClass().getMethod("getWifiApState");
            int apstate = (Integer) getWifiApStateMethod.invoke(wifiManager);
            Method getWifiApConfigurationMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            netConfig = (WifiConfiguration) getWifiApConfigurationMethod.invoke(wifiManager);
            if (DEBUG)
                Log.e("CLIENT", "\nSSID:" + netConfig.SSID + "\nPassword:" + netConfig.preSharedKey + "\n");

        } catch (Exception e) {
            if (DEBUG) Log.e(this.getClass().toString(), "", e);

        }
    }

    private void getNetworkStatusVars() {
        current_ssid = NetUtils.getSSID(this).toString();
        current_ip = NetUtils.getIPAddress(this).toString();
        if (DEBUG) Log.d(TAG, "current_ssid: " + current_ssid);
        if (DEBUG) Log.d(TAG, "current_ip: " + current_ip);
        if (DEBUG) Log.d(TAG, "MAC Address wlan0: " + NetUtils.getMACAddress("wlan0"));
        if (DEBUG) Log.d(TAG, "MAC Address eth0: " + NetUtils.getMACAddress("eth0"));
        if (DEBUG) Log.d(TAG, "current_ip IPV4: " + NetUtils.getIPAddress(true));
        if (DEBUG) Log.d(TAG, "current_ip: IPV6 " + NetUtils.getIPAddress(false));

        lastData = "\ncurrent_ip:" + NetUtils.getIPAddress(this) + "\ncurrent_ssid:" + NetUtils.getSSID(this);
        sendBroadcast(new Intent(StatusService.REFRESH_DATA_INTENT));
    }

    public boolean isCurrentConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(isConnected)inNewConfig=false;
        return isConnected;
    }

    private void startConfigServer() {

        if (DEBUG) Log.d(TAG, "startConfigServer..");
        AsyncHttpServer server = new AsyncHttpServer();

        List<WebSocket> _sockets = new ArrayList<WebSocket>();

        server.get(REQ_SET_CONFIG, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                processRequest(request, response);
            }
        });

        server.get(REQ_GET_CONFIG, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                processRequest(request, response);
            }
        });

        server.get(REQ_ROOT, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                processRequest(request, response);
            }
        });

        server.setErrorCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (DEBUG) Log.e(TAG, "ConfigServer: setErrorCallback: " + ex.getMessage());
            }
        });

        // listen on port 5000
        server.listen(5000);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public String getLastData() {
        return lastData;
    }

    public class StatusServiceBinder extends Binder {
        public StatusService getStatusService() {
            return StatusService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
