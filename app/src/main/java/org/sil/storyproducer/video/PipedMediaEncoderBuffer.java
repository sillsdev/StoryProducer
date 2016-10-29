package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaEncoderBuffer extends PipedMediaCodecBuffer {
    public PipedMediaEncoderBuffer(MediaFormat format) throws IOException {
        super(format);
        mCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);
        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        start();
    }

    @Override
    protected String getComponentName() {
        return "encoder";
    }
}
