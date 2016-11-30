package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;

public class PipedMediaEncoder extends PipedMediaCodecByteBufferDest {
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
        MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CAPTURE_RATE);

        //TODO: Make buffers appropriate size
        //encoder input buffers are too small by default
        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mComponentState = State.SETUP;

        start();
    }

    @Override
    protected String getComponentName() {
        return "encoder";
    }
}
