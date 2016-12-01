package org.sil.storyproducer.media.story;

import android.graphics.Rect;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.FileSystem;
import org.sil.storyproducer.media.KenBurnsEffect;
import org.sil.storyproducer.media.MediaHelper;

import java.io.File;

/**
 * This class generates a sample story video to illustrate how {@link StoryMaker} works.
 */
@Deprecated //TODO: remove this class
public class SampleStory extends Thread {
    private static final String TAG = "SampleStory";

    // size of a frame, in pixels
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
    private static final File OUTPUT_DIR = new File(FileSystem.getStoryPath("Fiery Furnace"));
    private static final File OUTPUT_FILE = new File(OUTPUT_DIR, "0SampleStory." + WIDTH + "x" + HEIGHT + ".mp4");

    private static final String IMG_1 = OUTPUT_DIR.getPath() + "/1.jpg";
    private static final String IMG_2 = OUTPUT_DIR.getPath() + "/4.jpg";
    private static final String SOUNDTRACK_PATH = OUTPUT_DIR.getPath() + "/SoundTrack0.mp3";
    private static final String NARRATION_PATH_1 = OUTPUT_DIR.getPath() + "/narration0.wav";
    private static final String NARRATION_PATH_2 = OUTPUT_DIR.getPath() + "/narration1.wav";

    // parameters for the video encoder
    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int VIDEO_FRAME_RATE = 30;               // 30fps
    private static final int VIDEO_IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    // bit rate, in bits per second
    // TODO: figure out more stable way of getting a number here; bad number causes bad problems here
    private static final int VIDEO_BIT_RATE = 32 * WIDTH * HEIGHT * VIDEO_FRAME_RATE / 100;

    // parameters for the audio encoder
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 48000;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_BITRATE = 64000;

    @Override
    public void run() {
        int outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, WIDTH, HEIGHT);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        MediaFormat audioFormat = MediaHelper.createFormat(AUDIO_MIME_TYPE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AUDIO_SAMPLE_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT);

        KenBurnsEffect kbfx1 = new KenBurnsEffect(new Rect(300, 150, 800, 500), StoryBitmapManager.getDimensions(IMG_1));
        Rect r2 = StoryBitmapManager.getDimensions(IMG_2);
        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "image 2 rectangle: (" + r2.left + ", " + r2.top + ", "
                    + r2.right + ", " + r2.bottom + ")");
        }
        KenBurnsEffect kbfx2 = new KenBurnsEffect(new Rect(0, 200, r2.right - 500, r2.bottom - 200), new Rect(500, 200, r2.right, r2.bottom - 200));

        StoryPage[] pages = {
                new StoryPage(new File(IMG_1), new File(NARRATION_PATH_1), kbfx1),
                new StoryPage(new File(IMG_2), new File(NARRATION_PATH_2), kbfx2),
        };

        File soundtrack = new File(SOUNDTRACK_PATH);

        long slideTransitionUs = 3000000;
        long audioTransitionUs = 500000;

        StoryMaker maker = new StoryMaker(OUTPUT_FILE, outputFormat, videoFormat, audioFormat,
                pages, soundtrack, audioTransitionUs, slideTransitionUs);
        maker.churn();
    }
}
