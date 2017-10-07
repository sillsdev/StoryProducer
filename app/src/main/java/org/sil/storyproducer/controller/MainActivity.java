package org.sil.storyproducer.controller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.NavItemAdapter;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.FileSystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

public class MainActivity extends AppCompatActivity implements Serializable {
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CHOOSE_TEMPLATES = 1;
    private static String TEMPLATES_DIR;
    private Context con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileSystem.init(getApplicationContext(), this);
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

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
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

    public void launchChooseTemplatesDialog(Context context, String templates_dir) {
        TEMPLATES_DIR = templates_dir;
        con = context;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.parse(getExternalFilesDir(null).toString()), "*/*");
        startActivityForResult(Intent.createChooser(intent, "Open folder"), REQUEST_CHOOSE_TEMPLATES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CHOOSE_TEMPLATES) {
            File[] folders = ContextCompat.getExternalFilesDirs(con, null);
            File dest = new File(folders[0].getPath() + '/' + TEMPLATES_DIR);
            copyFile(data.getData(), dest);
        }
    }

    protected void copyFile(Uri srcUri, File dest) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(con.getContentResolver().openInputStream(srcUri));
            bos = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buf = new byte[5024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while (bis.read(buf) != -1);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Error: could not copy template folder", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

