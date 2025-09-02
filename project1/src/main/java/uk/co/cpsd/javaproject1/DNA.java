package uk.co.cpsd.javaproject1;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DNA {

    private final Map<String,Object> traits=new HashMap<>();

    public <T> void setTrait(String name, T value){
        traits.put(name,value);
    }

    public <T> T getTrait(String name, Class<T> type){
        Object value=traits.get(name);
        if(value==null) return null;
        return type.cast(value);

    }

    public Set<String> getTraitsName(){
        return traits.keySet();
    }

    public boolean hasTraits(String name){
        return traits.containsKey(name);
    }
}
