package io.bdrc.iiif.presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JerseyCacheControl {

    
    /**
     * no-cache cache control.
    */
    boolean noCache() default false;

    /**
     * max-age cache control.
     *
    */
    int maxAge() default 172800;
    
}