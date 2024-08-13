package org.sil.bloom.reader;

import static android.content.Context.RECEIVER_EXPORTED;
import static org.sil.bloom.reader.BloomLibraryActivity.mBloomActivity;
import static org.sil.storyproducer.tools.file.FileIO.workspaceRelPathExists;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.Toast;

//import org.sil.bloom.reader.models.BookCollection;
import org.sil.storyproducer.controller.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Workspace;

import android.os.Environment;

import androidx.core.content.ContextCompat;

import kotlin.text.MatchResult;
import kotlin.text.Regex;


// This view appears at the bottom of BloomLibraryActivity or MainActivity. Usually it has size zero
// and cannot be seen. During and after downloads, it displays a DownloadProgressView or
// BookReadyView to indicate the progress and completion of downloads. It also contains the
// code to initialize a download and to check on the status of pending downloads when the parent
// activity is resumed and update its contents accordingly.
// The view was originally designed to support multiple child views (such as several downloads in
// progress and several notices about completed downloads) but we decided not to do that.
public class DownloadsView extends LinearLayout {

    // This is the directory (under our private directory in external storage) to which we
    // download books. Unlike our internal private storage, it is possible to give other apps
    // (in particular, the DownloadManager) access to this. Books downloaded here are copied to
    // our main books directory and then deleted.
    private static final String BL_DOWNLOADS = "bl-downloads";
    DownloadProgressView mProgressView;
    static List<DownloadsView> sInstances = new ArrayList<>();
    boolean mRecentMultipleDownloads;

    public DownloadsView(Context context) {
        super(context);
        initializeViews();
    }

    public DownloadsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews();
    }

    static class DownloadData {
        public DownloadData(String destPath) {
            this.destPath = destPath;
            //progressView = view;
        }
        public String destPath;
        public int progress;
        //public DownloadProgressView progressView;
    }

    // We use a concurrent hash map here because it can be accessed by callbacks on other threads
    // when the DownloadManager sends us notifications. There might be some case where this is
    // not enough protection from race conditions to produce exactly the ideal behavior, but it
    // should at least prevent crashes, and it should be extremely rare for multiple downloads to
    // finish at the same instant.
    ConcurrentHashMap<Long, DownloadData> mDownloadsInProgress = new ConcurrentHashMap<>();
    DownloadManager mDownloadManager;


    // Use a background thread to check the progress of downloading
    private ExecutorService executor;

    // The boilerplate I started from has this, but it seems to work fine to update progress directly:
//    // Arbitrary identifier to indicate that we would like to update download progress
//    private static final int UPDATE_DOWNLOAD_PROGRESS = 1973;
//    // Use a handler to update progress bar on the main thread
//    private final Handler mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
//        @Override
//        public boolean handleMessage(@NonNull Message msg) {
//            if (msg.what == UPDATE_DOWNLOAD_PROGRESS) {
//                int downloadProgress = msg.arg1;
//
//                // Update your progress bar here.
//                //progressBar.setProgress(downloadProgress);
//            }
//            return true;
//        }
//    });

    public void cancelDownloads() {
        for (long downloadId: mDownloadsInProgress.keySet()) {
            DownloadData data = mDownloadsInProgress.get(downloadId);
            // stop the download
            mDownloadManager.remove(downloadId);
            // Delete any incomplete temp file
            if (data != null) {
                File source = new File(data.destPath);
                source.delete();
            }
        }
        mDownloadsInProgress.clear();

        // remove the view showing its progress
        removeView(mProgressView);
        updateLayoutForChangedChildList();
    }

    private void updateLayoutForChangedChildList() {
        ViewParent root = this.getParent();
        //noinspection StatementWithEmptyBody
        if (root == null) {
            // Can get called in constructor, in which case, we assume the parent layout will be
            // correctly recomputed when we are added to it.
        } else {
            // If we're already in a root, need to update it, too, since we changed size.
            root.requestLayout();
        }
        this.invalidate();
    }

    // Update the progress bar. This is only an approximation if we have multiple downloads...it computes the average
    // progress of all the downloads still happening. To do better, we'd need to know the actual
    // size of each download, and keep track of completed ones also. I think this might be good
    // enough.
    private void updateProgress() {
        int progress = 0;
        for (DownloadData data : mDownloadsInProgress.values()) {
            progress += data.progress;
        }

        // The size can change while we are processing the progress.
        int numDownloads = mDownloadsInProgress.size();
        if (numDownloads == 0)
            return;
        mProgressView.setProgress(progress / numDownloads);
    }

    private void startMonitoringDownloads() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(1);
        }
        // Run a task in a background thread to check download progress
        executor.execute(() -> {
            int progress = 0;
            while (mDownloadsInProgress.size() > 0) {
                for (long downloadId: mDownloadsInProgress.keySet()) {
                    try {
                        DownloadData data = mDownloadsInProgress.get(downloadId);
                        Cursor cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                        if (cursor.moveToFirst()) {
                            int downloadStatus = CommonUtilities.getIntFromCursor(cursor, DownloadManager.COLUMN_STATUS);
                            switch (downloadStatus) {
                                case DownloadManager.STATUS_RUNNING:
                                    long totalBytes = CommonUtilities.getLongFromCursor(cursor, DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                                    if (totalBytes > 0) {
                                        long downloadedBytes = CommonUtilities.getLongFromCursor(cursor, DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                                        if (downloadedBytes > 0)
                                            progress = (int) (downloadedBytes * 100 / totalBytes);
                                    }
                                    break;
                                case DownloadManager.STATUS_SUCCESSFUL:
                                    progress = 100;
                                    // todo: any cleanup? I think it's handled in complete handler.
                                    break;
                                case DownloadManager.STATUS_PAUSED:
                                case DownloadManager.STATUS_PENDING:
                                    break;
                                case DownloadManager.STATUS_FAILED:
                                    // todo: something? Now logging an error to analytics
                                    String downloadUri = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_URI);
                                    String downloadDescription = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_DESCRIPTION);
                                    // Log that there was an error while downloading with Firebase analytics
                                    if (downloadUri != null && downloadDescription != null)
                                        Workspace.INSTANCE.logDownloadEvent(getContext(), Workspace.DOWNLOAD_TEMPLATE_TYPE.BLOOM_BOOK, Workspace.DOWNLOAD_EVENT.FAILED,
                                                downloadId, downloadStatus, downloadUri, downloadDescription, "");
                                    break;
                            }
                            // Use this if we find we do need to do it on the UI thread.
                            //Message message = Message.obtain();
                            //message.what = UPDATE_DOWNLOAD_PROGRESS;
                            //message.arg1 = progress;
                            //mainHandler.sendMessage(message);

                            if (data != null) { // Play console indicates data can be null here.
                                data.progress = progress;
                            }
                            updateProgress();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (executor != null) {
                // Not sure how we get here with it null, but I did.
                executor.shutdown(); // we'll restart it if we do another download.
                executor = null; // we'll make a new one if we need it.
            }
        });
    }

    private File getDownloadDir() {
        // We need to set a destination for our downloads. This took considerable experiment.
        // The following works, but the book is left in the main Downloads directory, from which we
        // don't seem to be able to delete it.
        // request.setDestinationInExternalFilesDir(BloomLibraryActivity.this, Environment.DIRECTORY_DOWNLOADS,fileName + ".bloompub");

        // The following should download the book directly into our private folder.
        // This would be ideal, except that we'd have to worry about incomplete ones,
        // but it generates a security exception on my test device.
        // https://stackoverflow.com/questions/39744505/how-to-save-files-with-downloadmanager-in-private-path
        // has an answer that says DownloadManager runs in its own process and does not have
        // permission to access our private storage; suggests a possible 3rd party library.
        // Uri target = Uri.fromFile(new File(BookCollection.getLocalBooksDirectory(), fileName + ".bloompub"));
        // request.setDestinationUri(target);

        // So, I finally came up with this, which downloads to our external files directory.
        // Passing null is supposed to give us a folder that we have access to, but which is
        // also a legitimate target for DownloadManager (at least if targeting something after
        // Android Q, which is now required).
        // See https://developer.android.com/reference/android/app/DownloadManager.Request#setDestinationUri(android.net.Uri).
        // Apparently there is a theoretical possibility that a device doesn't provide the app with
        // an externalFilesDir, but it seems to be unheard-of; devices without a real SD card
        // emulate one.
        File result = new File(getContext().getExternalFilesDir(null), Environment.DIRECTORY_DOWNLOADS);//BL_DOWNLOADS);
        result.mkdirs(); // make sure it exists.
        return result;
    }

    // This is a much simpler way of extracting a file basename than we use in IOUtilities, OK because
    // we create these URIs ourselves and know they are based on a local file.
    public static String getFileNameFromUri(Uri uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (path == null) return null;
        return getFilenameFromUriPath(path);
    }
    public static String getFileBasenameFromUri(Uri uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (path == null) return null;
        return getFileBasenameFromUriPath(path);
    }
    private static String getFileBasenameFromUriPath(String path) {
        return path.replaceFirst(".*/", "").replaceFirst("\\.[^.]*$", "");
    }
    public static String getBasenameFromUriPath(String path) {
        return path.replaceFirst(".*/", "")
                .replaceFirst("\\.[^.]*$", "")
                .replaceFirst("\\.[^.]*$", "")
                .replaceFirst("\\.[^.]*$", "");
    }
    public static String getFilenameFromUriPath(String path) {
        return path.replaceFirst(".*/", "");
    }

    static String getFileExtensionFromUri(String strUri) {
        Regex pattern = new Regex("\\.[^.]*$");
        MatchResult match = pattern.find(strUri, 0);
        String strExt = "";
        if (match != null)
            strExt = match.getValue();
        return strExt;
    }

    // If the user searched for a story in the Bloom library using the app hosted Url
    // we should be able to extract the search language from the download page and
    // use it for parsing the story when downloaded.
    public static String getFileLangFromPageUri(String strUri) {
        String langValue = getParameterValue(strUri, "lang");
        return langValue == null ? "" : langValue;
    }

    public static String getBookIdFromPageUri(String strUri) {
        Regex pattern = new Regex("/[^\\?/]+\\?");
        MatchResult match = pattern.find(strUri, 0);
        String strId = "";
        if (match != null) {
            strId = match.getValue();
            strId =  strId.substring(1, strId.length() - 1);
        }
        return strId;
    }

    // Gets a Uri parameter value
    public static String getParameterValue(String strUri, String parameterName) {
        if (strUri == null)
            return null;
        String query = strUri.replaceFirst(".*\\?", "");
        if (!query.isEmpty()) {
            Map<String, String> parameters = parseQuery(query);
            // Get the value for the specified parameter
            return parameters.get(parameterName);
        }
        return null; // Parameter not found
    }

    // Parse a Uri query and return a Map of all the parameters
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> parameters = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                // URL decode the key and value and put them in the map
                String key = keyValue[0];
                String value = keyValue[1];
                parameters.put(key, value);
            }
        }
        return parameters;
    }

    private void initializeViews() {
        sInstances.add(this);
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.downloads, this);

        mDownloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        //set filter to only when download is complete and register broadcast receiver
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().registerReceiver(mDownloadReceiver, filter, RECEIVER_EXPORTED);
        } else {
            getContext().registerReceiver(mDownloadReceiver, filter);
        }
    }

    public void updateUItoCurrentState() {
        // We will clear out any existing download progress view and then reinsert any needed
        // to reflect the current state.
        // I originally wanted to keep any BookReadyViews so that if there were several and the
        // user clicked to read one, they don't all go away. This is obsolete, since we now never
        // have more than one BookReadyView.
        // However, it's not obvious whether we should remove even just one.
        // - If we were in BloomLibraryActivity and hit "View books", we want to get rid of the
        // one in MainActivity when we resume that.
        // - If we were showing it, then switched to another app and back, we probably still want
        // to see it.
        // - If we were showing a single one in BloomLibraryActivity, and did several back presses
        // or Home to get to MainActivity, we probably still want to see it.
        // So, I'm currently thinking the right answer is to remove only non-book-ready views.
        // In the cases where they should be removed, the individual tasks do so.
        // Keeping this as a loop for now, though at present there is only at most one child.
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (!(child instanceof BookReadyView)) {
                removeViewAt(i);
            }
        }
        // we will rediscover any that are still running, and want to create new progress views for them.
        mDownloadsInProgress.clear();

        Cursor cursor = getDownloadManagerCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String uriString = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_LOCAL_URI);
                String path = "";
                try {
                    if (uriString != null) {
                        Uri uri = Uri.parse(uriString);
                        path = uri.getPath();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // Apart from logging, if it's not a parseable URI we'll just hope it's not one of ours.
                }
                // check to see if this is one of our (SP) Bloom downloaded files
                // TODO: do we need to do more than check the file extension as the SP templates folder could be almost anywhere
                // WAS: if (path == null || !path.contains("/" + BL_DOWNLOADS + "/")) {
                String strExt = getFileExtensionFromUri(path);
                if (!strExt.equals(".bloompub") && !strExt.equals(".bloomSource")) {
                    continue; // some unrelated download
                }
                String fileBasename = getFileBasenameFromUriPath(path);
                File downloadDest = new File(getDownloadDir(), fileBasename + strExt);
                long downloadId = CommonUtilities.getLongFromCursor(cursor, DownloadManager.COLUMN_ID);
                int downloadStatus = CommonUtilities.getIntFromCursor(cursor, DownloadManager.COLUMN_STATUS);
                String downloadUri = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_URI);
                String downloadDescription = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_DESCRIPTION);
                if (downloadId > 0 && downloadStatus > 0) {
                    switch (downloadStatus) {
                        default:
                            // if we see a failed download, for now we'll ignore it.
                            // Log an error while downloading with Firebase analytics
                            if (downloadUri != null && downloadDescription != null)
                                Workspace.INSTANCE.logDownloadEvent(getContext(), Workspace.DOWNLOAD_TEMPLATE_TYPE.BLOOM_BOOK, Workspace.DOWNLOAD_EVENT.FAILED,
                                        downloadId, downloadStatus, downloadUri, downloadDescription, "");
                            continue;
                            // But if it's running or paused or pending we want to show the status and allow it to complete.
                        case DownloadManager.STATUS_RUNNING:
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                            showDownloadProgress(downloadId, downloadDest);
                            break;
                        // And if one has finished since our last call of this method, even while
                        // our app was not running, we'll show the complete message.
                        case DownloadManager.STATUS_SUCCESSFUL:
                            handleDownloadComplete(downloadDest.getPath(), downloadId, downloadUri, downloadDescription);
                            break;
                    }
                }

            } while (cursor.moveToNext());
        }

        if (cursor != null)
            cursor.close();
        cleanupDownloadDirectory();
    }

    private Cursor getDownloadManagerCursor() {
        Cursor cursor = null;
        try {
            cursor = mDownloadManager.query(new DownloadManager.Query());
        } catch (IllegalStateException ise) {
            // Play console indicates this is happening sometimes on resume. My guess is we need to
            // set up the download manager again. But it's just a guess.
            mDownloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            try {
                cursor = mDownloadManager.query(new DownloadManager.Query());
            } catch (IllegalStateException ise2) {
                // ignore; we've done what we know to try
                ise2.printStackTrace();
            }
        }
        return cursor;
    }

    public void onDownloadStart(String url, String userAgent,
                                String contentDisposition, String mimetype,
                                long contentLength, String sourceUrl) {

        Uri downloadUri = Uri.parse(url);
        String fileBasename = getFileBasenameFromUri(downloadUri);
        String strExt = getFileExtensionFromUri(url);
        // get the language to use when parsing the story after downloading
        String lang = getFileLangFromPageUri(sourceUrl);
        String bookId = getBookIdFromPageUri(sourceUrl);
        if (fileBasename == null || fileBasename.equals(""))
        {
            // If there isn't a .bloompub file at the location we requested,
            // we seem to get a meaningless url (probably from a 404 redirect).
            // We have changed things on the blorg side now so this should
            // not be possible; so it isn't worth localizing the error message.
            Toast toast = Toast.makeText(getContext(), "A problem occurred while downloading that book.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        String template = getContext().getString(R.string.downloading_file);
        request.setTitle(String.format(template, fileBasename));
        // Use the Book ID and story language to create extra file extensions which can then
        // indicate which language to open the story in after it has been downloaded
        // and uniquely identify the Bloom book being downloaded
        if (!bookId.isEmpty())
            fileBasename = fileBasename + "." + bookId;
        if (!lang.isEmpty())
            fileBasename = fileBasename + ".lang_" + lang;
        File downloadDest = new File(getDownloadDir(), fileBasename + strExt);
        boolean isInWorkspace = workspaceRelPathExists(getContext(), fileBasename);
        if (downloadDest.exists() || isInWorkspace) {
            String message = String.format(getContext().getString(R.string.book_already_downloaded), lang);
            Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        Uri target = Uri.fromFile(downloadDest);
        request.setDestinationUri(target);
        request.setDescription(sourceUrl);
        long downloadId = mDownloadManager.enqueue(request);
        int dmStatus = downloadId == -1 ? DownloadManager.STATUS_FAILED : DownloadManager.STATUS_RUNNING;
        Workspace.INSTANCE.logDownloadEvent(getContext(), Workspace.DOWNLOAD_TEMPLATE_TYPE.BLOOM_BOOK, Workspace.DOWNLOAD_EVENT.START,
                downloadId, dmStatus, url, sourceUrl, "");
        showDownloadProgress(downloadId, downloadDest);
    }


    @Override
    public void onDetachedFromWindow() {
        getContext().unregisterReceiver(mDownloadReceiver);
        sInstances.remove(this);
        super.onDetachedFromWindow();
    }

    private void cleanupDownloadDirectory() {
        if (mDownloadsInProgress.size() > 0) {
            return; // unsafe to clean up, may be in use
        }
        File downloadDir = getDownloadDir();
        File[] downloadDirFiles = downloadDir.listFiles();
        if (downloadDirFiles != null) {
            for (File leftover : downloadDirFiles) {
//                leftover.delete();
            }
        }
    }

    private void showDownloadProgress(long downloadId, File dest) {
        // We save the information about current downloads in a dictionary because it's possible that
        // the user has more than one download going on, and we need this information in our
        // downloadReceiver function that gets called when the download is complete. This also helps
        // keep track of how many downloads are in progress.
        if (mDownloadsInProgress.get(downloadId) != null) {
            // When initializing, we can find several references to the same download-in-progress.
            // (Or it may have just been that I was calling the function repeatedly. In any case,
            // a good precaution.)
            return;
        }
        if (mDownloadsInProgress.size() > 0) { // will be > 1, after we add this one
            mProgressView.setBook(""); // puts it in multiple downloads mode
            // remember that we've been in this state, it affects how we report completion.
            mRecentMultipleDownloads = true;
        } else {
            // we'll add one for the current single download.
            mProgressView = new DownloadProgressView(getContext(), this, downloadId);
            mProgressView.setBook(dest.getPath());
            if (getChildCount() > 0) {
                removeViewAt(0); // maybe a previous message about a successful delivery
            }
            addView(mProgressView);
            updateLayoutForChangedChildList();
        }
        mDownloadsInProgress.put(downloadId, new DownloadData(dest.getPath()));
        if (mDownloadsInProgress.size() == 1) {
            startMonitoringDownloads();
        }
    }

    private final BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {

        // Remember, this is a method of the BroadcastReceiver stored in downloadReceiver, not
        // of the parent class. We override this to receive messages when the download manager
        // broadcasts them.
        @Override
        public void onReceive(Context context, Intent intent) {

            //check if the broadcast message is for one of out our Enqueued downloads
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadData data = mDownloadsInProgress.get(downloadId);
            if (data != null) { // otherwise, for some reason we're getting a notification about a download we didn't start!
                String action = intent.getAction();
                if (action != null && action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    Cursor cursor = mDownloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                    if (cursor.moveToFirst()) {
                        String downloadUri = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_URI);
                        String downloadDescription = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_DESCRIPTION);
                        mDownloadsInProgress.remove(downloadId);
                        handleDownloadComplete(data.destPath, downloadId, downloadUri, downloadDescription);
                        if (mDownloadsInProgress.size() == 0) {
                            cleanupDownloadDirectory();
                            //  We may have just finished multiple ones, but if so, we already put
                            // up a BookReadyView in multiple mode. Any subsequent single download
                            // will count as a single.
                            mRecentMultipleDownloads = false;
                        }
                    }

                    cursor.close();
                }
            }
        }
    };

    private void handleDownloadComplete(String downloadDestPath, long downloadId, String uri, String pageUrl) {
        File source = new File(downloadDestPath);
        if (!source.exists()) {
            // just ignore any download we think we got that didn't result in a file.
            // Also, if there is more than one DownloadsView in different activities,
            // presumably one of them will win and move the file; the other will not find it.
            // Review: do we need thread locking to ensure this?

            // We don't want to hear about this again! Later, we might be reloading
            // the list while we're also re-doing the download. Then we might think the
            // incomplete download was complete. This may also help our downloads, which are no
            // longer where the DownloadManager put them, from showing up in the device's
            // downloads list.
            mDownloadManager.remove(downloadId);
            return;
        }

        // Log a successful download with Firebase analytics
        Workspace.INSTANCE.logDownloadEvent(getContext(), Workspace.DOWNLOAD_TEMPLATE_TYPE.BLOOM_BOOK, Workspace.DOWNLOAD_EVENT.COMPLETE,
                downloadId, DownloadManager.STATUS_SUCCESSFUL, uri, pageUrl, "");

        // The download has completed so make the app-hosted webview downloads page go back
        // to the initial starting view, ready for the next webview download command

        while (mBloomActivity.mBrowser.canGoBack()) {
            mBloomActivity.mBrowser.goBack();
        }

        // initiate the SP UpdateStories() method to process the downloaded story template
        if (MainActivity.Companion.getMainActivity() != null) {
            MainActivity.Companion.getMainActivity().controller.updateStories(false); // process all the downloaded templates
        }

        mBloomActivity.onBackPressed(); // go back to main SP activity

    }

// TODO: Is the equivalent needed in SP?
//    private void ReportDownloadAnalytics(String downloadDescription, File dest) {
//        String bookDbId = "";
//        String lang = "";
//        if (downloadDescription != null) {
//            // In the downloadDescription we capture the URL of the book instance page that requested the download.
//            // Something like https://alpha.bloomlibrary.org/app-hosted-v1/language:af/book/FONq0aa85h?lang=af
//            int lastSlash = downloadDescription.lastIndexOf('/');
//            int queryIndex = downloadDescription.lastIndexOf('?');
//            if (queryIndex < 0) {
//                queryIndex = downloadDescription.length();
//            }
//            if (lastSlash >= 0 && queryIndex > lastSlash) {
//                bookDbId = downloadDescription.substring(lastSlash + 1, queryIndex);
//            }
//            int lastEquals = downloadDescription.lastIndexOf("lang=");
//            if (lastEquals > 0) {
//                lang = downloadDescription.substring(lastEquals + 5);
//            }
//        }
//
//        Properties props = new Properties();
//        BloomFileReader reader = new BloomFileReader(getContext(), dest.getPath());
//        props.putValue("bookInstanceId", reader.getStringMetaProperty("bookInstanceId", ""));
//        props.putValue("title", reader.getStringMetaProperty("title", ""));
//        props.putValue("originalTitle", reader.getStringMetaProperty("originalTitle", ""));
//        props.putValue("brandingProjectName", reader.getStringMetaProperty("brandingProjectName", ""));
//        props.putValue("publisher", reader.getStringMetaProperty("publisher", ""));
//        props.putValue("originalPublisher", reader.getStringMetaProperty("originalPublisher", ""));
//        props.putValue("bookDbId", bookDbId);
//        props.putValue("lang", lang);
//        props.putValue("downloadSourceUrl", downloadDescription);
//        BloomReaderApplication.ReportAnalyticsWithLocationIfPossible(getContext(), "Download Book", props);
//    }

    // Called by the ViewBooks button in the BookReady view.
    // If we downloaded just one book, open it. Otherwise, return to the main Books view.
    public void viewBooks(String bookPath) {
        // If we're closing this in one instance, we want to get rid of it in all of them.
        // Currently this is simplified since we only ever have one.
        for (DownloadsView instance : sInstances) {
            if (instance.getChildCount() > 0) {
                instance.removeViewAt(0);
            }
        }
        if (!"".equals(bookPath)) {
//            MainActivity.launchReader(getContext(), bookPath, null);
        } else if (getContext() instanceof BloomLibraryActivity) {
            // View Books button kicks us back to the main activity. Would need enhancing if BloomLibraryActivity
            // could be launched from elsewhere, or if DownloadsView were embedded in more places.
            ((BloomLibraryActivity) getContext()).finish();
        }
    }

}

