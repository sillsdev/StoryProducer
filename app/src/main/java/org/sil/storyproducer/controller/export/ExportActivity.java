package org.sil.storyproducer.controller.export;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.FileSystem;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;

public class ExportActivity extends AppCompatActivity {

    public static final String PROJECT_DIRECTORY = "projectDirectory";
    private static final int FILE_CHOOSER_CODE = 1;
    private GestureDetectorCompat mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        //get the current phase
        Phase phase = StoryState.getCurrentPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources(), phase.getColor(), null)));

        Button exportButton = (Button) findViewById(R.id.exportButton);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });

        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));
    }

    /**
     * sets the Menu spinner_item object
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_phases, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        spinner.setOnItemSelectedListener(new PhaseMenuItemListener(this));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.phases_menu_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(StoryState.getCurrentPhaseIndex());
        return true;
    }

    /**
     * get the touch event so that it can be passed on to GestureDetector
     * @param event the MotionEvent
     * @return the super version of the function
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void openFileExplorerToExport() {
        Intent intent = new Intent(this, FileChooserActivity.class);
        intent.putExtra(PROJECT_DIRECTORY, FileSystem.getProjectDirectory(StoryState.getStoryName()).getPath());
        startActivityForResult(intent, FILE_CHOOSER_CODE);
    }

    // Listen for results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (requestCode == FILE_CHOOSER_CODE) {
            if (resultCode == RESULT_OK) {
                final String path = data.getStringExtra("GetFileName");
                final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
                errorDialog.setTitle("Info");
                errorDialog.setMessage(path);
                errorDialog.setPositiveButton("OK", null);
                AlertDialog ret = errorDialog.create();
                ret.show();
            }
        }
    }
}
