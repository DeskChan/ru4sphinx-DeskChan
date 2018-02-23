package info.deskchan.sphinxrecognition.creator;

import java.util.HashSet;
import java.util.Iterator;

public class Word implements Comparable<Word> {

    final String word;
    HashSet<String> pronounces = new HashSet<>();
    boolean required = false;
    long count = 1;
    private int textsCount = 1;
    private int textIndex = 0;

    Word(String word){
        int pos; if ((pos = word.indexOf('(')) > 0) word = word.substring(0, pos);
        this.word = word.toLowerCase();
    }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    static Word fromString(String text){
        String[] parts = text.split(",");
        try {
            Word word = new Word(parts[0]);
            word.count = Long.parseLong(parts[1]);
            word.textsCount = Integer.parseInt(parts[2]);
            return word;
        } catch (Exception e){ }
        return null;
    }

    static Word fromPronounce(String text){
        try {
            String[] parts = text.split(" ", 2);
            Word word = new Word(parts[0]);
            if (parts.length > 1)
                word.pronounces.add(parts[1].trim());

            word.required = true;
            return word;
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Word) {
            return ((Word) obj).word.equals(word);
        }
        return obj.toString().equals(word);
    }

    @Override
    public String toString() {
        return word + "," + count + "," + textsCount;
    }

    public String toPronouncesString(){
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = pronounces.iterator();
        for (int i = 0; i < pronounces.size(); i++)
            sb.append(word + (i > 0 ? ("(" + (i+1) + ")") : "") + " " + it.next() + "\n");
        return sb.toString();
    }

    public void addPronounces(Word other){
        if (other.required) required = true;
        pronounces.addAll(other.pronounces);
    }

    @Override
    public int compareTo(Word other){
        if (required && !other.required) return -1;
        if (!required && other.required) return 1;
        return Long.compare(other.count*other.textsCount, count*textsCount);
    }

    private final static String ENGLISH = "qwertyuiopasdfghjklzxcvbnm";
    private final static String RUSSIAN = "йцукенгшщзхъфывапролджэячсмитьбюё";

    enum Language { NO , ENGLISH, RUSSIAN }

    Language checkLanguage(){
        boolean ru = false, en = false;
        for(int i=0; i<word.length(); i++){
            if (ENGLISH.indexOf(word.charAt(i)) >= 0)
                en = true;
            else
            if (RUSSIAN.indexOf(word.charAt(i)) >= 0)
                ru = true;
            if (en && ru) return Language.NO;
        }
        if (en) return Language.ENGLISH;
        if (ru) return Language.RUSSIAN;
        return Language.NO;
    }

    public long setSqrt(){
        count = (long) Math.ceil(Math.sqrt(count));
        return count;
    }

    public void inc(int currentText){
        count++;
        if (textIndex != currentText){
            textsCount++;
            textIndex = currentText;
        }
    }
}
