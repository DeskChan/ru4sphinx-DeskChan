package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.Microphone;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Path;

public class ImprovedMicrophone {

    protected Microphone microphone;
    protected float sampleRate;
    protected int sampleSize;
    protected boolean signed;
    protected boolean bigEndian;

    public ImprovedMicrophone(float sampleRate, int sampleSize, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.signed = signed;
        this.bigEndian = bigEndian;
        initMicrophone();
    }

    protected void initMicrophone(){
        microphone = new Microphone(sampleRate, sampleSize, signed, bigEndian);
    }

    public void startRecording(){
        microphone.startRecording();
    }

    public void startRecording(Path path){
        final File file = path.toFile();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run(){
            try {
                file.createNewFile();
                AudioSystem.write((AudioInputStream) microphone.getStream(), AudioFileFormat.Type.WAVE, file);
            } catch (Exception e){
                Main.log(new Exception("Cannot save recording", e));
            }
            }
        });
        startRecording();
        thread.start();
    }

    public void stopRecording(){
        microphone.stopRecording();
        try {
            microphone.getStream().close();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        initMicrophone();
    }
}
