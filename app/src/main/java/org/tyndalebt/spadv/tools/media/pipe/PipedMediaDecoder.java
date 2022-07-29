package org.tyndalebt.spadv.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.tyndalebt.spadv.tools.media.MediaHelper;

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
        return MediaHelper.INSTANCE.getTypeFromFormat(mSourceFormat);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(getMComponentState() != State.UNINITIALIZED) {
            return;
        }

        getMSource().setup();
        mSourceFormat = getMSource().getOutputFormat();
        setMCodec(MediaCodec.createDecoderByType(mSourceFormat.getString(MediaFormat.KEY_MIME)));
        getMCodec().configure(mSourceFormat, null, null, 0);

        setMComponentState(State.SETUP);

        start();
    }
}
