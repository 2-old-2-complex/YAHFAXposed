package external.com.alibaba.fastjson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import external.com.alibaba.fastjson.PropertyNamingStrategy;
import external.com.alibaba.fastjson.parser.Feature;
import external.com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface JSONType {

    boolean asm() default true;

    String[] orders() default {};
    
    String[] ignores() default {};

    SerializerFeature[] serialzeFeatures() default {};
    Feature[] parseFeatures() default {};
    
    boolean alphabetic() default true;
    
    Class<?> mappingTo() default Void.class;
    
    /**
     * @since 1.2.11 backport to 1.1.52.android
     */
    String typeName() default "";

    String typeKey() default "";
    
    /**
     * @since 1.2.11 backport to 1.1.52.android
     */
    Class<?>[] seeAlso() default{};

    /**
     * 
     * @return
     */
    PropertyNamingStrategy naming() default PropertyNamingStrategy.CamelCase;
}
