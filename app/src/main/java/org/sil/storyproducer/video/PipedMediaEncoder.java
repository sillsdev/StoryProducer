package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaEncoder extends PipedMediaCodec {
    public PipedMediaEncoder(MediaFormat format) throws IOException {
        mCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
//        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_INPUT_BUFFER_SIZE);
        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        start();
    }

    @Override
    protected String getComponentName() {
        return "encoder";
    }
}
