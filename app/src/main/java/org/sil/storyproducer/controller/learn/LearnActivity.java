package org.sil.storyproducer.controller.learn;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.FileSystem;

import java.io.IOException;

public class LearnActivity extends AppCompatActivity {

    ImageView learnImageView;
    int slideNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        //turn on the background music
        AudioPlayer backgroundPlayer = new AudioPlayer();
        backgroundPlayer.playWithPath(FileSystem.getStoryPath("Fiery Furnace") + "/SoundTrack0.mp3");
        backgroundPlayer.setVolume(0.4f);

        //get the image view and change the image
        learnImageView = (ImageView) findViewById(R.id.learnImageView);

        playVideo();
    }

    /**
     * Plays the video and runs everytime the audio is completed
     */
    void playVideo() {
        System.out.println("The audio stopped");
        learnImageView.setImageBitmap(FileSystem.getImage("Fiery Furnace", slideNum));          //set the next image
        AudioPlayer aPlayer = new AudioPlayer();                                                //set the next audio
        aPlayer.playWithPath(FileSystem.getStoryPath("Fiery Furnace") + "/narration" + slideNum + ".wav");
        aPlayer.audioCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //TODO: change this to actually count the amount of files
                if(slideNum < 23) {
                    playVideo();
                }
            }
        });
        slideNum++;         //move to the next slide
    }

}
