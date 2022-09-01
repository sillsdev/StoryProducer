package org.tyndalebt.storyproduceradv.tools.file;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.tyndalebt.storyproduceradv.activities.DownloadActivity;
import java.nio.charset.StandardCharsets;

/**
 * Background Async Task to download file
 * */
public class DownloadFileFromURL extends AsyncTask<String, String, String> {

    public int progress_bar_type = 0;
    Context con;
    public String fileName;
    public DownloadFileFromURL(Context context){
        this.con = context;
    }

    /**
     * Before starting background thread Show Progress Bar Dialog
     * */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String... f_url) {
        int count;
        int arrayIndex;
        DownloadActivity da = (DownloadActivity) con;

        Log.e("Warning", "entering doInBackground");
        for (arrayIndex = 0; arrayIndex < f_url.length; arrayIndex++) {
            String fName = f_url[arrayIndex].substring(f_url[arrayIndex].lastIndexOf("/") + 1);
            try {
                fileName = java.net.URLDecoder.decode(fName, StandardCharsets.UTF_8.name());
                String folderName = fileName.replace(".bloom", "");
                // If bloom file has not already been parsed, download it and parse it
                if (!org.tyndalebt.storyproduceradv.tools.file.FileIO.workspaceRelPathExists(con, folderName)) {
                    try {
                        da.runOnUiThread(new Runnable() {
                            public void run() {
                                da.pText.setText(folderName);
                            }
                        });
                        URL url = new URL(f_url[arrayIndex]);
                        URLConnection connection = url.openConnection();
                        connection.connect();

                        // this will be useful so that you can show a typical 0-100%
                        // progress bar
                        int lengthOfFile = connection.getContentLength();

                        // download the file
                        InputStream input = new BufferedInputStream(url.openStream(),
                                8192);

                        OutputStream output = org.tyndalebt.storyproduceradv.tools.file.FileIO.getChildOutputStream(con, fileName, "", "w");

                        byte data[] = new byte[1024];

                        long total = 0;

                        while ((count = input.read(data)) != -1) {
                            total += count;
                            // publishing the progress....
                            // After this onProgressUpdate will be called
                            publishProgress("" + (int) ((total * 100) / lengthOfFile));

                            // writing data to file
                            output.write(data, 0, count);
                        }

                        // flushing output
                        output.flush();

                        // closing streams
                        output.close();
                        input.close();

                    } catch (Exception e) {
                        Log.e("Error: ", e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
        }
        return "";
    }
    /**
     * Updating progress bar
     * */
    protected void onProgressUpdate(String... progress) {
        // setting progress percentage
        DownloadActivity da = (DownloadActivity) con;
        Integer item;
        item = Integer.parseInt(progress[0]);
        da.runOnUiThread(new Runnable() {
            public void run() {
                da.pBar.setProgress(item);
            }
        });
    }

    /**
     * After completing background task Dismiss the progress dialog
     * **/
    @Override
    protected void onPostExecute(String file_url) {
        DownloadActivity da = (DownloadActivity) con;
        if (da.copyFile(fileName))
        {
            
        }
    }

}
