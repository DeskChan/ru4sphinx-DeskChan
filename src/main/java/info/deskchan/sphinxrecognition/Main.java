package info.deskchan.sphinxrecognition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.sphinx.api.*;
import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

public class Main implements Plugin{

    private static PluginProxyInterface pluginProxy;

    private static final String ACOUSTIC_MODEL =
            "resource:/edu/cmu/sphinx/models/en-us/en-us";
    private static final String DICTIONARY_PATH =
            "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict";
    private static final String GRAMMAR_PATH =
            "resource:/info/deskchan/sphinxrecognition/Main/";
    private static final String LANGUAGE_MODEL =
            "resource:/info/deskchan/sphinxrecognition/Main/weather.lm";

    private static final Map<String, Integer> DIGITS =
            new HashMap<String, Integer>();


    private static void recognizeWeather(PatchedLiveRecognizer recognizer) {
        System.out.println("Try some forecast. End with \"the end\"");
        System.out.println("-------------------------------------");
        System.out.println("Example: mostly dry some fog patches tonight");
        System.out.println("Example: sunny spells on wednesday");
        System.out.println("-------------------------------------");

        recognizer.startRecognition(true);
        while (true) {
            String utterance = recognizer.getResult().getHypothesis();
            if (utterance.equals("the end"))
                break;
            else
                System.out.println(utterance);
        }
        recognizer.stopRecognition();
    }

    Microphone microphone;

    public boolean initialize(PluginProxyInterface ppi){
        pluginProxy = ppi;

        //G2PConverter converter = new G2PConverter("resource:/info/deskchan/sphinxrecognition/g2p/G2PEnglish/");
        //System.out.println(converter.phoneticize("Hello", 1));

        microphone = new Microphone(16000, 16, true, false);

        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setGrammarPath(GRAMMAR_PATH);
        configuration.setUseGrammar(true);

        configuration.setGrammarName("dialog");
        PatchedLiveRecognizer jsgfRecognizer, grxmlRecognizer, lmRecognizer;

        try {
            jsgfRecognizer = new PatchedLiveRecognizer(configuration, microphone);

            configuration.setGrammarName("digits.grxml");
            grxmlRecognizer = new PatchedLiveRecognizer(configuration, microphone);

            configuration.setUseGrammar(false);
            configuration.setLanguageModelPath(LANGUAGE_MODEL);
            lmRecognizer = new PatchedLiveRecognizer(configuration, microphone);

        } catch (Exception e){
            log(e);
            return false;
        }

        jsgfRecognizer.startRecognition(true);
        while (true) {
            System.out.println("Choose menu item:");
            System.out.println("Example: go to the bank account");
            System.out.println("Example: exit the program");
            System.out.println("Example: weather forecast");
            System.out.println("Example: digits\n");

            String utterance = jsgfRecognizer.getResult().getHypothesis();

            if (utterance.startsWith("exit"))
                break;

            if (utterance.equals("digits")) {
                jsgfRecognizer.stopRecognition();
                jsgfRecognizer.startRecognition(true);
            }

            if (utterance.equals("bank account")) {
                jsgfRecognizer.stopRecognition();
                jsgfRecognizer.startRecognition(true);
            }

            if (utterance.endsWith("weather forecast")) {
                jsgfRecognizer.stopRecognition();
                recognizeWeather(lmRecognizer);
                jsgfRecognizer.startRecognition(true);
            }
        }

        jsgfRecognizer.stopRecognition();
        return true;
    }

    public static void main(String[] args) throws Exception {
        new Main().initialize(null);

        DictionaryCreator creator = new RussianDictionaryCreator();
        creator.download();

    }

    public static void log(Throwable e){
        //pluginProxy.log(e);
        System.out.println(e.getMessage() + "\n" + e.getCause() + "\n" + e.getStackTrace());
    }

    public static PluginProxyInterface getPluginProxy(){ return pluginProxy; }
}