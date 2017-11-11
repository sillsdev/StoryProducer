package org.sil.storyproducer.controller.remote;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;

/**
 * Created by annmcostantino on 11/4/2017.
 */

public class SubmissionRemoteConsultantActivity extends PhaseBaseActivity {

    private Button submitButton;
    private TextView submissionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_remote_consultant);
        submitButton = (Button)findViewById(R.id.submit_backtranslations);
        submissionText = (TextView)findViewById(R.id.submission_status_text);
        setSubmissionStatusText();
    }

    //TODO: new icon
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_concheck);
        return true;
    }

    //TODO: Subroutine: Change text and button visibility based on conditions.

    private void setSubmissionStatusText(){
        //can submit
        //cond: not all slides have a recording
        submissionText.setText(R.string.recordings_ready);
        submitButton.setVisibility(View.VISIBLE);

        //sent & awaiting response
        //cond: all slides have yellow status
        submissionText.setText(R.string.recordings_sent);
        submitButton.setVisibility(View.GONE);

        //not all accepted please re-record appropriate slides and resubmit
        // cond:any slide has red status
        submissionText.setText(R.string.recordings_disapproved);
        submitButton.setVisibility(View.GONE);

        //all accepted
        //cond: all slides green status
        submissionText.setText(R.string.recordings_approved);
        submitButton.setVisibility(View.GONE);
    }
}
