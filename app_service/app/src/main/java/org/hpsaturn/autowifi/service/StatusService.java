package org.hpsaturn.autowifi.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.hpsaturn.autowifi.Config;
import org.hpsaturn.autowifi.NetUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StatusService extends Service {

	public static final String TAG = StatusService.class.getSimpleName();
	private static final boolean DEBUG = Config.DEBUG;

    private static final String SSID_AP_NAME = "ConfigMeridianAP";

    private WifiManager wifiManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if(DEBUG) Log.i(TAG, "[MainService] === onCreate ===");
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if(DEBUG) Log.i(TAG, "[MainService] === onStartCommand ===");

        if (!isCurrentConnected()) buildDefaultHostpot();
        else getNetworkStatusVars();

        startConfigServer();

        return Service.START_STICKY;
    }

    private void startConfigServer() {

        if(DEBUG)Log.d(TAG,"startConfigServer..");
        AsyncHttpServer server = new AsyncHttpServer();

        List<WebSocket> _sockets = new ArrayList<WebSocket>();

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                if(DEBUG)Log.d(TAG,"ConfigServer: onRequest");
                response.send("Hello!!!");
            }

        });

        // listen on port 5000
        server.listen(5000);
    }

    private void getNetworkStatusVars() {
        if (DEBUG) Log.d(TAG, "essid: " + NetUtils.getSSID(this));
        if (DEBUG) Log.d(TAG, "ipaddress: " + NetUtils.getIPAddress(this));
        if (DEBUG) Log.d(TAG, "MAC Address wlan0: " + NetUtils.getMACAddress("wlan0"));
        if (DEBUG) Log.d(TAG, "MAC Address eth0: " + NetUtils.getMACAddress("eth0"));
        if (DEBUG) Log.d(TAG, "ipaddress IPV4: " + NetUtils.getIPAddress(true));
        if (DEBUG) Log.d(TAG, "ipaddress: IPV6 " + NetUtils.getIPAddress(false));
    }

    public boolean isCurrentConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public void buildDefaultHostpot() {

        if (DEBUG) Log.d(TAG, "enable wireless hostpot with ssid: " + SSID_AP_NAME);

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
            ;
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


    @Override
	public IBinder onBind(Intent arg0) {
		return null;
	}


}
