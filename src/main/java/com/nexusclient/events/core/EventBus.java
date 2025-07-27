package com.nexusclient.events.core;

import com.nexusclient.events.world.seed.SeedChangedEvent;
import com.nexusclient.events.world.seed.SeedChangedListener;

import java.util.ArrayList;
import java.util.List;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final List<Object> listeners = new ArrayList<>();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void post(Object event) {
        for (Object listener : listeners) {
            if (event instanceof SeedChangedEvent && listener instanceof SeedChangedListener) {
                ((SeedChangedListener) listener).onSeedChanged((SeedChangedEvent) event);
            }
        }
    }

    public void subscribe(Object listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Object listener) {
        listeners.remove(listener);
    }
}

