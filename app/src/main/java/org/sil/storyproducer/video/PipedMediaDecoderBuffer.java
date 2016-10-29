package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaDecoderBuffer extends PipedMediaCodecBuffer {
    private MediaFormat mSourceFormat;

    public PipedMediaDecoderBuffer() { }

    @Override
    public MediaHelper.MediaType getType() {
        return MediaHelper.getTypeFromFormat(mSourceFormat);
    }

    @Override
    public void setup() throws IOException {
        mSource.setup();
        mSourceFormat = mSource.getFormat();
        mCodec = MediaCodec.createDecoderByType(mSourceFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mSourceFormat, null, null, 0);
        start();
    }

    @Override
    protected String getComponentName() {
        return "decoder";
    }
}
