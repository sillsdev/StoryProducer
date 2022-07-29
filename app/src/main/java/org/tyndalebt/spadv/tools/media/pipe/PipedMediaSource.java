package org.tyndalebt.spadv.tools.media.pipe;

import android.media.MediaFormat;

import org.tyndalebt.spadv.tools.media.MediaHelper;

import java.io.Closeable;
import java.io.IOException;

/**
 * <p>Describes a component of the media pipeline which provides data to its proceeding component.</p>
 * <p>Note: This interface is only intended to be used by other media pipeline components.</p>
 */
public interface PipedMediaSource extends Closeable {
    /**
     * Note: This function should only be called after {@link #setup()}.
     * @return the type of media this component provides.
     */
    MediaHelper.MediaType getMediaType();

    /**
     * <p>Initialize this component. Generally, setup will be called recursively,
     * allowing each component to retrieve its source's output format after setting up.</p>
     * <p>Note: This method should be called <b>after the pipeline is fully constructed</b>.</p>
     * @throws IOException
     */
    void setup() throws IOException, SourceUnacceptableException;

    /**
     * <p>Get the output format from this component. The returned format should not be modified.</p>
     * <p>Note: This function should only be called after {@link #setup()}.</p>
     * @return
     */
    MediaFormat getOutputFormat();

    /**
     * Query the component to find out if it is done.
     * @return whether this component has finished providing output.
     */
    boolean isDone();

    //TODO: add reset method

    /**
     * Close the component <b>without throwing any exceptions</b>.
     * The component should not be used after a call to this method.
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
