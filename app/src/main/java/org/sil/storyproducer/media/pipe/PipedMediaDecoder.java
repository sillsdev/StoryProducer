package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;

public class PipedMediaDecoder extends PipedMediaCodecByteBufferDest {
    private MediaFormat mSourceFormat;

    public PipedMediaDecoder() { }

    @Override
    public MediaHelper.MediaType getMediaType() {
        if(mSourceFormat == null) {
            return null;
        }
        return MediaHelper.getTypeFromFormat(mSourceFormat);
    }

    @Override
    public void setup() throws IOException {
        mSource.setup();
        mSourceFormat = mSource.getOutputFormat();
        mCodec = MediaCodec.createDecoderByType(mSourceFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mSourceFormat, null, null, 0);
        start();
    }

    @Override
    protected String getComponentName() {
        return "decoder";
    }
}
