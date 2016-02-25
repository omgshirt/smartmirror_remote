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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class ControllerActivity extends AppCompatActivity {

    public static final String TAG = "Remote";

    public static final String SERVER_STARTED = "server started";

    private HashMap<String,NsdServiceInfo> mServiceMap;
    ArrayAdapter<String> peerAdapter;

    private FloatingActionButton fabShowPeers;
    private ListView lstActionList;
    private ListView lstPeerList;
    private ListView lstGeneralList;
    private LinearLayout layPeerLayout;
    private LinearLayout layModuleLayout;
    private List<String> mActionList;
    private List<String> mGeneralList;
    private Button btnSleep;
    private Button btnWake;

    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    RemoteConnection mRemoteConnection;
    final ArrayList<String> peerList = new ArrayList<>();

    // TODO: import Constants from main application to ensure similar naming - consider grunt to keep synced?
    private String[] screenSizes = { "close window", "small screen", "wide screen", "full screen"};
    private int screenState = 0;
    private String[] weatherVisibility = { "show weather", "hide weather" };
    private int weatherState = 0;

    public class RemoteHandler extends Handler {
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
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        mServiceMap = new HashMap<>();

        // start NSD
        mUpdateHandler = new RemoteHandler();
        mRemoteConnection= new RemoteConnection(mUpdateHandler);
        mNsdHelper = new NsdHelper(this);
        //registerNsdService();


        //txtConnectionMessage = (TextView) findViewById(R.id.connection_message);
        mActionList = new ArrayList<>();
        mActionList.addAll(Arrays.asList(getResources().getStringArray(R.array.fragment_commands)));

        mGeneralList = new ArrayList<>();
        mGeneralList.addAll(Arrays.asList(getResources().getStringArray(R.array.general_commands)));
        mGeneralList.addAll(Arrays.asList(getResources().getStringArray(R.array.settings_commands)));
        mGeneralList.addAll(Arrays.asList(getResources().getStringArray(R.array.numbers)));

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

        ArrayAdapter<String> generalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mGeneralList);
        lstGeneralList = (ListView) findViewById(R.id.mirror_general_list);
        lstGeneralList.setAdapter(generalAdapter);
        lstGeneralList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String command = mGeneralList.get(position).toLowerCase();
                sendCommandToMirror(command);
            }
        });

        // initialize ActionList
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mActionList);
        lstActionList = (ListView) findViewById(R.id.mirror_fragment_list);
        lstActionList.setAdapter(actionAdapter);
        lstActionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String command = mActionList.get(position).toLowerCase();
                sendCommandToMirror(command);
            }
        });

        peerList.addAll(mServiceMap.keySet());
        peerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                peerList);
        lstPeerList = (ListView) findViewById(R.id.peer_list);
        lstPeerList.setAdapter(peerAdapter);
        lstPeerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: connect to selected item
                Log.d(TAG, "connecting to :: " + peerList.get(position));
            }
        });

        // toggle between peer list and command list
        fabShowPeers = (FloatingActionButton) findViewById(R.id.show_peers);
        fabShowPeers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layPeerLayout.getVisibility() == View.VISIBLE)
                    showModuleList();
                else
                    showPeers();
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
            mRemoteConnection.tearDown();
        }
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        mRemoteConnection.tearDown();
        super.onDestroy();

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

    public void sendCommandToMirror(String command) {
        if (!command.isEmpty()) {
            command = interpretCommand(command);
            mRemoteConnection.sendMessage(command);
        }
    }

    public String interpretCommand(String command) {
        if (command.equals("screen size")) {
            command = screenSizes[ ++screenState % screenSizes.length ];
        } else if (command.equals("show weather")) {
            command = weatherVisibility[ ++weatherState % weatherVisibility.length ];
        }

        return command;
    }

    public void serviceDiscovered(NsdServiceInfo service){
        String name = service.getServiceName();
        if (mServiceMap.containsKey(name)) return;
        mServiceMap.put(name, service);
        peerList.add(name);
        //ArrayAdapter<String> adapter = (ArrayAdapter<String>)lstPeerList.getAdapter();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peerAdapter.notifyDataSetChanged();
                Log.d(TAG, "Adding service :: " + peerAdapter.toString());
            }
        });
    }

    public void serviceLost(NsdServiceInfo service) {
        mServiceMap.remove(service.getServiceName());
        peerList.remove(service.getServiceName());
        peerAdapter.notifyDataSetChanged();
    }

    public void clearServices() {
        mServiceMap.clear();
    }

    public HashMap<String, NsdServiceInfo> getServiceMap() {
        return mServiceMap;
    }
}
