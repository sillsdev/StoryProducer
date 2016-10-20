package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaDecoder extends PipedMediaCodec {
    public PipedMediaDecoder(MediaFormat format) throws IOException {
        mCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
        mCodec.configure(format, null, null, 0);
        start();
    }

    @Override
    protected String getComponentName() {
        return "decoder";
    }
}
