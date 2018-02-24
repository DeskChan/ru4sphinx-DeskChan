package info.deskchan.sphinxrecognition;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import info.deskchan.sphinxrecognition.creator.Dictionary;
import info.deskchan.sphinxrecognition.creator.LanguageModelCreator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class PocketsphinxRecognizer implements Recognizer{

    Configuration configuration;
    Process recognizerProcess = null;
    BufferedReader reader;
    BufferedWriter writer;
    String cache;
    private SpeechCallback callback;
    private boolean recordingNow;

    PocketsphinxRecognizer(Configuration configuration){
        this.configuration = configuration;
    }

    void startProcess(){
        Dictionary.checkDictionary();
        LanguageModelCreator.checkModel();

        ArrayList<String> arguments = new ArrayList<>();
        if (Main.getPluginProxy() != null) {
            arguments.addAll(Arrays.asList(
                    Main.getPluginProxy().getProperties().getString("pocketsphinx-path") + "\\pocketsphinx_continuous",
                    "-samprate", Integer.toString(Main.SAMPLE_RATE),
                    "-hmm", configuration.getAcousticModelPath(),
                    "-lm", LanguageModelCreator.getModelPath(),
                    "-dict", Dictionary.getDictionaryPath(),
                    "-inmic", "yes"
            ));
        } else {
            arguments.addAll(Arrays.asList(
                    "C:\\DeskChan\\ru4sphinx-DeskChan\\prebuilds\\Release\\Win32\\pocketsphinx_continuous",
                    "-samprate", Integer.toString(Main.SAMPLE_RATE),
                    "-hmm",  configuration.getAcousticModelPath(),
                    "-lm",   configuration.getLanguageModelPath(),
                    "-dict", configuration.getDictionaryPath(),
                    "-inmic", "yes"
            ));
        }
        if (Main.getPluginProxy() == null || Main.getPluginProxy().getProperties().getBoolean("log-pocketsphinx", false)) {
            arguments.addAll( Arrays.asList(
                "-backtrace", "yes",
                "-logfn", "log.txt"
            ));
        }

        Path model = new File(configuration.getAcousticModelPath()).toPath();
        if (model.resolve("tmat_counts").toFile().exists()) {
            arguments.add("-tmat"); arguments.add(model.resolve("tmat_counts").toString());
        }
        if (model.resolve("mixw_counts").toFile().exists()) {
            arguments.add("-mixw"); arguments.add(model.resolve("mixw_counts").toString());
        }
        //if (model.resolve("mllr_matrix").toFile().exists()) {
       //     arguments.add("-mllr"); arguments.add(model.resolve("mllr_matrix").toString());
       // }

        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.redirectErrorStream(false);
        try {
            recognizerProcess = builder.start();
            reader = new BufferedReader(new InputStreamReader(recognizerProcess.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(recognizerProcess.getOutputStream()));
            String line;
            while ((line = reader.readLine()) != null && !line.equals("ready"));
               // System.out.println(line);
        } catch (Exception e){
            Main.log(e);
            return;
        }
    }

    public void free(){ recognizerProcess.destroy(); }

    public void startRecognition(){
        if (recognizerProcess == null)
            startProcess();
        try {
            writer.write("start\n");
            writer.flush();
        } catch (Exception e){
            Main.log(e);
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                cache = getHypothesis();
                recordingNow = false;
                callback.run(getHypothesis());
            }
        }).start();
        recordingNow = true;
    }

    @Override
    public boolean isRecording() {
        return recordingNow;
    }

    public void setCallback(SpeechCallback callback){
        this.callback = callback;
    }

    public String getHypothesis(){
        String result;
        if (cache != null){
            result = cache;
        } else {
            try {
                result = reader.readLine();
                while (reader.ready())
                    result += " " +reader.readLine();
            } catch (Exception e){
                Main.log(e);
                result = null;
            }
        }
        cache = null;
        return result;
    }

    public void loadTransform(String path, int num){ }
}
