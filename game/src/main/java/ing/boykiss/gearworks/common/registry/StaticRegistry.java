package ing.boykiss.gearworks.common.registry;

import ing.boykiss.gearworks.common.registry.key.Key;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;

// TODO: add exceptions
public class StaticRegistry<T> implements Registry<T> {
    protected final Key registryKey;
    protected final HashMap<Key, T> dataMap = new HashMap<>();

    public StaticRegistry(Key key) {
        this.registryKey = key;
    }

    @Override
    public int size() {
        return dataMap.size();
    }

    @Override
    public @Nullable T register(@NonNull Key key, @NonNull T object) {
        return dataMap.putIfAbsent(key, object);
    }

    @Override
    public @Nullable T get(@NonNull Key key) {
        return dataMap.get(key);
    }

    @Override
    public @Nullable Key getKey(@NonNull T object) {
        return null;
    }

    @Override
    public boolean hasKey(@NonNull Key key) {
        return dataMap.containsKey(key);
    }

    @Override
    public boolean hasObject(@NonNull T object) {
        return dataMap.containsValue(object);
    }

    @Override
    public Collection<@NonNull Key> keys() {
        return dataMap.keySet();
    }

    @Override
    public Collection<T> objects() {
        return dataMap.values();
    }

    @Override
    public @NonNull Key key() {
        return registryKey;
    }
}
