package ing.boykiss.gearworks.common.registry;

import ing.boykiss.gearworks.common.registry.key.Key;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

// TODO: add exceptions
public class DynamicRegistry<T> extends StaticRegistry<T> {
    public DynamicRegistry(Key key) {
        super(key);
    }

    public @Nullable T override(@NonNull Key key, @NonNull T object) {
        return dataMap.put(key, object);
    }

    public @Nullable T removeKey(@NonNull Key key) {
        return dataMap.remove(key);
    }

    public @Nullable T removeObject(@NonNull T object) {
        // TODO: implement
        return null;
    }
}
