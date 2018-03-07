package info.deskchan.sphinxrecognition.creator;

import info.deskchan.core.MessageListener;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.sphinxrecognition.Main;
import info.deskchan.sphinxrecognition.g2p.G2PConvert;
import info.deskchan.sphinxrecognition.g2p.G2PEnglish;
import info.deskchan.sphinxrecognition.g2p.G2PRussian;

import java.util.*;

public class DictionaryOptions {
    public static void initialize(){
        final PluginProxyInterface pluginProxy = Main.getPluginProxy();

        pluginProxy.addMessageListener("recognition:dictionary-open-settings", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                DictionaryOptions options = new DictionaryOptions();
                pluginProxy.getProperties().put("dictionary.length", options.dictionary.size());
                pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
                    put("id", "dictionary-settings");
                    put("name", pluginProxy.getString("dictionary.settings"));
                    put("type", "panel");
                    put("action", "show");
                    put("onSave", "recognition:dictionary-save-settings");
                    List<HashMap<String, Object>> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "words-count");
                        put("type", "IntSpinner");
                        put("label", pluginProxy.getString("dictionary.length"));
                        put("value", pluginProxy.getProperties().getInteger("dictionary.length", 1000));
                        put("min", 400);
                        put("max", 100000);
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("id", "model-percent");
                        put("type", "IntSpinner");
                        put("label", pluginProxy.getString("dictionary.percent"));
                        put("value", pluginProxy.getProperties().getInteger("modelPercentage", 90));
                        put("min", 5);
                        put("max", 100);
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("type", "Separator");
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("type", "Label");
                        put("value", pluginProxy.getString("dictionary.add-word"));
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("type", "Label");
                        put("value", pluginProxy.getString("dictionary.add-word-info"));
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("id", "word");
                        put("type", "TextField");
                        put("label", pluginProxy.getString("word"));
                        put("enterTag", "recognition:dictionary-changed-input-word");
                        put("onFocusLostTag", "recognition:dictionary-changed-input-word");
                    }});
                    final List<HashMap<String, Object>> subelements = new LinkedList<>();
                    subelements.add(new HashMap<String, Object>() {{
                        put("id", "transcription-next");
                        put("type", "Button");
                        put("msgTag", "recognition:dictionary-next-transcription");
                        put("value", pluginProxy.getString("next"));
                    }});
                    subelements.add(new HashMap<String, Object>() {{
                        put("id", "transcription");
                        put("type", "TextField");
                        put("enterTag", "recognition:dictionary-changed-transcription");
                    }});
                    list.add(new HashMap<String, Object>(){{
                        put("elements", subelements);
                        put("label", pluginProxy.getString("transcription"));
                    }});
                    list.add(new HashMap<String, Object>() {{
                        put("id", "is-present");
                        put("type", "Label");
                        put("value", "");
                    }});

                    put("controls", list);
                }});
            }
        });

        pluginProxy.addMessageListener("recognition:dictionary-changed-input-word", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                instance.setCurrentText((String) o);
            }
        });

        pluginProxy.addMessageListener("recognition:dictionary-next-transcription", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                try {
                    instance.nextTranscription();
                } catch (Exception e){
                    pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
                        put("id", "dictionary-settings");
                        put("action", "update");
                        List<HashMap<String, Object>> list = new LinkedList<>();
                        list.add(new HashMap<String, Object>() {{
                            put("id", "transcription");
                            put("value", pluginProxy.getString("click-again"));
                            put("disabled", true);
                        }});
                        put("controls", list);
                    }});
                }
            }
        });

        pluginProxy.addMessageListener("recognition:dictionary-changed-transcription", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                instance.resultTranscription = (String) o;
            }
        });

        pluginProxy.addMessageListener("recognition:dictionary-save-settings", new MessageListener() {
            @Override
            public void handleMessage(String s, String s1, Object o) {
                Map map = (Map) o;

                Integer wordsCount = (Integer) map.get("words-count");
                Integer modelPercentage = (Integer) map.get("model-percent");

                if (wordsCount != null) {
                    if (wordsCount == Main.getPluginProxy().getProperties().getInteger("dictionary.length", 1000))
                        wordsCount = null;
                    else
                        pluginProxy.getProperties().put("dictionary.length", wordsCount);
                }
                if (modelPercentage != null) {
                    if (modelPercentage == Main.getPluginProxy().getProperties().getInteger("modelPercentage", 90))
                        modelPercentage = null;
                    else
                        pluginProxy.getProperties().put("modelPercentage", wordsCount);
                }

                if (wordsCount != null || modelPercentage != null){
                    if (wordsCount == null)
                        wordsCount = Main.getPluginProxy().getProperties().getInteger("dictionary.length", Dictionary.DEFAULT_COUNT);

                    RussianStatistics statistics = new RussianStatistics();
                    statistics.load();

                    instance.dictionary = new Dictionary(statistics, wordsCount);
                    instance.dictionary.save();

                    new LanguageModelCreator().save(instance.dictionary);
                }

                if ( map.get("word") != null && !map.get("word").equals("") &&
                     map.get("transcription") != null && !map.get("transcription").equals("") ) {
                    instance.resultTranscription = (String) map.get("transcription");
                    instance.save();
                }
            }
        });
    }

    static DictionaryOptions instance;

    protected Dictionary dictionary;
    protected List<String> currentWords;
    protected List<String> transcriptions;
    protected String resultTranscription;
    protected PluginProxyInterface pluginProxy;
    boolean single;
    int transcriptionCounter;

    DictionaryOptions(){
        instance = this;
        currentWords = null;
        transcriptions = null;
        dictionary = new Dictionary();
        pluginProxy = Main.getPluginProxy();
    }

    public void nextTranscription(){

        if (single){
            pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
                put("id", "dictionary-settings");
                put("action", "update");
                List<HashMap<String, Object>> list = new LinkedList<>();
                list.add(new HashMap<String, Object>() {{
                    put("id", "transcription");
                    put("value", transcriptions.get(transcriptionCounter));
                    put("disabled", false);
                }});
                put("controls", list);
            }});
            transcriptionCounter = (transcriptionCounter+1) % transcriptions.size();
        } else {
            pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
                put("id", "dictionary-settings");
                put("action", "update");
                List<HashMap<String, Object>> list = new LinkedList<>();
                list.add(new HashMap<String, Object>() {{
                    put("id", "transcription");
                    StringBuilder value = new StringBuilder();
                    for (String val : transcriptions) value.append(val + " ");
                    resultTranscription = value.toString().trim();
                    put("value", resultTranscription);
                    put("disabled", true);
                }});
                put("controls", list);
            }});
        }
    }

    public void setCurrentText(String text){
        currentWords = new LinkedList(
                Arrays.asList(RussianStatistics.clearWord(text).split("[\\n\\s-]"))
        );
        currentWords.remove("");
        single = (currentWords.size() == 1);
        transcriptionCounter = 0;

        transcriptions = new ArrayList<>();
        G2PConvert russian = new G2PRussian(), english = new G2PEnglish();
        try {
            english.allocate();
            russian.allocate();
        } catch (Exception e){
            Main.log(e);
            return;
        }
        boolean notPresent = false;
        for (String item : currentWords){
            Word word = dictionary.get(item);
            if (word.pronounces.size() == 0) {
                Word.Language lan = word.checkLanguage();
                G2PConvert converter;
                switch (lan) {
                    case ENGLISH:
                        converter = english;
                        break;
                    case RUSSIAN:
                        converter = russian;
                        break;
                    default:
                        continue;
                }
                if (single)
                    transcriptions = converter.getPossible(item, 20);
                else
                    transcriptions.add(converter.convert(item));
                notPresent = true;
            } else {
                if (single) {
                    transcriptions = new ArrayList<>();
                    transcriptions.addAll(word.pronounces);
                } else
                    transcriptions.add(word.pronounces.iterator().next());
            }
        }

        english.deallocate();
        russian.deallocate();

        final String value = pluginProxy.getString(notPresent ? "not-present" : "present");
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "dictionary-settings");
            put("action", "update");
            List<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "is-present");
                put("value",  value);
            }});
            put("controls", list);
        }});

    }

    public void save(){
        if (single){
            currentWords.set(0, currentWords.get(0) + " " + resultTranscription);
        } else {
            for (int i = 0; i < currentWords.size(); i++) {
                currentWords.set(i, currentWords.get(i) + " " + transcriptions.get(i));
            }
        }

        Dictionary.saveDefaultWords(currentWords);
    }
}
