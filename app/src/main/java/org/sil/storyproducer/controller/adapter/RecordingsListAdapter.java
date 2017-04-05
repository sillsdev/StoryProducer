package org.sil.storyproducer.controller.adapter;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.community.CommunityCheckFrag;
import org.sil.storyproducer.controller.draft.DraftFrag;
import org.sil.storyproducer.controller.draft.DraftListRecordingsModal;
import org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;

/**
 * This class handles the layout inflation for the audio comment list
 */

public class RecordingsListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int slidePosition;
    private ClickListeners listeners;

    public RecordingsListAdapter(Context context, String[] values, int slidePosition, Fragment fragment) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePosition;
        if(fragment instanceof CommunityCheckFrag) {
            listeners = (CommunityCheckFrag) fragment;
        }
    }

    public RecordingsListAdapter(Context context, String[] values, int slidePosition, DraftListRecordingsModal modal) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePosition;
        if(modal instanceof DraftListRecordingsModal) {
            listeners = modal;
        }
    }

    public RecordingsListAdapter(Context context, String[] values, int slidePosition, DramaListRecordingsModal modal) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePosition;
        if(modal instanceof DramaListRecordingsModal) {
            listeners = modal;
        }
    }

    public interface ClickListeners {
        void onRowClickListener(String name);
        void onPlayClickListener(String name);
        void onDeleteClickListener(String name);
        AudioFiles.RenameCode onRenameClickListener(String name, String newName);
        void onRenameSuccess();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.audio_comment_list_item, parent, false);
        TextView titleView = (TextView) rowView.findViewById(R.id.audio_comment_title);
        ImageButton playButton = (ImageButton) rowView.findViewById(R.id.audio_comment_play_button);
        ImageButton deleteButton = (ImageButton) rowView.findViewById(R.id.audio_comment_delete_button);

        titleView.setText(values[position]);

        //things specifically for the modals
        if(listeners instanceof DramaListRecordingsModal || listeners instanceof  DraftListRecordingsModal) {
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listeners.onRowClickListener(values[position]);
                }
            });
            titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listeners.onRowClickListener(values[position]);
                }
            });
            if(listeners instanceof DraftListRecordingsModal &&
                    StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(values[position])) {
                rowView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                deleteButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));      //have to set the background here as well so the corners are the right color
                playButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
            }
            if(listeners instanceof DramaListRecordingsModal &&
                    StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(values[position])) {
                rowView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                deleteButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                playButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
            }
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listeners.onPlayClickListener(values[position]);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteCommentDialog(position);
            }
        });

        titleView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showCommentRenameDialog(position);
                return true;
            }
        });
        return rowView;
    }

    /**
     * Shows a dialog to the user asking if they really want to delete the comment
     *
     * @param position the integer position of the comment where the button was pressed
     */
    private void showDeleteCommentDialog(final int position) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.comment_delete_title))
                .setMessage(context.getString(R.string.comment_delete_message))
                .setNegativeButton(context.getString(R.string.no), null)
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listeners.onDeleteClickListener(values[position]);
                    }
                }).create();

        dialog.show();
    }

    /**
     * Show to the user a dialog to rename the audio comment
     *
     * @param position the integer position of the comment the user "long-clicked"
     */
    private void showCommentRenameDialog(final int position) {
        final EditText newName = new EditText(context);

        // Programmatically set layout properties for edit text field
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // Apply layout properties
        newName.setLayoutParams(params);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.comment_rename_title))
                .setView(newName)
                .setNegativeButton(context.getString(R.string.cancel), null)
                .setPositiveButton(context.getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newNameText = newName.getText().toString();
                        AudioFiles.RenameCode returnCode = listeners.onRenameClickListener(values[position], newName.getText().toString());
                        switch(returnCode) {

                            case SUCCESS:
                                listeners.onRenameSuccess();
                                Toast.makeText(getContext(), "File successfully renamed", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_LENGTH:
                                Toast.makeText(getContext(), "Filename must be less than 20 characters", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_SPECIAL_CHARS:
                                Toast.makeText(getContext(), "Filename cannot contain special characters", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_CONTAINED_DESIGNATOR:
                                Toast.makeText(getContext(), "Invalid filename", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_UNDEFINED:
                                Toast.makeText(getContext(), "Rename failed", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }).create();

        dialog.show();
        // show keyboard for renaming
        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

    }
}
