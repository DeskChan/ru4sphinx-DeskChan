package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

public class PatchedLiveRecognizer extends AbstractSpeechRecognizer {

    private final ImprovedMicrophone microphone;
    private boolean recordingNow;
    protected static Charset ENCODING;
    private String cache = null;
    private Timer turnOffTimer = new Timer();

    /**
     * Constructs new live recognition object.
     *
     * @param configuration common configuration
     * @throws IOException if model IO went wrong
     */
    public PatchedLiveRecognizer(Configuration configuration, ImprovedMicrophone microphone) throws IOException
    {
        super(configuration);
        recordingNow = false;
        ENCODING = Charset.forName( System.getProperty("os.name").toLowerCase().contains("win") ? "WINDOWS-1251" : "UTF-8" );

        this.microphone = microphone;
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
        turnOffTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            new Thread(new Runnable() {
                @Override public void run() {
                    while(true) {
                        //cache =  getHypothesis();
                        System.out.println("result: " + getHypothesis());
                    }
                    //microphone.stopRecording();
                    //stopRecognition();
                }
            }).start();
            }
        }, 1000 * (Main.getPluginProxy() != null ? Main.getPluginProxy().getProperties().getInteger("recordingTimeOut", 5) : 5));
        recordingNow = true;
    }

    /**
     * Stops recognition process.
     *
     * Recognition process is paused until the next call to startRecognition.
     *
     * @see edu.cmu.sphinx.api.LiveSpeechRecognizer#startRecognition(boolean)
     */
    public void stopRecognition() {
        recordingNow = false;
        microphone.stopRecording();
        recognizer.deallocate();
        microphone.reset();
        System.out.println("result: "+getHypothesis());
    }

    public boolean isRecording(){
        return recordingNow;
    }

    public String getHypothesis(){
        String result;
        if (cache != null){
            result = cache;
        } else {
            System.out.println("waiting");
            result = getResult().getHypothesis();
            result = new String(result.getBytes(ENCODING), Charset.forName("UTF-8"));
        }
        cache = null;
        return result;
    }
}

