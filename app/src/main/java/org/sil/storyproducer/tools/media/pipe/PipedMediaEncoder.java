package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;

/**
 * <p>This media pipeline component provides a simple encoder encapsulating a {@link MediaCodec}.
 * Therefore, it takes a raw media stream and outputs an encoded media stream.</p>
 * <p>Common source for this component include {@link PipedMediaDecoder}
 * or any child class of {@link PipedAudioShortManipulator}.</p>
 */
public class PipedMediaEncoder extends PipedMediaCodecByteBufferDest {
    private static final String TAG = "PipedMediaEncoder";
    @Override
    protected String getComponentName() {
        return TAG;
    }

    private MediaFormat mConfigureFormat;
    private MediaFormat mSourceFormat;

    public PipedMediaEncoder(MediaFormat format) {
        mConfigureFormat = format;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.getTypeFromFormat(mConfigureFormat);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mSource.setup();
        mSourceFormat = mSource.getOutputFormat();

        //audio keys
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CHANNEL_COUNT);
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_SAMPLE_RATE);

        //video keys
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_WIDTH);
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_HEIGHT);
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_COLOR_FORMAT);
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_FRAME_RATE);
        //TODO: worrry about KEY_CAPTURE_RATE being API 21+
        //MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CAPTURE_RATE);

        //TODO: Make buffers appropriate size
        //encoder input buffers are too small, by default, to handle some decoder output buffers
        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mComponentState = State.SETUP;

        start();
    }
}
