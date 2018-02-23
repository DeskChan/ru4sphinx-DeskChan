package info.deskchan.sphinxrecognition;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.Result;
import info.deskchan.core.MessageListener;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core.ResponseListener;
import info.deskchan.sphinxrecognition.creator.Dictionary;
import info.deskchan.sphinxrecognition.creator.DictionaryOptions;
import info.deskchan.sphinxrecognition.creator.LanguageModelCreator;
import info.deskchan.sphinxrecognition.creator.RussianStatistics;
import info.deskchan.sphinxrecognition.g2p.G2PConvert;
import info.deskchan.sphinxrecognition.g2p.G2PEnglish;
import info.deskchan.sphinxrecognition.g2p.G2PRussian;

public class Main implements Plugin{

    static Main instance;

    private static PluginProxyInterface pluginProxy;

    protected static String ACOUSTIC_MODEL =  "cmusphinx-ru";
    protected static String DICTIONARY_PATH = "dict.dic";
    protected static String GRAMMAR_PATH =    "lm.lm";
    protected static int SAMPLE_RATE = 8000;

    protected ImprovedMicrophone microphone;
    PatchedLiveRecognizer recognizer;

    final Set<String> words = new HashSet<>();

    public boolean initialize(PluginProxyInterface ppi){
        instance = this;
        pluginProxy = ppi;

        microphone = new ImprovedMicrophone(SAMPLE_RATE, 16, true);

        ACOUSTIC_MODEL = pluginProxy.getPluginDirPath().resolve(ACOUSTIC_MODEL).toString();

        pluginProxy.getProperties().load();

        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("name", pluginProxy.getString("options"));
            put("type", "submenu");
            put("action", "set");
            List<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("type", "Button");
                put("value", pluginProxy.getString("dictionary.settings"));
                put("msgTag", "recognition:dictionary-open-settings");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "rescan");
                put("type", "Button");
                put("msgTag", "recognition:rescan-plugins");
                put("value", pluginProxy.getString("rescan"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Separator");
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", pluginProxy.getString("adaptation"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "sphinxtrain");
                put("type", "DirectoryField");
                put("msgTag", "recognition:set-sphinxtrain-path");
                put("label", pluginProxy.getString("sphinxtrain-path"));
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

        pluginProxy.addMessageListener("recognition:rescan-plugins", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
            pluginProxy.sendMessage("recognition:get-words", null, new ResponseListener() {
                @Override public void handle(String s, Object o) {
                    Collection<String> col = (Collection<String>) o;
                    for (String item : col)
                        words.addAll(Arrays.asList(item.split("\\s")));
                }
            }, new ResponseListener() {
                @Override public void handle(String s, Object o) {
                    Dictionary.saveDefaultWords(words);
                }
            });
            }
        });

        Adapter.initialize(pluginProxy);
        DictionaryOptions.initialize();

        pluginProxy.addMessageListener("recognition:start-listening", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                startRecording();
            }
        });

        pluginProxy.addMessageListener("recognition:stop-listening", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                stopRecording();
            }
        });

        /* Toggle recognition state: recording/not recording.
        * Public message
        * Params: None
        * Returns: None */
       pluginProxy.addMessageListener("recognition:toggle-listening", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                if (recognizer == null || !recognizer.isRecording())
                    startRecording();
                else
                    stopRecording();
            }
        });

        pluginProxy.sendMessage("core:add-command", new HashMap(){{
            put("tag", "recognition:toggle-listening");
        }});
        pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>(){{
            put("eventName", "gui:keyboard-handle");
            put("commandName", "recognition:toggle-listening");
            put("rule", "F5");
        }});

        pluginProxy.addMessageListener("core:update-links:speech:get", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object data) {
                extractWordList((List) data);
            }
        });

        pluginProxy.log("Recognition module is ready");
        return true;
    }

    public void initRecognizer(){
        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);

        File file = pluginProxy.getPluginDirPath().resolve(DICTIONARY_PATH).toFile();
        if (file.exists()){
            configuration.setDictionaryPath("file:///" + file.toString());
        } else {
            configuration.setDictionaryPath("resource:/" + DICTIONARY_PATH);
        }

        file = pluginProxy.getPluginDirPath().resolve(GRAMMAR_PATH).toFile();
        if (file.exists()){
            configuration.setLanguageModelPath("file:///" + file.toString());
        } else {
            configuration.setLanguageModelPath("resource:/" + GRAMMAR_PATH);
        }

        try {
            PatchedLiveRecognizer recognizer = new PatchedLiveRecognizer(configuration, microphone);

            Path path = Paths.get(ACOUSTIC_MODEL).resolve("mllr_matrix");
            if (path.toFile().exists())
                recognizer.loadTransform("C:/DeskChan/DeskChan/build/launch4j/plugins/ru4sphinx-DeskChan/cmusphinx-ru/mllr_matrix", 1);

                recognizer.startRecognition();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public static void flushRecognizer(){
        if (instance.recognizer != null) {
            instance.recognizer.stopRecognition();
            instance.recognizer = null;
        }
    }

    public void startRecording(){
        if (recognizer == null)
            initRecognizer();

        pluginProxy.sendMessage("DeskChan:say", "Говори.");
        recognizer.startRecognition();
    }

    public void stopRecording(){
        recognizer.stopRecognition();
    }


    public void extractWordList(List<Map<String, Object>> commandsInfo){
        HashSet<String> words = new HashSet<>();
        try {
            for (int i = 0; i < commandsInfo.size(); i++) {
                try {
                    String rule = (String) commandsInfo.get(i).get("rule");
                    if (rule == null || rule.trim().length() == 0) continue;
                    words.addAll(Arrays.asList(rule.trim().split(" ")));
                } catch (Exception e){ }
            }

            Dictionary.saveDefaultWords(words);

        } catch (Exception e){
            Main.getPluginProxy().log("Error while parsing links list");
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().initialize(null);
        System.out.println("main is done");
    }

    static void createRussianResources(){
        RussianStatistics statistics = new RussianStatistics();
        statistics.download();
        statistics.save();

        Dictionary dictionary = new Dictionary(statistics, Main.getPluginProxy().getProperties().getInteger("dictionary.length", Dictionary.DEFAULT_COUNT));
        dictionary.save();

        new LanguageModelCreator().save(dictionary);
    }

    public static void log(Throwable e){
        pluginProxy.log(e);
        //e.printStackTrace();
    }

    public static Path getPluginDirPath(){
        if (pluginProxy != null) return pluginProxy.getPluginDirPath();
        return Paths.get(".");
    }

    public static PluginProxyInterface getPluginProxy(){ return pluginProxy; }

    public static BufferedReader getFileReader(String filename) throws Exception{
        try {
            return new BufferedReader(
                   new InputStreamReader(
                   new FileInputStream(getPluginDirPath().resolve(filename).toString()), "UTF-8")
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
               new FileOutputStream(getPluginDirPath().resolve(filename).toString()), "UTF-8")
        );
    }

    public static ImprovedMicrophone getMicrophone(){
        return instance.microphone;
    }
}