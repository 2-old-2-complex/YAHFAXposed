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
import java.lang.reflect.Type;

import external.com.alibaba.fastjson.parser.DefaultJSONParser;
import external.com.alibaba.fastjson.parser.JSONLexer;
import external.com.alibaba.fastjson.parser.JSONToken;
import external.com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import external.com.alibaba.fastjson.util.TypeUtils;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public final class BooleanCodec implements ObjectSerializer, ObjectDeserializer {

    public final static BooleanCodec instance = new BooleanCodec();
    
    private BooleanCodec() {
        
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType) throws IOException {
        SerializeWriter out = serializer.out;

        Boolean value = (Boolean) object;
        if (value == null) {
            if ((out.features & SerializerFeature.WriteNullBooleanAsFalse.mask) != 0) {
                out.write("false");
            } else {
                out.writeNull();
            }
            return;
        }

        if (value.booleanValue()) {
            out.write("true");
        } else {
            out.write("false");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type clazz, Object fieldName) {
        final JSONLexer lexer = parser.lexer;

        Boolean boolObj;
        int token = lexer.token();
        if (token == JSONToken.TRUE) {
            lexer.nextToken(JSONToken.COMMA);
            boolObj = Boolean.TRUE;
        } else if (token == JSONToken.FALSE) {
            lexer.nextToken(JSONToken.COMMA);
            boolObj = Boolean.FALSE;
        } else if (token == JSONToken.LITERAL_INT) {
            int intValue = lexer.intValue();
            lexer.nextToken(JSONToken.COMMA);

            if (intValue == 1) {
                boolObj = Boolean.TRUE;
            } else {
                boolObj = Boolean.FALSE;
            }
        } else {
            Object value = parser.parse();

            if (value == null) {
                return null;
            }

            boolObj = TypeUtils.castToBoolean(value);
        }

        return (T) boolObj;
    }
}
