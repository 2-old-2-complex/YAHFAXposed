/*
 * Copyright 1999-2101 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.com.alibaba.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

import external.com.alibaba.fastjson.JSONArray;
import external.com.alibaba.fastjson.JSONException;
import external.com.alibaba.fastjson.parser.DefaultJSONParser;
import external.com.alibaba.fastjson.parser.JSONToken;
import external.com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import external.com.alibaba.fastjson.util.TypeUtils;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class CollectionCodec implements ObjectSerializer, ObjectDeserializer {

    public final static CollectionCodec instance = new CollectionCodec();
    
    private CollectionCodec() {
        
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType) throws IOException {
        SerializeWriter out = serializer.out;

        if (object == null) {
            if ((out.features & SerializerFeature.WriteNullListAsEmpty.mask) != 0) {
                out.write("[]");
            } else {
                out.writeNull();
            }
            return;
        }

        Type elementType = null;
        if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
            elementType = TypeUtils.getCollectionItemType(fieldType);
        }

        Collection<?> collection = (Collection<?>) object;

        SerialContext context = serializer.context;
        serializer.setContext(context, object, fieldName, 0);

        if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
            if (HashSet.class == collection.getClass()) {
                out.append("Set");
            } else if (TreeSet.class == collection.getClass()) {
                out.append("TreeSet");
            }
        }

        try {
            int i = 0;
            out.write('[');
            for (Object item : collection) {

                if (i++ != 0) {
                    out.write(',');
                }

                if (item == null) {
                    out.writeNull();
                    continue;
                }

                Class<?> clazz = item.getClass();

                if (clazz == Integer.class) {
                    out.writeInt(((Integer) item).intValue());
                    continue;
                }

                if (clazz == Long.class) {
                    out.writeLong(((Long) item).longValue());

                    if ((out.features & SerializerFeature.WriteClassName.mask) != 0) {
                        out.write('L');
                    }
                    continue;
                }

                ObjectSerializer itemSerializer = serializer.config.get(clazz);
                itemSerializer.write(serializer, item, i - 1, elementType);
            }
            out.write(']');
        } finally {
            serializer.context = context;
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        if (parser.lexer.token() == JSONToken.NULL) {
            parser.lexer.nextToken(JSONToken.COMMA);
            return null;
        }
        
        if (type == JSONArray.class) {
            JSONArray array = new JSONArray();
            parser.parseArray(array);
            return (T) array;
        }

        Class<?> rawClass; //= getRawClass(type);
        for (Type t = type;;) {
            if (t instanceof Class<?>) {
                rawClass = (Class<?>) t;
                break;
            } else if (t instanceof ParameterizedType) {
                t = ((ParameterizedType) t).getRawType();
            } else {
                throw new JSONException("TODO");
            }
        }

        Collection list;
        if (rawClass == AbstractCollection.class // 
                || rawClass == Collection.class) {
            list = new ArrayList();
        } else if (rawClass.isAssignableFrom(HashSet.class)) {
            list = new HashSet();
        } else if (rawClass.isAssignableFrom(LinkedHashSet.class)) {
            list = new LinkedHashSet();
        } else if (rawClass.isAssignableFrom(TreeSet.class)) {
            list = new TreeSet();
        } else if (rawClass.isAssignableFrom(ArrayList.class)) {
            list = new ArrayList();
        } else if (rawClass.isAssignableFrom(EnumSet.class)) {
            Type itemType;
            if (type instanceof ParameterizedType) {
                itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
            } else {
                itemType = Object.class;
            }
            list = EnumSet.noneOf((Class<Enum>)itemType);
        } else {
            try {
                list = (Collection) rawClass.newInstance();
            } catch (Exception e) {
                throw new JSONException("create instane error, class " + rawClass.getName());
            }
        }

        Type itemType = TypeUtils.getCollectionItemType(type);
        parser.parseArray(itemType, list, fieldName);

        return (T) list;
    }

    
}
