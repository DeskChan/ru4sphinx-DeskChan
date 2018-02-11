package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class LanguageModelCreator {

    private static final String FILENAME = "lm.lm";

    public void save(Statistics stats){

        int len = 0, reqCount = 0;
        for(Word word : stats.words){
            if(word.required)
                reqCount++;
            else
                len += word.setSqrt();
        }

        double fwc = 0, reqNum;
        if (len > 0){
            fwc = len * 2 * 10/9;
            reqNum = Math.log10( (double) len / reqCount / fwc );
        } else {
            reqNum = Math.log10( (double) 1 / reqCount );
        }

        System.out.println(fwc + " " + reqCount + " " + (len / reqCount / fwc));
        try {
            BufferedWriter sw = new BufferedWriter(new FileWriter(FILENAME));
            //Console.WriteLine("Open file for writing");
            sw.write("\n\\data\\\nngram 1=" + stats.words.size() + "\n");
            sw.write("\n\n\\1-grams:\n");

            sw.write(String.format("%.4f", Math.log10((fwc-2*len)/fwc))+" <UNK>\n");
            sw.write("-99.0000 <s>\n");
            sw.write("-1.0000 </s>\n");
            for(Word word : stats.words){
                if(word.required)
                    sw.write(String.format("%.4f", reqNum));
                else
                    sw.write(String.format("%.4f", Math.log10((double) word.count / fwc)));
                sw.write(" " + word.word + "\n");
            }
            sw.write("\n\\end\\\n");

            sw.close();
        } catch (Exception e){
            Main.log(e);
        }
        //Console.WriteLine("Complete writing");
    }

}
