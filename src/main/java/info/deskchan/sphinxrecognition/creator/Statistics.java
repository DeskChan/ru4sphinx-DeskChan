package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Statistics {

    HashList words = new HashList();

    protected static final String STATS_FILENAME = "stats";
    protected int currentText = 0;

    void add(String word){
        Iterator<Word> it = words.iterator();
        while (it.hasNext()){
            Word next = it.next();
            if (next.word.equals(word)){
                next.inc(currentText);
                return;
            }
        }
        words.addNoCheck(new Word(word));
    }

    public void sort(){
        words.sort();
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
                    words.addNoCheck(Word.fromString(line));
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

    HashList getWords(){
        return words;
    }
}
