package org.sil.storyproducer.controller.remote;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;

/**
 * Created by annmcostantino on 11/4/2017.
 */

public class SubmissionRemoteConsultantActivity extends PhaseBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_remote_consultant);

    }

    //TODO: new icon
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_concheck);
        return true;
    }
}
