package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;

public class PatchedLiveRecognizer extends AbstractSpeechRecognizer implements Recognizer{

    private final ImprovedMicrophone microphone;
    private boolean recordingNow;
    private Thread listeningThread;

    private String cache = null;
    private Timer turnOffTimer = new Timer();

    private SpeechCallback callback;

    /**
     * Constructs new live recognition object.
     *
     * @param configuration common configuration
     * @throws IOException if model IO went wrong
     */
    public PatchedLiveRecognizer(Configuration configuration, ImprovedMicrophone microphone) throws IOException
    {
        super(replaceConfiguration(configuration));
        recordingNow = false;
        this.microphone = microphone;
    }

    private static Configuration replaceConfiguration(Configuration configuration){
        configuration.setLanguageModelPath("file:\\" + configuration.getLanguageModelPath());
        configuration.setDictionaryPath("file:\\" + configuration.getDictionaryPath());
        return configuration;
    }

    /**
     * Starts recognition process.
     *
     * @see         edu.cmu.sphinx.api.LiveSpeechRecognizer#stopRecognition()
     */
    public void startRecognition() {
        try {
            recognizer.allocate();
        } catch (Exception e){
            stopRecognition();
        }
        context.getInstance(StreamDataSource.class)
                .setInputStream(microphone.getStream());
        microphone.startRecording();
        listeningThread = new Thread(new Runnable() {
            @Override public void run() {
                cache = getHypothesis();
                stopRecognition();
                callback.run(getHypothesis());
            }
        });
        listeningThread.start();
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
        try {
            if (listeningThread != null && Thread.currentThread() != listeningThread)
                listeningThread.join();
            //listeningThread.interrupt();
        } catch (Exception e){ }
        try {
            recognizer.deallocate();
        } catch (Exception e){ }
        System.out.println("stopped");
    }

    public void setCallback(SpeechCallback callback){
        this.callback = callback;
    }

    public boolean isRecording(){
        return recordingNow;
    }

    public void free(){
        if (Thread.currentThread() != listeningThread)
            listeningThread.interrupt();
        listeningThread = null;
        stopRecognition();
    }

    public String getHypothesis(){
        String result;
        if (cache != null){
            result = cache;
        } else {
            result = getResult().getHypothesis();
           // result = new String(result.getBytes(Main.ENCODING), Charset.forName("UTF-8"));
        }
        cache = null;
        return result;
    }

    public void loadTransform(String path, int numClass) throws Exception {
        clusters = new ClusteredDensityFileData(context.getLoader(), numClass);
        Transform transform = new PatchedTransform((Sphinx3Loader)context.getLoader(), numClass);
        transform.load(path);
        context.getLoader().update(transform, clusters);
    }

    class PatchedTransform extends Transform {
        public PatchedTransform(Sphinx3Loader loader, int nrOfClusters) {
            super(loader, nrOfClusters);
        }

        public void load(String filePath) throws Exception {

            Scanner input = new Scanner(new File(filePath)).useLocale(Locale.US);
            int numStreams, nMllrClass;

            nMllrClass = input.nextInt();

            assert nMllrClass == 1;

            numStreams = input.nextInt();

            float[][][][] As = new float[nMllrClass][numStreams][][];
            float[][][] Bs = new float[nMllrClass][numStreams][];

            for (int i = 0; i < numStreams; i++) {
                int length = input.nextInt();

                As[0][i] = new float[length][length];
                Bs[0][i] = new float[length];

                for (int j = 0; j < length; j++) {
                    for (int k = 0; k < length; k++) {
                        As[0][i][j][k] = input.nextFloat();
                    }
                }
                for (int j = 0; j < length; j++) {
                    Bs[0][i][j] = input.nextFloat();
                }
                for (int j = 0; j < length; j++) {
                    // Skip MLLR variance scale
                    input.nextFloat();
                }
            }
            input.close();

            Field[] a = this.getClass().getSuperclass().getDeclaredFields();
            for (Field field : a){
                if (field.getName().equals("As")){
                    field.setAccessible(true);
                    field.set(this, As);
                }
                if (field.getName().equals("Bs")) {
                    field.setAccessible(true);
                    field.set(this, Bs);
                }
            }
        }
    }
}

