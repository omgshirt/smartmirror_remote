package org.remote.smartmirror.smartmirror_remote;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;


public class ControllerActivity extends AppCompatActivity {

    public static final String TAG = "Remote";

    public static final String SERVER_STARTED = "server started";

    private FloatingActionButton FabConnectToServer;

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

    public class RemoteHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String command = msg.getData().getString("msg");
            Log.i(TAG, "received command :: " + command);
            assert command != null;
            if (command.equals(SERVER_STARTED)) {
                // wait until the server socket is built before registering
                registerNsdService();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller_activity);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // start NSD
        mUpdateHandler = new RemoteHandler();
        mRemoteConnection= new RemoteConnection(mUpdateHandler);
        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        //registerNsdService();


        //txtConnectionMessage = (TextView) findViewById(R.id.connection_message);
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
            }
        });

        FabConnectToServer = (FloatingActionButton) findViewById(R.id.show_peers);
        FabConnectToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToRemote();
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
        mRemoteConnection.tearDown();
        super.onDestroy();

    }

    // call helper to register
    public void registerNsdService() {
        // Register service
        if(mRemoteConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mRemoteConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }

    // connect to a server
    public void connectToRemote() {
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
        if (service != null) {
            Log.d(TAG, "Connecting to server :: " + service.toString());
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
