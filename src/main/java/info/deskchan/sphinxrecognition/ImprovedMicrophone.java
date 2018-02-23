package info.deskchan.sphinxrecognition;

import javax.sound.sampled.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public class ImprovedMicrophone {

    protected AudioFormat format;
    protected TargetDataLine line;
    protected InputStream inputStream;

    public ImprovedMicrophone(float sampleRate, int sampleSize, boolean signed) {
        format = new AudioFormat(sampleRate, sampleSize, 1, signed, false);
        reset();
    }

    protected void reset(){
        try {
            inputStream.close();
            line.close();
        } catch (Exception e) { }
        try {
            line = AudioSystem.getTargetDataLine(format);
            line.open();
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
        inputStream = new AudioInputStream(line);
    }

    public void startRecording(){
        line.start();
    }

    public void startRecording(Path path){
        final File file = path.toFile();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run(){
            try {
                file.createNewFile();
                AudioSystem.write((AudioInputStream) getStream(), AudioFileFormat.Type.WAVE, file);
            } catch (Exception e){
                Main.log(new Exception("Cannot save recording", e));
            }
            }
        });
        startRecording();
        thread.start();
    }

    public void stopRecording(){
        line.stop();
        reset();
    }

    public InputStream getStream(){
        return inputStream;
    }
}
