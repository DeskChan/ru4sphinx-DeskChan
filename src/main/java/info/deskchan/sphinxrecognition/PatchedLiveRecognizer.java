package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Microphone;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import java.io.IOException;

public class PatchedLiveRecognizer extends AbstractSpeechRecognizer {

    private final Microphone microphone;

    /**
     * Constructs new live recognition object.
     *
     * @param configuration common configuration
     * @throws IOException if model IO went wrong
     */
    public PatchedLiveRecognizer(Configuration configuration, Microphone microphone) throws IOException
    {
        super(configuration);
        this.microphone = microphone;
        microphone.stopRecording();
        context.getInstance(StreamDataSource.class)
                .setInputStream(microphone.getStream());
    }

    /**
     * Starts recognition process.
     *
     * @see         edu.cmu.sphinx.api.LiveSpeechRecognizer#stopRecognition()
     */
    public void startRecognition() {
        recognizer.allocate();
        microphone.startRecording();
    }

    /**
     * Stops recognition process.
     *
     * Recognition process is paused until the next call to startRecognition.
     *
     * @see edu.cmu.sphinx.api.LiveSpeechRecognizer#startRecognition(boolean)
     */
    public void stopRecognition() {
        microphone.stopRecording();
        recognizer.deallocate();
    }
}

