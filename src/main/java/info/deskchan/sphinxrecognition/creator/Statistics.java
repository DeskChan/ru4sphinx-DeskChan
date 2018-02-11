package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Statistics {

    LinkedList<Word> words = new LinkedList<>();

    protected static final String STATS_FILENAME = "stats";
    protected int currentText = 0;

    Statistics() {
        loadDefault();
        loadCustomPronounces();
    }


    private void loadDefault(){
        try {
            words.clear();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getClassLoader().getResourceAsStream("default_words.txt"), "UTF-8")
            );

            String line;
            while ((line = reader.readLine()) != null){
                try {
                    Word word = new Word(line);
                    word.required = true;
                    words.add(word);
                } catch (Exception e){ }
            }

            reader.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    private static final String PRONOUNCES_FILENAME = "custom";

    private void loadCustomPronounces(){
        try {
            BufferedReader reader = Main.getFileReader(PRONOUNCES_FILENAME);
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    words.add(Word.fromPronounce(line));
                } catch (Exception e) { }
            }

            reader.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    void add(String word){
        Iterator<Word> it = words.iterator();
        while (it.hasNext()){
            Word next = it.next();
            if (next.word.equals(word)){
                next.count++;
                if (next.textIndex != currentText){
                    next.textsCount++;
                    next.textIndex = currentText;
                }
                return;
            }
        }
        words.add(new Word(word));
    }

    public void sort(){
        Collections.sort(words);
    }

    public void resize(int newSize){
        sort();
        while (words.size() > newSize)
            words.removeLast();
    }

    public void save(){
        try {
            BufferedWriter writer = Main.getFileWriter(STATS_FILENAME);
            for(Word word : words)
                writer.write(word.toString() + "\n");

            writer.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public void load(){
        try {
            BufferedReader reader = Main.getFileReader(STATS_FILENAME);
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    words.add(Word.fromStats(line));
                } catch (Exception e) { }
            }

            reader.close();
        } catch (Exception e){
            Main.log(e);
        }
    }

    public void delete(){
        try {
            Files.delete(new File(STATS_FILENAME).toPath());
        } catch (Exception e){ }
    }

    public void print(){
        System.out.println("Size: "+words.size());
        int index = 10;
        for(Word word : words) {
            System.out.println(word.toString());
            index--;
            if (index < 0) return;
        }
    }

    List<Word> getWords(){
        return words;
    }
}
