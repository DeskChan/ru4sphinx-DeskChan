package info.deskchan.sphinxrecognition.creator;

import info.deskchan.sphinxrecognition.Main;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class RussianStatistics extends Statistics {

    public int parsingCount = 1000;

    public RussianStatistics() {
        super();
    }

    public RussianStatistics(int parsingCount){
        super();
        this.parsingCount = parsingCount;
    }

    private final static String LETTERS = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

    public static String clearWord(String word){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<word.length(); i++) {
            if (LETTERS.indexOf(word.charAt(i)) >= 0)
                sb.append(Character.toLowerCase(word.charAt(i)));
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ')
                sb.append(" ");
        }

        return sb.toString();
    }

    private int choose(int txt, int dir){
        if (dir < 0) return txt < 0 ? 0 : 1;
        if (txt < 0 || dir < txt) return 2;
        return 1;
    }

    String getPage(String url) throws Exception{
        StringBuilder result = new StringBuilder();

        URL page = new URL(url);
        URLConnection connection = page.openConnection();

        UniversalDetector detector = new UniversalDetector(null);
        BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
        stream.mark(10001);
        byte[] bytes = new byte[10000];
        int bytesCount = stream.read(bytes);
        detector.handleData(bytes, 0, bytesCount);
        stream.reset();

        String charset = detector.getDetectedCharset();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(stream, charset != null ? charset : "KOI8-R"));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            result.append(inputLine);
            result.append(" ");
        }
        in.close();

        return result.toString();
    }

    protected boolean parsePage(String url){
        Page page = null;
        try {
            page = new Page(url);
        } catch (Exception e){
            Main.log(e);
            return false;
        }
        
        System.out.println("Recieving text from page: "+url);

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
        String[] wordsFound = page.code.split("[\\.\\s\\n-]");

        for(int i=0, len = wordsFound.length; i<len; i++){
            if (wordsFound[i].length() == 0) continue;
            add(wordsFound[i]);
        }

        System.out.print("Saving... ");
        save();
        System.out.println("Completed!");

        currentText++;

        return true;
    }

    private static final String
            block1   = "<li><tt><small>",
            block2   = "<A HREF=",
            blockDir = "<b>dir</b>",
            blockTxt = ".txt_Contents";

    protected void parseNet(){
        Set<String> pagesVisited = new HashSet<>();
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
                case 0: pages.pop(); break;
                case 1:{
                    pos = currentPage.code.indexOf(block2, pos) + block2.length();
                    String href = currentPage.code.substring(pos, currentPage.code.indexOf(">", pos)-9);
                    if(parsePage(currentPage.url+href))
                        pagesParsed--;
                } break;
                case 2:{
                    pos = currentPage.code.indexOf(block2,pos) + block2.length();
                    String href = currentPage.code.substring(pos, currentPage.code.indexOf(">", pos));
                    href = currentPage.url + href;
                    if(!href.contains("..") && !pagesVisited.contains(href))
                        try {
                            pages.push(new Page(href));
                            pagesVisited.add(href);
                        } catch (Exception e){ continue; }
                } break;
            }
        } while(pages.size() > 0 && pagesParsed > 0);

    }

    public void download(){
        parseNet();
        sort();
    }

    protected class Page {
        String url;
        String code = null;
        int pos = 0;
        void pushPos(int p){
            pos = p+1;
        }
        Page(String URL) throws Exception{
            pos = 0;
            url = URL;
            code = getPage(url);
        }
    }
}
