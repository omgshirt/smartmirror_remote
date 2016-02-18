package org.remote.smartmirror.smartmirror_remote;

import android.content.IntentFilter;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ControllerActivity extends AppCompatActivity {

    public static final String TAG = "remote";

    private FloatingActionButton FabConnectToServer;
    private TextView txtConnectionMessage;

    private IntentFilter mWifiIntentFilter;
    private ArrayList<WifiP2pDevice> mWifiDeviceList;
    private ScheduledFuture<?> wifiHeartbeat;
    private ArrayList<String> mPeerList;

    private ListView lstActionList;
    private ListView lstPeerList;
    private LinearLayout layPeerLayout;
    private LinearLayout layModuleLayout;
    private String[] mActionList;
    private Button btnSleep;
    private Button btnWake;

    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    RemoteConnection mRemoteConnection;

    protected static class RemoteHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String command = msg.getData().getString("msg");
            Log.i(TAG, "received command :: " + command);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // start NSD
        mUpdateHandler = new RemoteHandler();
        mRemoteConnection= new RemoteConnection(mUpdateHandler);
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        registerService();


        txtConnectionMessage = (TextView) findViewById(R.id.connection_message);
        mActionList = getResources().getStringArray(R.array.module_list);
        layPeerLayout = (LinearLayout) findViewById(R.id.peer_layout);
        layModuleLayout = (LinearLayout) findViewById(R.id.module_layout);

        // set up sleep and wake buttons
        btnSleep = (Button) findViewById(R.id.sleep_button);
        btnWake = (Button) findViewById(R.id.wake_button);
        btnSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToMirror(getResources().getString(R.string.sleep).toLowerCase());
            }
        });
        btnWake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToMirror(getResources().getString(R.string.wake).toLowerCase());
            }
        });

        // initialize ActionList
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mActionList);
        lstActionList = (ListView) findViewById(R.id.mirror_action_list);
        lstActionList.setAdapter(actionAdapter);
        lstActionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String command = mActionList[position].toLowerCase();
                sendCommandToMirror(command);
                Log.i("Action", command);
            }
        });

        // initialize PeerList
        /*
        mPeerList = new ArrayList<>();
        ArrayAdapter<String> peerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mPeerList);
        lstPeerList = (ListView) findViewById(R.id.peer_list);
        lstPeerList.setAdapter(peerAdapter);
        lstPeerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectToPeer(mWifiDeviceList.get(position));
            }
        });


        // Initialize intent filter
        mWifiIntentFilter = new IntentFilter();
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Now that the manager is initialized, see if there are any peers

        FabConnectToServer = (FloatingActionButton) findViewById(R.id.show_peers);
        FabConnectToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layPeerLayout.getVisibility() == View.GONE) {
                    showPeers();
                    discoverPeers();
                } else {
                    showModuleList();
                }
            }
        });
        */

        FabConnectToServer = (FloatingActionButton) findViewById(R.id.show_peers);
        FabConnectToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickConnect(view);
            }
        });
    }

    // show PeerList, hide Actions
    public void showPeers() {
        layPeerLayout.setVisibility(View.VISIBLE);
        layModuleLayout.setVisibility(View.GONE);
    }

    // show Actions, hide PeerList
    public void showModuleList() {
        layPeerLayout.setVisibility(View.GONE);
        layModuleLayout.setVisibility(View.VISIBLE);
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

    // --------------------------- Lifecycle ------------------------------

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        if (mRemoteConnection != null) {
            mRemoteConnection.tearDown();
        }
        super.onDestroy();

    }


    // OnStop, start a thread that keeps the wifip2p connection alive by pinging every 60 seconds
    private void startWifiHeartbeat() {
        ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);

        final Runnable heartbeatTask = new Runnable() {
            @Override
            public void run() {

                Log.i("Wifi", "Heartbeat: discoverPeers()");
            }
        };
        wifiHeartbeat = scheduler.scheduleAtFixedRate(heartbeatTask, 60, 60,
                TimeUnit.SECONDS);
    }

    // Stop the heartbeat thread
    public void stopWifiHeartbeat() {
        if (wifiHeartbeat != null) {
            wifiHeartbeat.cancel(true);
        }
    }

    // call helper to register
    public void registerService() {
        // Register service
        if(mRemoteConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mRemoteConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }

    // connect to a server
    public void clickConnect(View v) {
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
        if (service != null) {
            Log.d(TAG, "Connecting.");
            mRemoteConnection.connectToServer(service.getHost(),
                    service.getPort());
        } else {
            Log.d(TAG, "No service to connect to!");
        }
    }

    public void clickDiscover(View v) {
        mNsdHelper.discoverServices();
    }

    public void sendCommandToMirror(String command) {
        if (!command.isEmpty()) {
            mRemoteConnection.sendMessage(command);
        }
    }
}
