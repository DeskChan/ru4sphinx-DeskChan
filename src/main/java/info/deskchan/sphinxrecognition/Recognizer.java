package info.deskchan.sphinxrecognition;

public interface Recognizer {

    boolean startRecognition();

    boolean isRecording();

    String getHypothesis();

    interface SpeechCallback{
        void run(String hypothesis);
    }

    void setCallback(SpeechCallback callback);

    void free();

    void loadTransform(String path, int num) throws Exception;
}
