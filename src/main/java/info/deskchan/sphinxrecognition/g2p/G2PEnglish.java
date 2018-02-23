package info.deskchan.sphinxrecognition.g2p;

import java.util.Arrays;
import java.util.List;

public class G2PEnglish extends G2PConvert {

    public G2PEnglish() { modelName = "en"; }

    public G2PEnglish(boolean euristic) { super(euristic); modelName = "en"; }

    protected List<String[]> replacing1 = Arrays.asList(
    /*        new String[]{"ao", "o1"},
            new String[]{"aw", "a1 u0"},
            new String[]{"ah", "a0"},
            new String[]{"aa", "a1"},
            new String[]{"ae", "e1"},
            new String[]{"ay", "a1 j"},

            new String[]{"ey", "e1 j"},
            new String[]{"er", "e0"},
            new String[]{"eh", "e0"},

            new String[]{"ow", "o0 u0"},
            new String[]{"oy", "o1 j"},

            new String[]{"iy", "i1"},
            new String[]{"ih", "i0"},

            new String[]{"uw", "u1"},
            new String[]{"uh", "u0"},

            new String[]{"ng", "n"},
            new String[]{"hh", "h"},
            new String[]{"w", "v"},
            new String[]{"jh", "d zh"},
            new String[]{"y", "j"}
    */
    /// ------------------------
            new String[]{"ao", "o"},
            new String[]{"aw", "aa u"},
            new String[]{"ah", "a"},
            new String[]{"aa", "aa"},
            new String[]{"ae", "ee"},
            new String[]{"ay", "aa j"},

            new String[]{"ey", "ee j"},
            new String[]{"er", "e"},
            new String[]{"eh", "e"},

            new String[]{"ow", "o u"},
            new String[]{"oy", "oo j"},

            new String[]{"iy", "ii"},
            new String[]{"ih", "i"},

            new String[]{"uw", "uu"},
            new String[]{"uh", "u"},

            new String[]{"ng", "n"},
            new String[]{"jh", "dd zh"}
    );

    protected List<String[]> replacing2 = Arrays.asList(
            new String[]{"y(\\s|$)", "j "},
            new String[]{"w", "v"}
    );
    protected String translateFirst(String word){
        return word;
    }
    protected String translateLetter(String letter) {
        for (String[] replacement : replacing1) {
            if (replacement[0].equals(letter)) {
                letter = replacement[1];
                break;
            }
        }
        return letter;
    }
    protected String translateWord(String word){
        for (String[] replacement : replacing2){
            word = word.replaceAll(replacement[0], replacement[1]);
        }
        return word.trim();
    }
}
