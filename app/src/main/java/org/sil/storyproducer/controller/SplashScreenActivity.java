package org.sil.storyproducer.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sil.storyproducer.R;

import java.util.Map;

public class SplashScreenActivity extends AppCompatActivity {
    //Time in ms for splash screen to be shown
    private static int TIME_OUT = 500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Checks registration file to see if email has been sent and launches registration if it hasn't
                SharedPreferences prefs = getSharedPreferences(getString(R.string.registration_filename), MODE_PRIVATE);
                Map<String, ?> preferences = prefs.getAll();
                Object registrationComplete = preferences.get(RegistrationActivity.EMAIL_SENT);
                if (registrationComplete == null || !(Boolean)registrationComplete) {
                    Intent intent = new Intent(SplashScreenActivity.this, RegistrationActivity.class);
                    intent.putExtra(RegistrationActivity.FIRST_ACTIVITY_KEY, true);
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
