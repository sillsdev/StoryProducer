package org.sil.storyproducer.controller.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.draft.DraftFrag;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.FileSystem;

/**
 * Created by andrewlockridge on 2/4/17.
 */

public class CommentListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int slidePosition;
    private final CommunityCheckFrag commCheck;

    public CommentListAdapter(Context context, String[] values, int slidePostion, CommunityCheckFrag commCheck) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.slidePosition = slidePostion;
        this.commCheck = commCheck;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.audio_comment_list_item, parent, false);
        TextView titleView = (TextView) rowView.findViewById(R.id.audio_comment_title);
        Button playButton = (Button) rowView.findViewById(R.id.audio_comment_play_button);
        Button deleteButton = (Button) rowView.findViewById(R.id.audio_comment_delete_button);

        titleView.setText(values[position]);

        //TODO: Comment filenames don't match up with position once one is deleted
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commCheck.playComment(position);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSystem.deleteAudioComment(StoryState.getStoryName(), slidePosition, position);
                commCheck.updateCommentList();
            }
        });

        return rowView;
    }

}
