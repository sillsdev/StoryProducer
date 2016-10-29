package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaDecoderBuffer extends PipedMediaCodecBuffer {
    public PipedMediaDecoderBuffer(MediaFormat format) throws IOException {
        super(format);
        mCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        mCodec.configure(format, null, null, 0);
        start();
    }

    @Override
    protected String getComponentName() {
        return "decoder";
    }
}
