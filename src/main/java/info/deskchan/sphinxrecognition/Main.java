package info.deskchan.sphinxrecognition;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import edu.cmu.sphinx.api.Microphone;
import info.deskchan.core.MessageListener;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.sphinxrecognition.creator.Dictionary;
import info.deskchan.sphinxrecognition.creator.LanguageModelCreator;
import info.deskchan.sphinxrecognition.creator.RussianStatistics;

public class Main implements Plugin{

    static Main instance;

    private static PluginProxyInterface pluginProxy;

    protected static String ACOUSTIC_MODEL =  "cmusphinx-ru";
    protected static String DICTIONARY_PATH = "resource:/dict.dic";
    protected static String GRAMMAR_PATH =    "resource:/lm.lm";
    protected static int SAMPLE_RATE = 16000;

    protected ImprovedMicrophone microphone;
    Adapter adapter;

    public boolean initialize(PluginProxyInterface ppi){
        instance = this;
        pluginProxy = ppi;
        ACOUSTIC_MODEL = pluginProxy.getPluginDirPath().resolve(ACOUSTIC_MODEL).toString();

        pluginProxy.getProperties().load();

        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adapt-menu");
            put("type", "submenu");
            put("action", "set");
            List<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", pluginProxy.getString("what-is-adaptation"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "sphinxtrain");
                put("type", "DirectoryField");
                put("msgTag", "recognition:set-sphinxtrain-path");
                put("label", pluginProxy.getString("sphinxtrain"));
                put("value", pluginProxy.getProperties().getString("sphinxtrain-path", ""));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "adapt");
                put("type", "Button");
                put("msgTag", "recognition:adapt");
                put("value", pluginProxy.getString("adapt"));
            }});
            put("controls", list);
        }});

        pluginProxy.addMessageListener("recognition:set-sphinxtrain-path", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                pluginProxy.getProperties().put("sphinxtrain-path", o);
            }
        });

        Adapter.initialize(pluginProxy);

        microphone = new ImprovedMicrophone(SAMPLE_RATE, 16, true, false);

        /*Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setLanguageModelPath(GRAMMAR_PATH);

        PatchedLiveRecognizer recognizer;
        try {
            recognizer = new PatchedLiveRecognizer(configuration, microphone);
            recognizer.startRecognition();
        } catch (Exception e){
            log(e);
            return false;
        }

        int i = 3;
        while (i > 0) {
            System.out.println("Say something");

            String utterance = recognizer.getResult().getHypothesis();

            System.out.println(utterance);
            i--;
        }*/

        new Adapter(Adapter.State.TRAINING);
        return true;
    }

    public static void main(String[] args) throws Exception {
        //new Main().initialize(null);
    }

    static void createRussianResources(){
        RussianStatistics statistics = new RussianStatistics();
        statistics.download();
        statistics.save();
        statistics.resize(15000);

        Dictionary dictionary = new Dictionary(statistics);
        dictionary.save();

        new LanguageModelCreator().save(statistics);
    }

    public static void log(Throwable e){
        pluginProxy.log(e);
    }

    public static PluginProxyInterface getPluginProxy(){ return pluginProxy; }

    public static BufferedReader getFileReader(String filename) throws Exception{
        try {
            return new BufferedReader(
                   new InputStreamReader(
                   new FileInputStream(pluginProxy.getPluginDirPath().resolve(filename).toString()), "UTF-8")
            );
        } catch (Exception e) {
            return new BufferedReader(
                   new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(filename), "UTF-8")
            );
        }
    }

    public static BufferedWriter getFileWriter(String filename) throws Exception {
        return new BufferedWriter(
               new OutputStreamWriter(
               new FileOutputStream(pluginProxy.getPluginDirPath().resolve(filename).toString()), "UTF-8")
        );
    }

    public static ImprovedMicrophone getMicrophone(){
        return instance.microphone;
    }
}