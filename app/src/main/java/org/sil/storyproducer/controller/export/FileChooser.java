package org.sil.storyproducer.controller.export;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

public class FileChooser extends AppCompatActivity {

    private File currentDir;
    private FileArrayAdapter adapter;
    private final Stack<File> history=new Stack<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        //actionBar.setMenu();
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       // currentDir = ContextCompat.getExternalFilesDirs(this.getApplicationContext(), null)[0];
       // fill(currentDir);
   //     setContentView(R.activity_file_explorer.activity_file_chooser);
        File ext = ContextCompat.getExternalFilesDirs(this.getApplicationContext(), null)[0];
        String projDir = getIntent().getStringExtra("HomeBoyDirectory");

        navigateToFolder(new File(projDir));
//        navigateToFolder(ext);
    }

    private void fill(File f) {
        File[] contents = f.listFiles();
        this.setTitle("Current Dir: "+f.getName());
        //Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
        //actionBar.setTitle("Current Dir: "+f.getName());

        List<Item> dirs = new ArrayList<>();
        List<Item> fls = new ArrayList<>();

        if(contents != null){
            for(File ff: contents){
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){

                    File[] fbuf = ff.listFiles();
                    int buf = 0;
                    if(fbuf != null){
                        buf = fbuf.length;
                    }
                    String num_item = String.valueOf(buf);
                    if (buf == 1){
                        num_item += " item";
                    } else {
                        num_item += " items";
                    }

                    //String formated = lastModDate.toString();
                    dirs.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),false));
                } else {
                    long size = ff.length();
                    String units = " bytes";
                    if (size==1){
                        units=" byte";
                    }
                    fls.add(new Item(ff.getName(),size + units, date_modify, ff.getAbsolutePath(),true));
                }
            }
        }

        Collections.sort(dirs);
        Collections.sort(fls);
        dirs.addAll(fls);
        String parent = f.getParent();
        if(parent != null) {
            dirs.add(0, new Item("..", "Parent Directory", "", parent, false));
        }
        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view,dirs);
        //this.setListAdapter(adapter);

        ListView lv = (ListView)findViewById(android.R.id.list);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
         //       super.onListItemClick(l, v, position, id);
                Item o = adapter.getItem(position);
                if(o.isFile()){
                    onFileClick(o);
                } else {
                    navigateToFolder(new File(o.getPath()));
                }
            }
        });
    }

    private void navigateToFolder(File f){
        navigateToFolder(f, true);
    }

    private void navigateToFolder(File f, boolean addToHistory){
        if (currentDir != null && addToHistory) {
            history.push(currentDir);
        }
        currentDir = f;
        fill(currentDir);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_explorer, menu);
        return true;
    }

    private void onFileClick(Item o)
    {
        //Toast.makeText(this, "Folder Clicked: "+ currentDir, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.putExtra("GetPath",currentDir.toString());
        intent.putExtra("GetFileName",o.getName());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_folder:
                newFolder();
                return true;
            case android.R.id.home:
                goBack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void newFolder(){
        System.out.println("Yo, new folda bro");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FileChooser.this);
        final EditText input = new EditText(getApplicationContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.create().show();
    }

    private void goBack(){
        if(! history.isEmpty()){
            navigateToFolder(history.pop(), false);
        }
    }

    public void saveFile(View view){
        EditText textBox = (EditText) findViewById(R.id.editText3);
        Intent intent = new Intent();
        intent.putExtra("GetPath",currentDir.toString());
        intent.putExtra("GetFileName",currentDir.getAbsolutePath()+"/"+textBox.getText());
        setResult(RESULT_OK, intent);
        finish();
    }
}