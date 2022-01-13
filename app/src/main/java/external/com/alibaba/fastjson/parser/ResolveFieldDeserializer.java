package external.com.alibaba.fastjson.parser;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import external.com.alibaba.fastjson.JSONArray;
import external.com.alibaba.fastjson.parser.deserializer.FieldDeserializer;
import external.com.alibaba.fastjson.util.TypeUtils;

@SuppressWarnings("rawtypes")
final class ResolveFieldDeserializer extends FieldDeserializer {

    private final int               index;
    private final List              list;
    private final DefaultJSONParser parser;
    
    private final Object            key;
    private final Map               map;
    
    private final Collection collection;

    public ResolveFieldDeserializer(DefaultJSONParser parser, List list, int index){
        super(null, null, 0);
        this.parser = parser;
        this.index = index;
        this.list = list;
        
        key = null;
        map = null;
        
        collection = null;
    }
    
    public ResolveFieldDeserializer(Map map, Object index){
        super(null, null, 0);
        
        this.parser = null;
        this.index = -1;
        this.list = null;
        
        this.key = index;
        this.map = map;
        
        collection = null;
    }
    
    public ResolveFieldDeserializer(Collection collection){
        super(null, null, 0);
        
        this.parser = null;
        this.index = -1;
        this.list = null;
        
        key = null;
        map = null;
        
        this.collection = collection;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object object, Object value) {
        if (map != null) {
            map.put(key, value);
            return;
        }
        
        if (collection != null) {
            collection.add(value);
            return;
        }
        
        list.set(index, value);

        if (list instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) list;
            Object array = jsonArray.getRelatedArray();

            if (array != null) {
                int arrayLength = Array.getLength(array);

                if (arrayLength > index) {
                    Object item;
                    if (jsonArray.getComponentType() != null) {
                        item = TypeUtils.cast(value, jsonArray.getComponentType(), parser.config);
                    } else {
                        item = value;
                    }
                    Array.set(array, index, item);
                }
            }
        }
    }

    public void parseField(DefaultJSONParser parser, Object object, Type objectType, Map<String, Object> fieldValues) {

    }

}
