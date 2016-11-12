package org.sil.storyproducer.controller.learn;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.FileSystem;

import java.io.IOException;

public class LearnActivity extends AppCompatActivity {

    private ImageView learnImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        //get the image view and change the image
        learnImageView = (ImageView) findViewById(R.id.learnImageView);
        learnImageView.setImageBitmap(FileSystem.getImage("Fiery Furnace", 6));

        //TODO: change this test to actually running the learn activity
        //play the audio for this scene
        MediaPlayer mPlayer = new MediaPlayer();
        try {
           /* mPlayer.setDataSource(FileSystem.getStoryFile("Fiery Furnace").getAbsolutePath()
                    + "/narration6.wav"); */
            mPlayer.setDataSource(FileSystem.getNarrationAudio("Fiery Furnace", 6).getAbsolutePath());

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
