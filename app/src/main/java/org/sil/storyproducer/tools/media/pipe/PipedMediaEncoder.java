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
        return MediaHelper.INSTANCE.getTypeFromFormat(mConfigureFormat);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(getMComponentState() != State.UNINITIALIZED) {
            return;
        }

        getMSource().setup();
        mSourceFormat = getMSource().getOutputFormat();

        //audio keys
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CHANNEL_COUNT);
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_SAMPLE_RATE);

        //video keys
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_WIDTH);
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_HEIGHT);
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_COLOR_FORMAT);
        MediaHelper.INSTANCE.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_FRAME_RATE);
        //TODO: worry about KEY_CAPTURE_RATE being API 21+
        //TODO This may be why the video playback is not synced with newer phones.
        //MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CAPTURE_RATE);

        //encoder input buffers are too small, by default, to handle some decoder output buffers
        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.INSTANCE.getMAX_INPUT_BUFFER_SIZE());

        setMCodec(MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME)));
        getMCodec().configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        setMComponentState(State.SETUP);

        start();
    }
}
