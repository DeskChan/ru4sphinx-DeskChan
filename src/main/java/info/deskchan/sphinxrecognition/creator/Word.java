package info.deskchan.sphinxrecognition.creator;

public class Word implements Comparable<Word>{
    final String word;
    String pronounce = null;
    boolean required = false;
    long count = 1;
    int textsCount = 1;
    int textIndex = 0;

    Word(String word){  this.word = word;  }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    static Word fromString(String text){
        String[] parts = text.split(",");
        try {
            Word word = new Word(parts[0]);
            word.count = Long.parseLong(parts[1]);
            word.textsCount = Integer.parseInt(parts[1]);
            return word;
        } catch (Exception e){ }
        return null;
    }

    static Word fromPronounce(String text){
        String[] parts = text.split(" ", 2);
        try {
            Word word = new Word(parts[0]);
            word.pronounce = parts[1].trim();
            word.required = true;
            return word;
        } catch (Exception e){ }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Word)
            return ((Word) obj).word.equals(word);
        return obj.toString().equals(word);
    }

    @Override
    public String toString() {
        return word + "," + count + "," + textsCount;
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
}
