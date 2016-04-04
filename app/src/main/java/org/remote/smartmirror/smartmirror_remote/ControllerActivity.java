package org.remote.smartmirror.smartmirror_remote;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class ControllerActivity extends AppCompatActivity {

    public static final String TAG = "Remote";

    private HashMap<String,NsdServiceInfo> mServiceMap;
    ArrayAdapter<String> peerAdapter;

    private HashMap<Integer, String> mGenreMap;

    private FloatingActionButton fabShowPeers;

    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    RemoteConnection mRemoteConnection;
    final ArrayList<String> peerList = new ArrayList<>();

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

        mServiceMap = new HashMap<>();

        // start NSD
        mUpdateHandler = new RemoteHandler();
        mRemoteConnection= new RemoteConnection(mUpdateHandler);
        mNsdHelper = new NsdHelper(this);

        //txtConnectionMessage = (TextView) findViewById(R.id.connection_message);

        // create a ContextControlsFragment using the DEFAULT_CONTROLS layout
        if (savedInstanceState == null) {
            replaceFragment(ContextFragment.NewInstance(R.layout.default_controls));
        }

        //
        mGenreMap = new HashMap<>();
        mGenreMap.put(R.id.alternative, "remote play alternative");
        mGenreMap.put(R.id.ambient, "remote play ambient");
        mGenreMap.put(R.id.classical, "remote play classical");
        mGenreMap.put(R.id.dance, "remote play dance");
        mGenreMap.put(R.id.jazz, "remote play jazz");
        mGenreMap.put(R.id.rap, "remote play rap");
        mGenreMap.put(R.id.rock, "remote play rock");

        /*
        //    KEEPING IN CASE WE WANT TO SEE THE PEER LIST OF ALL VISIBLE MIRRORS

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layPeerLayout = (LinearLayout) findViewById(R.id.peer_layout);
        
        peerList.addAll(mServiceMap.keySet());
        peerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                peerList);
        lstPeerList = (ListView) findViewById(R.id.peer_list);
        lstPeerList.setAdapter(peerAdapter);
        lstPeerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
        */
    }

    private void replaceFragment(Fragment fragment){
        Log.i(TAG, "creating fragment :: " + fragment);
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.context_controls, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Called from buttons via XML. See 'styles.xml'. Commands are interpreted here, by changing
     * the remotes layout, broadcasting a command to the mirror, or both.
     * @param view clicked view
     */
    public void onButtonClicked(View view){
        String command = "";

        // Change to article selection if a news desk is chosen
        switch (view.getId()) {
            case R.id.books:
            case R.id.business:
            case R.id.economics:
            case R.id.environment:
            case R.id.fashion:
            case R.id.games:
            case R.id.lifestyle:
            case R.id.media:
            case R.id.movies:
            case R.id.opinion:
            case R.id.science:
            case R.id.sports:
            case R.id.technology:
            case R.id.travel:
            case R.id.world:
                replaceFragment(ContextFragment.NewInstance(R.layout.article_controls));
                break;
            default:
                break;
        }

        // change context views and send commands to mirror
        switch (view.getId()) {
            case R.id.go_back:
                goBackPressed();
                command = "go back";
                break;
            case R.id.camera:
                replaceFragment(ContextFragment.NewInstance(R.layout.camera_controls));
                command = "camera";
                break;
            case R.id.gmail:
                replaceFragment(ContextFragment.NewInstance(R.layout.gmail_controls));
                command = "gmail";
                break;
            case R.id.increase_screen_size:
                command = "increase screen size";
                break;
            case R.id.listening:
            case R.id.listening_settings:
                command = "toggle listening";
                break;
            case R.id.music:
                command = "music";
                replaceFragment(ContextFragment.NewInstance(R.layout.music_controls));
                break;
            case R.id.news:
                replaceFragment(ContextFragment.NewInstance(R.layout.news_controls));
                break;
            case R.id.pause:
                command = "pause music";
                break;
            case R.id.play:
                command = "play music";
                break;
            case R.id.power:
                command = "toggle wake";
                break;
            case R.id.settings:
                command = "settings";
                replaceFragment(ContextFragment.NewInstance(R.layout.settings_controls));
                break;
            case R.id.sound:
                command = "toggle sound";
                break;
            case R.id.stop:
                command = "stop music";
                replaceFragment(ContextFragment.NewInstance(R.layout.music_controls));
                break;
            case R.id.time_format:
                command = "toggle time format";
                break;
            case R.id.weather_format:
                command = "toggle weather format";
                break;
            default:
                command = ((TextView)view).getText().toString().toLowerCase(Locale.US);
                break;
        }

        // if the clicked view is one of the music genres, send the corresponding play command.
        // Then, switch view to play_controls
        if (mGenreMap.containsKey(view.getId())) {
            command = mGenreMap.get(view.getId());
            replaceFragment(ContextFragment.NewInstance(R.layout.play_controls));
        }

        if (!command.isEmpty()) {
            Log.i(TAG, "sending command :: " + command);
            sendCommandToMirror(command);
        }
    }

    // called by pressing the 'go back' button on the UI. Triggers the back stack to pop unless
    // at the top-level menu.
    private void goBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed(){

        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            // ignore back press if it would remove first entry
            super.onBackPressed();
        } else {
            // instead, close the app
            finish();
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
            Toast.makeText(this, "Connected to SmartMirror", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "No service to connect to!");
        }
    }

    public void sendCommandToMirror(String command) {
        if (!command.isEmpty()) {
            mRemoteConnection.sendMessage(command);
        }
    }

    public void serviceDiscovered(NsdServiceInfo service){
        String name = service.getServiceName();
        if (mServiceMap.containsKey(name)) return;
        mServiceMap.put(name, service);
        peerList.add(name);
        //ArrayAdapter<String> adapter = (ArrayAdapter<String>)lstPeerList.getAdapter();

        /* Taken out while FAB is removed. Normally would show list of discovered mirrors in the area.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peerAdapter.notifyDataSetChanged();
                Log.d(TAG, "Adding service :: " + peerAdapter.toString());
            }
        });
        */
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
