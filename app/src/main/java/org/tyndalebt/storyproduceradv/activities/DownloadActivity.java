package org.tyndalebt.storyproduceradv.activities;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tyndalebt.storyproduceradv.BuildConfig;
import org.tyndalebt.storyproduceradv.R;

import org.tyndalebt.storyproduceradv.controller.BaseController;
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadAdapter;
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS;
import org.tyndalebt.storyproduceradv.model.Workspace;
import org.tyndalebt.storyproduceradv.tools.file.*;

// This needs to correspond to name on server

public class DownloadActivity extends BaseActivity {
    public static final String BLOOM_LIST_FILE = "BloomfileLang";

    private static String file_url;
    public ProgressBar pBar;
    public TextView pText;
    public ListView pView;
    public ImageView pDownloadImage;
    public static final int progress_bar_type = 0;
    public DownloadFileFromURL at;
    public DrawerLayout mDrawerLayout = null;
    // First pass is to parse languages, select the language, second pass is to choose story within that language
    public Boolean firstPass;
    public String chosenLanguage;
    public String bloomFileContents;
    ArrayList<HashMap<String, String>> formList = new ArrayList<HashMap<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        pText = (TextView) findViewById(R.id.pProgressText);
        pBar = (ProgressBar) findViewById(R.id.progressBar);

        BuildNativeLanguageArray();
        at = new DownloadFileFromURL(this);
        at.progress_bar_type = this.progress_bar_type;
        file_url = BuildConfig.ROCC_URL_PREFIX + "/Files/Bloom/";
        at.execute(file_url + BLOOM_LIST_FILE);
        firstPass = true;
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

        ArrayList<DownloadDS> arrayList = new ArrayList<>();
        Integer idx;
        String tmp;

        for (idx = 0; idx < pList.length; idx++) {
            if (!folderExists(this, pURL[idx])) {
                tmp = at.removeExtension((pList[idx]));
                if (pURL[idx].equals("Language")) {
                    tmp = tmp + " / " + getNativeLangName(pList[idx]);
                }
                arrayList.add(new DownloadDS(tmp, pURL[idx], false));
            }
        }
        DownloadAdapter arrayAdapter = new DownloadAdapter(arrayList, this);
        pView.setAdapter(arrayAdapter);

        pDownloadImage.setOnClickListener(clickListener);
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("TemplateLanguages.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public String getNativeLangName(String pLanguage) {
        String nativeLang = null;

        for (int i = 0; i < formList.size(); i++) {
            if (formList.get(i).get("filename").equals(pLanguage)) {
                nativeLang = formList.get(i).get("displayname");
                break;
            }
        }
        return nativeLang;
    }

    public void BuildNativeLanguageArray() {
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            JSONArray m_jArry = obj.getJSONArray("language");
            HashMap<String, String> m_li;

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                String formula_value = jo_inside.getString("filename");
                String url_value = jo_inside.getString("displayname");

                //Add your values in your `ArrayList` as below:
                m_li = new HashMap<String, String>();
                m_li.put("filename", formula_value);
                m_li.put("displayname", url_value);

                formList.add(m_li);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            String folderName = at.removeExtension(fileName);
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
            DownloadDS dataModel=(DownloadDS) obj;
            if (dataModel.getChecked() == true) {
                if (pURLs != "") {
                    pURLs = pURLs + "|";
                }
                pURLs = pURLs + dataModel.getURL();
            }
        }
        return pURLs.split("\\|");
    }

    public String URLEncodeUTF8(String pSource) {
        String tmpNew = "";
        Integer tmpInt = 0;
        char tmpByte = 0;
        Integer idx;

        for (idx = 0; idx < pSource.length(); idx++) {
            if (pSource.charAt(idx) >= 128) {
                tmpByte = pSource.charAt(idx);
                tmpNew = tmpNew + "%";
                tmpInt = (int)tmpByte;
                tmpNew = tmpNew + String.format("%02X", tmpInt);
            }
            else if (pSource.charAt(idx) == ' ') {
                tmpNew = tmpNew + "%20";
            } else {
                tmpNew = tmpNew + pSource.charAt(idx);
            }
        }
        return tmpNew;
    }

    public boolean copyFile(String outFile) {
        int i;
        String result = "";

        if (outFile.compareTo(BLOOM_LIST_FILE) == 0) {
            if (firstPass == true) {
                InputStream fis = null;
                try {
                    File sourceFile = new File(this.getFilesDir() + "/" + outFile);
                    fis = new FileInputStream(sourceFile);
                    char current;
                    while (fis.available() > 0) {
                        current = (char) fis.read();
                        result = result + String.valueOf(current);
                    }
                } catch (Exception e) {

                    Log.d("DownloadActivity:copyFile", e.toString());
                    Intent mDisplayAlert = new Intent(this, DisplayAlert.class);
                    mDisplayAlert.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mDisplayAlert.putExtra("title", getString(R.string.more_templates));
                    mDisplayAlert.putExtra("body", getString(R.string.remote_check_msg_no_connection));
                    startActivity(mDisplayAlert);
                }
                bloomFileContents = result;
            } else {
                result = bloomFileContents;
            }

            String lines[] = result.split("\\r?\\n");
            String itemString = "";
            String tagString = "";
            int idx;
            String lastLang = "";

            for (idx = 0; idx < lines.length; idx++) {
                String lang[] = lines[idx].split("/");
                if (firstPass == true) {
                    if (!lastLang.equals(lang[0])) {
                        if (!itemString.equals("")) {
                            itemString = itemString + "|";
                            tagString = tagString + "|";
                        }
                        lastLang = lang[0];
                        itemString = itemString + lang[0];
                        tagString = tagString + "Language";
                    }
                } else {
                    if (lang[0].equals(this.chosenLanguage)) {
                        if (!itemString.equals("")) {
                            itemString = itemString + "|";
                            tagString = tagString + "|";
                        }
                        if (lang.length > 1) {
                            ByteBuffer buffer = StandardCharsets.ISO_8859_1.encode(lang[1]);
                            String encodedString = StandardCharsets.UTF_8.decode(buffer).toString();
                            itemString = itemString + encodedString;
                        }
                        tagString = tagString + file_url + URLEncodeUTF8(lines[idx]);
                    }
                }
            }
            String itemArray[] = itemString.split("\\|");
            String tagArray[] = tagString.split("\\|");
            at = new DownloadFileFromURL(this);
            buildBloomList(itemArray, tagArray);
            firstPass = false;
        } else {
            BaseController upstor = new BaseController(this, this);
            pBar.setVisibility(View.INVISIBLE);
            pText.setVisibility(View.INVISIBLE);
            Workspace.parseLanguage = this.chosenLanguage;
            File sourceFile = new File(this.getFilesDir() + "/" + BLOOM_LIST_FILE);
            sourceFile.delete();
            //  All bloom files have been download and Bloomlist file deleted.  Wait for a second, then build list of bloom files and parse them
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Build list of (downloaded) bloom files on internal folder
            ArrayList<DocumentFile> storyFiles=new ArrayList<>();
            DocumentFile root = DocumentFile.fromFile(new File(this.getFilesDir() + "/"));
            for(DocumentFile f:root.listFiles()){
                 if(f.isFile()){
                    String name=f.getName();
                    if(name.endsWith(".bloomSource")
                            || name.endsWith(".bloom"))
                        storyFiles.add(f);
                }
            }
            upstor.updateStoriesCommon(storyFiles);
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
                showMain();
                break;
            case R.id.nav_registration:
                showRegistration(false);
                break;
            case R.id.change_language:
                showChooseLanguage();
                break;
            case R.id.nav_spadv_website:
                org.tyndalebt.storyproduceradv.tools.file.FileIO.goToURL(this, Workspace.URL_FOR_WEBSITE);
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
