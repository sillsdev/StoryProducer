package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.logging.DraftEntry;
import org.sil.storyproducer.tools.file.LogFiles;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;

import java.io.File;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public final class DraftFrag extends Fragment {
    private View rootView;
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private String storyName;
    private int slideNumber;
    private SlideText slideText;

    private AudioPlayer LWCAudioPlayer;
    private boolean LWCAudioExists;
    private ImageButton LWCPlayButton;

    private String recordFilePath;

    private RecordingToolbar recordingToolbar;

    public DraftFrag() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        storyName = StoryState.getStoryName();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        slideText = TextFiles.getSlideText(storyName, slideNumber);
        recordFilePath = AudioFiles.getDraft(storyName, slideNumber).getPath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);
        View rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);

        LWCPlayButton = (ImageButton)rootView.findViewById(R.id.fragment_draft_lwc_audio_button);

        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_draft_image_view), slideNumber);
        setScriptureText((TextView)rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText((TextView)rootView.findViewById(R.id.fragment_draft_reference_text));
        setLWCAudioButton(LWCPlayButton);
        setToolbar(rootViewToolbar);
        TextView slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + 1 + "");

        return rootView;
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar.closeToolbar();
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LWCAudioPlayer = new AudioPlayer();
        File LWCFile = AudioFiles.getLWC(storyName, slideNumber);
        if (LWCFile.exists()) {
            LWCAudioExists = true;
            LWCAudioPlayer.setPath(LWCFile.getPath());
        } else {
            LWCAudioExists = false;
        }

        LWCAudioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                LWCPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
            }
        });
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.closeToolbar();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();

        recordingToolbar.stopToolbarMedia();
        LWCAudioPlayer.release();
     
        if(recordingToolbar != null){
            recordingToolbar.closeToolbar();
            recordingToolbar.releaseToolbarAudio();
        }

    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_root_relayout_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_Relative_Layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));

            TextView tv = (TextView) rootView.findViewById(R.id.fragment_draft_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_draft_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(final ImageView slideImage, int slideNum) {
        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, R.string.dramatization_draft_no_picture, Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.4;
        height = (int) (height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(final TextView textView) {
        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private void setReferenceText(final TextView textView) {
        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }
        textView.setText(R.string.draft_bible_story);
    }

    /**
     * This function sets the LWC playback to the correct audio file. Also, the LWC narration
     * button will have a listener added to it in order to detect playback when pressed.
     *
     * @param LWCPlayButton the button to set the listeners for
     */
    private void setLWCAudioButton(final ImageButton LWCPlayButton) {
        LWCPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (!LWCAudioExists) {
                Snackbar.make(rootView, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show();
            } else {
                if(LWCAudioPlayer.isAudioPlaying()){
                    LWCAudioPlayer.stopAudio();
                    LWCPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                }else{
                    //stop other playback streams.
                    recordingToolbar.stopToolbarMedia();
                    LWCAudioPlayer.playAudio();
                    if(recordingToolbar != null){
                        recordingToolbar.onToolbarTouchStopAudio(LWCPlayButton, R.drawable.ic_menu_play, LWCAudioPlayer);
                    }

                    LWCPlayButton.setBackgroundResource(R.drawable.ic_stop_white_36dp);
                    Toast.makeText(getContext(), R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show();
                    LogFiles.saveLogEntry(DraftEntry.Type.LWC_PLAYBACK.makeEntry());
                }
            }
            }
        });
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar){
        if(rootView instanceof RelativeLayout){
            recordingToolbar = new RecordingToolbar(getActivity(),toolbar, (RelativeLayout)rootView, true, false, recordFilePath);
            recordingToolbar.keepToolbarVisible();
        }
    }



    /* Don't remove! below code  */
//    /**
//     * This function adds two different audio files together to make one audio file into an
//     * .mp3 file. More comments will be added to this function later.
//     */
//    private void ConcatenateAudioFiles() {
//        Movie finalFile = new Movie();
//        String writtenToAudioFile = String.format(recordFilePath.substring(0, recordFilePath.indexOf(".m4a")) + "final.m4a");
//        Movie movieArray[];
//
//        try {
//            if (!new File(recordFilePath).exists()) {
//                movieArray = new Movie[]{MovieCreator.build(tempRecordFilePath)};
//            } else {
//                movieArray = new Movie[]{MovieCreator.build(recordFilePath),
//                        MovieCreator.build(tempRecordFilePath)};
//            }
//
//            List<Track> audioTrack = new ArrayList<>();
//
//            for (int i = 0; i < movieArray.length; i++)
//                for (Track t : movieArray[i].getTracks()) {
//                    if (t.getHandler().equals("soun")) {
//                        audioTrack.add(t);
//                    }
//                }
//
//            if (!audioTrack.isEmpty()) {
//                finalFile.addTrack(new AppendTrack(audioTrack.toArray(new Track[audioTrack.size()])));
//            }
//
//            Container out = new DefaultMp4Builder().build(finalFile);
//
//            FileChannel fc = new RandomAccessFile(writtenToAudioFile, "rwd").getChannel();
//            out.writeContainer(fc);
//            fc.close();
//
//            tryDeleteFile(recordFilePath);
//            boolean renamed = (new File(writtenToAudioFile).renameTo(tryCreateFile(recordFilePath)));
//            if (renamed) {
//                //delete old file
//                tryDeleteFile(writtenToAudioFile);
//            }
//
//        } catch (IOException e) {
//            Log.e(getActivity().toString(), e.getMessage());
//        }
//    }
//
//    /**
//     * Tries to create a new file.
//     *
//     * @param filePath The file path where a file should be created at.
//     * @return The file instantiation of the file that was created at the filePath.
//     */
//    private File tryCreateFile(String filePath) {
//        File toReturnFile = new File(filePath);
//        if (!toReturnFile.exists()) {
//            try {
//                toReturnFile.setExecutable(true);
//                toReturnFile.setReadable(true);
//                toReturnFile.setWritable(true);
//                toReturnFile.createNewFile();
//            } catch (IOException e) {
//                Log.w(getActivity().toString(), "Could not create file for recording!");
//            }
//        }
//
//        return toReturnFile;
//    }
  
  /* Don't remove above code!! */

}
