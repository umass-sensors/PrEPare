package edu.umass.cs.shared;

import android.content.Intent;

public final class DataLayerUtil {
    public static class Serializer<T extends Enum<T>> extends Deserializer<T> {
        private final T victim;
        @SuppressWarnings("unchecked")
        public Serializer(T victim) {
            super(victim.getDeclaringClass());
            this.victim = victim;
        }
        public void to(Intent intent) {
            intent.putExtra(name, victim.ordinal());
        }
    }
    public static class Deserializer<T extends Enum<T>> {
        final Class<T> victimType;
        final String name;
        public Deserializer(Class<T> victimType) {
            this.victimType = victimType;
            this.name = victimType.getName();
        }
        public T from(Intent intent) {
            if (!intent.hasExtra(name)) throw new IllegalStateException();
            return victimType.getEnumConstants()[intent.getIntExtra(name, -1)];
        }
    }
    public static <T extends Enum<T>> Deserializer<T> deserialize(Class<T> victim) {
        return new Deserializer<>(victim);
    }
    public static <T extends Enum<T>> Serializer<T> serialize(T victim) {
        return new Serializer<>(victim);
    }
}