package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;
import info.deskchan.sphinxrecognition.g2p.G2PConvert;
import info.deskchan.sphinxrecognition.g2p.G2PEnglish;
import info.deskchan.sphinxrecognition.g2p.G2PRussian;

import java.io.*;
import java.util.*;

public class Dictionary {

    protected List<Word> words;
    private static final String FILENAME = "dict.dic";

    public Dictionary() {
        try {
            BufferedReader reader = Main.getFileReader(FILENAME);
            String line;
            while ((line = reader.readLine()) != null){
                try {
                    Word word = Word.fromPronounce(line);
                    word.required = true;
                    words.add(word);
                } catch (Exception e){ }
            }

            reader.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public Dictionary(Statistics stats) {
        words = stats.getWords();

        G2PConvert english = new G2PEnglish(), russian = new G2PRussian();

        for (Word word : words) {
            if (word.pronounce != null) continue;

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
            word.pronounce = converter.convert(word.word);
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
                   writer.write(word.word + " " + word.pronounce + "\n");

            writer.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public boolean contains(String word){
        return words.contains(new Word(word));
    }

}
