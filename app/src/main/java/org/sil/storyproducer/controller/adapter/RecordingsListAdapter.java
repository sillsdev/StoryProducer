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
import org.sil.storyproducer.controller.draft.DraftListRecordingsModal;
import org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;

/**
 * This class handles the layout inflation for an audio recording list
 */

public class RecordingsListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int slidePosition;
    private ClickListeners listeners;
    private String deleteTitle;
    private String deleteMessage;

    public RecordingsListAdapter(Context context, String[] values, int slidePosition, ClickListeners listeners) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePosition;
        this.listeners = listeners;
    }

    public interface ClickListeners {
        void onRowClick(String name);
        void onPlayClick(String name);
        void onDeleteClick(String name);
        AudioFiles.RenameCode onRenameClick(String name, String newName);
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
                    listeners.onRowClick(values[position]);
                }
            });
            titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listeners.onRowClick(values[position]);
                }
            });
            if(listeners instanceof DraftListRecordingsModal &&
                    StorySharedPreferences.getDraftForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(values[position])) {
                setUiForSelectedView(rowView, deleteButton, playButton);
            }
            if(listeners instanceof DramaListRecordingsModal &&
                    StorySharedPreferences.getDramatizationForSlideAndStory(slidePosition, StoryState.getStoryName()).equals(values[position])) {
                setUiForSelectedView(rowView, deleteButton, playButton);
            }
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listeners.onPlayClick(values[position]);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteItemDialog(position);
            }
        });

        titleView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showItemRenameDialog(position);
                return true;
            }
        });
        return rowView;
    }

    private void setUiForSelectedView(View rowView, ImageButton deleteButton, ImageButton playButton) {
        rowView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
        deleteButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));      //have to set the background here as well so the corners are the right color
        playButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
    }

    /**
     * Shows a dialog to the user asking if they really want to delete the recording
     *
     * @param position the integer position of the recording where the button was pressed
     */
    private void showDeleteItemDialog(final int position) {
        String title = context.getResources().getString(R.string.delete_comment_title);
        String message = context.getResources().getString(R.string.delete_comment_message);
        if(listeners instanceof DraftListRecordingsModal) {
            title = context.getResources().getString(R.string.delete_draft_title);
            message = context.getResources().getString(R.string.delete_draft_message);
        } else if(listeners instanceof  DramaListRecordingsModal) {
            title = context.getResources().getString(R.string.delete_dramatize_title);
            message = context.getResources().getString(R.string.delete_dramatize_message);
        }
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(context.getString(R.string.no), null)
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listeners.onDeleteClick(values[position]);
                    }
                }).create();

        dialog.show();
    }

    public void setDeleteTitle(String title) {
        deleteTitle = title;
    }

    public void setDeleteMessage(String message) {
        deleteMessage = message;
    }

    /**
     * Show to the user a dialog to rename the audio comment
     *
     * @param position the integer position of the comment the user "long-clicked"
     */
    private void showItemRenameDialog(final int position) {
        final EditText newName = new EditText(context);

        // Programmatically set layout properties for edit text field
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // Apply layout properties
        newName.setLayoutParams(params);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.rename_title))
                .setView(newName)
                .setNegativeButton(context.getString(R.string.cancel), null)
                .setPositiveButton(context.getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newNameText = newName.getText().toString();
                        AudioFiles.RenameCode returnCode = listeners.onRenameClick(values[position], newName.getText().toString());
                        switch(returnCode) {

                            case SUCCESS:
                                listeners.onRenameSuccess();
                                Toast.makeText(getContext(), context.getResources().getString(R.string.renamed_success), Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_LENGTH:
                                Toast.makeText(getContext(), context.getResources().getString(R.string.rename_must_be_20), Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_SPECIAL_CHARS:
                                Toast.makeText(getContext(), context.getResources().getString(R.string.rename_no_special), Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR_UNDEFINED:
                                Toast.makeText(getContext(), context.getResources().getString(R.string.rename_failed), Toast.LENGTH_SHORT).show();
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
