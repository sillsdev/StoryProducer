package org.sil.storyproducer.media.pipe;

import android.media.MediaFormat;

import java.io.Closeable;
import java.io.IOException;

/**
 * <p>Describes a component of the media pipeline which provides data to its proceeding component.</p>
 * <p>Note: This interface is only intended to be used by other media pipeline components.</p>
 */
public interface PipedMediaSource extends Closeable {
    /**
     * <p>Initialize this component.</p>
     * <p>Note: This method should be called <b>after the pipeline is fully constructed</b>.</p>
     * @throws IOException
     */
    void setup() throws IOException, SourceUnacceptableException;

    /**
     * Get the output format from this component. The returned format should not be modified.
     * @return
     */
    MediaFormat getOutputFormat();

    /**
     * @return whether this component has finished providing output.
     */
    boolean isDone();

    /**
     * Close the component <b>without throwing any exceptions</b>.
     */
    @Override
    void close();

    enum State {
        UNINITIALIZED,
        SETUP,
        RUNNING,
        CLOSED,
        ;
    }
}
