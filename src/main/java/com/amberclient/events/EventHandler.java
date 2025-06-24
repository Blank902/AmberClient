package com.amberclient.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods as event handlers.
 * Methods annotated with @EventHandler will be automatically registered
 * when the containing object is registered with the EventManager.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    /**
     * Priority of the event handler (optional).
     * Higher values = higher priority (executed first).
     */
    int priority() default 0;
}