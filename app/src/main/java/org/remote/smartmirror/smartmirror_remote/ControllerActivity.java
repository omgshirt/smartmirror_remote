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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ControllerActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener{

    private FloatingActionButton fabShowPeers;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mWifiReceiver;
    private IntentFilter mIntentFilter;
    private ArrayList<WifiP2pDevice> mDeviceList;
    private ListView lstActionList;
    private ListView lstPeerList;
    private LinearLayout layPeerLayout;
    private String[] mActionList = {"Calendar",
                                    "Light",
                                    "News",
                                    "Sports",
                                    "Weather",
                                    "Settings"};

    private ArrayList<String> mPeerList;
    private final int PORT = 8888;           // port to communicate on
    private String mOwnerIP;                   // group owner's IP
    private final int SOCKET_TIMEOUT = 500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layPeerLayout = (LinearLayout) findViewById(R.id.peer_layout);

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
                connectToPeer(mDeviceList.get(position));
            }
        });

        // Initialize Wifi
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiReceiver = new WiFiDirectBroadcastRec(mManager, mChannel, this);

        // Initialize intent filter
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Now that the manager is initialized, see if there are any peers
        fabShowPeers = (FloatingActionButton) findViewById(R.id.show_peers);
        fabShowPeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layPeerLayout.getVisibility() == View.GONE) {
                    showPeers();
                    discoverPeers();
                } else {
                    showActions();
                }
            }
        });
        //discoverPeers();
    }

    // show PeerList, hide Actions
    public void showPeers(){
        layPeerLayout.setVisibility(View.VISIBLE);
        lstActionList.setVisibility(View.GONE);
    }

    // show Actions, hide PeerList
    public void showActions() {
        layPeerLayout.setVisibility(View.GONE);
        lstActionList.setVisibility(View.VISIBLE);
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

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiReceiver);
    }

    // calls the P2pManager to refresh peer list
    private void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i("discoverPeers", "successful");
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
        mDeviceList = new ArrayList<>(peers.getDeviceList());
        mPeerList.clear();
        for(WifiP2pDevice device: mDeviceList) {
            mPeerList.add(device.deviceName);
        }

        // update the peer list adapter
        ( (BaseAdapter)lstPeerList.getAdapter() ).notifyDataSetChanged();
        //Log.i("peers", mPeerList.toString());
    }

    // connect to the selected Wifi device
    public void connectToPeer(WifiP2pDevice device) {
        //obtain a peer from the WifiP2pDeviceList
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        //config.groupOwnerIntent = 0;              // don't make this remote the owner
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //success logic
                Log.i("connectToPeer", "connection successful");
                Toast.makeText(ControllerActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                showActions();
                // check if connection is complete
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                Log.i("networkInfo", networkInfo.toString());
                if (networkInfo.isConnected()) {
                    mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            // get the group owner's IP
                            try {
                            mOwnerIP = info.groupOwnerAddress.getHostAddress();
                            Log.i("Owner IP", mOwnerIP);
                            } catch(Exception e) {
                                Log.e("WifiP2pInfo", "error");
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Toast.makeText(ControllerActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                Log.i("connectToPeer", "connection failed");
            }
        });
    }

    private void sendCommandToMirror(String command) {
        new SendCommandTask().execute(command);
    }

    private class SendCommandTask extends AsyncTask<String, Void, String> {

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
                Log.i("SendCommandTask", commands[0]);

            } catch (IOException e) {
                e.printStackTrace();
            }

            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */ finally {
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
    }
}
