package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.decoder.adaptation.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;

public class PatchedLiveRecognizer extends AbstractSpeechRecognizer implements Recognizer{

    private final ImprovedMicrophone microphone;
    private int recordingState;
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
    public PatchedLiveRecognizer(Configuration configuration, ImprovedMicrophone micro) throws IOException
    {
        super(replaceConfiguration(configuration));
        recordingState = 0;
        microphone = micro;
        recognizer.addStateListener(new StateListener() {
            @Override
            public void statusChanged(edu.cmu.sphinx.recognizer.Recognizer.State status) {
                recognizerStatusChanged(status);
            }

            @Override
            public void newProperties(PropertySheet ps) throws PropertyException { }
        });
    }

    private void recognizerStatusChanged(edu.cmu.sphinx.recognizer.Recognizer.State state){
        switch (state){
            case DEALLOCATED:{
                if (recordingState == 0)
                    recognizer.allocate();
            } break;
            case READY:{
                if (recordingState > 0) return;
                context.getInstance(StreamDataSource.class)
                        .setInputStream(microphone.getStream());
                recordingState = 1;
                microphone.startRecording();
                listeningThread = new Thread(new Runnable() {
                    @Override public void run() {
                        recordingState = 2;
                        cache = getHypothesis();
                        stopRecognition();
                        if (cache != null)
                            callback.run(getHypothesis());
                    }
                });
                listeningThread.start();
            } break;
            default: System.out.println(state); break;
        }
    }

    public void startRecognition() {
        if (recordingState > 0) return;
        recordingState = 0;
        if (recognizer.getState() != edu.cmu.sphinx.recognizer.Recognizer.State.DEALLOCATED){
            stopRecognition();
        } else {
            recognizer.allocate();
        }
    }

    public void stopRecognition() {
        System.out.println("need to stop");
        microphone.stopRecording();
        try {
            if (listeningThread != null && Thread.currentThread() != listeningThread)
                listeningThread.interrupt();
            //listeningThread.interrupt();
        } catch (Exception e){ }
        try {
            recognizer.deallocate();
        } catch (Exception e){ }
        recordingState = 0;
        System.out.println("stopped");
    }

    public void setCallback(SpeechCallback callback){
        this.callback = callback;
    }

    public boolean isRecording(){
        return recordingState > 0;
    }

    public void free(){
        if (Thread.currentThread() != listeningThread)
            listeningThread.interrupt();
        listeningThread = null;
        stopRecognition();
        recordingState = 0;
    }

    public String getHypothesis(){
        String result;
        if (cache != null){
            result = cache;
        } else {
            System.out.println("waiting");
            result = getResult().getHypothesis();
            System.out.println("got");
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

    private static String addPrefix(String text){
        return ((text.charAt(0) == '/' || text.charAt(0) == '\\') ? "" : "file:\\") + text;
    }
    private static Configuration replaceConfiguration(Configuration configuration){
        configuration.setAcousticModelPath(addPrefix(configuration.getAcousticModelPath()));
        configuration.setLanguageModelPath(addPrefix(configuration.getLanguageModelPath()));
        configuration.setDictionaryPath(addPrefix(configuration.getDictionaryPath()));
        return configuration;
    }
}

