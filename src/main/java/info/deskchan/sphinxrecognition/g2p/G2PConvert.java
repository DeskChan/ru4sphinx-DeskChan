package info.deskchan.sphinxrecognition.g2p;

import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import edu.cmu.sphinx.linguist.g2p.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class G2PConvert {

    boolean euristic = true;

    protected String modelName = null;

    protected String VOWELS = "aouie";

    private G2PConverter converter = null;

    G2PConvert(boolean euristic){
        this.euristic = euristic;
    }

    G2PConvert(){}

    public void allocate() throws Exception{
        converter = new G2PConverter(
                getClass().getClassLoader().getResource("g2p/" + modelName + "-model.fst.ser")
        );
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


    protected String convertWord(Path out){
        StringBuilder sb = new StringBuilder(" ");
        for (String letter : out.getPath()) {
            if (euristic) sb.append(translateLetter(letter));
            else sb.append(letter);
            sb.append(" ");
        }
        if (euristic) return translateWord(sb.toString()).trim();
        else return sb.toString().trim();
    }


    protected abstract String translateLetter(String letter);

    protected abstract String translateWord(String word);

    protected abstract String translateFirst(String word);

    public boolean allocated(){
        return converter != null;
    }
}
