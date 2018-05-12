package org.sil.storyproducer.controller.export;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.sil.storyproducer.R;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;


/**
 * NOTICE: This code adapted from the tutorial found at
 * http://custom-android-dn.blogspot.com/2013/01/create-simple-file-explore-in-android.html.
 */

public class FileChooserActivity extends AppCompatActivity {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[^a-zA-Z0-9\\-_ ]");

    private File currentDir;
    private FileArrayAdapter adapter;
    private final Stack<File> history=new Stack<>();
    private boolean allowOverwrite = false;

    public static final String ALLOW_OVERWRITE = "allowOverwrite";
    public static final String INITIAL_PATH = "initialPath";
    public static final String FILE_DIR_PATH = "fileDirPath";
    public static final String FILE_PATH = "filePath";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        //Set up toolbar
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        allowOverwrite = getIntent().getBooleanExtra(ALLOW_OVERWRITE, false);

        //Navigate to folder passed through intent
        File initialFile = new File(getIntent().getStringExtra(INITIAL_PATH));
        if(initialFile.isDirectory()) {
            navigateToFolder(initialFile);
        }
        else {
            navigateToFolder(initialFile.getParentFile());
        }

        //bind onClickListener to save button
        Button saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFile(v);
            }
        });
    }

    private void fill(File dir) {
        File[] contents = dir.listFiles();
        this.setTitle(dir.getName());

        List<Item> subdirs = new ArrayList<>();
        List<Item> files = new ArrayList<>();

        if(contents != null){
            for(File subdirOrFile: contents){
                Date lastModDate = new Date(subdirOrFile.lastModified());
                DateFormat formatter = DateFormat.getDateTimeInstance();
                String date_modify = formatter.format(lastModDate);

                if(subdirOrFile.isDirectory()){

                    int numContents = subdirOrFile.listFiles().length;

                    String itemCountString = getResources().getQuantityString(
                            R.plurals.file_explorer_numItems, numContents, numContents);


                    subdirs.add(new Item(subdirOrFile.getName(),itemCountString, date_modify,
                            subdirOrFile.getAbsolutePath(),false));

                } else { //subdirOrFile is a file and not a directory
                    long size = subdirOrFile.length();

                    /**
                     * We have to use this 'sizePlurality' int because 'size' is a long and can't be
                     * passed into the getQuantityString method, which takes only an int.
                     */
                    int sizePlurality = 2;

                    if(size==1){
                        sizePlurality = 1;
                    }

                    String sizeWithUnits = getResources().getQuantityString(
                            R.plurals.file_explorer_numBytes, sizePlurality, size);


                    files.add(new Item(subdirOrFile.getName(),sizeWithUnits, date_modify,
                            subdirOrFile.getAbsolutePath(),true));
                }
            }
        }

        List<Item> displayList = new ArrayList<>();
        String parent = dir.getParent();
        if(parent != null && new File(parent).canWrite()) {
            displayList.add(0, new Item("..", getString(R.string.Parent_Dir_Label),
                    "", parent, false));
        }
        Collections.sort(subdirs);
        Collections.sort(files);
        displayList.addAll(subdirs);
        displayList.addAll(files);


        adapter = new FileArrayAdapter(FileChooserActivity.this, R.layout.file_view,displayList);

        ListView listView = findViewById(android.R.id.list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
                Item item = adapter.getItem(position);
                if(! item.isFile()){
                    navigateToFolder(new File(item.getPath()));
                }
            }
        });
    }

    private void navigateToFolder(File dir){
        navigateToFolder(dir, true);
    }

    private void refresh(){navigateToFolder(currentDir, false);}

    private void navigateToFolder(File dir, boolean addToHistory){
        if (currentDir != null && addToHistory) {
            history.push(currentDir);
        }
        currentDir = dir;
        fill(currentDir);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_explorer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to buttons being pressed on the toolbar
        switch (item.getItemId()) {
            case R.id.new_folder: //new folder button
                newFolder();
                return true;
            case android.R.id.home: //back button. (I believe this name is built-in. -MDB)
                goBack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveFile(View view){
        EditText textBox = findViewById(R.id.fileName);
        String fileName = textBox.getText().toString();

        if( ILLEGAL_CHARS.matcher(fileName).find() ){
            createErrorDialog(getString(R.string.file_explorer_chars_error));
        } else if (fileName.length()==0){
            createErrorDialog(getString(R.string.file_explorer_emptyFileName));
        } else {
            File newFile = new File(currentDir, fileName);
            if (newFile.exists() && !allowOverwrite){
                createErrorDialog(getString(R.string.file_explorer_fileAlreadyExists_1)
                        +newFile.getName()+getString(R.string.file_explorer_fileAlreadyExists_2));
            } else {
                Intent intent = new Intent();
                intent.putExtra(FILE_DIR_PATH,currentDir.toString());
                intent.putExtra(FILE_PATH,newFile.getAbsolutePath());
                setResult(RESULT_OK, intent);
                finish();
            }
        }

    }

    private void newFolder(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(FileChooserActivity.this);
        final EditText input = new EditText(getApplicationContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setTitle(R.string.file_explorer_newFolder);
        alertDialog.setMessage(R.string.file_explorer_folderNamePrompt);
        alertDialog.setView(input);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hideKeyboard(input);
                if( ILLEGAL_CHARS.matcher(input.getText()).find() ){
                    createErrorDialog(getString(R.string.file_explorer_chars_error));
                } else if (input.getText().length()==0){
                    createErrorDialog(getString(R.string.file_explorer_emptyFolderName));
                } else {
                    File newFolder = new File(currentDir, input.getText().toString());
                    if (newFolder.exists()){
                        createErrorDialog(getString(R.string.file_explorer_folderAlreadyExists_1)
                                +input.getText()
                                +getString(R.string.file_explorer_folderAlreadyExists_2));
                    } else {
                        boolean success = newFolder.mkdir();
                        if (! success){
                            createErrorDialog(getString(R.string.file_explorer_folder_creation_failure));
                        } else {
                            refresh();
                        }
                    }
                }
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hideKeyboard(input);
            }
        });
        alertDialog.create().show();
        showKeyboard();
    }

    private void showKeyboard(){
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .toggleSoftInput(InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void hideKeyboard(EditText editText){
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    private AlertDialog createErrorDialog(String text){
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(FileChooserActivity.this);
        errorDialog.setTitle(R.string.error);
        errorDialog.setMessage(text);
        errorDialog.setPositiveButton(R.string.ok, null);
        AlertDialog ret = errorDialog.create();
        ret.show();
        return ret;
    }

    private void goBack(){
        if(! history.isEmpty()){
            navigateToFolder(history.pop(), false);
        }
        else {
            finish();
        }
    }



}