package org.hpsaturn.autowifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final boolean DEBUG = Config.DEBUG;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SSID_NAME = "ConfigMeridianAP";

    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!isCurrentConnected()) buildDefaultHostpot();
        else getNetworkStatusVars();

        startConfigServer();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(onFabClickListener);
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

        if (DEBUG) Log.d(TAG, "enable wireless hostpot with ssid: " + SSID_NAME);

        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }

        WifiConfiguration netConfig = new WifiConfiguration();

        netConfig.SSID = SSID_NAME;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener onFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    };

}
