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
package external.com.alibaba.fastjson.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import external.com.alibaba.fastjson.JSON;
import external.com.alibaba.fastjson.JSONException;
import external.com.alibaba.fastjson.JSONObject;
import external.com.alibaba.fastjson.PropertyNamingStrategy;
import external.com.alibaba.fastjson.annotation.JSONField;
import external.com.alibaba.fastjson.annotation.JSONType;
import external.com.alibaba.fastjson.parser.JSONLexer;
import external.com.alibaba.fastjson.parser.JavaBeanDeserializer;
import external.com.alibaba.fastjson.parser.ParserConfig;
import external.com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import external.com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class TypeUtils {
	
    public static boolean compatibleWithJavaBean = false;
    private static boolean setAccessibleEnable    = true;

    private static volatile Class kotlin_metadata;
    private static volatile boolean kotlin_metadata_error;

    private static volatile boolean kotlin_class_klass_error;
    private static volatile Constructor kotlin_kclass_constructor;
    private static volatile Method kotlin_kclass_getConstructors;
    private static volatile Method kotlin_kfunction_getParameters;
    private static volatile Method kotlin_kparameter_getName;

    private static volatile boolean kotlin_error;

    private static volatile Map<Class, String[]> kotlinIgnores;
    private static volatile boolean kotlinIgnores_error;

    public static boolean isKotlin(Class clazz) {
        if (kotlin_metadata == null && !kotlin_metadata_error) {
            try {
                kotlin_metadata = Class.forName("kotlin.Metadata");
            } catch (Throwable e) {
                kotlin_metadata_error = true;
            }
        }

        if (kotlin_metadata == null) {
            return false;
        }

        return clazz.isAnnotationPresent(kotlin_metadata);
    }

    private static boolean isKotlinIgnore(Class clazz, String methodName) {
        if (kotlinIgnores == null && !kotlinIgnores_error) {
            try {
                Map<Class, String[]> map = new HashMap<Class, String[]>();

                Class charRangeClass = Class.forName("kotlin.ranges.CharRange");
                map.put(charRangeClass, new String[]{"getEndInclusive","isEmpty"});

                Class intRangeClass = Class.forName("kotlin.ranges.IntRange");
                map.put(intRangeClass, new String[]{"getEndInclusive","isEmpty"});

                Class longRangeClass = Class.forName("kotlin.ranges.LongRange");
                map.put(longRangeClass, new String[]{"getEndInclusive", "isEmpty"});

                Class floatRangeClass = Class.forName("kotlin.ranges.ClosedFloatRange");
                map.put(floatRangeClass, new String[]{"getEndInclusive","isEmpty"});

                Class doubleRangeClass = Class.forName("kotlin.ranges.ClosedDoubleRange");
                map.put(doubleRangeClass, new String[]{"getEndInclusive","isEmpty"});

                kotlinIgnores = map;
            } catch (Throwable error) {
                kotlinIgnores_error = true;
            }
        }

        if (kotlinIgnores == null) {
            return false;
        }

        String[] ignores = kotlinIgnores.get(clazz);
        if (ignores == null) {
            return false;
        }

        return Arrays.binarySearch(ignores, methodName) >= 0;
    }

    public static String[] getKoltinConstructorParameters(Class clazz) {
        if (kotlin_kclass_constructor == null && !kotlin_class_klass_error) {
            try {
                Class class_kotlin_kclass = Class.forName("kotlin.reflect.jvm.internal.KClassImpl");
                kotlin_kclass_constructor = class_kotlin_kclass.getConstructor(Class.class);
                kotlin_kclass_getConstructors = class_kotlin_kclass.getMethod("getConstructors");

                Class class_kotlin_kfunction = Class.forName("kotlin.reflect.KFunction");
                kotlin_kfunction_getParameters = class_kotlin_kfunction.getMethod("getParameters");

                Class class_kotlinn_kparameter = Class.forName("kotlin.reflect.KParameter");
                kotlin_kparameter_getName = class_kotlinn_kparameter.getMethod("getName");
            } catch (Throwable e) {
                kotlin_class_klass_error = true;
            }
        }

        if (kotlin_kclass_constructor == null) {
            return null;
        }

        if (kotlin_error) {
            return null;
        }

        try {
            Object constructor = null;
            Object kclassImpl = kotlin_kclass_constructor.newInstance(clazz);
            Iterable it = (Iterable) kotlin_kclass_getConstructors.invoke(kclassImpl);
            for (Iterator iterator = it.iterator();iterator.hasNext();iterator.hasNext()) {
                Object item = iterator.next();
                List parameters = (List) kotlin_kfunction_getParameters.invoke(item);
                if (constructor != null && parameters.size() == 0) {
                    continue;
                }
                constructor = item;
            }

            List parameters = (List) kotlin_kfunction_getParameters.invoke(constructor);
            String[] names = new String[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                names[i] = (String) kotlin_kparameter_getName.invoke(param);
            }
            return names;
        } catch (Throwable e) {
            kotlin_error = true;
        }

        return null;
    }
    
    public static final String castToString(Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public static final Byte castToByte(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0 // 
                    || "null".equals(strVal)) {
                return null;
            }
            
            return Byte.parseByte(strVal);
        }

        throw new JSONException("can not cast to byte, value : " + value);
    }

    public static final Character castToChar(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Character) {
            return (Character) value;
        }

        if (value instanceof String) {
            String strVal = (String) value;

            if (strVal.length() == 0) {
                return null;
            }

            if (strVal.length() != 1) {
                throw new JSONException("can not cast to byte, value : " + value);
            }

            return strVal.charAt(0);
        }

        throw new JSONException("can not cast to byte, value : " + value);
    }

    public static final Short castToShort(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }
            
            return Short.parseShort(strVal);
        }

        throw new JSONException("can not cast to short, value : " + value);
    }

    public static final BigDecimal castToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        }

        String strVal = value.toString();
        if (strVal.length() == 0 //
            || "null".equals(strVal)) {
            return null;
        }

        return new BigDecimal(strVal);
    }

    public static final BigInteger castToBigInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }

        if (value instanceof Float || value instanceof Double) {
            return BigInteger.valueOf(((Number) value).longValue());
        }

        String strVal = value.toString();
        if (strVal.length() == 0 //
            || "null".equals(strVal)) {
            return null;
        }

        return new BigInteger(strVal);
    }

    public static final Float castToFloat(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }

            return Float.parseFloat(strVal);
        }

        throw new JSONException("can not cast to float, value : " + value);
    }

    public static final Double castToDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            String strVal = value.toString();
            if(strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)){
                return null;
            }
            
            return Double.parseDouble(strVal);
        }

        throw new JSONException("can not cast to double, value : " + value);
    }

    public static final Date castToDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        long longValue = -1;

        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            int scale = decimal.scale();
            if (scale >= -100 && scale <= 100) {
                longValue = decimal.longValue();
            } else {
                longValue = decimal.longValueExact();
            }
        } else if (value instanceof Number) {
            longValue = ((Number) value).longValue();
        } else if (value instanceof String) {
            String strVal = (String) value;

            if (strVal.indexOf('-') != -1) {
                String format;
                if (strVal.length() == JSON.DEFFAULT_DATE_FORMAT.length()) {
                    format = JSON.DEFFAULT_DATE_FORMAT;
                } else if (strVal.length() == 10) {
                    format = "yyyy-MM-dd";
                } else if (strVal.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                    format = "yyyy-MM-dd HH:mm:ss";
                } else if (strVal.length() == 29
                        && strVal.charAt(26) == ':'
                        && strVal.charAt(28) == '0') {
                    format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
                } else {
                    format = "yyyy-MM-dd HH:mm:ss.SSS";
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat(format, JSON.defaultLocale);
                dateFormat.setTimeZone(JSON.defaultTimeZone);
                try {
                    return (Date) dateFormat.parse(strVal);
                } catch (ParseException e) {
                    throw new JSONException("can not cast to Date, value : " + strVal);
                }
            }

            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }

            longValue = Long.parseLong(strVal);
        }

        if (longValue < 0) {
            throw new JSONException("can not cast to Date, value : " + value);
        }

        return new Date(longValue);
    }

    public static final Long castToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            int scale = decimal.scale();
            if (scale >= -100 && scale <= 100) {
                return decimal.longValue();
            }

            return decimal.longValueExact();
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }

            try {
                return Long.parseLong(strVal);
            } catch (NumberFormatException ex) {
                //
            }

            JSONLexer dateParser = new JSONLexer(strVal);
            Calendar calendar = null;
            if (dateParser.scanISO8601DateIfMatch(false)) {
                calendar = dateParser.calendar;
            }
            dateParser.close();

            if (calendar != null) {
                return calendar.getTimeInMillis();
            }
        }

        throw new JSONException("can not cast to long, value : " + value);
    }

    public static final Integer castToInt(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;

            int scale = decimal.scale();
            if (scale >= -100 && scale <= 100) {
                return decimal.intValue();
            }

            return decimal.intValueExact();
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }

            return Integer.parseInt(strVal);
        }

        throw new JSONException("can not cast to int, value : " + value);
    }

    public static final byte[] castToBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }

        if (value instanceof String) {
            String string = (String) value;
            return JSONLexer.decodeFast(string, 0, string.length());
        }
        throw new JSONException("can not cast to int, value : " + value);
    }

    public static final Boolean castToBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValueExact() == 1;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }

            if ("true".equalsIgnoreCase(strVal) //
                    || "1".equals(strVal)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(strVal) //
                || "0".equals(strVal)) {
                return Boolean.FALSE;
            }
        }

        throw new JSONException("can not cast to int, value : " + value);
    }

    public static final <T> T castToJavaBean(Object obj, Class<T> clazz) {
        return cast(obj, clazz, ParserConfig.global);
    }

    public static final <T> T cast(Object obj, Class<T> clazz, ParserConfig mapping) {
        return cast(obj, clazz, mapping, 0);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final <T> T cast(Object obj, Class<T> clazz, ParserConfig mapping, int features) {
        if (obj == null) {
            return null;
        }

        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }

        if (clazz == obj.getClass()) {
            return (T) obj;
        }

        if (obj instanceof Map) {
            if (clazz == Map.class) {
                return (T) obj;
            }

            Map map = (Map) obj;
            if (clazz == Object.class && !map.containsKey(JSON.DEFAULT_TYPE_KEY)) {
                return (T) obj;
            }

            return castToJavaBean((Map<String, Object>) obj, clazz, mapping, features);
        }

        if (clazz.isArray()) {
            if (obj instanceof Collection) {

                Collection collection = (Collection) obj;
                int index = 0;
                Object array = Array.newInstance(clazz.getComponentType(), collection.size());
                for (Object item : collection) {
                    Object value = cast(item, clazz.getComponentType(), mapping);
                    Array.set(array, index, value);
                    index++;
                }

                return (T) array;
            }
            
            if (clazz == byte[].class) {
                return (T) castToBytes(obj);
            }
        }

        if (clazz.isAssignableFrom(obj.getClass())) {
            return (T) obj;
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            return (T) castToBoolean(obj);
        }

        if (clazz == byte.class || clazz == Byte.class) {
            return (T) castToByte(obj);
        }

         if (clazz == char.class
                 || clazz == Character.class) {
            if (obj instanceof String) {
                String strVal = (String) obj;
                if (strVal.length() == 1) {
                    return (T) Character.valueOf(strVal.charAt(0));
                }
            }
         }

        if (clazz == short.class || clazz == Short.class) {
            return (T) castToShort(obj);
        }

        if (clazz == int.class || clazz == Integer.class) {
            return (T) castToInt(obj);
        }

        if (clazz == long.class || clazz == Long.class) {
            return (T) castToLong(obj);
        }

        if (clazz == float.class || clazz == Float.class) {
            return (T) castToFloat(obj);
        }

        if (clazz == double.class || clazz == Double.class) {
            return (T) castToDouble(obj);
        }

        if (clazz == String.class) {
            return (T) castToString(obj);
        }

        if (clazz == BigDecimal.class) {
            return (T) castToBigDecimal(obj);
        }

        if (clazz == BigInteger.class) {
            return (T) castToBigInteger(obj);
        }

        if (clazz == Date.class) {
            return (T) castToDate(obj);
        }

        if (clazz.isEnum()) {
            return (T) castToEnum(obj, clazz, mapping);
        }

        if (Calendar.class.isAssignableFrom(clazz)) {
            Date date = castToDate(obj);
            Calendar calendar;
            if (clazz == Calendar.class) {
                calendar = Calendar.getInstance(JSON.defaultTimeZone, JSON.defaultLocale);
            } else {
                try {
                    calendar = (Calendar) clazz.newInstance();
                } catch (Exception e) {
                    throw new JSONException("can not cast to : " + clazz.getName(), e);
                }
            }
            calendar.setTime(date);
            return (T) calendar;
        }

        if (obj instanceof String) {
            String strVal = (String) obj;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }
            
            if (clazz == Currency.class) {
                return (T) Currency.getInstance(strVal);
            }
        }

        throw new JSONException("can not cast to : " + clazz.getName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final <T> T castToEnum(Object obj, Class<T> clazz, ParserConfig mapping) {
        try {
            if (obj instanceof String) {
                String name = (String) obj;
                if (name.length() == 0) {
                    return null;
                } else {
                    return (T) Enum.valueOf((Class<? extends Enum>) clazz, name);
                }
            } else if (obj instanceof Integer || obj instanceof Long) {
                int ordinal = ((Number) obj).intValue();
                Object[] values = clazz.getEnumConstants();
                if (ordinal < values.length) {
                    return (T) values[ordinal];
                }
            }
        } catch (Exception ex) {
            throw new JSONException("can not cast to : " + clazz.getName(), ex);
        }

        throw new JSONException("can not cast to : " + clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public static final <T> T cast(Object obj, Type type, ParserConfig mapping) {
        if (obj == null) {
            return null;
        }

        if (type instanceof Class) {
            return (T) cast(obj, (Class<T>) type, mapping, 0);
        }

        if (type instanceof ParameterizedType) {
            return (T) cast(obj, (ParameterizedType) type, mapping);
        }

        if (obj instanceof String) {
            String strVal = (String) obj;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }
        }

        if (type instanceof TypeVariable) {
            return (T) obj;
        }

        throw new JSONException("can not cast to : " + type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final <T> T cast(Object obj, ParameterizedType type, ParserConfig mapping) {
        Type rawTye = type.getRawType();

        if (rawTye == List.class //
                || rawTye == ArrayList.class) {
            Type itemType = type.getActualTypeArguments()[0];

            if (obj instanceof List) {
                List listObj = (List) obj;

                int listObjSize = listObj.size();
                ArrayList arrayList = new ArrayList(listObjSize);

                for (int i = 0; i < listObjSize; i++) {
                    Object item = listObj.get(i);

                    Object itemValue;
                    if (itemType instanceof Class) {
                        if (item != null && item.getClass() == JSONObject.class) {
                            itemValue = ((JSONObject) item).toJavaObject((Class<T>) itemType, mapping, 0);
                        } else {
                            itemValue = cast(item, (Class<T>) itemType, mapping, 0);
                        }
                    } else {
                        itemValue = cast(item, itemType, mapping);
                    }

                    arrayList.add(itemValue);
                }

                return (T) arrayList;
            }
        }

        if (rawTye == Set.class 
                || rawTye == HashSet.class //
                ||  rawTye == TreeSet.class //
                || rawTye == List.class //
                || rawTye == ArrayList.class) {
            Type itemType = type.getActualTypeArguments()[0];

            if (obj instanceof Iterable) {
                Collection collection; 
                if (rawTye == Set.class || rawTye == HashSet.class) {
                    collection = new HashSet();
                } else if (rawTye == TreeSet.class) {
                    collection = new TreeSet();
                } else {
                    collection = new ArrayList();    
                }
                
                for (Iterator it = ((Iterable) obj).iterator(); it.hasNext();) {
                    Object item = it.next();

                    Object itemValue;
                    if (itemType instanceof Class) {
                        if (item != null && item.getClass() == JSONObject.class) {
                            itemValue = ((JSONObject) item).toJavaObject((Class<T>) itemType, mapping, 0);
                        } else {
                            itemValue = cast(item, (Class<T>) itemType, mapping, 0);
                        }
                    } else {
                        itemValue = cast(item, itemType, mapping);
                    }

                    collection.add(itemValue);
                }

                return (T) collection;
            }
        }

        if (rawTye == Map.class || rawTye == HashMap.class) {
            Type keyType = type.getActualTypeArguments()[0];
            Type valueType = type.getActualTypeArguments()[1];

            if (obj instanceof Map) {
                Map map = new HashMap();

                for (Map.Entry entry : ((Map<?, ?>) obj).entrySet()) {
                    Object key = cast(entry.getKey(), keyType, mapping);
                    Object value = cast(entry.getValue(), valueType, mapping);

                    map.put(key, value);
                }

                return (T) map;
            }
        }

        if (obj instanceof String) {
            String strVal = (String) obj;
            if (strVal.length() == 0 //
                || "null".equals(strVal)) {
                return null;
            }
        }

        if (type.getActualTypeArguments().length == 1) {
            Type argType = type.getActualTypeArguments()[0];
            if (argType instanceof WildcardType) {
                return (T) cast(obj, rawTye, mapping);
            }
        }

        throw new JSONException("can not cast to : " + type);
    }

    public static final <T> T castToJavaBean(Map<String, Object> map, Class<T> clazz, ParserConfig config) {
        return castToJavaBean(map, clazz, config, 0);
    }

    @SuppressWarnings({ "unchecked" })
    public static final <T> T castToJavaBean(Map<String, Object> map, Class<T> clazz, ParserConfig config, int features) {
        try {
            if (clazz == StackTraceElement.class) {
                String declaringClass = (String) map.get("className");
                String methodName = (String) map.get("methodName");
                String fileName = (String) map.get("fileName");
                int lineNumber;
                {
                    Number value = (Number) map.get("lineNumber");
                    if (value == null) {
                        lineNumber = 0;
                    } else if (value instanceof BigDecimal) {
                        lineNumber = ((BigDecimal) value).intValueExact();
                    } else {
                        lineNumber = value.intValue();
                    }
                }

                return (T) new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
            }

            {
                Object iClassObject = map.get(JSON.DEFAULT_TYPE_KEY);
                if (iClassObject instanceof String) {
                    String className = (String) iClassObject;

                    if (config == null) {
                        config = ParserConfig.global;
                    }

                    Class<?> loadClazz = (Class<T>) config.checkAutoType(className, null, features);

                    if (loadClazz == null) {
                        throw new ClassNotFoundException(className + " not found");
                    }

                    if (!loadClazz.equals(clazz)) {
                        return (T) castToJavaBean(map, loadClazz, config, features);
                    }
                }
            }

            if (clazz.isInterface()) {
                JSONObject object;

                if (map instanceof JSONObject) {
                    object = (JSONObject) map;
                } else {
                    object = new JSONObject(map);
                }

                if (config == null) {
                    config = ParserConfig.getGlobalInstance();
                }

                ObjectDeserializer deserializer = config.getDeserializer(clazz);
                if (deserializer != null) {
                    String json = JSON.toJSONString(object);
                    return (T) JSON.parseObject(json, clazz);
                }

                return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                  new Class<?>[] { clazz }, object);
            }

            if (clazz == String.class && map instanceof JSONObject) {
                return (T) map.toString();
            }

            if (config == null) {
                config = ParserConfig.global;
            }

            JavaBeanDeserializer javaBeanDeser = null;
            ObjectDeserializer deserizer = config.getDeserializer(clazz);
            if (deserizer instanceof JavaBeanDeserializer) {
                javaBeanDeser = (JavaBeanDeserializer) deserizer;
            }
            
            if (javaBeanDeser == null) {
                throw new JSONException("can not get javaBeanDeserializer");
            }
            
            return (T) javaBeanDeser.createInstance(map, config);
        } catch (Exception e) {
            throw new JSONException(e.getMessage(), e);
        }
    }

    private final static ConcurrentMap<String, Class<?>> mappings = new ConcurrentHashMap<String, Class<?>>(36, 0.75f, 1);

    static {
        mappings.put("byte", byte.class);
        mappings.put("short", short.class);
        mappings.put("int", int.class);
        mappings.put("long", long.class);
        mappings.put("float", float.class);
        mappings.put("double", double.class);
        mappings.put("boolean", boolean.class);
        mappings.put("char", char.class);

        mappings.put("[byte", byte[].class);
        mappings.put("[short", short[].class);
        mappings.put("[int", int[].class);
        mappings.put("[long", long[].class);
        mappings.put("[float", float[].class);
        mappings.put("[double", double[].class);
        mappings.put("[boolean", boolean[].class);
        mappings.put("[char", char[].class);

        mappings.put("[B", byte[].class);
        mappings.put("[S", short[].class);
        mappings.put("[I", int[].class);
        mappings.put("[J", long[].class);
        mappings.put("[F", float[].class);
        mappings.put("[D", double[].class);
        mappings.put("[C", char[].class);
        mappings.put("[Z", boolean[].class);

        mappings.put("java.util.HashMap", HashMap.class);
        mappings.put("java.util.TreeMap", TreeMap.class);
        mappings.put("java.util.Date", Date.class);
        mappings.put("external.com.alibaba.fastjson.JSONObject", JSONObject.class);
        mappings.put("java.util.concurrent.ConcurrentHashMap", ConcurrentHashMap.class);
        mappings.put("java.text.SimpleDateFormat", SimpleDateFormat.class);
        mappings.put("java.lang.StackTraceElement", StackTraceElement.class);
        mappings.put("java.lang.RuntimeException", RuntimeException.class);
    }

    public static Class<?> getClassFromMapping(String className){
        return mappings.get(className);
    }

    public static Class<?> loadClass(String className, ClassLoader classLoader) {
        return loadClass(className, classLoader, false);
    }

    public static Class<?> loadClass(String className, ClassLoader classLoader, boolean cache) {
        if (className == null || className.length() == 0) {
            return null;
        }

        if (className.length() >= 256) {
            throw new JSONException("className too long. " + className);
        }
        
        Class<?> clazz = mappings.get(className);

        if (clazz != null) {
            return clazz;
        }

        if (className.charAt(0) == '[') {
            Class<?> componentType = loadClass(className.substring(1), classLoader, false);
            if (componentType == null) {
                return null;
            }
            return Array.newInstance(componentType, 0).getClass();
        }

        if (className.startsWith("L") && className.endsWith(";")) {
            String newClassName = className.substring(1, className.length() - 1);
            return loadClass(newClassName, classLoader, false);
        }
        
        try {
            if (classLoader != null) {
                clazz = classLoader.loadClass(className);

                if (cache) {
                    mappings.put(className, clazz);
                }

                return clazz;
            }
        } catch (Exception e) {
//            e.printStackTrace();
            // skip
        }

        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

            if (contextClassLoader != null && contextClassLoader != classLoader) {
                clazz = contextClassLoader.loadClass(className);

                if (cache) {
                    mappings.put(className, clazz);
                }

                return clazz;
            }
        } catch (Exception e) {
//            e.printStackTrace();
            // skip
        }

        try {
            clazz = Class.forName(className);

            mappings.put(className, clazz);

            return clazz;
        } catch (Exception e) {
//            e.printStackTrace();
            // skip
        }

        return clazz;
    }

    public static List<FieldInfo> computeGetters(Class<?> clazz, //
                                                 int modifiers, // class modifier, Class.getModifiers is very slow
                                                 boolean fieldOnly, //
                                                 JSONType jsonType, //
                                                 Map<String, String> aliasMap, //
                                                 boolean sorted,  //
                                                 boolean jsonFieldSupport, //
                                                 boolean fieldGenericSupport, //
                                                 PropertyNamingStrategy propertyNamingStrategy) {
        Map<String, FieldInfo> fieldInfoMap = new LinkedHashMap<String, FieldInfo>();
        Map<Class<?>, Field[]> classFieldCache = new HashMap<Class<?>, Field[]>();

        Field[] declaredFields = clazz.getDeclaredFields();
        if (!fieldOnly) {
            boolean kotlin = TypeUtils.isKotlin(clazz);

            List<Method> methodList = new ArrayList<Method>();

            for (Class cls = clazz; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
                Method[] declaredMethods = cls.getDeclaredMethods();
                for (Method method : declaredMethods) {
                    int modifier = method.getModifiers();

                    if ((modifier & Modifier.STATIC) != 0
                            || (modifier & Modifier.PRIVATE) != 0
                            || (modifier & Modifier.NATIVE) != 0
                            || (modifier & Modifier.PROTECTED) != 0) {
                        continue;
                    }

                    if (method.getReturnType().equals(Void.TYPE) //
                            || method.getParameterTypes().length != 0 //
                            || method.getReturnType() == ClassLoader.class //
                            || method.getDeclaringClass() == Object.class //
                            ) {
                        continue;
                    }

                    methodList.add(method);
                }
            }

            // for kotlin
            Constructor[] constructors = null;
            Annotation[][] paramAnnotationArrays = null;
            String[] paramNames = null;
            short[] paramNameMapping = null;

            for (Method method : methodList) {
                String methodName = method.getName();
                int ordinal = 0, serialzeFeatures = 0;

                if (methodName.equals("getMetaClass") //
                        && method.getReturnType().getName().equals("groovy.lang.MetaClass")) {
                    continue;
                }

                JSONField annotation = jsonFieldSupport ? method.getAnnotation(JSONField.class) : null;

                if (annotation == null && jsonFieldSupport) {
                    annotation = getSupperMethodAnnotation(clazz, method);
                }

                if (kotlin && isKotlinIgnore(clazz, methodName)) {
                    continue;
                }

                if (annotation == null && kotlin) {
                    if (constructors == null) {
                        constructors = clazz.getDeclaredConstructors();
                        if (constructors.length == 1) {
                            paramAnnotationArrays = constructors[0].getParameterAnnotations();
                            paramNames = TypeUtils.getKoltinConstructorParameters(clazz);

                            if (paramNames != null) {
                                String[] paramNames_sorted = new String[paramNames.length];
                                System.arraycopy(paramNames, 0,paramNames_sorted, 0, paramNames.length);
                                Arrays.sort(paramNames_sorted);

                                paramNameMapping = new short[paramNames.length];
                                for (short p = 0; p < paramNames.length; p++) {
                                    int index = Arrays.binarySearch(paramNames_sorted, paramNames[p]);
                                    paramNameMapping[index] = p;
                                }
                                paramNames = paramNames_sorted;
                            }

                        }
                    }
                    if (paramNames != null && paramNameMapping != null && methodName.startsWith("get")) {
                        String propertyName = decapitalize(methodName.substring(3));
                        int p = Arrays.binarySearch(paramNames, propertyName);
                        if (p < 0) {
                            for (int i = 0; i < paramNames.length; i++) {
                                if (propertyName.equalsIgnoreCase(paramNames[i])) {
                                    p = i;
                                    break;
                                }
                            }
                        }
                        if (p >= 0) {
                            short index = paramNameMapping[p];
                            Annotation[] paramAnnotations = paramAnnotationArrays[index];
                            if (paramAnnotations != null) {
                                for (Annotation paramAnnotation : paramAnnotations) {
                                    if (paramAnnotation instanceof JSONField) {
                                        annotation = (JSONField) paramAnnotation;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                boolean fieldAnnotationExists = false;
                if (annotation != null) {
                    if (!annotation.serialize()) {
                        continue;
                    }

                    ordinal = annotation.ordinal();
                    serialzeFeatures = SerializerFeature.of(annotation.serialzeFeatures());

                    if (annotation.name().length() != 0) {
                        String propertyName = annotation.name();
                        fieldAnnotationExists = true;

                        if (aliasMap != null) {
                            propertyName = aliasMap.get(propertyName);
                            if (propertyName == null) {
                                continue;
                            }
                        }

                        TypeUtils.setAccessible(clazz, method, modifiers);
                        fieldInfoMap.put(propertyName, new FieldInfo(propertyName, method, null, clazz, null, ordinal,
                                                                     serialzeFeatures, annotation, null, true));
                        continue;
                    }
                }

                if (methodName.startsWith("get")) {
                    if (methodName.length() < 4 //
                        || methodName.equals("getClass") //
                            ) {
                        continue;
                    }

                    char c3 = methodName.charAt(3);

                    String propertyName;
                    if (Character.isUpperCase(c3)) {
                        if (compatibleWithJavaBean) {
                            propertyName = decapitalize(methodName.substring(3));
                        } else {
                            propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                        }
                    } else if (c3 == '_') {
                        propertyName = methodName.substring(4);
                    } else if (c3 == 'f') {
                        propertyName = methodName.substring(3);
                    } else if (methodName.length() >= 5 && Character.isUpperCase(methodName.charAt(4))) {
                        propertyName = decapitalize(methodName.substring(3));
                    } else {
                        continue;
                    }

                    if (isJSONTypeIgnore(clazz, jsonType, propertyName)) {
                        continue;
                    }

                    Field field = getField(clazz, propertyName, declaredFields, classFieldCache);
                    JSONField fieldAnnotation = null;
                    if (field != null) {
                        fieldAnnotation = jsonFieldSupport ? field.getAnnotation(JSONField.class) : null;

                        if (fieldAnnotation != null) {
                            if (!fieldAnnotation.serialize()) {
                                continue;
                            }

                            ordinal = fieldAnnotation.ordinal();
                            serialzeFeatures = SerializerFeature.of(fieldAnnotation.serialzeFeatures());

                            if (fieldAnnotation.name().length() != 0) {
                                propertyName = fieldAnnotation.name();
                                fieldAnnotationExists = true;

                                if (aliasMap != null) {
                                    propertyName = aliasMap.get(propertyName);
                                    if (propertyName == null) {
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (propertyNamingStrategy != null && !fieldAnnotationExists) {
                        propertyName = propertyNamingStrategy.translate(propertyName);
                    }

                    if (aliasMap != null) {
                        propertyName = aliasMap.get(propertyName);
                        if (propertyName == null) {
                            continue;
                        }
                    }

                    TypeUtils.setAccessible(clazz, method, modifiers);
                    fieldInfoMap.put(propertyName,
                                     new FieldInfo(propertyName, method, field, clazz, null, ordinal, serialzeFeatures,
                                                   annotation, fieldAnnotation, fieldGenericSupport));
                }

                if (methodName.startsWith("is")) {
                    if (methodName.length() < 3) {
                        continue;
                    }

                    char c2 = methodName.charAt(2);

                    String propertyName;
                    if (Character.isUpperCase(c2)) {
                        if (compatibleWithJavaBean) {
                            propertyName = decapitalize(methodName.substring(2));
                        } else {
                            propertyName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                        }
                    } else if (c2 == '_') {
                        propertyName = methodName.substring(3);
                    } else if (c2 == 'f') {
                        propertyName = methodName.substring(2);
                    } else {
                        continue;
                    }

                    if (isJSONTypeIgnore(clazz, jsonType, propertyName)) {
                        continue;
                    }

                    Field field = getField(clazz, propertyName, declaredFields, classFieldCache);

                    if (field == null) {
                        field = getField(clazz, methodName, declaredFields, classFieldCache);
                    }

                    JSONField fieldAnnotation = null;
                    if (field != null) {
                        fieldAnnotation = jsonFieldSupport ? field.getAnnotation(JSONField.class) : null;

                        if (fieldAnnotation != null) {
                            if (!fieldAnnotation.serialize()) {
                                continue;
                            }

                            ordinal = fieldAnnotation.ordinal();
                            serialzeFeatures = SerializerFeature.of(fieldAnnotation.serialzeFeatures());

                            if (fieldAnnotation.name().length() != 0) {
                                propertyName = fieldAnnotation.name();

                                if (aliasMap != null) {
                                    propertyName = aliasMap.get(propertyName);
                                    if (propertyName == null) {
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (propertyNamingStrategy != null) {
                        propertyName = propertyNamingStrategy.translate(propertyName);
                    }

                    if (aliasMap != null) {
                        propertyName = aliasMap.get(propertyName);
                        if (propertyName == null) {
                            continue;
                        }
                    }

                    TypeUtils.setAccessible(clazz, field, modifiers);
                    TypeUtils.setAccessible(clazz, method, modifiers);
                    fieldInfoMap.put(propertyName,
                                     new FieldInfo(propertyName, method, field, clazz, null, ordinal, serialzeFeatures,
                                                   annotation, fieldAnnotation, fieldGenericSupport));
                }
            }
        }
        
        List<Field> classfields;
        {
            classfields = new ArrayList<Field>(declaredFields.length);
            for (Field f : declaredFields) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                if (f.getName().equals("this$0")) {
                    continue;
                }
                
                if ((f.getModifiers() & Modifier.PUBLIC) != 0) {
                    classfields.add(f);
                }
            }
            
            for (Class<?> c = clazz.getSuperclass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0) {
                        continue;
                    }
                    
                    if ((f.getModifiers() & Modifier.PUBLIC) != 0) {
                        classfields.add(f);
                    }
                }
            }
        }
        
        for (Field field : classfields) {
            JSONField fieldAnnotation = jsonFieldSupport ? field.getAnnotation(JSONField.class) : null;

            int ordinal = 0, serialzeFeatures = 0;
            String propertyName = field.getName();
            if (fieldAnnotation != null) {
                if (!fieldAnnotation.serialize()) {
                    continue;
                }

                ordinal = fieldAnnotation.ordinal();
                serialzeFeatures = SerializerFeature.of(fieldAnnotation.serialzeFeatures());
                
                if (fieldAnnotation.name().length() != 0) {
                    propertyName = fieldAnnotation.name();
                }
            }

            if (aliasMap != null) {
                propertyName = aliasMap.get(propertyName);
                if (propertyName == null) {
                    continue;
                }
            }
            
            if (propertyNamingStrategy != null) {
                propertyName = propertyNamingStrategy.translate(propertyName);
            }

            if (!fieldInfoMap.containsKey(propertyName)) {
                TypeUtils.setAccessible(clazz, field, modifiers);
                fieldInfoMap.put(propertyName, //
                                 new FieldInfo(propertyName, //
                                               null, //
                                               field, //
                                               clazz, //
                                               null, //
                                               ordinal, //
                                               serialzeFeatures, //
                                               null, //
                                               fieldAnnotation, //
                                               fieldGenericSupport));
            }
        }

        List<FieldInfo> fieldInfoList = new ArrayList<FieldInfo>();

        boolean containsAll = false;
        String[] orders = null;

        if (jsonType != null) {
            orders = jsonType.orders();

            if (orders != null && orders.length == fieldInfoMap.size()) {
                containsAll = true;
                for (String item : orders) {
                    if (!fieldInfoMap.containsKey(item)) {
                        containsAll = false;
                        break;
                    }
                }
            } else {
                containsAll = false;
            }
        }

        if (containsAll) {
            for (String item : orders) {
                FieldInfo fieldInfo = fieldInfoMap.get(item);
                fieldInfoList.add(fieldInfo);
            }
        } else {
            for (FieldInfo fieldInfo : fieldInfoMap.values()) {
                fieldInfoList.add(fieldInfo);
            }

            if (sorted) {
                Collections.sort(fieldInfoList);
            }
        }

        return fieldInfoList;
    }

    public static JSONField getSupperMethodAnnotation(Class<?> clazz, Method method) {
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            for (Method interfaceMethod : interfaceClass.getMethods()) {
                if (!interfaceMethod.getName().equals(method.getName())) {
                    continue;
                }

                Class<?>[] interfaceParameterTypes = interfaceMethod.getParameterTypes();
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (interfaceParameterTypes.length != parameterTypes.length) {
                    continue;
                }

                boolean match = true;
                for (int i = 0; i < interfaceParameterTypes.length; ++i) {
                    if (!interfaceParameterTypes[i].equals(parameterTypes[i])) {
                        match = false;
                        break;
                    }
                }

                if (!match) {
                    continue;
                }

                JSONField annotation = interfaceMethod.getAnnotation(JSONField.class);
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass == null) {
            return null;
        }

        if (Modifier.isAbstract(superClass.getModifiers())) {
            Class<?>[] types = method.getParameterTypes();

            for (Method interfaceMethod : superClass.getMethods()) {
                Class<?>[] interfaceTypes = interfaceMethod.getParameterTypes();
                if (interfaceTypes.length != types.length) {
                    continue;
                }
                if (!interfaceMethod.getName().equals(method.getName())) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < types.length; ++i) {
                    if (!interfaceTypes[i].equals(types[i])) {
                        match = false;
                        break;
                    }
                }

                if (!match) {
                    continue;
                }

                JSONField annotation = interfaceMethod.getAnnotation(JSONField.class);
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        return null;
    }

    private static boolean isJSONTypeIgnore(Class<?> clazz, JSONType jsonType, String propertyName) {
        if (jsonType != null && jsonType.ignores() != null) {
            for (String item : jsonType.ignores()) {
                if (propertyName.equalsIgnoreCase(item)) {
                    return true;
                }
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        return superClass != Object.class //
               && superClass != null //
               && isJSONTypeIgnore(superClass, //
                                   superClass.getAnnotation(JSONType.class), //
                                   propertyName);
    }
    
    public static boolean isGenericParamType(Type type) {
        if (type instanceof ParameterizedType) {
            return true;
        }
        
        if (type instanceof Class) {
            Type superType = ((Class<?>) type).getGenericSuperclass();
            return superType != Object.class //
                    && isGenericParamType(superType);
        }
        
        return false;
    }
    
    public static Type getGenericParamType(Type type) {
        return type instanceof Class //
            ? getGenericParamType(((Class<?>) type).getGenericSuperclass()) //
            : type;
    }
    
    public static Class<?> getClass(Type type) {
        if (type.getClass() == Class.class) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        }
        
        if (type instanceof TypeVariable) {
            return (Class<?>) ((TypeVariable<?>) type).getBounds()[0];
        }

        if(type instanceof WildcardType){
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length == 1) {
                return getClass(upperBounds[0]);
            }
        }

        return Object.class;
    }
    
    public static String decapitalize(String name) {
        if (name == null //
            || name.length() == 0 //
            || (name.length() > 1 && Character.isUpperCase(name.charAt(1)) //
                && Character.isUpperCase(name.charAt(0))) //
        ) {
            return name;
        }
        
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
    
    public static boolean setAccessible(Class<?> clazz, Member member, int classMofifiers) {
        if (member == null //
            || !setAccessibleEnable //
        ) {
            return false;
        }
        
        Class<?> supperClass = clazz.getSuperclass();
        
        if ((supperClass == null || supperClass == Object.class) //
            && (member.getModifiers() & Modifier.PUBLIC) != 0 //
            && (classMofifiers & Modifier.PUBLIC) != 0 //
        ) {
            return false;
        }
        
        AccessibleObject obj = (AccessibleObject) member;
        
        try {
            obj.setAccessible(true);
            return true;
        } catch (AccessControlException error) {
            setAccessibleEnable = false;
            return false;
        }
    }

    public static Field getField(Class<?> clazz, String fieldName, Field[] declaredFields) {
        return getField(clazz, fieldName, declaredFields, null);
    }
    
    public static Field getField(Class<?> clazz, String fieldName, Field[] declaredFields, Map<Class<?>, Field[]> classFieldCache) {
        Field field = getField0(clazz, fieldName, declaredFields, classFieldCache);
        if (field == null) {
            field = getField0(clazz, "_" + fieldName, declaredFields, classFieldCache);
        }
        
        if (field == null) {
            field = getField0(clazz, "m_" + fieldName, declaredFields, classFieldCache);
        }
        
        if (field == null) {
            String mName = "m" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            field = getField0(clazz, mName, declaredFields, classFieldCache);
        }
        
        return field;
    }

    private static Field getField0(Class<?> clazz, String fieldName, Field[] declaredFields, Map<Class<?>, Field[]> classFieldCache) {
        for (Field item : declaredFields) {
            String itemName = item.getName();
            if (fieldName.equals(itemName)) {
                return item;
            }

            char c0, c1;
            if (fieldName.length() > 2
                    && (c0 = fieldName.charAt(0)) >= 'a' && c0 <= 'z'
                    && (c1 = fieldName.charAt(1)) >= 'A' && c1 <= 'Z'
                    && fieldName.equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        
        Class<?> superClass = clazz.getSuperclass();

        if (superClass == null || superClass == Object.class) {
            return  null;
        }

        Field[] superClassFields = classFieldCache != null ? classFieldCache.get(superClass) : null;
        if (superClassFields == null) {
            superClassFields = superClass.getDeclaredFields();
            if (classFieldCache != null) {
                classFieldCache.put(superClass, superClassFields);
            }
        }

        return getField(superClass, fieldName, superClassFields, classFieldCache);
    }

    public static Type getCollectionItemType(Type fieldType) {
        Type itemType = null;
        Class<?> clazz = null;
        if (fieldType instanceof ParameterizedType) {
            Type actualTypeArgument = ((ParameterizedType) fieldType).getActualTypeArguments()[0];

            if (actualTypeArgument instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) actualTypeArgument;
                Type[] upperBounds = wildcardType.getUpperBounds();
                if (upperBounds.length == 1) {
                    actualTypeArgument = upperBounds[0];
                }
            }

            itemType = actualTypeArgument;
        } else if (fieldType instanceof Class<?> //
                && !(clazz = (Class<?>) fieldType).getName().startsWith("java.")) {
            Type superClass = clazz.getGenericSuperclass();
            itemType = TypeUtils.getCollectionItemType(superClass);
        }

        if (itemType == null) {
            itemType = Object.class;
        }

        return itemType;
    }

    public static Object defaultValue(Class<?> fieldType) {
        if (fieldType == byte.class) {
            return (byte) 0;
        } else if (fieldType == short.class) {
            return  (short) 0;
        } else if (fieldType == int.class) {
            return  0;
        } else if (fieldType == long.class) {
            return  0L;
        } else if (fieldType == float.class) {
            return  0F;
        } else if (fieldType == double.class) {
            return  0D;
        } else if (fieldType == boolean.class) {
            return Boolean.FALSE;
        } else if (fieldType == char.class) {
            return  '0';
        }
        return null;
    }

    public static boolean getArgument(Type[] typeArgs, TypeVariable[] typeVariables, Type[] arguments) {
        if (arguments == null || typeVariables.length == 0) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < typeArgs.length; ++i) {
            Type typeArg = typeArgs[i];
            if (typeArg instanceof ParameterizedType) {
                ParameterizedType p_typeArg = (ParameterizedType) typeArg;
                Type[] p_typeArg_args = p_typeArg.getActualTypeArguments();
                boolean p_changed = getArgument(p_typeArg_args, typeVariables, arguments);
                if (p_changed) {
                    typeArgs[i] = new ParameterizedTypeImpl(p_typeArg_args, p_typeArg.getOwnerType(), p_typeArg.getRawType());
                    changed = true;
                }
            } else if (typeArg instanceof TypeVariable) {
                for (int j = 0; j < typeVariables.length; ++j) {
                    if (typeArg.equals(typeVariables[j])) {
                        typeArgs[i] = arguments[j];
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    public static double parseDouble(String str) {
        final int len = str.length();
        if (len > 10) {
            return Double.parseDouble(str);
        }

        boolean negative = false;

        long longValue = 0;
        int scale = 0;
        for (int i = 0; i < len; ++i) {
            char ch = str.charAt(i);
            if (ch == '-' && i == 0) {
                negative = true;
                continue;
            }

            if (ch == '.') {
                if (scale != 0) {
                    return Double.parseDouble(str);
                }
                scale = len - i - 1;
                continue;
            }

            if (ch >= '0' && ch <= '9') {
                int digit = ch - '0';
                longValue = longValue * 10 + digit;
            } else {
                return Double.parseDouble(str);
            }
        }

        if (negative) {
            longValue = -longValue;
        }

        switch (scale) {
            case 0:
                return (double) longValue;
            case 1:
                return ((double) longValue) / 10;
            case 2:
                return ((double) longValue) / 100;
            case 3:
                return ((double) longValue) / 1000;
            case 4:
                return ((double) longValue) / 10000;
            case 5:
                return ((double) longValue) / 100000;
            case 6:
                return ((double) longValue) / 1000000;
            case 7:
                return ((double) longValue) / 10000000;
            case 8:
                return ((double) longValue) / 100000000;
            case 9:
                return ((double) longValue) / 1000000000;
        }

        return Double.parseDouble(str);
    }

    public static float parseFloat(String str) {
        final int len = str.length();
        if (len >= 10) {
            return Float.parseFloat(str);
        }

        boolean negative = false;

        long longValue = 0;
        int scale = 0;
        for (int i = 0; i < len; ++i) {
            char ch = str.charAt(i);
            if (ch == '-' && i == 0) {
                negative = true;
                continue;
            }

            if (ch == '.') {
                if (scale != 0) {
                    return Float.parseFloat(str);
                }
                scale = len - i - 1;
                continue;
            }

            if (ch >= '0' && ch <= '9') {
                int digit = ch - '0';
                longValue = longValue * 10 + digit;
            } else {
                return Float.parseFloat(str);
            }
        }

        if (negative) {
            longValue = -longValue;
        }

        switch (scale) {
            case 0:
                return (float) longValue;
            case 1:
                return ((float) longValue) / 10;
            case 2:
                return ((float) longValue) / 100;
            case 3:
                return ((float) longValue) / 1000;
            case 4:
                return ((float) longValue) / 10000;
            case 5:
                return ((float) longValue) / 100000;
            case 6:
                return ((float) longValue) / 1000000;
            case 7:
                return ((float) longValue) / 10000000;
            case 8:
                return ((float) longValue) / 100000000;
            case 9:
                return ((float) longValue) / 1000000000;
        }

        return Float.parseFloat(str);
    }

    public static long fnv_64_lower(String key) {
        if (key == null) {
            return 0L;
        }

        long hashCode = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); ++i) {
            char ch = key.charAt(i);
            if (ch == '_' || ch == '-') {
                continue;
            }

            if (ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch + 32);
            }

            hashCode ^= ch;
            hashCode *= 0x100000001b3L;
        }

        return hashCode;
    }

    public static void addMapping(String className, Class<?> clazz) {
        mappings.put(className, clazz);
    }

    public static Type checkPrimitiveArray(GenericArrayType genericArrayType) {
        Type clz = genericArrayType;
        Type genericComponentType  = genericArrayType.getGenericComponentType();

        String prefix = "[";
        while (genericComponentType instanceof GenericArrayType) {
            genericComponentType = ((GenericArrayType) genericComponentType)
                    .getGenericComponentType();
            prefix += prefix;
        }

        if (genericComponentType instanceof Class<?>) {
            Class<?> ck = (Class<?>) genericComponentType;
            if (ck.isPrimitive()) {
                try {
                    if (ck == boolean.class) {
                        clz = Class.forName(prefix + "Z");
                    } else if (ck == char.class) {
                        clz = Class.forName(prefix + "C");
                    } else if (ck == byte.class) {
                        clz = Class.forName(prefix + "B");
                    } else if (ck == short.class) {
                        clz = Class.forName(prefix + "S");
                    } else if (ck == int.class) {
                        clz = Class.forName(prefix + "I");
                    } else if (ck == long.class) {
                        clz = Class.forName(prefix + "J");
                    } else if (ck == float.class) {
                        clz = Class.forName(prefix + "F");
                    } else if (ck == double.class) {
                        clz = Class.forName(prefix + "D");
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }

        return clz;
    }

//    public static long fnv_hash(char[] chars) {
//        long hash = 0xcbf29ce484222325L;
//        for (int i = 0; i < chars.length; ++i) {
//            char c = chars[i];
//            hash ^= c;
//            hash *= 0x100000001b3L;
//        }
//        return hash;
//    }
}
