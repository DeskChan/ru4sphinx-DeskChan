package info.deskchan.sphinxrecognition.creator;

import java.util.Collections;
import java.util.LinkedList;

public class HashList extends LinkedList<Word> {
    @Override
    public boolean add(Word o) {
        if (o == null) return false;

        int index = indexOf(o);
        if (index < 0) return super.add(o);

        get(index).addPronounces(o);
        return true;
    }

    public boolean addNoCheck(Word o) {
        if (o == null) return false;
        return super.add(o);
    }

    public void sort(){
        Collections.sort(this);
    }
}
