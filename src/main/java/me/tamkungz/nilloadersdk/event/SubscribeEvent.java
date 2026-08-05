package me.tamkungz.nilloadersdk.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an event listener method for automatic registration via EventBus.register(Object).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    EventPriority priority() default EventPriority.NORMAL;

    boolean receiveCancelled() default false;
}

