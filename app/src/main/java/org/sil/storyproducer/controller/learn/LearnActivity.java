package org.sil.storyproducer.controller.learn;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.FileSystem;

import java.io.IOException;

public class LearnActivity extends AppCompatActivity {

    ImageView learnImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        //get the image view and change the image
        learnImageView = (ImageView) findViewById(R.id.learnImageView);
        learnImageView.setImageBitmap(FileSystem.getImage("Fiery Furnace", 6));

        //play the audio for this scene
        MediaPlayer mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(FileSystem.getStoryPath("Fiery Furnace") + "/narration6.wav");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.start();
    }
}
