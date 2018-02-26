package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;
import info.deskchan.sphinxrecognition.g2p.G2PConvert;
import info.deskchan.sphinxrecognition.g2p.G2PEnglish;
import info.deskchan.sphinxrecognition.g2p.G2PRussian;

import java.io.*;
import java.util.*;

public class Dictionary {

    public static final int DEFAULT_COUNT = 600;
    protected HashList words;
    private static final String FILENAME = "dict.dic";
    private static final String DEFAULT_WORDS = "default_words.txt";

    public Dictionary() {
        words = new HashList();

        try {
            loadDefault(Main.getFileReader(FILENAME), words);
        } catch (Exception e){ }
    }

    private static void loadDefault(InputStream stream, HashList words) throws Exception{
        loadDefault(new BufferedReader(
                new InputStreamReader(stream, "UTF-8")
        ), words);
    }

    private static void loadDefault(BufferedReader reader, HashList words) throws Exception{
        String line;
        while ((line = reader.readLine()) != null){
            try {
                Word word = Word.fromPronounce(line);
                word.required = true;
                words.add(word);
            } catch (Exception e){
                Main.log(e);
            }
        }
        reader.close();
    }

    public Dictionary(Statistics stats, int size) {
        words = stats.getWords();

        try {
            loadDefault(new FileInputStream(Main.getPluginProxy().getPluginDirPath().resolve(DEFAULT_WORDS).toString()), words);
        } catch (Exception e) { }
        try {
            loadDefault(Main.class.getClassLoader().getResourceAsStream(DEFAULT_WORDS), words);
        } catch (Exception e) {
            Main.log(e);
        }

        stats.sort();
        resize(size);
        addPronounces(words);
    }
    private static void addPronounces(HashList words){
        G2PConvert english = new G2PEnglish(), russian = new G2PRussian();
        try {
            english.allocate();
            russian.allocate();
        } catch (Exception e){
            Main.log(e);
            return;
        }
        for (Word word : words) {
            if (word.pronounces.size() > 0) continue;

            Word.Language lan = word.checkLanguage();
            G2PConvert converter = null;
            switch (lan) {
                case ENGLISH:
                    converter = english;
                    break;
                case RUSSIAN:
                    converter = russian;
                    break;
                default:
                    throw new RuntimeException("Unknown language for word: " + word.word);
            }
            if (!converter.allocated()) {
                try {  converter.allocate();
                } catch (Exception e) {
                    Main.log(e);
                    break;
                }
            }
            word.pronounces.add(converter.convert(word.word));
        }
    }

    public void save(){
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word one, Word two){
                return one.word.compareTo(two.word);
            }
        });
        try {
            BufferedWriter writer = Main.getFileWriter(FILENAME);
                for(Word word : words)
                   writer.write(word.toPronouncesString());

            writer.close();
        } catch (Exception e){
            Main.log(e);
        }

        Main.flushRecognizer();
    }

    public int size(){ return words.size(); }

    public boolean contains(String word){
        return words.contains(new Word(word));
    }

    public void resize(int newSize){
        while (words.size() > newSize)
            words.removeLast();
    }

    public Word get(String word){
        Iterator<Word> it = words.iterator();
        while (it.hasNext()){
            Word w = it.next();
            if (w.word.equals(word))
                return w;
        }
        return new Word(word);
    }

    public static String getDictionaryPath(){
        return Main.getPluginProxy().getPluginDirPath().resolve(FILENAME).toAbsolutePath().toString();
    }

    public static void checkDictionary(){
        if (Main.getPluginProxy() == null || Main.getPluginProxy().getPluginDirPath().resolve(FILENAME).toFile().exists()) return;

        try {
            BufferedReader reader = Main.getFileReader(FILENAME);
            BufferedWriter writer = Main.getFileWriter(FILENAME);
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }

            reader.close();
            writer.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public static void saveDefaultWords(Collection<String> words){
        HashList existingWords = new Dictionary().words;
        HashList wordsToSave = new HashList();

        int oldSize = existingWords.size();
        for (String text : words) {
            Word word = Word.fromPronounce(text);
            if (word != null && existingWords.add(word)) wordsToSave.add(word);
        }

        int newSize = existingWords.size();
        if (oldSize == newSize) return;

        addPronounces(existingWords);
        try {
            BufferedWriter writer = Main.getFileWriter(DEFAULT_WORDS);

            for (Word word : wordsToSave)
                writer.write(word.toPronouncesString());

            writer.close();
        } catch (Exception e){
            Main.log(e);
        }

        RussianStatistics statistics = new RussianStatistics();
        statistics.load();

        Dictionary dictionary = new Dictionary(statistics, Main.getPluginProxy().getProperties().getInteger("dictionary.length", Dictionary.DEFAULT_COUNT));
        dictionary.save();

        new LanguageModelCreator().save(dictionary);

        Main.flushRecognizer();
    }
}
