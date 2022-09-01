package org.tyndalebt.storyproduceradv.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.tyndalebt.storyproduceradv.BuildConfig;
import org.tyndalebt.storyproduceradv.R;

import org.tyndalebt.storyproduceradv.controller.BaseController;
import org.tyndalebt.storyproduceradv.controller.MainActivity;
import org.tyndalebt.storyproduceradv.tools.file.*;

// This needs to correspond to name on server

public class DownloadActivity extends BaseActivity {
    public static final String BLOOM_LIST_FILE = "bloomlist";

    private static String file_url;
    public ProgressBar pBar;
    public TextView pText;
    public ListView pView;
    public ImageView pDownloadImage;
    public static final int progress_bar_type = 0;
    public DownloadFileFromURL at;
    public DrawerLayout mDrawerLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        pText = (TextView) findViewById(R.id.pProgressText);
        pBar = (ProgressBar) findViewById(R.id.progressBar);

        at = new DownloadFileFromURL(this);
        at.progress_bar_type = this.progress_bar_type;
        file_url = BuildConfig.ROCC_URL_PREFIX + "/Files/Bloom/";
        at.execute(file_url + BLOOM_LIST_FILE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_with_help, menu);
        return true;
    }

    public void buildBloomList(String pList[], String pURL[]) {
        setContentView(R.layout.bloom_list_container);

        Toolbar mActionBarToolbar = findViewById(R.id.toolbarMoreTemplates);
        ActionBar supportActionBar;
        setSupportActionBar(mActionBarToolbar);
        supportActionBar = getSupportActionBar();
        supportActionBar.setTitle(R.string.more_templates);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        mDrawerLayout = findViewById(R.id.drawer_layout_bloom);
        //Lock from opening with left swipe
        mDrawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        NavigationView nav  = (NavigationView)findViewById(R.id.nav_view_bloom);
        nav.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return NavItemSelected(item);
            }
        });

        pView = (ListView) findViewById(R.id.bloom_list_view);
        pDownloadImage = (ImageView) findViewById(R.id.image_download);

        ArrayList<DataModel> arrayList = new ArrayList<>();
        Integer idx;
        String tmp;

        for (idx = 0; idx < pList.length; idx++) {
            if (folderExists(this, pURL[idx]) == false) {
                tmp = pList[idx].replace(".bloom", "");
                arrayList.add(new DataModel(tmp, pURL[idx], false));
            }
        }
        CustomAdapter arrayAdapter = new CustomAdapter(arrayList, this);

        pView.setAdapter(arrayAdapter);

        pDownloadImage.setOnClickListener(clickListener);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.equals(pDownloadImage)) {
                // Build urlList then download
                String urlList[] = BuildURLList();
                setContentView(R.layout.activity_download);

                pText = (TextView) findViewById(R.id.pProgressText);
                pBar = (ProgressBar) findViewById(R.id.progressBar);
                at.execute(urlList);
            }
        }
    };

    public boolean folderExists(Context con, String pURL) {
        String fName = pURL.substring(pURL.lastIndexOf("/") + 1);
        String fileName;
        try {
            fileName = java.net.URLDecoder.decode(fName, StandardCharsets.UTF_8.name());
            String folderName = fileName.replace(".bloom", "");
            // If bloom file has not already been parsed, download it and parse it
            return org.tyndalebt.storyproduceradv.tools.file.FileIO.workspaceRelPathExists(con, folderName);
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            return true;  // This won't do anything (Download file or show in list)
        }
    }

    public String[] BuildURLList() {
        String pURLs = "";
        Integer idx;

        for (idx = 0; idx < pView.getCount(); idx++) {
            Object obj = pView.getAdapter().getItem(idx);
            DataModel dataModel=(DataModel)obj;
            if (dataModel.getChecked() == true) {
                if (pURLs != "") {
                    pURLs = pURLs + ",";
                }
                pURLs = pURLs + dataModel.getURL();
            }
        }
        return pURLs.split(",");
    }

    public boolean copyFile(String outFile) {
        int i;
        String result = "";

        if (outFile.compareTo(BLOOM_LIST_FILE) == 0) {
            InputStream fis = null;
            try {
//                fis = new FileInputStream(outFile);
                fis = org.tyndalebt.storyproduceradv.tools.file.FileIO.getChildInputStream(this, outFile);
                char current;
                while (fis.available() > 0) {
                    current = (char) fis.read();
                    result = result + String.valueOf(current);
                }
            } catch (Exception e) {
                Log.d("DownloadActivity:copyFile", e.toString());
            }
            String lines[] = result.split("\\r?\\n");
            String urlList[] = new String[lines.length];
            int idx;
            String apos;
            // Special Apostrophe (not single quote) doesn't transfer in a URL, encode it along with spaces
            apos = new Character((char) 226).toString();
            apos = apos + new Character((char) 128).toString();
            apos = apos + new Character((char) 153).toString();
            for (idx = 0; idx < lines.length; idx++) {
                urlList[idx] = file_url + lines[idx].replaceAll(" ", "%20");
                urlList[idx] = urlList[idx].replaceAll(apos, "%E2%80%99");
            }
            at = new DownloadFileFromURL(this);
//            at.execute(urlList);
            buildBloomList(lines, urlList);
            // remote bloomlist file after arrays are created
            if (org.tyndalebt.storyproduceradv.tools.file.FileIO.deleteWorkspaceFile(this, outFile)) {
            }
        } else {
            BaseController upstor = new BaseController(this, this);
            pBar.setVisibility(View.INVISIBLE);
            pText.setVisibility(View.INVISIBLE);
            upstor.updateStories();
        }
        return true;
    }

    private Boolean NavItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.nav_workspace:
                showSelectTemplatesFolderDialog();
                break;
            case R.id.nav_word_link_list:
                showWordLinksList();
                break;
            case R.id.nav_more_templates:
                // current fragment
                break;
            case R.id.nav_stories:
                Intent intent = new Intent(this, MainActivity.class);
                this.startActivity(intent);
                this.finish();
                break;
            case R.id.nav_registration:
                showRegistration(false);
                break;
            case R.id.nav_about:
                showAboutDialog();
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.helpButton:
/*
                WebView wv = WebView(this);
                val iStream = assets.open(Phase.getHelpDocFile(PhaseType.STORY_LIST))
                val text = iStream.reader().use {
                it.readText() }

            wv.loadDataWithBaseURL(null,text,"text/html",null,null)
            val dialog = AlertDialog.Builder(this)
                    .setTitle("Story List Help")
                    .setView(wv)
                    .setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
            }
            dialog.show()
 */
                break;
            default:
                super.onOptionsItemSelected(item);
                break;
        }
        return true;

    }
}

class CustomAdapter extends ArrayAdapter<DataModel> implements View.OnClickListener {
    Context mContext;
    private ArrayList<DataModel> dataSet;
    String apos;

    private static class ViewHolder {
        CheckedTextView chkItem;
    }

    public CustomAdapter(ArrayList<DataModel> data, Context context) {
        super(context, R.layout.bloom_list_item, data);
        this.mContext = context;
        this.dataSet = data;
        // Special Apostrophe (not single quote) doesn't transfer in a URL, encode it along with spaces
        apos = new Character((char) 226).toString();
        apos = apos + new Character((char) 128).toString();
        apos = apos + new Character((char) 153).toString();

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        DataModel dataModel=(DataModel)object;

        switch (v.getId())
        {
            case R.id.checkedTextView:
                CheckedTextView ctv = (CheckedTextView) v;
                dataModel.setChecked(!ctv.isChecked());   // toggle check
                setCheckmark(ctv, dataModel.getChecked());
                break;
        }
    }

    private int lastPosition = -1;

    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.bloom_list_item, parent,false);
            viewHolder.chkItem = (CheckedTextView) convertView.findViewById(R.id.checkedTextView);

            result = convertView;
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.enter_up : R.anim.enter_down);
        result.startAnimation(animation);
        lastPosition = position;

        String tmpString;
        tmpString = dataModel.getName();
        tmpString = tmpString.replaceAll(apos, "’");

        viewHolder.chkItem.setText(tmpString);
        setCheckmark(viewHolder.chkItem, dataModel.getChecked());
        viewHolder.chkItem.setOnClickListener(this);
        viewHolder.chkItem.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }


    void setCheckmark(CheckedTextView ctv, Boolean checked) {
        ctv.setChecked(checked);
        if (!ctv.isChecked()) {
            ctv.setCompoundDrawables(null, null, null, null);
//            ctv.setCheckMarkDrawable(null);
        } else {
            Drawable img = getContext().getResources().getDrawable(R.drawable.ic_checkmark_green);
            img.setBounds(0, 0, 120, 120);
            ctv.setCompoundDrawables(img, null, null, null);
//            ctv.setCheckMarkDrawable(R.drawable.ic_checkmark_green);
        }
    }

}

class DataModel {
    String fileName;
    String URL;
    Boolean checked;

    public DataModel(String name, String url, Boolean checked) {
        this.fileName = name;
        this.URL = url;
        this.checked = checked;
    }

    public String getName() {
        return fileName;
    }

    public String getURL() {
        return URL;
    }

    public Boolean getChecked() { return checked; }

    public void setChecked(Boolean checked) { this.checked = checked; }

}