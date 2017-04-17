package org.sil.storyproducer.controller.community;

import android.app.AlertDialog;
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
import org.sil.storyproducer.controller.logging.ComChkEntry;
import org.sil.storyproducer.controller.logging.Logging;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.AudioFiles;

/**
 * This class handles the layout inflation for the audio comment list
 */

public class CommentListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int slidePosition;
    private final CommunityCheckFrag commCheck;

    public CommentListAdapter(Context context, String[] values, int slidePosition, CommunityCheckFrag commCheck) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePosition;
        this.commCheck = commCheck;
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

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commCheck.playComment(values[position]);
                Logging.saveLogEntry(ComChkEntry.Type.cmt_pb.makeEntry());
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
        AlertDialog dialog = new AlertDialog.Builder(commCheck.getContext())
                .setTitle(commCheck.getString(R.string.comment_delete_title))
                .setMessage(commCheck.getString(R.string.comment_delete_message))
                .setNegativeButton(commCheck.getString(R.string.no), null)
                .setPositiveButton(commCheck.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AudioFiles.deleteComment(StoryState.getStoryName(), slidePosition, values[position]);
                        commCheck.updateCommentList();
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
        final EditText newName = new EditText(commCheck.getContext());

        // Programmatically set layout properties for edit text field
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        // Apply layout properties
        newName.setLayoutParams(params);

        AlertDialog dialog = new AlertDialog.Builder(commCheck.getContext())
                .setTitle(commCheck.getString(R.string.comment_rename_title))
                .setView(newName)
                .setNegativeButton(commCheck.getString(R.string.cancel), null)
                .setPositiveButton(commCheck.getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newNameText = newName.getText().toString();
                        AudioFiles.RenameCode returnCode = AudioFiles.renameComment(StoryState.getStoryName(), slidePosition, values[position], newName.getText().toString());
                        switch(returnCode) {

                            case SUCCESS:
                                commCheck.updateCommentList();
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
