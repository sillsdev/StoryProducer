package org.sil.storyproducer.controller;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Workspace;

public class SplashScreenActivity extends AppCompatActivity {
    //Time in ms for splash screen to be shown
    private static int TIME_OUT = 300;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Workspace.INSTANCE.initializeWorkspace(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!Workspace.INSTANCE.getWorkspace().isDirectory()){
                    Intent intent = new Intent(SplashScreenActivity.this, WorkspaceDialogUpdateActivity.class);
                    startActivity(intent);
                } else if (! Workspace.INSTANCE.getRegistration().getRegistrationComplete()) {
                    Intent intent = new Intent(SplashScreenActivity.this, RegistrationActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }, TIME_OUT);
    }
}
