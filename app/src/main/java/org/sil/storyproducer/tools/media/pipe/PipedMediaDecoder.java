package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;

/**
 * <p>This media pipeline component provides a simple decoder encapsulating a {@link MediaCodec}.
 * Therefore, it takes an encoded media stream and outputs a raw media stream.</p>
 * <p>A common source for this component is {@link PipedMediaExtractor}.</p>
 */
public class PipedMediaDecoder extends PipedMediaCodecByteBufferDest {
    private static final String TAG = "PipedMediaDecoder";
    @Override
    protected String getComponentName() {
        return TAG;
    }

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
    public void setup() throws IOException, SourceUnacceptableException {
        if(mComponentState != State.UNINITIALIZED) {
            return;
        }

        mSource.setup();
        mSourceFormat = mSource.getOutputFormat();
        mCodec = MediaCodec.createDecoderByType(mSourceFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mSourceFormat, null, null, 0);

        mComponentState = State.SETUP;

        start();
    }
}
