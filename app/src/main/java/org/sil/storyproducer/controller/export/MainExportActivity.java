package org.sil.storyproducer.controller.export;

/**
 * NOTICE: This code adapted from the tutorial found at
 *
 * http://custom-android-dn.blogspot.com/2013/01/create-simple-file-explore-in-android.html.
 *
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.sil.storyproducer.R;


public class MainExportActivity extends AppCompatActivity {

    private static final int REQUEST_PATH = 1;
    String curFileName;
    EditText textField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_main);
        textField = (EditText)findViewById(R.id.editText);
    }

    public void getFile(View view){
        Intent intent1 = new Intent(this, FileChooser.class);
        startActivityForResult(intent1,REQUEST_PATH);
    }

    // Listen for results.
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        if (requestCode == REQUEST_PATH){
            if (resultCode == RESULT_OK) {
                curFileName = data.getStringExtra("GetFileName");
                textField.setText(curFileName);
            }
        }
    }
}
