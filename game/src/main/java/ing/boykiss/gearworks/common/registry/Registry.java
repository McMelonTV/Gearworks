package ing.boykiss.gearworks.common.registry;

import ing.boykiss.gearworks.common.registry.key.Key;
import ing.boykiss.gearworks.common.registry.key.Keyed;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface Registry<T> extends Keyed {
    int size();

    @Nullable T register(@NonNull Key key, @NonNull T object);

    @Nullable T get(@NonNull Key key);

    @Nullable Key getKey(@NonNull T object);

    boolean hasKey(@NonNull Key key);

    boolean hasObject(@NonNull T object);

    Collection<@NonNull Key> keys();

    Collection<@NonNull T> objects();
}
