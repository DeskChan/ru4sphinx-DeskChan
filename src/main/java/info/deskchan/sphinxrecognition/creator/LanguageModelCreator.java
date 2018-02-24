package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Locale;

public class LanguageModelCreator {

    private static final String FILENAME = "lm.lm";

    public void save(Dictionary dictionary){

        Double frequencySum = null;
        int requiredWordsCount = 0;
        for(Word word : dictionary.words){
            if(word.required)
                requiredWordsCount++;
            else {
                if (frequencySum == null) frequencySum = 0D;
                frequencySum += word.setSqrt();
            }
        }

        if (frequencySum == null && requiredWordsCount == 0)
            throw new RuntimeException("THIS CAN'T BE HAPPENING!");

        double percentage;


        if (requiredWordsCount == 0){
            percentage = 0;
            requiredWordsCount = 1;
        } else if (frequencySum == null){
            percentage = 100;
            frequencySum = 1D;
        } else {
            if (Main.getPluginProxy() != null)
                percentage = Main.getPluginProxy().getProperties().getDouble("modelPercentage", 90);
            else percentage = 90;
        }
        percentage /= 100;

        double requiredWordWeight = Math.log10(percentage / requiredWordsCount);
        double commonWordMultiplier = (1 - percentage) / frequencySum;

        try {
            BufferedWriter sw = Main.getFileWriter(FILENAME);
            //Console.WriteLine("Open file for writing");
            sw.write("\n\\data\\\nngram 1=" + dictionary.words.size() + "\n");
            sw.write("\n\n\\1-grams:\n");

            sw.write("-4.0000 <UNK>\n");
            sw.write("-99.0000 <s>\n");
            sw.write("-99.0000 </s>\n");
            for(Word word : dictionary.words){
                if(word.required)
                    sw.write(String.format(Locale.US, "%.4f", requiredWordWeight));
                else
                    sw.write(String.format(Locale.US, "%.4f", Math.log10((double) word.count * commonWordMultiplier)));
                sw.write(" " + word.word + "\n");
            }
            sw.write("\n\\end\\\n");

            sw.close();
        } catch (Exception e){
            Main.log(e);
        }
        //Console.WriteLine("Complete writing");
    }

    public static String getModelPath(){
        return Main.getPluginProxy().getPluginDirPath().resolve(FILENAME).toAbsolutePath().toString();
    }

    public static void checkModel(){
        if (Main.getPluginProxy() == null || Main.getPluginProxy().getPluginDirPath().resolve(FILENAME).toFile().exists()) return;

        try {
            BufferedReader reader = Main.getFileReader(FILENAME);
            BufferedWriter writer = Main.getFileWriter(FILENAME);
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }

            reader.close();
            writer.close();
        } catch (Exception e){
            Main.log(e);
        }
    }
}
