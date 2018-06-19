package org.sil.storyproducer.controller;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Story;
import org.sil.storyproducer.tools.Network.ConnectivityStatus;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.StorySharedPreferences;

import org.sil.storyproducer.model.Workspace;


import java.io.Serializable;

import static java.security.AccessController.getContext;
import static org.sil.storyproducer.controller.remote.RemoteCheckFrag.R_CONSULTANT_PREFS;


public class MainActivity extends AppCompatActivity implements Serializable {

    private BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!ConnectivityStatus.isConnected(context)){
                Log.i("Connection Change", "no connection");

                VolleySingleton.getInstance(context).stopQueue();
            }else {
                Log.i("Connection Change", "Connected");

                VolleySingleton.getInstance(context).startQueue();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StorySharedPreferences.init(getApplicationContext());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new StoryListFrag()).commit();

        this.getApplicationContext().registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        Workspace.INSTANCE.updateStories(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_story_templates, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_lang:
                //TODO remove this option.
                break;
            case R.id.menu_registration:
                Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_license:
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(this.getString(R.string.license_title))
                        .setMessage(this.getString(R.string.license_body))
                        .setPositiveButton(this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        }).create();
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Upon language change, reload list of templates in that language
     * The actual language change is done within the FileSystem class
     */
    private void reloadStories() {
        getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentById(R.id.fragment_container)).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new StoryListFrag()).commit();

    }

    /**
     * move to the chosen story
     */
    public void switchToStory(Story story) {
        Workspace.INSTANCE.setActiveStory(story);
        Intent intent = new Intent(this.getApplicationContext(), Workspace.INSTANCE.getActivePhase().getTheClass());
        startActivity(intent);
    }
}

