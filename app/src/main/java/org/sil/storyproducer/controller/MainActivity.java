package org.sil.storyproducer.controller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.Network.ConnectivityStatus;
import org.sil.storyproducer.tools.Network.VolleySingleton;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.FileSystem;


import java.io.Serializable;




public class MainActivity extends AppCompatActivity implements Serializable {

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

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

        FileSystem.init(getApplicationContext());
        StoryState.init(getApplicationContext());
        StorySharedPreferences.init(getApplicationContext());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new StoryListFrag()).commit();

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        this.getApplicationContext().registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

       // this.reloadStories();

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
                launchChangeLWCDialog();
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
    public void switchToStory(String storyName) {
        StoryState.setStoryName(storyName);
        Phase currPhase = StoryState.getSavedPhase();
        StoryState.setCurrentPhase(currPhase);
        StoryState.setCurrentStorySlide(0);
        Intent intent = new Intent(this.getApplicationContext(), currPhase.getTheClass());
        startActivity(intent);
    }

    /**
     * Launch a dialog to change the LWC used for templates
     */
    private void launchChangeLWCDialog() {
        final Spinner languageSpinner = new Spinner(this);
        String[] languages = FileSystem.getLanguages();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_dropdown_item, languages);
        languageSpinner.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.change_language_title))
                .setView(languageSpinner)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String selectedLanguage = languageSpinner.getSelectedItem().toString();
                        boolean languageChanged = FileSystem.changeLanguage(selectedLanguage, getApplicationContext());
                        if (!languageChanged) {
                            Toast.makeText(MainActivity.this, "Error: could not change language", Toast.LENGTH_SHORT).show();
                        } else {
                            MainActivity.this.reloadStories();
                        }
                    }
                }).create();
        dialog.show();
    }
}

