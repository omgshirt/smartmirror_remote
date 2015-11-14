package org.remote.smartmirror.smartmirror_remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ControllerActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener,
    WifiP2pManager.ConnectionInfoListener {

    private FloatingActionButton fabShowPeers;
    private TextView txtConnectionMessage;

    private WifiP2pManager mWifiManager;
    private WifiP2pManager.Channel mWifiChannel;
    private BroadcastReceiver mWifiReceiver;
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

    public static final String TITLE = "SmartRemote";
    public static final String TITLE_CONNECTED = "SmartRemote - Connected";
    public static final String TITLE_DISCONNECTED = "SmartRemote - Disconnected";
    public static final String TITLE_NOT_RESPONDING = "SmartRemote - Not Responding";

    private final int PORT = 8888;           // port to communicate on
    private String mOwnerIP;                   // group owner's IP
    private final int SOCKET_TIMEOUT = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                sendCommandToMirror(getResources().getString(R.string.sleep));
            }
        });
        btnWake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommandToMirror(getResources().getString(R.string.wake));
            }
        });

        // initialize ActionList
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mActionList);
        lstActionList = (ListView)findViewById(R.id.mirror_action_list);
        lstActionList.setAdapter(actionAdapter);
        lstActionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                sendCommandToMirror(mActionList[position]);
            }
        });

        // initialize PeerList
        mPeerList = new ArrayList<>();
        ArrayAdapter<String> peerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mPeerList);
        lstPeerList = (ListView)findViewById(R.id.peer_list);
        lstPeerList.setAdapter(peerAdapter);
        lstPeerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectToPeer(mWifiDeviceList.get(position));
            }
        });

        // Initialize Wifi
        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), null);
        mWifiReceiver = new WiFiDirectBroadcastReceiver(mWifiManager, mWifiChannel, this);

        // Initialize intent filter
        mWifiIntentFilter = new IntentFilter();
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Now that the manager is initialized, see if there are any peers
        fabShowPeers = (FloatingActionButton) findViewById(R.id.show_peers);
        fabShowPeers.setOnClickListener(new View.OnClickListener() {
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
    }

    // show PeerList, hide Actions
    public void showPeers(){
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
        stopWifiHeartbeat();
        registerReceiver(mWifiReceiver, mWifiIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        startWifiHeartbeat();
        unregisterReceiver(mWifiReceiver);
    }

    @Override
    protected void onDestroy() {
        stopWifiHeartbeat();
        wifiHeartbeat = null;
        super.onDestroy();

    }

    // ------------------------------- WifiP2P ---------------------------------

    // calls the P2pManager to refresh peer list
    private void discoverPeers() {
        txtConnectionMessage.setText(R.string.scanning);
        mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.i("discoverPeers", "successful");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i("discoverPeers", "failed: " + reasonCode);
            }
        });
    }

    // Interface passes back a device list when the peer list changes, or discovery is successful
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        // Set device list and update the peer list
        mWifiDeviceList = new ArrayList<>(peers.getDeviceList());
        mPeerList.clear();
        for(WifiP2pDevice device: mWifiDeviceList) {
            mPeerList.add(device.deviceName);
        }

        txtConnectionMessage.setText("Available Devices");
        // update the peer list adapter
        ( (BaseAdapter)lstPeerList.getAdapter() ).notifyDataSetChanged();
        Log.i("peers", mPeerList.toString());
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        mOwnerIP = info.groupOwnerAddress.getHostAddress();

        if (info.groupFormed && info.isGroupOwner) {
            //Log.i("Wifi", " is group owner");
        } else if (info.groupFormed) {
            //Log.i("Wifi", "not group owner");
            Log.i("Wifi", "testing connection");
            sendCommandToMirror("test");
        }
    }

    // OnStop, start a thread that keeps the wifip2p connection alive by pinging every 60 seconds
    private void startWifiHeartbeat() {
        ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);

        final Runnable heartbeatTask = new Runnable() {
            @Override
            public void run() {
                discoverPeers();
                Log.i("Wifi", "Heartbeat: discoverPeers()" );
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

    // ---------------------------------- Connect and send command -----------------------------

    public void disconnect() {
        mWifiManager.removeGroup(mWifiChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ControllerActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.d("Wifi", "Disconnect failure. Reason:" + reason);
            }
        });
    }

    // connect to the selected Wifi device
    public void connectToPeer(WifiP2pDevice device) {
        //obtain a peer from the WifiP2pDeviceList
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        Log.d("Wifi", "address:"  + config.deviceAddress);
        config.groupOwnerIntent = 0;              // don't make this remote the owner
        mWifiManager.connect(mWifiChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
                Toast.makeText(ControllerActivity.this, getResources().getString(R.string.wifi_connected),
                        Toast.LENGTH_SHORT).show();
                showModuleList();
                Log.i("Wifi", "testing connection");
                sendCommandToMirror("test");
                // check if connection is complete
                /*
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo.isConnected()) {
                    mWifiManager.requestConnectionInfo(mWifiChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            // get the group owner's IP
                            try {
                            mOwnerIP = info.groupOwnerAddress.getHostAddress();
                            } catch(Exception e) {
                                Log.e("WifiP2pInfo", "error");
                                e.printStackTrace();
                            }
                        }
                    });
                }*/
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Toast.makeText(ControllerActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                Log.i("connectToPeer", "connection failed reason: " + reason);
                getActionBar().setTitle(TITLE);
            }
        });
    }

    public void sendCommandToMirror(String command) {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo.isConnected() && mOwnerIP != null) {
            new SendCommandTask().execute(command);
        }
    }

    // this will create a background task, connect to the server and attempt to send the command string
    private class SendCommandTask extends AsyncTask<String, Void, String> {

        String titleMsg;

        @Override
        protected String doInBackground(String... commands) {
            Socket socket = new Socket();
            try {
                /**
                 * Create a client socket with the host,
                 * port, and timeout information.
                 */

                socket.bind(null);
                socket.connect((new InetSocketAddress(mOwnerIP, PORT)), SOCKET_TIMEOUT);
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(commands[0]);
                oos.close();
                os.close();
                titleMsg = TITLE_CONNECTED;
            } catch (IOException e) {
                Log.e("Socket", e.getMessage());
                titleMsg = TITLE_NOT_RESPONDING;
            }

            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */
            finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            //catch logic
                        }
                    }
                }
            }
            return commands[0];
        }

        protected void onPostExecute(String command) {
            getSupportActionBar().setTitle(titleMsg);
            if (titleMsg.equals(TITLE_NOT_RESPONDING))
            {
                Toast.makeText(ControllerActivity.this,
                        getResources().getString(R.string.not_responding), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
