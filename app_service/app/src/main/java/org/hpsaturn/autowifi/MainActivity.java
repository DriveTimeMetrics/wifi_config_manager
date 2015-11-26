package org.hpsaturn.autowifi;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.hpsaturn.autowifi.System.Storage;
import org.hpsaturn.autowifi.service.StartStatusServiceReceiver;
import org.hpsaturn.autowifi.service.StatusScheduleReceiver;
import org.hpsaturn.autowifi.service.StatusService;

public class MainActivity extends AppCompatActivity {

    private static final boolean DEBUG = Config.DEBUG;
    private static final String TAG = MainActivity.class.getSimpleName();

    private StatusService mService;
    boolean mBound = false;
    private DataUpdateReceiver dataUpdateReceiver;
    private TextView mTvServerStatus;
    private FloatingActionButton mFabStartStopService;
    private TextView mTvServerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mTvServerStatus = (TextView) findViewById(R.id.tv_server_status);
        mTvServerData = (TextView) findViewById(R.id.tv_server_data);
        setSupportActionBar(toolbar);

        mFabStartStopService = (FloatingActionButton) findViewById(R.id.fab);
        mFabStartStopService.setOnClickListener(onFabClickListener);


        if(isMyServiceRunning())setStartServiceUI();
        else setStopServiceUI();

    }

    private void startMainService(){
		if(DEBUG)Log.i(TAG,"[MainActivity] startMainService");
        Intent intent= new Intent(this, StatusService.class);
        startService(intent);
		StatusScheduleReceiver.startScheduleService(this, Config.DEFAULT_INTERVAL);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

	private void stopMainService() {
        if(DEBUG)Log.i(TAG,"[MainActivity] stopMainService");
        StatusScheduleReceiver.stopSheduleService(this);
        stopService(new Intent(this, StatusService.class));
        setStopServiceUI();
        Storage.setStartService(this,false);
	}

    private void setStopServiceUI() {
        mFabStartStopService.setImageResource(R.drawable.ic_play_white_48dp);
        mTvServerStatus.setText(R.string.msg_server_stop);
        mTvServerData.setText("");
    }

    private void setStartServiceUI() {
        mFabStartStopService.setImageResource(R.drawable.ic_stop_white_48dp);
        mTvServerStatus.setText(R.string.msg_server_running);
    }

    public boolean isMyServiceRunning() {
        return Storage.isStartService(this);
    }

    private View.OnClickListener onFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(isMyServiceRunning())stopMainService();
            else startMainService();
        }
    };

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
            if(DEBUG)Log.i(TAG,"[MainActivity] service connected..");
			StatusService.StatusServiceBinder binder = (StatusService.StatusServiceBinder) service;
			mService = binder.getStatusService();
			mBound = true;
            updateData();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
            if(DEBUG)Log.i(TAG,"[MainActivity] service disconnected..");
			mBound = false;
		}
	};

    @Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
        if(DEBUG)Log.i(TAG,"[MainActivity] bindService..");
		Intent intent = new Intent(this, StatusService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

    @Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
        unregisterReceiver(dataUpdateReceiver);
	}

    @Override
    protected void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(StatusService.REFRESH_DATA_INTENT);
        registerReceiver(dataUpdateReceiver, intentFilter);
    }

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(StatusService.REFRESH_DATA_INTENT)) {
                if (DEBUG) Log.i(TAG, "[MainActivity] DataUpdateReceiver");
                setStartServiceUI();
                updateData();
            }
        }
    }

    private void updateData() {
        if(mTvServerData!=null&&mService!=null) mTvServerData.setText(mService.getLastData());

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


}
