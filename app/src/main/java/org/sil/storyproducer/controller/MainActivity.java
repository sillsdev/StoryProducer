package org.sil.storyproducer.controller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.FileSystem;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity implements Serializable {
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileSystem.init(getApplicationContext());
        StoryState.init(getApplicationContext());
        StorySharedPreferences.init(getApplicationContext());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new StoryListFrag()).commit();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_story_templates, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_lang) {
            launchChangeLWCDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Upon language change, reload list of templates in that language
     * The actual language change is done within the FileSystem class
     */
    private void reloadStories() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new StoryListFrag()).commit();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_dropdown_item, languages);
        languageSpinner.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.change_language_title))
                .setView(languageSpinner)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String selectedLanguage = languageSpinner.getSelectedItem().toString();
                        boolean languageChanged = FileSystem.changeLanguage(selectedLanguage);
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

