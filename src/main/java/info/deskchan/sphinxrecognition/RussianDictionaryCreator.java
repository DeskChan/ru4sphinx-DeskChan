package info.deskchan.sphinxrecognition;

import java.util.Stack;

public class RussianDictionaryCreator extends DictionaryCreator {

    private final String letters="АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя \n-";

    protected String clearWord(String word){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<word.length(); i++)
            if(letters.indexOf(word.charAt(i)) >= 0)
                sb.append(word.charAt(i));

        return sb.toString();
    }

    protected int choose(int txt, int dir){
        if (dir < 0) return txt < 0 ? 0 : 1;
        if (txt < 0 || dir < txt) return 2;
        return 1;
    }

    protected boolean parsePage(String url){
        Page page = null;
        try {
            page = new Page(url);
        } catch (Exception e){
            Main.log(e);
            return false;
        }
        
        System.out.println("Recieving text");

        int start = page.code.indexOf("</h2></ul>", 0);
        if (start < 0) {
            int end = page.code.indexOf("<pre><hr noshade><small>", start);
            if (end > 0)
                page.code = page.code.substring(start, end - start);
        }
        
        System.out.print("Clearing... ");
        page.code = clearWord(page.code);
        System.out.println("Clearing completed");

        System.out.print("Adding to vocabulary... ");
        String[] wordsFound = page.code.split("[\\s\\n-]");

        for(int i=0, len = wordsFound.length; i<len; i++){
            if (wordsFound[i].length() == 0) continue;
            wordsFound[i] = wordsFound[i].toLowerCase();
            int pos = words.indexOf(wordsFound[i]);
            if (pos < 0)
                words.add(new Word(wordsFound[i]));
            else
                words.get(pos).count++;
        }

        System.out.print("Saving... ");
        saveStats();
        System.out.print("Completed!");

        return true;
    }

    private static final String
            block1   = "<li><tt><small>",
            block2   = "<A HREF=",
            blockDir = "<b>dir</b>",
            blockTxt = ".txt_Contents";

    protected void parseNet(){
        Stack<Page> pages = new Stack<>();
        
        System.out.println("Recieving page");
        try {
            pages.push(new Page("http://lib.ru/RUSSLIT/"));
            pages.peek().pos = pages.peek().code.indexOf("AWERCHENKO") - 100;
        } catch (Exception e){
            Main.log(e);
            return;
        }

        int pagesParsed = parsingCount, pos;
        do {
            System.out.println("Got page: " + pages.peek().url);
            Page currentPage = pages.peek();
            if(currentPage.code == null){
                pages.pop();
                continue;
            }
            pos = currentPage.pos;
            pos = currentPage.code.indexOf(block1, pos);
            if(pos < 0){
                pages.pop();
                continue;
            }
            int posTxt = currentPage.code.indexOf(blockTxt, pos);
            int posDir = currentPage.code.indexOf(blockDir, pos);
            pages.peek().pushPos(pos);

            switch(choose(posTxt, posDir)){
                case 0: continue;
                case 1:{
                    pos = currentPage.code.indexOf(block2, pos) + block2.length();
                    String href = currentPage.code.substring(pos, currentPage.code.indexOf(">", pos)-pos-9);
                    if(parsePage(pages.peek().url+href)) pagesParsed--;
                } break;
                case 2:{
                    pos = currentPage.code.indexOf(block2,pos) + block2.length();
                    String href = currentPage.code.substring(pos, currentPage.code.indexOf(">", pos)-pos);
                    if(href.contains("..")) continue;

                } break;
            }
        } while(pages.size()>0 && pagesParsed>0);
        //System.out.println();
        //Words.Save();
    }

    void download(){
        parseNet();
    }
}
