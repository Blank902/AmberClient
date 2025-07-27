package com.nexusclient.events.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods as event listeners.
 * Methods annotated with @EventListener will be automatically registered
 * when the containing object is registered with the EventManager.
 *
 * This annotation is used to identify methods that should handle specific events.
 * The method signature determines which event type it will handle.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    /**
     * Priority of the event listener (optional).
     * Higher values = higher priority (executed first).
     * Default is 0.
     */
    int priority() default 0;

    /**
     * Whether this listener should receive cancelled events.
     * If false, the listener will not be called for cancelled events.
     * Default is false.
     */
    boolean receiveCancelled() default false;
}