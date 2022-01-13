package external.com.alibaba.fastjson;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

import external.com.alibaba.fastjson.parser.DefaultJSONParser;
import external.com.alibaba.fastjson.parser.Feature;
import external.com.alibaba.fastjson.parser.JSONLexer;
import external.com.alibaba.fastjson.parser.JSONToken;
import external.com.alibaba.fastjson.util.TypeUtils;

import static external.com.alibaba.fastjson.JSONStreamContext.ArrayValue;
import static external.com.alibaba.fastjson.JSONStreamContext.PropertyKey;
import static external.com.alibaba.fastjson.JSONStreamContext.PropertyValue;
import static external.com.alibaba.fastjson.JSONStreamContext.StartArray;
import static external.com.alibaba.fastjson.JSONStreamContext.StartObject;

public class JSONReader implements Closeable {

    private final DefaultJSONParser parser;
    private JSONStreamContext       context;
    
    private Reader reader;

    public JSONReader(Reader reader){
        this(new JSONLexer(readAll(reader)));
        this.reader = reader;
    }
    
    static String readAll(Reader reader) {
        StringBuilder buf = new StringBuilder();
        
        try {
            char[] chars = new char[2048];
            for (;;) {
                int len = reader.read(chars, 0, chars.length);
                if (len < 0) {
                    break;
                }
                buf.append(chars, 0, len);
            }
        } catch(Exception ex) {
            throw new JSONException("read string from reader error", ex);
        }
        
        return buf.toString();
    }

    public JSONReader(JSONLexer lexer){
        this(new DefaultJSONParser(lexer));
    }

    public JSONReader(DefaultJSONParser parser){
        this.parser = parser;
    }

    public void config(Feature feature, boolean state) {
        this.parser.config(feature, state);
    }

    public void startObject() {
        if (context == null) {
            context = new JSONStreamContext(null, JSONStreamContext.StartObject);
        } else {
            startStructure();
            context = new JSONStreamContext(context, JSONStreamContext.StartObject);
        }

        this.parser.accept(JSONToken.LBRACE);
    }

    public void endObject() {
        this.parser.accept(JSONToken.RBRACE);
        endStructure();
    }

    public void startArray() {
        if (context == null) {
            context = new JSONStreamContext(null, StartArray);
        } else {
            startStructure();

            context = new JSONStreamContext(context, StartArray);
        }
        this.parser.accept(JSONToken.LBRACKET);
    }

    public void endArray() {
        this.parser.accept(JSONToken.RBRACKET);
        endStructure();
    }

    private void startStructure() {
        final int state = context.state;
        switch (state) {
            case PropertyKey:
                parser.accept(JSONToken.COLON);
                break;
            case PropertyValue:
            case ArrayValue:
                parser.accept(JSONToken.COMMA);
                break;
            case StartArray:
            case StartObject:
                break;
            default:
                throw new JSONException("illegal state : " + context.state);
        }
    }

    private void endStructure() {
        context = context.parent;

        if (context == null) {
            return;
        }
        
        final int state = context.state;
        int newState = -1;
        switch (state) {
            case PropertyKey:
                newState = PropertyValue;
                break;
            case StartArray:
                newState = ArrayValue;
                break;
            case PropertyValue:
            case StartObject:
                newState = PropertyKey;
                break;
            default:
                break;
        }
        if (newState != -1) {
            context.state = newState;
        }
    }

    public boolean hasNext() {
        if (context == null) {
            throw new JSONException("context is null");
        }

        final int token = parser.lexer.token();
        final int state = context.state;
        switch (state) {
            case StartArray:
            case ArrayValue:
                return token != JSONToken.RBRACKET;
            case StartObject:
            case PropertyValue:
                return token != JSONToken.RBRACE;
            default:
                throw new JSONException("illegal state : " + state);
        }
    }

    public int peek() {
        return parser.lexer.token();
    }

    public void close() {
        parser.lexer.close();
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new JSONException("closed reader error", ex);
            }
        }
    }

    public Integer readInteger() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            object = parser.parse();
            readAfter();
        }

        return TypeUtils.castToInt(object);
    }

    public Long readLong() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            object = parser.parse();
            readAfter();
        }

        return TypeUtils.castToLong(object);
    }

    public String readString() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            object = parser.parse();
            readAfter();
        }

        return TypeUtils.castToString(object);
    }
    
    public <T> T readObject(TypeReference<T> typeRef) {
        return readObject(typeRef.type);
    }

    public <T> T readObject(Type type) {
        if (context == null) {
            return parser.parseObject(type);
        }

        readBefore();
        T object = parser.parseObject(type);
        readAfter();
        return object;
    }

    public <T> T readObject(Class<T> type) {
        if (context == null) {
            return parser.parseObject(type);
        }

        readBefore();
        T object = parser.parseObject(type);
        readAfter();
        return object;
    }

    public void readObject(Object object) {
        if (context == null) {
            parser.parseObject(object);
            return;
        }

        readBefore();
        parser.parseObject(object);
        readAfter();
    }

    public Object readObject() {
        if (context == null) {
            return parser.parse();
        }

        readBefore();
        Object object = parser.parse();
        readAfter();
        return object;
    }

    @SuppressWarnings("rawtypes")
    public Object readObject(Map object) {
        if (context == null) {
            return parser.parseObject(object);
        }

        readBefore();
        Object value = parser.parseObject(object);
        readAfter();
        return value;
    }

    private void readBefore() {
        int state = context.state;
        // before
        switch (state) {
            case PropertyKey:
                parser.accept(JSONToken.COLON);
                break;
            case PropertyValue:
            case ArrayValue:
                parser.accept(JSONToken.COMMA);
                break;
            case StartObject:
                break;
            case StartArray:
                break;
            default:
                throw new JSONException("illegal state : " + state);
        }
    }

    private void readAfter() {
        int state = context.state;
        int newStat = -1;
        switch (state) {
            case StartObject:
                newStat = PropertyKey;
                break;
            case PropertyKey:
                newStat = PropertyValue;
                break;
            case PropertyValue:
                newStat = PropertyKey;
                break;
            case ArrayValue:
                break;
            case StartArray:
                newStat = ArrayValue;
                break;
            default:
                throw new JSONException("illegal state : " + state);
        }
        if (newStat != -1) {
            context.state = newStat;
        }
    }

}
