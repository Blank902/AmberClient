package com.nexusclient.events.world.seed;

public class SeedChangedEvent {
    private static final SeedChangedEvent INSTANCE = new SeedChangedEvent();

    public Long seed;

    public static SeedChangedEvent get(long seed) {
        INSTANCE.seed = seed;
        return INSTANCE;
    }
}
