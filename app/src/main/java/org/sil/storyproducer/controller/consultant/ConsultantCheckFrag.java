package org.sil.storyproducer.controller.consultant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.logging.LogView;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;

import java.io.File;

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
public class ConsultantCheckFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    public static final String CONSULTANT_PREFS = "Consultant_Checks";
    public static final String IS_CONSULTANT_APPROVED = "isApproved";
    private static final String IS_CHECKED = "isChecked";
    private static final String PASSWORD = "appr00ved";
    private String storyName;
    private int slidePosition;
    private View rootView;
    private boolean isChecked;
    private AudioPlayer draftPlayer;
    private SlideText slideText;
    private TextView slideTextView;
    private boolean draftAudioExists;
    private boolean draftAudioPaused;
    private ImageButton draftPlaybackButton;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_consultant_check, container, false);
        draftPlaybackButton = (ImageButton)rootView.findViewById(R.id.concheck_draft_playback_button);
        storyName = StoryState.getStoryName();
        slideText = TextFiles.getSlideText(storyName, slidePosition);

        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_concheck_image_view), slidePosition);
        setScriptureText((TextView)rootView.findViewById(R.id.fragment_concheck_scripture_text));
        setReferenceText((TextView)rootView.findViewById(R.id.fragment_concheck_reference_text));
        setDraftPlaybackButton((ImageButton)rootView.findViewById(R.id.concheck_draft_playback_button));
        setCheckmarkButton((ImageButton)rootView.findViewById(R.id.concheck_checkmark_button));
        setLogsButton((ImageButton)rootView.findViewById(R.id.concheck_logs_button));
        slideTextView = (TextView)rootView.findViewById(R.id.slide_number_text);
        slideTextView.setText(slidePosition + "");

        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_concheck);
    }
    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                draftPlayer.stopAudio();
                draftPlaybackButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        draftPlayer = new AudioPlayer();
        final File draftFile = AudioFiles.getDraft(storyName, slidePosition);
        if (draftFile.exists()) {
            draftAudioExists = true;
            draftPlayer.setPath(draftFile.getPath());
        } else {
            draftAudioExists = false;
        }
        draftAudioPaused = false;
        draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //TODO: use non-deprecated method; currently used to support older devices
                draftPlaybackButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
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
        draftPlayer.stopAudio();
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        draftPlayer.stopAudio();
        draftPlayer.release();
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slidePosition == 0){
            RelativeLayout rl =  (RelativeLayout)rootView.findViewById(R.id.concheck_relative_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout)rootView.findViewById(R.id.concheck_button_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            TextView tv = (TextView) rootView.findViewById(R.id.fragment_concheck_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_concheck_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(ImageView slideImage, int slideNum) {
        Bitmap slidePicture = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.25;
        height = (int)(height * scalingFactor);

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
    private void setScriptureText(TextView textView) {

        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private void setReferenceText(TextView textView) {
        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }
    }

    /**
     * This function sets the draft playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param button the ImageButton view handler to set the onclicklistener to
     */
    private void setDraftPlaybackButton(final ImageButton button) {
        //TODO: use non-deprecated method; currently used to support older devices
        button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop other playback streams.
                boolean wasPlaying = draftPlayer.isAudioPlaying();
                if (draftAudioExists && !wasPlaying) {
                    if (draftAudioPaused) {
                        draftPlayer.resumeAudio();
                        draftAudioPaused = false;
                    } else {
                        draftPlayer.playAudio();
                    }
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_pause_blue, null));
                    Toast.makeText(getContext(), "Playing Draft Audio...", Toast.LENGTH_SHORT).show();
                } else if (wasPlaying) {
                    draftPlayer.pauseAudio();
                    draftAudioPaused = true;
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
                } else {
                    Toast.makeText(getContext(), "No Draft Audio Found...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Sets on click listener for consultant to check off the slide and approve
     * @param button the check button
     */
    private void setCheckmarkButton(final ImageButton button) {
        final SharedPreferences prefs = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        final String prefsKeyString = storyName + slidePosition + IS_CHECKED;
        isChecked = prefs.getBoolean(prefsKeyString, false);
        if(isChecked) {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_green, null));
        } else {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_red, null));
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isApproved = prefs.getBoolean(storyName + IS_CONSULTANT_APPROVED, false);
                if (isApproved) {
                    Toast.makeText(getContext(), "Story already approved", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(isChecked) {
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_red, null));
                    isChecked = false;
                    prefsEditor.putBoolean(prefsKeyString, false);
                    prefsEditor.apply();
                } else {
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_checkmark_green, null));
                    isChecked = true;
                    prefsEditor.putBoolean(prefsKeyString, true);
                    prefsEditor.commit();
                    if(checkAllMarked()) {
                        showConsultantPasswordDialog();
                    }
                }
            }
        });
    }

    /**
     * Set an on click listener to launch the interface to view the logs for that slide
     * @param button the logs button
     */
    private void setLogsButton(ImageButton button) {
        //TODO: use non-deprecated method; currently used to support older devices
        button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_logs_blue, null));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogView.makeModal(getContext());
            }
        });
    }

    /**
     * Checks each slide of the story to see if all slides have been approved
     * @return true if all approved, otherwise false
     */
    private boolean checkAllMarked() {
        boolean marked;
        SharedPreferences prefs = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE);
        int numStorySlides = FileSystem.getContentSlideAmount(storyName);
        for (int i = 0; i < numStorySlides; i ++) {
            marked = prefs.getBoolean(storyName + i + IS_CHECKED, false);
            if (!marked) {
                return false;
            }
        }
        return true;
    }

    /**
     * Launches a dialog for the consultant to enter a password once all slides approved
     */
    private void showConsultantPasswordDialog() {
        final EditText password = new EditText(getContext());
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Programmatically set layout properties for edit text field
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // Apply layout properties
        password.setLayoutParams(params);
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.consultant_password_title))
                .setMessage(getString(R.string.consultant_password_message))
                .setView(password)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.submit), null)
                .create();
        // This is set to dismiss the keyboard manually on dialog dismiss
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                toggleKeyboard(false, getView());
            }
        });

        // This manually sets the submit button listener so that the dialog doesn't always submit
        // If the password is incorrect, we want to stay on the dialog and give an error message
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialog) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String passwordText = password.getText().toString();
                        if (passwordText.contentEquals(PASSWORD)) {
                            saveConsultantApproval();
                            dialog.dismiss();
                            launchDramatizationPhase();
                        } else {
                            password.setError(getString(R.string.consultant_incorrect_password_message));
                        }
                    }
                });
            }
        });

        dialog.show();
        toggleKeyboard(true, password);
    }

    /**
     * Updates the shared preference file to mark the story as approved
     */
    private void saveConsultantApproval() {
        SharedPreferences.Editor prefsEditor = getActivity().getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE).edit();
        prefsEditor.putBoolean(storyName + IS_CONSULTANT_APPROVED, true);
        prefsEditor.apply();
    }

    /**
     * Launches the dramatization phase for the story and starts back at first slide
     * TODO: moving back to first slide is currently broken
     */
    private void launchDramatizationPhase() {
        Toast.makeText(getContext(), "Congrats!", Toast.LENGTH_SHORT).show();
        int dramatizationPhaseIndex = 4;
        Phase[] phases = StoryState.getPhases();
        StoryState.setCurrentPhase(phases[dramatizationPhaseIndex]);
        Intent intent = new Intent(getContext(), StoryState.getCurrentPhase().getTheClass());
        intent.putExtra(SLIDE_NUM, 0);
        getActivity().startActivity(intent);
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private void toggleKeyboard(boolean showKeyBoard, View aView) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (showKeyBoard) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            imm.hideSoftInputFromWindow(aView.getWindowToken(), 0);
        }
    }

}
