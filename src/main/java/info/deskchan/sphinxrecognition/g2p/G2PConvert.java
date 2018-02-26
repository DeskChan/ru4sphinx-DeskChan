package info.deskchan.sphinxrecognition.g2p;

import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import edu.cmu.sphinx.linguist.g2p.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class G2PConvert {

    boolean euristic = true;

    protected String modelName = null;

    protected String VOWELS = "aouie";

    private G2PConverter converter = null;

    G2PConvert(boolean euristic){
        this.euristic = euristic;
    }

    G2PConvert(){}

    public void allocate(){
        try {
            converter = new G2PConverter(
                    getClass().getClassLoader().getResource("g2p/" + modelName + "-model.fst.ser")
            );
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void deallocate(){
        converter = null;
    }

    public Collection<String> convert(Collection<String> words){
        ArrayList<String> list = new ArrayList<>();
        for(String word : words)
            list.addAll(getPossible(word, 1));

        return list;
    }

    public String convert(String word){
        return getPossible(word, 1).iterator().next();
    }

    public List<String> getPossible(String word, int count){
        StringBuilder sb = new StringBuilder(word.toLowerCase());
        for (int i = 1; i < sb.length(); i++) if (sb.charAt(i) == sb.charAt(i-1)) { sb.deleteCharAt(i); i--; }
        word = sb.toString();

        if (euristic) word = translateFirst(word);

        ArrayList<String> list = new ArrayList<>();
        for(Path out : converter.phoneticize(word, Math.min(5, count * 2))) {
            String result = convertWord(out);
            if (result.contains("1") || !result.matches(".*["+VOWELS+"]+.*"))
                list.add(result);
            if (list.size() >= count) break;
        }
        if (list.size() == 0) {
            try {
                list.add(convertWord(converter.phoneticize(word, 1).get(0)));
            } catch (Exception e){
                System.out.println("|"+word+"|");
            }
        }

        return list;
    }

    Pattern pattern1 = Pattern.compile("([^\\s"+VOWELS+"])j (["+VOWELS+"]) ");
    Pattern pattern2 = Pattern.compile("([^\\s"+VOWELS+"]{2}) ([eauo])([eauo]?) ");
    Pattern pattern3 = Pattern.compile("([^\\s"+VOWELS+"]{2}) (e) ");
    Pattern pattern4 = Pattern.compile(" j ([eauo])([eauo]?) ");
    Matcher matcher;
    protected List<String[]> replacing2 = Arrays.asList(
            new String[]{" w ", " v "},
            new String[]{" l ll ", " l "},
            new String[]{" y e ", " y i "},
            new String[]{" e ", " y "}
    );
    protected String translateWord(String word){
        //System.out.println(word);
        String[] parts = word.split(" ");
        ArrayList<Integer> vow = new ArrayList<>();
        for (int i=0; i<parts.length; i++){
            if (parts[i].matches("["+VOWELS+"]+"))
                vow.add(i);
        }
        if (vow.size() == 1){
            parts[vow.get(0)] = Character.toString(parts[vow.get(0)].charAt(0)) + parts[vow.get(0)].charAt(0);
            word = "";
            for (String part : parts) word += part+" ";
        }

        matcher = pattern1.matcher(word);
        word = matcher.replaceAll("$1$1 j$2");

        matcher = pattern3.matcher(word);
        word = matcher.replaceAll("$1 i ");

        matcher = pattern4.matcher(word);
        word = matcher.replaceAll(" j$1 ");

        for (String[] replacement : replacing2){
            word = word.replaceAll(replacement[0], replacement[1]);
        }

        matcher = pattern2.matcher(word);
        word = matcher.replaceAll("$1 j$2 ");

        return word.trim();
    }

    protected String convertWord(Path out){
        StringBuilder sb = new StringBuilder(" ");
        for (String letter : out.getPath()) {
            letter = letter.toLowerCase();
            if (euristic) sb.append(translateLetter(letter));
            else sb.append(letter);
            sb.append(" ");
        }
        if (euristic) return translateWord(sb.toString()).trim();
        else return sb.toString().trim();
    }


    protected abstract String translateLetter(String letter);

    protected abstract String translateFirst(String word);

    public boolean allocated(){
        return converter != null;
    }
}
