package info.deskchan.sphinxrecognition;

import info.deskchan.core.MessageListener;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.sphinxrecognition.creator.Dictionary;
import info.deskchan.sphinxrecognition.creator.RussianStatistics;

import java.io.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class Adapter {

    public enum State { NO_STATE, INPUT_TEXT, CLEARING_TEXT, WAITING_RECORDING, RECORDING, RECORDED, SAVING_RECORDING, TRAINING }
    protected State state;

    private static final String VOICE_PATH = "training";
    static PluginProxyInterface pluginProxy;
    protected static Adapter instance = null;

    public static Adapter getInstance(){
        if (instance == null) throw new RuntimeException("Adapter is not initialized yet.");
        return instance;
    }

    TrainingText trainingText;

    public static void initialize(PluginProxyInterface pluginProxyInterface){
        pluginProxy = pluginProxyInterface;

        pluginProxy.addMessageListener("recognition:adapt", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                String sphinxtrain = Main.getPluginProxy().getProperties().getString("sphinxtrain-path");
                if (sphinxtrain == null || sphinxtrain.length() == 0){
                    pluginProxy.sendMessage("gui:show-notification", new HashMap<String, Object>(){{
                        put("name", pluginProxy.getString("error"));
                        put("text", pluginProxy.getString("no-path"));
                    }});
                    return;
                }
                pluginProxy.getDataDirPath().resolve(VOICE_PATH).toFile().mkdir();
                instance = new Adapter();
            }
        });

        pluginProxy.addMessageListener("recognition:adapt-next-stage", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                if (instance == null)
                    instance = new Adapter();
                else
                    instance.nextStage(o);
            }
        });

        pluginProxy.addMessageListener("recognition:adapt-use-old", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                instance = new Adapter(State.TRAINING);
            }
        });

        pluginProxy.addMessageListener("recognition:adapt-toggle-record", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                getInstance().toggleRecording();
            }
        });

        pluginProxy.addMessageListener("recognition:adapt-cancel", new MessageListener() {
            @Override public void handleMessage(String s, String s1, Object o) {
                instance.closeAdapter();
                instance = null;
            }
        });
    }

    public Adapter(){
        this(State.INPUT_TEXT);
    }

    public Adapter(State state){
        this.state = state;
        toStage(null);
    }

    public void toStage(Object data){
        switch(state){
            case INPUT_TEXT: setTextReceiving(); break;
            case WAITING_RECORDING: case RECORDING:
                initRecording(data);
            break;
            case TRAINING: setTraining(); break;
            default: Main.log(new Exception("Unpredicted state: " + state)); break;
        }
    }

    public void nextStage(Object data){
        switch(state){
            case NO_STATE:   setTextReceiving(); break;
            case INPUT_TEXT: initRecording(data); break;
            case RECORDED: {
                if (trainingText.hasNext())
                    setRecording();
                else
                    setTraining();
            } break;
            case WAITING_RECORDING: case RECORDING: {
                pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
                    put("id", "adaptation");
                    put("action", "update");
                    LinkedList<Object> list = new LinkedList<>();
                    list.add(new HashMap<String, Object>() {{
                        put("id", "saving");
                        put("type", "Label");
                        put("value", pluginProxy.getString("record-first"));
                    }});
                    put("controls", list);
                }});
            } break;
            default: Main.log(new Exception("Unpredicted state: " + state)); break;
        }
    }

    private void setTextReceiving(){
        state = State.INPUT_TEXT;
        Map m = new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("name", pluginProxy.getString("adaptation"));
            put("type", "window");
            put("action", "show");
            put("msgTag", "recognition:adapt-next-stage");
            LinkedList<HashMap<String, Object>> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", pluginProxy.getString("what-is-adaptation"));
                put("width", 350);
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Separator");
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", pluginProxy.getString("set-text"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "adaptation-text");
                put("type", "TextArea");
                put("height", 150);
                put("value", pluginProxy.getString("adaptation-text"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "use-old-recording");
                put("type", "Button");
                put("value", pluginProxy.getString("use-old-recording"));
                put("msgTag", "recognition:adapt-use-old");
            }});
            put("controls", list);
        }};
        pluginProxy.sendMessage("gui:set-panel", m);
    }
    private void initRecording(Object data) {
        Map map = (Map) data;
        state = State.CLEARING_TEXT;
        try {
            trainingText = new TrainingText((String) map.get("adaptation-text"));
        } catch (Exception e) {
            Main.log(e);
            return;
        }

        setRecording();
    }
    private void setRecording() {
        state = State.WAITING_RECORDING;
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("name", pluginProxy.getString("adaptation"));
            put("type", "window");
            put("action", "set");
            LinkedList<Object> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", pluginProxy.getString("adaptation-start-talk"));
            }});
            list.add(new HashMap<String, Object>() {{
                put("type", "Label");
                put("value", (trainingText.currentIndex+1) + "/" + trainingText.size());
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "adaptation-text");
                put("type", "Label");
                put("value", trainingText.next());
            }});
            LinkedList<HashMap<String, Object>> buttons = new LinkedList<>();
            buttons.add(new HashMap<String, Object>() {{
                put("id", "cancel");
                put("type", "Button");
                put("value", pluginProxy.getString("cancel"));
                put("msgTag", "recognition:adapt-cancel");
            }});
            buttons.add(new HashMap<String, Object>() {{
                put("id", "record");
                put("type", "Button");
                String value;
                switch (state){
                    case WAITING_RECORDING: value = "start"; break;
                    case RECORDING: value = "stop"; break;
                    case RECORDED: value = "start-again"; break;
                    default: value = "error"; break;
                }
                put("value", pluginProxy.getString(value));
                put("msgTag", "recognition:adapt-toggle-record");
            }});
            buttons.add(new HashMap<String, Object>() {{
                put("id", "next");
                put("type", "Button");
                put("value", pluginProxy.getString("next"));
                put("msgTag", "recognition:adapt-next-stage");
            }});
            Map buttonsMap = new HashMap();
            buttonsMap.put("elements", buttons);
            list.add(buttonsMap);
            list.add(new HashMap<String, Object>() {{
                put("id", "saving");
                put("type", "Label");
                put("value", "");
            }});
            put("controls", list);
        }});
    }
    private void setFileSaving(){
        state = State.SAVING_RECORDING;
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("type", "window");
            put("action", "update");
            LinkedList<Object> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "saving");
                put("type", "Label");
                put("value", pluginProxy.getString("saving"));
            }});
            put("controls", list);
        }});
    }

    private void setTraining(){
        state = State.TRAINING;
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("type", "window");
            put("action", "set");
            LinkedList<Object> list = new LinkedList<>();
            list.add(new HashMap<String, Object>() {{
                put("id", "saving");
                put("type", "Label");
                put("value", pluginProxy.getString("saving"));
            }});
            put("controls", list);
        }});
        startTraining();
    }

    private void toggleRecording(){
        switch (state){
            case WAITING_RECORDING: case RECORDED: {
                state = State.RECORDING;
                try {
                    Main.getMicrophone().startRecording(
                            pluginProxy.getDataDirPath().resolve(VOICE_PATH)
                                    .resolve("training_" + (trainingText.currentIndex-1) + ".wav"));
                } catch (Exception e){
                    Main.log(e);
                }

            } break;
            case RECORDING: {
                setFileSaving();
                Main.getMicrophone().stopRecording();
                state = State.RECORDED;
            } break;
        }
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("action", "update");
            LinkedList<HashMap<String, Object>> buttons = new LinkedList<>();
            buttons.add(new HashMap<String, Object>() {{
                put("id", "record");
                put("type", "Button");
                String value;
                switch (state){
                    case WAITING_RECORDING: value = "start"; break;
                    case RECORDING: value = "stop"; break;
                    case RECORDED: value = "start-again"; break;
                    default: value = "error"; break;
                }
                put("value", pluginProxy.getString(value));
                put("msgTag", "recognition:adapt-toggle-record");
            }});
            put("controls", buttons);
            buttons.add(new HashMap<String, Object>() {{
                put("id", "saving");
                put("value", state == State.RECORDED ? pluginProxy.getString("done") : "");
            }});
        }});
    }

    private void closeAdapter(){
        pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>(){{
            put("id", "adaptation");
            put("action", "hide");
        }});
    }

    void startTraining(){
        Path voicePath = Main.getPluginProxy().getDataDirPath().resolve(VOICE_PATH),
               logPath = voicePath.resolve(new SimpleDateFormat("dd-MM-yyyy--HH-mm-ss").format(Calendar.getInstance().getTime()));

        voicePath.toFile().mkdir();
        logPath.toFile().mkdir();

        ProcessBuilder builder = new ProcessBuilder( Main.getPluginProxy().getProperties().getString("sphinxtrain-path") + "\\sphinx_fe",
                "-argfile", Main.ACOUSTIC_MODEL + "\\feat.params",
                "-samprate", Integer.toString(Main.SAMPLE_RATE),
                "-c", "training.fileids",
                "-di", ".",
                "-do", ".",
                "-ei", "wav",
                "-eo", "mfc",
                "-mswav", "yes"
        );

        builder.directory(voicePath.toFile());
        builder.redirectError(logPath.resolve("sphinx_fe.error.log").toFile());
        builder.redirectOutput(logPath.resolve("sphinx_fe.out.log").toFile());
        Process p;
        try {
            p = builder.start();
            p.waitFor();
        } catch (Exception e){
            Main.log(e);
            instance.closeAdapter();
            return;
        }


        Dictionary.checkDictionary();
        builder = new ProcessBuilder( Main.getPluginProxy().getProperties().getString("sphinxtrain-path") + "\\bw",
                "-hmmdir", Main.ACOUSTIC_MODEL,
                "-moddeffn", Main.ACOUSTIC_MODEL + "\\mdef",
                "-ts2cbfn", ".cont.",
                "-feat", "1s_c_d_dd",
                "-cmn", "current",
                "-agc", "none",
                "-dictfn", Dictionary.getDictionaryPath(),
                "-ctlfn", "training.fileids",
                "-lsnfn", "training.description",
                "-accumdir", Main.ACOUSTIC_MODEL,
                "-lda",  Main.ACOUSTIC_MODEL + "\\feature_transform"
        );

        builder.directory(voicePath.toFile());
        builder.redirectError(logPath.resolve("bw.error.log").toFile());
        builder.redirectOutput(logPath.resolve("bw.out.log").toFile());
        try {
            p = builder.start();
            p.waitFor();
        } catch (Exception e){
            Main.log(e);
            instance.closeAdapter();
            return;
        }

        builder = new ProcessBuilder( Main.getPluginProxy().getProperties().getString("sphinxtrain-path") + "\\mllr_solve",
                "-meanfn", Main.ACOUSTIC_MODEL + "\\means",
                "-varfn", Main.ACOUSTIC_MODEL + "\\variances",
                "-outmllrfn", Main.ACOUSTIC_MODEL + "\\mllr_matrix",
                "-accumdir", Main.ACOUSTIC_MODEL
        );

        builder.directory(voicePath.toFile());
        builder.redirectError(logPath.resolve("mllr_solve.error.log").toFile());
        builder.redirectOutput(logPath.resolve("mllr_solve.out.log").toFile());
        try {
            p = builder.start();
            p.waitFor();
        } catch (Exception e){
            Main.log(e);
        }
        instance.closeAdapter();
    }

    protected static class TrainingText extends ArrayList<String> {
        public int currentIndex;

        public String next(){
            return get(currentIndex++);
        }

        public boolean hasNext(){
            return size() > currentIndex;
        }

        private static final int maxTextLength = 150;

        protected TrainingText(String text) throws Exception{
            currentIndex = 0;
            text = RussianStatistics.clearWord(text);
            String[] words = text.split("[\\n\\s-]", -1);
            if(words.length == 0)
                throw new Exception("no-text");

            Dictionary dictionary = new Dictionary();

            StringBuilder last = new StringBuilder();
            for(int i=0; i<words.length; i++){
                words[i] = words[i].toLowerCase().replace('ё', 'е');
                if(!dictionary.contains(words[i])) continue;
                if(words[i].length() >= 70)
                    throw new RuntimeException("I do not believe that words such " + words[i] + " even exist. Stop it.");

                if(last.length() + words[i].length() > maxTextLength){
                    add(last.toString());
                    last = new StringBuilder();
                }
                last.append(words[i]); last.append(" ");
            }
            add(last.toString());
            try {
                Path path = Main.getPluginProxy().getDataDirPath().resolve(VOICE_PATH); //Main.getPluginProxy().getDataDirPath().resolve(VOICE_PATH);
                path.toFile().mkdir();

                BufferedWriter writer =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        new FileOutputStream(path.resolve("training.fileids").toString()), "UTF-8")
                        );
                for(int i=0; i<size(); i++)
                    writer.write("training_" + i + "\n");

                writer.close();

                writer =
                        new BufferedWriter(
                                new OutputStreamWriter(
                                        new FileOutputStream(path.resolve("training.description").toString()), "UTF-8")
                        );

                int i=0;
                for(String st : this){
                    writer.write("<s> "+st+" </s> (training_"+i+")\n");
                    i++;
                }

                writer.close();

            } catch(Exception e){
                throw new RuntimeException("Error while creating files for training: "+e.getMessage(), e);
            }
        }
    }

}
