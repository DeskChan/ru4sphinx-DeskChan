package info.deskchan.sphinxrecognition.g2p;

import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import edu.cmu.sphinx.linguist.g2p.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class G2PConvert {

    protected String modelName = null;

    protected String VOWELS = "aouie";
    protected List<String[]> replacing = Arrays.asList(
            new String[]{"ur", "u0"},
            new String[]{"ae", "i0"},
            new String[]{"ao", "o0"}
    );

    private G2PConverter converter = null;

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


    public Collection<String> getPossible(String word, int count){
        ArrayList<String> list = new ArrayList<>();
        for(Path out : converter.phoneticize(word, count)) {
            StringBuilder sb = new StringBuilder();
            for (String letter : out.getPath()) {
                letter = letter.toLowerCase();
                for (String[] replacement : replacing){
                    if (replacement[0].equals(letter)) {
                        letter = replacement[1];
                        break;
                    }
                }
                char first = letter.charAt(0);
                int length = letter.length();
                boolean same = length == 2 && letter.charAt(0) == letter.charAt(1);
                if (VOWELS.indexOf(first) >= 0){
                    letter = first + (same ? "1" : "0");
                } else if (same){
                    letter = first + "j";
                }
                sb.append(letter);
                sb.append(" ");
            }
            sb.setLength(sb.length() - 1);
            list.add(word + " " + sb.toString());
        }

        return list;
    }

    public boolean allocated(){
        return converter != null;
    }
}
