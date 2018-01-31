package info.deskchan.sphinxrecognition.g2p;

import java.util.*;

public class MatrixMap<TK1, TK2, TV> {

    public class Entry<eTK1, eTK2, eTV>{
        public final eTK1 key1;
        public final eTK2 key2;
        public final eTV value;
        public Entry(eTK1 key1, eTK2 key2, eTV value){
            this.key1 = key1;
            this.key2 = key2;
            this.value = value;
        }
        public boolean equals(Entry other){
            return key1.equals(other.key1) && key2.equals(other.key2) && value.equals(other.value);
        }
    }

    private Map<TK1, Map<TK2, TV>> map = new HashMap<>();
    private Set<Entry<TK1, TK2, TV>> entrySet = new LinkedHashSet<>();

    public Map<TK2, TV> get(TK1 key){
        return map.get(key);
    }

    public TV get(TK1 key1, TK2 key2){
        try {
            return map.get(key1).get(key2);
        } catch (Exception e){
            return null;
        }
    }

    public Object remove(TK1 key1, TK2 key2) throws Exception{
        Object val = map.get(key1).remove(key2);
        Iterator<Entry<TK1, TK2, TV>> it = entrySet.iterator();
        while(it.hasNext()){
            Entry<TK1, TK2, TV> entry = it.next();
            if(entry.key1.equals(key1) && entry.key2.equals(key2))
                it.remove();
        }
        return val;
    }

    public Object remove(TK1 key){
        Object value = map.remove(key);
        Entry[] array = (Entry[]) entrySet.toArray();
        for(Entry entry : array)
            if(entry.key1 == key)
                entrySet.remove(entry);
        return value;
    }

    public Object put(TK1 key1, TK2 key2, TV value){
        Map submap = map.get(key1);
        if(submap == null)
            map.put(key1, submap = new HashMap<TK2, TV>());

        entrySet.add(new Entry<>(key1, key2, value));
        return submap.put(key2, value);
    }

    public Object put(TK1 key, Object value) throws Exception{
        Map<TK2, TV> val = (Map) value;
        map.put(key,  val);
        for(Map.Entry<TK2, TV> entry : val.entrySet())
            entrySet.add(new Entry<>(key, entry.getKey(), entry.getValue()));
        return val;
    }

    public Set<Entry<TK1, TK2, TV>> entrySet() {
        return entrySet;
    }

    public MatrixMap<TK1, TK2, TV> clone(){
        MatrixMap<TK1, TK2, TV> clonedMap = new MatrixMap<>();
        for(Entry<TK1, TK2, TV> entry : entrySet)
            clonedMap.put(entry.key1, entry.key2, entry.value);

        return clonedMap;
    }

    public void clear(){
        map.clear();
        entrySet.clear();
    }
}
