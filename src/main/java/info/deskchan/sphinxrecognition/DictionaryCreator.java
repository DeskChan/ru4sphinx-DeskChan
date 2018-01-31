package info.deskchan.sphinxrecognition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public abstract class DictionaryCreator {

    int size = 10000;
    int parsingCount = 200;
    public DictionaryCreator(){

    }

    public DictionaryCreator(int size, int parsingCount){
        this.size = size;
        this.parsingCount = parsingCount;
    }

    static class Word {
        String word;
        int count = 0;

        Word(String word){  this.word = word;  }

        @Override
        public int hashCode() {
            return word.hashCode();
        }

        static Word fromString(String text){
            String[] parts = text.split(",");
            try {
                Word word = new Word(parts[0]);
                word.count = Integer.parseInt(parts[1]);
                return word;
            } catch (Exception e){ }
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return obj.toString().equals(word);
        }

        @Override
        public String toString() {
            return word + "," + count;
        }
    }

    ArrayList<Word> words = new ArrayList<>();
    abstract void download();

    String getPage(String url) throws Exception{
        StringBuilder result = new StringBuilder();

        URL page = new URL(url);
        BufferedReader in = new BufferedReader(
                    new InputStreamReader(page.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            result.append(inputLine);
        in.close();

        return result.toString();
    }

    protected class Page {
        public String url;
        public String code = null;
        public int pos = 0;
        public void pushPos(int p){
            pos = p+1;
        }
        public Page(String URL) throws Exception{
            pos = 0;
            url = URL;
            code = getPage(url);
        }
    }

    private static final String STATS_FILENAME = "stats";
    void saveStats(){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(STATS_FILENAME));
            for(Word word : words)
                writer.write(word.toString() + "\n");

            writer.close();
        } catch (Exception e){
            Main.log(e);
        }
    }
}
