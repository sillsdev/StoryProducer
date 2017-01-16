package org.sil.storyproducer.tools.media.story;

import android.graphics.Rect;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.tools.FileSystem;
import org.sil.storyproducer.tools.media.KenBurnsEffect;
import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.File;

/**
 * This class generates a sample story video to illustrate how {@link StoryMaker} works.
 */
@Deprecated //TODO: remove this class
public class SampleStory extends Thread {
    private static final String TAG = "SampleStory";

    private static final String STORY = "Fiery Furnace";

    // size of a frame, in pixels
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private static final long SLIDE_TRANSITION_US = 3000000;
    private static final long AUDIO_TRANSITION_US = 500000;

    private final File OUTPUT_DIR;
    private final File OUTPUT_FILE;

    private final File IMG_1;
    private final File IMG_2;
    private final File SOUNDTRACK;
    private final File NARRATION_1;
    private final File NARRATION_2;

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

    public SampleStory() {
        OUTPUT_DIR = FileSystem.getProjectDirectory("Fiery Furnace");
        OUTPUT_FILE = new File(OUTPUT_DIR, "0SampleStory." + WIDTH + "x" + HEIGHT + ".mp4");

        IMG_1 = FileSystem.getImageFile(STORY, 1);
        IMG_2 = FileSystem.getImageFile(STORY, 4);
        SOUNDTRACK = FileSystem.getSoundtrackAudio(STORY, 0);
        NARRATION_1 = FileSystem.getNarrationAudio(STORY, 0);
        NARRATION_2 = FileSystem.getNarrationAudio(STORY, 1);
    }

    @Override
    public void run() {
        System.out.println("Starting to make story...");
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

        KenBurnsEffect kbfx1 = new KenBurnsEffect(new Rect(300, 150, 800, 500), StoryBitmapManager.getDimensions(IMG_1.getPath()));
        Rect r2 = StoryBitmapManager.getDimensions(IMG_2.getPath());
        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "image 2 rectangle: (" + r2.left + ", " + r2.top + ", "
                    + r2.right + ", " + r2.bottom + ")");
        }
        KenBurnsEffect kbfx2 = new KenBurnsEffect(new Rect(0, 200, r2.right - 500, r2.bottom - 200), new Rect(500, 200, r2.right, r2.bottom - 200));

        StoryPage[] pages = {
                new StoryPage(IMG_1, NARRATION_1, kbfx1),
                new StoryPage(IMG_2, NARRATION_2, kbfx2),
        };

        StoryMaker maker = new StoryMaker(OUTPUT_FILE, outputFormat, videoFormat, audioFormat,
                pages, SOUNDTRACK, AUDIO_TRANSITION_US, SLIDE_TRANSITION_US);
        maker.churn();
    }
}
