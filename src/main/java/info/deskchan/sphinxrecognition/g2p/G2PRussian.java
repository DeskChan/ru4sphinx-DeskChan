package info.deskchan.sphinxrecognition.g2p;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class G2PRussian extends G2PConvert{

    public G2PRussian(){
        modelName = "ru";
        VOWELS = "aouiey";
    }

    public G2PRussian(boolean euristic) { super(euristic); modelName = "ru"; VOWELS = "aouiey"; }

    protected List<String[]> replacing0 = Arrays.asList(
            new String[]{"сего", "сево"},
            new String[]{"его$", "ево"},
            new String[]{"ого$", "ово"},
            new String[]{"тcя", "ца"},
            new String[]{"ться", "ца"},
            new String[]{"тc", "ц"},
            new String[]{"ци", "цы"},
            new String[]{"чш", "тш"},
            new String[]{"дц", "ц"},
            new String[]{"тц", "ц"}
    );

    protected List<String[]> replacing1 = Arrays.asList(
       //     new String[]{"ur", "u0"},
       //     new String[]{"ae", "i0"},
       //     new String[]{"ao", "o0"}
            new String[]{"ur", "u"},
            new String[]{"ae", "i"},
            new String[]{"ao", "o"},
            new String[]{"ay", "y"},
            new String[]{"ji", "ii"}
    );

    protected List<String[]> replacing2 = Arrays.asList(
            new String[]{"l ll", "l"},
            new String[]{"y e", "y i"},
            new String[]{"e", "y"}
    );

    protected String translateFirst(String word){
        for (String[] replacement : replacing0){
            try {
                word = word.replaceAll(replacement[0], replacement[1]);
            } catch (Exception e){ }
        }
        return word;
    }
    Pattern pattern0 = Pattern.compile("([аеиуо])j (["+VOWELS+"]) ");
    protected String translateLetter(String letter){
        for (String[] replacement : replacing1){
            if (replacement[0].equals(letter)) {
                letter = replacement[1];
                break;
            }
        }
        char first = letter.charAt(0);
        int length = letter.length();
        if (VOWELS.indexOf(first) < 0 && length == 2 && letter.charAt(1) == 'j'){
            letter = Character.toString(first) + first;
        }
        /*char first = letter.charAt(0);
        int length = letter.length();
        boolean same = length == 2 && letter.charAt(0) == letter.charAt(1);
        if (VOWELS.indexOf(first) >= 0){
            letter = first + (same ? Character.toString(first) : "");
        } else if (same){
            letter = first + "j";
        }*/
        return letter.trim();
    }

    Pattern pattern1 = Pattern.compile("([^\\s"+VOWELS+"])j (["+VOWELS+"]) ");
    Pattern pattern2 = Pattern.compile("([^\\s"+VOWELS+"]{2}) ([eauo])([eauo]?) ");
    Pattern pattern3 = Pattern.compile("([^\\s"+VOWELS+"]{2}) (e) ");
    Pattern pattern4 = Pattern.compile(" j ([eauo])([eauo]?) ");
    Matcher matcher;
    protected String translateWord(String word){
        matcher = pattern1.matcher(word);
        word = matcher.replaceAll("$1$1 j$2");

        matcher = pattern3.matcher(word);
        word = matcher.replaceAll("$1 i ");

        matcher = pattern4.matcher(word);
        word = matcher.replaceAll(" j$1 ");

        for (String[] replacement : replacing2){
            word = word.replaceAll(" " + replacement[0] + " ", " " + replacement[1] + " ");
        }

        matcher = pattern2.matcher(word);
        word = matcher.replaceAll("$1 j$2 ");

        return word.trim();
    }
}
