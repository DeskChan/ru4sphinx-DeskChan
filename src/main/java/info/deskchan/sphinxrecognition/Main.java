package info.deskchan.sphinxrecognition;

import java.io.*;
import java.util.*;

import edu.cmu.sphinx.api.*;
import info.deskchan.core.MessageListener;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.sphinxrecognition.creator.Dictionary;
import info.deskchan.sphinxrecognition.creator.LanguageModelCreator;
import info.deskchan.sphinxrecognition.creator.RussianStatistics;
import info.deskchan.sphinxrecognition.creator.Statistics;

public class Main implements Plugin{

    private static PluginProxyInterface pluginProxy;

    private static final String ACOUSTIC_MODEL =
            "resource:/cmusphinx-ru";
    private static final String DICTIONARY_PATH =
            "resource:/dict.dic";
    private static final String GRAMMAR_PATH =
            "resource:/lm.lm";

    Microphone microphone;

    public boolean initialize(PluginProxyInterface ppi){
        pluginProxy = ppi;

        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>(){{
            List<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "adapt");
                put("type", "Button");
                put("msgTag", "recognition:adapt");
                put("value", pluginProxy.getString("adapt"));
            }});
            put("controls", list);
        }});

        pluginProxy.addMessageListener("recognition:adapt", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                pluginProxy.sendMessage("gui:show-custom-window", new HashMap<String, Object>() {{
                    List<HashMap<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "adapt");
                        put("type", "Button");
                        put("msgTag", "recognition:adapt");
                        put("value", pluginProxy.getString("adapt"));
                    }});
                    put("controls", list);
                }});
            }}
        );

        microphone = new Microphone(16000, 16, true, false);

        Configuration configuration = new Configuration();
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
        }

        return true;
    }

    public static void main(String[] args) throws Exception {
        new Main().initialize(null);
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
        //pluginProxy.log(e);
        System.out.println(e.getClass().toString() + " " + e.getMessage() + "\n" + e.getCause());
        e.printStackTrace();
    }

    public static PluginProxyInterface getPluginProxy(){ return pluginProxy; }

    public static BufferedReader getFileReader(String filename) throws Exception{
        try {
            return new BufferedReader(
                   new InputStreamReader(
                   new FileInputStream(pluginProxy.getDataDirPath().resolve(filename).toString()), "UTF-8")
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
               new FileOutputStream(pluginProxy.getDataDirPath().resolve(filename).toString()), "UTF-8")
        );
    }
}