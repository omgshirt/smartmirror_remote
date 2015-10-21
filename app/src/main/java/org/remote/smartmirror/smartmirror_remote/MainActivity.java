package org.remote.smartmirror.smartmirror_remote;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Inserting into ListView

        //Get ListView from content_main.xml
        ListView remoteView = (ListView) findViewById(R.id.remote_list);

        //Add dummy info to array
        String[] applications = {
                new String("Weather"),
                new String("News"),
                new String("Calendar")
        };

        //Initialize ArrayAdapter with Array of Strings above
        ArrayAdapter<String> rListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, applications);

        //Set the adapter to the ListView
        remoteView.setAdapter(rListAdapter);

        //Clicking functions that display name of item on bottom of screen as a toast
        remoteView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String appItem = ((TextView)view).getText().toString();
                Toast.makeText(getBaseContext(), appItem, Toast.LENGTH_SHORT).show();
            }
        });
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
