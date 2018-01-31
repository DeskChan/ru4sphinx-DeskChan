package info.deskchan.sphinxrecognition.g2p;

import info.deskchan.sphinxrecognition.Main;

import java.io.*;
import java.util.*;


/** Reimplementation of ru4sphinx/text2dict/dict2transcript.pl **/
public class G2PRussian implements G2PAbstract{
    
    private static boolean use_dictionary = true;		    // использовать словарь ударений
    private static boolean use_auto_accent = true;		    // автопростановка ударений в незнакомых словах - точнось ~80%
    private static boolean use_all_transcriptions = false;	// предусмотреть возможность оглушения/озвончивания первой буквы в слове (для выравнимания и обучения?)

    private static final String[]   sogl={"бб","тт","сс","фф","рр","нн","мм","кк","пп","лл","зз"};
    private static final String[][] sogl2={{"б","b"},{"в","v"},{"г","g"},{"д","d"},{"з","z"},{"к","k"},{"л","l"},{"м","m"},{"н","n"},{"п","p"},{"р","r"},{"с","s"},{"т","t"},{"ф","f"},{"х","h"}};
    private static final String[][] sogl3={{"ч","ch"},{"щ","sch"},{"й","j"},{"ж","zh"},{"ш","sh"},{"ц","c"}};
    private static final String[][] sogl4={{"b","p"},{"v","f"},{"g","k"},{"d","t"},{"z","s"},{"zh","sh"},{"bb","pp"},{"vv","ff"},{"gg","kk"},{"dd","tt"},{"zz","ss"}};
    private static final String[][] glas={{"а","я"},{"ы","и"},{"э","е"},{"о","ё"},{"у","ю"}};
    private static final String[][] glas2={{"а","ay","aa"},{"о","ay","oo"},{"у","u","uu"},{"и","i","ii"},{"ы","y","yy"},{"э","y","ee"},{"я","i","ja"},{"ю","uj","ju"},{"е","i","je"},{"ё","jo","jo"}};
    private static final String[]   HARD_SONAR = {"b","v","g","d","z","k","l","m","n","p","r","s","t","f","h","zh","sh","c"};
    private static final String[]   SOFT_SONAR = {"bb","vv","gg","dd","zz","j","kk","ll","mm","nn","pp","rr","ss","tt","ff","hh","ch","sch"};
    private static final String[]   SOFTLETTERS = {"ь","я","ё","ю","е","и"};
    private static final String[]   SURD = {"p","pp","f","ff","k","kk","t","tt","sh","s","ss","h","hh","c","ch","sch"};
    private static final String[]   RINGING1 = {"b","bb","g","gg","d","dd","zh","z","zz"};
    private static final String[]   VOWEL = {"а","я","о","ё","у","ю","э","е","ы","и","aa","a","oo","o","uu","u","ee","e","yy","y","ii","i","uj","ay","jo","je","ja","ju"};
    private static final String[]   STARTSYL = new String[30];
    private static final String[]   ALL_SONAR= new String[38];
    private static final String[]   SOGL = {"б","в","г","д","з","к","л","м","н","й","п","р","с","т","ф","х","ж","ш","щ","ц","ч","ь","ъ","'"};
    private static final String[]   SHIP = {"tt","sch","ch","щ","ч"};

    private static final String[] files = {"yo_word.txt","add_word.txt","all_form.txt","sokr_word.txt","emo_word.txt","morph_word.txt","small_word.txt","not_word.txt","affix.txt","tire_word.txt"};

    private static final String[][] REPLACEMENTS1 = {
            {"-", "ъ"},{"аэл","аjл"},{"стл","сл"},{"стн","сн"},{"здн","зн"},{"здц","сц"},{"ндш","нш"},{"нтг","нг"},{"нтц","нц"},{"рдц","рц"},{"ртц","рц"},{"рдч","рч"},
            {"лнц","нц"},{"стся","сца"},{"сться","сца"},{"стс","с"},{"стьс","с"},{"тьс","ц"},{"дьс","ц"},{"стц","ц"},{"чш","тш"},{"хтс","хс"},{"хдс","хс"},{"нкт","кт"},
            {"нгт","гт"},{"нтс","нс"},{"ндс","нс"},{"нтц","нц"},{"ндц","нц"},{"вств","ств"},{"фств","ств"},{"зч","щ"},{"сч","щ"},{"зш","ш"},{"сш","ш"},{"зщ","щ"},
            {"сщ","щ"},{"зж","ж"},{"сж","ж"},{"тц","ц"},{"дц","ц"},{"тч","ч"},{"дч","ч"},{"тщ","чщ"},{"дщ","чщ"},{"дст","цт"},{"иоа","иа"},{"иэ","и"},{"гк","hк"}};

    static {
        STARTSYL[0]="ь"; STARTSYL[1]="ъ";
        System.arraycopy(VOWEL, 0, STARTSYL, 2, VOWEL.length);

        ALL_SONAR[0]="ь"; ALL_SONAR[1]="ъ";
        System.arraycopy(HARD_SONAR, 0, ALL_SONAR, 2, HARD_SONAR.length);
        System.arraycopy(SOFT_SONAR, 0, ALL_SONAR, 2 + HARD_SONAR.length, SOFT_SONAR.length);
    }
    
    public void store (Collection<String> words) throws Exception{
        
        MatrixMap<String, String, Integer> accents = new MatrixMap<>();
        
        if (use_dictionary) {
            System.out.println("Parsing accent base");
            BufferedReader br = getResourceReader("accent.base");
            String line = br.readLine();

            while (line != null) {
                line = br.readLine();
                String[] parts = line.split("[|\\s]");
                accents.put(parts[0], parts[1], Integer.parseInt(parts[2]));
            }
            System.out.println("Аccent base parsing completed: " + accents.entrySet().size());
        }

        MatrixMap<String, String, Boolean> udar = new MatrixMap<>();
        
        if(use_dictionary){  //113
            System.out.println("Parsing dictionaries");
            for(String file : files)
                try {
                    BufferedReader br = getResourceReader(file);
                    String line = br.readLine();

                    while (line != null) {
                        String clword, udword;
                        int pos = line.indexOf(' ');
                        if(pos >= 0){
                            clword = line.substring(0, pos);
                            udword = line.substring(pos + 1);
                        } else
                            clword = udword = line;
                        
                        udword = udword.replace("ё","+ё");  //127
                        udword = udword.replace("++","+");  //128
                        udar.put(clword, udword, true);
                        if(clword.contains("ё")){ //144
                            clword = clword.replace('ё','е');
                            udar.put(clword, udword, true);
                        }
                        if(use_all_transcriptions && clword.length() > 1){ //156-220
                            char last = clword.charAt(clword.length()-1);
                            if(last == 'т' || last == 'д') clword=clword.substring(0,clword.length()-1)+'Д';
                            if(last == 'п' || last == 'б') clword=clword.substring(0,clword.length()-1)+'Б';
                            if(last == 'к' || last == 'г') clword=clword.substring(0,clword.length()-1)+'Г';
                            if(last == 'с' || last == 'з') clword=clword.substring(0,clword.length()-1)+'З';
                            if(last == 'ш' || last == 'ж') clword=clword.substring(0,clword.length()-1)+'Ж';
                            udar.put(clword, udword, true);
                            if(clword.charAt(0) == 'и') clword = 'ы'+clword.substring(1);
                            udar.put(clword, udword, true);
                        }
                    }
                } catch (Exception e){
                    return;
                }
            System.out.println("Dictionaries parsing completed");
        }
        System.out.println("Converting...");
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(Main.getPluginProxy().getDataDirPath().resolve("ru.dic").toFile())
        );
        for(String word : words)
            for(String out : convert(word, accents, udar))
               writer.write(word + " " + out + "\n");

        writer.close();
    }

    Collection<String> convert(String word, MatrixMap<String, String, Integer> accents, MatrixMap<String, String, Boolean> udar){
        
        ArrayList<String> newWords = new ArrayList<>();

        int word_count = 1;
        word = word.replace("\\(.+\\)$", "");  // 240

        Collection<String> IN2 = null;

        if(udar.get(word) != null)
            IN2 = udar.get(word).keySet();
        else if (use_auto_accent){
            String[] wordparts = word.split("-");
            word = "";
            for(String part : wordparts){
                if (udar.get(part) != null){
                    word += udar.get(part).keySet().iterator().next() + "-";
                    continue;
                }
                int glas_count = 0, glas_pos = -1;
                for (int i = 0; i < part.length(); i++)
                    for (int k=0; k<10; k++)
                        if(part.charAt(i) == glas2[k][0].charAt(0)){
                            glas_count++;
                            glas_pos = i;
                        }

                if(glas_count == 1){
                    word += part.substring(0, glas_pos) + "+" + part.substring(glas_pos) + "-";
                    continue;
                }
                glas_pos = part.indexOf('ё');
                if(glas_pos >= 0){
                    word += part.substring(0, glas_pos) + "+" + part.substring(glas_pos) + "-";
                    continue;
                }
                int ic = 0, pos, st;
                while(true){
                    for(; ic < part.length(); ic++)
                        if(arrayContains(VOWEL, part.charAt(ic))){
                            ic++;
                            break;
                        }
                    if(ic >= part.length()) break;
                    pos = ic;
                    if(arrayContains(SOGL, part.charAt(ic)))
                        if(ic < part.length() - 2 && arrayContains(SOGL, part.charAt(ic+1)))
                            pos = ic+1;

                    for(; ic < part.length(); ic++)
                        if(arrayContains(VOWEL, part.charAt(ic))) break;

                    if(ic >= part.length()) break;
                    part = part.substring(0, pos) + " " + part.substring(pos);
                    ic++;
                }
                String pre_slog="N", slg_s, nwrd="";
                int slg_p, pri=0;
                pos = -1;
                st = part.indexOf(" ");
                while(st >= 0){
                    slg_s = part.substring(0, st);
                    part = part.substring(st + 1);
                    slg_p = accents.get(pre_slog, slg_s);
                    pre_slog = slg_s;
                    if(pri < slg_p){
                        pri = slg_p;
                        ic = 0;
                        for(; ic < slg_s.length(); ic++)
                            if(arrayContains(VOWEL, slg_s.charAt(ic))) break;
                        pos = ic + nwrd.length();
                    }
                    nwrd += slg_s;
                    st = part.indexOf(" ");
                }
                slg_p = accents.get(pre_slog, part);
                if(pri < slg_p){
                    pri = slg_p;
                    ic = 0;
                    for(; ic<part.length(); ic++)
                        if(arrayContains(VOWEL, part.charAt(ic))) break;
                    pos = ic + nwrd.length();
                }
                nwrd += part;
                if(pos >= 0)
                    nwrd = nwrd.substring(0, pos-1) + "+" + nwrd.substring(pos);
                word += nwrd+"-";
            }
            IN2 = new ArrayList<>();
            if(word.length()==0) return IN2;
            word = word.substring(0 ,word.length() - 1);
            IN2.add(word);
        }
        for(String w : IN2){
            word = w.toLowerCase();
            StringBuilder ns = new StringBuilder(word);

            for(int i=0; i<sogl.length; i++)
                ns = replace(ns, sogl[i], Character.toString(sogl[i].charAt(0)));

            if(ns.toString().endsWith("ого"))
                ns = ns.replace(ns.length() - 3, ns.length(), "ovo");
            else if(ns.toString().endsWith("eго"))
                ns = ns.replace(ns.length() - 3, ns.length(), "evo");

            for (String[] replacement : REPLACEMENTS1)
                ns = replace(ns, replacement[0], replacement[1]);

            List<String> ns2 = new ArrayList<>();
            for(int i=0; i<ns.length(); i++)
                ns2.add(Character.toString(ns.charAt(i)));

            for(int i=1; i<ns2.size(); i++){
                if(arrayContains(SOFTLETTERS, ns2.get(i))){
                    int i2 = (equals(ns2, i-1, "+") ? 2 : 1);
                    if (i-i2 < 0) continue;
                    for(String[] sogls : sogl2)
                        if(equals(ns2, i-i2, sogls[0])){
                            ns2.set(i-i2, sogls[1] + sogls[1]);
                            break;
                        }
                }
            }

            for(int i=0;i<ns2.size()-1;i++){
                if(!ns2.get(i).matches("[нс]")) continue;
                for(String sh : SHIP)
                    if(equals(ns2, i+1, sh))
                        ns2.set(i, equals(ns2, i, "н") ? "nn" : "ss");
            }
            for(int i=0; i<ns2.size(); i++){
                for(String[] sog : sogl2)
                    if(equals(ns2, i, sogl[0])){
                        ns2.set(i, sog[1]);
                        break;
                    }
                for(String[] sog : sogl3)
                    if(equals(ns2, i, sogl[0])){
                        ns2.set(i, sog[1]);
                        break;
                    }
            }
            for(int k=0, l=ns2.size()-1; k<sogl4.length; k++){
                if(equals(ns2, l, sogl4[k][0]))
                    ns2.set(l, sogl4[k][1]);
                if(l == 0) continue;
                if(equals(ns2, l-1, sogl4[k][0]) && equals(ns2, l, "ъ"))
                    ns2.set(l-1, sogl4[k][1]);
                if(k > 4 && equals(ns2, l-1, sogl4[k][0]) && equals(ns2, l, "ь"))
                    ns2.set(l-1, sogl4[k][1]);
            }
            for(int i=0; i<ns2.size()-1; i++){
                for(String[] sog : sogl4)
                    if(equals(ns2, i, sog[0]) && arrayContains(SURD, ns2.get(i+1))){
                        ns2.set(i, sog[1]);
                        break;
                    }
            }
            for(int i=0; i<ns2.size()-1; i++){
                for(String[] sog : sogl4)
                    if(equals(ns2, i, sog[1]) && arrayContains(RINGING1, ns2.get(i+1))){
                        ns2.set(i, sog[0]);
                        break;
                    }
            }
            if(equals(ns2, ns2.size()-1, "ь")) ns2.remove(ns2.size()-1);

            ns2 = replace(ns2,"t s","c");
            ns2 = replace(ns2,"s sh","sh");
            ns2 = replace(ns2,"s ch","sch");
            ns2 = replace(ns2,"s sch","sch");
            ns2 = replace(ns2,"z zh","zh");
            ns2 = replace(ns2,"t c","c");
            ns2 = replace(ns2,"t ch","ch");
            ns2 = replace(ns2,"t sch","ch sch");

            for(int i=0; i<ns2.size();i++){
                if(ns2.get(i).matches("[юяеё]")){
                    if(i>0){
                        if(i>1){
                            if(arrayContains(STARTSYL, ns2.get(i-2)) && ns2.get(i-1)=="+")
                                ns2.add(i-1, "j");
                        }  //1
                        else if(ns2.get(i-1).matches("[ъь]")){  //2
                            ns2.add(i, "j");
                            i++;
                        } else if(equals(ns2, i-1, "+")){  //4.2
                            ns2.add(0, "j");
                            i++;
                        }
                    } else {  //4.1
                        ns2.add(0,"j");
                        i++;
                    }
                } else if(ns2.get(i).matches("[иоэ]")){
                    if(i>0 && ns2.get(i-1).matches("[ъь]")){  //5.1
                        ns2.add(i, "j");
                        i++;
                    } else if(i>1 && equals(ns2, i-1, "+") && ns2.get(i-2).matches("[ъь]")){  //5.2
                        ns2.set(i-1, "j");
                    }
                }
            }
            for(int i=0; i<ns2.size()-1; i++){
                boolean b1;
                if( (b1=arrayContains(HARD_SONAR, ns2.get(i))) || arrayContains(SOFT_SONAR, ns2.get(i)) ){
                    for(int k = 0, n = (b1 ? 1 : 0); k<5; k++){
                        if(equals(ns2, i+1, glas[k][n])){
                            ns2.set(i+1, glas[k][1-n]);
                            break;
                        }
                        if(i < ns2.size()-2 && equals(ns2, i+1, "+") && equals(ns2, i+2, glas[k][1])){
                            ns2.set(i+2, glas[k][0]);
                            break;
                        }
                    }
                }
            }
            if(ns2.get(0).matches("[ао]")) ns2.set(0, "a");
            for(int i=0, k=-1, st=0; i<ns2.size(); i++){ /*    / (zh|sh) [о](( ($ALL_SONAR))* \+($STARTSYL))/ $1 y$2/     */
                switch (st){
                    case 1:{
                        if(equals(ns2, i, "о")){ k=i; st=2; }
                        else { k=-1; st=0; }
                    } break;
                    case 2: case 3:{
                        if(arrayContains(ALL_SONAR, ns2.get(i))){
                            if(st==2) st=3;
                        } else if(equals(ns2, i, "+")) st=4;
                    } break;
                    case 4: if(arrayContains(STARTSYL, ns2.get(i))){
                        if(k>=0) ns2.set(k, "y");
                        st=0;
                    } break;
                }
                if(ns2.get(i).matches("(zh|sh)") && st==0) st=1;
            }
            for(int i=0, k=-1, st=0; i<ns2.size(); i++){ /*    / [ао](( ($ALL_SONAR))* \+($STARTSYL))/ a$1/     */
                switch (st){
                    case 1: case 2:{
                        if(arrayContains(ALL_SONAR, ns2.get(i))){
                            if(st==1) st=2;
                        } else if(equals(ns2, i, "+")) st=3;
                    } break;
                    case 3: if(arrayContains(STARTSYL, ns2.get(i))){
                        if(k>=0) ns2.set(k, "a");
                        st=0;
                    }
                }
                if(ns2.get(i).matches("[ао]") && st==0) { k=i; st=1; }
            }
            for(int i=0, n; i<ns2.size(); i++){
                n = (i>0 && equals(ns2, i-1, "+")) ? 2 : 1;
                for(String[] gl : glas2)
                    if(equals(ns2, i, gl[0])){
                        ns2.set(i, gl[n]);
                        break;
                    }
                if (n == 2){
                    ns2.remove(i-1);
                    i--;
                }
            }
            for(int i=0;i<ns2.size();i++)
                if(ns2.get(i-1).matches("[ъь]")){
                    ns2.remove(i);
                    i--;
                }

            word = "";
            for(int i=0; i<ns2.size(); i++)
                if(ns2.get(i).length() > 0) word += ns2.get(i) + " ";

            if(IN2.contains(word)) continue;

            word_count++;
            IN2.add(word);
        }
        return IN2;
    }


    BufferedReader getResourceReader(String url){
        return new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream(url)));
    }

    boolean arrayContains(String[] array, char item) {
        for (int i=0; i<array.length; i++)
            if (array[i].equals(Character.toString(item))) return true;
        return false;
    }

    boolean arrayContains(String[] array, String item) {
        for (int i=0; i<array.length; i++)
            if (array[i].equals(item)) return true;
        return false;
    }

    StringBuilder replace(StringBuilder sb, String from, String to){
        int pos = sb.indexOf(from);
        if (pos < 0) return sb;
        sb.replace(pos, pos + from.length(), to);
        return sb;
    }
    List<String> replace(List<String> list, String find, String replace){
        String[] to_find = find.split(" "),
                 to_replace = replace.split(" ");
        for(int i=0; i <= list.size() - to_find.length;i++){
            boolean match = true;
            for(int j=0; j<to_find.length; j++)
                if(!list.get(i+j).equals(to_find[j])){
                    match = false;
                    break;
                }
            if(!match) continue;
            for(int j=0; j<to_find.length; j++)
                list.remove(i);
            for(int j=to_replace.length-1; j>=0; j--)
                list.add(i, to_replace[j]);
            i += to_replace.length-1;
        }
        return list;
    }

    boolean equals(List<String> list, int pos, String value) {
        return list.get(pos).equals(value);
    }
}
