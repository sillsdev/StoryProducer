package org.sil.storyproducer.controller;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Workspace;

import static org.sil.storyproducer.controller.RegistrationActivityKt.FIRST_ACTIVITY_KEY;

public class SplashScreenActivity extends AppCompatActivity {
    //Time in ms for splash screen to be shown
    private static int TIME_OUT = 300;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Workspace.INSTANCE.initializeWorskpace(this);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Checks registration file to see if email has been sent and launches registration if it hasn't
                if (! Workspace.INSTANCE.getRegistration().getComplete()) {
                    Intent intent = new Intent(SplashScreenActivity.this, RegistrationActivity.class);
                    intent.putExtra(FIRST_ACTIVITY_KEY, true);
                    startActivity(intent);
                    return;
                }

                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, TIME_OUT);
    }
}
