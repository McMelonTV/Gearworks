package ing.boykiss.gearworks.common.registry;

import ing.boykiss.gearworks.common.registry.key.Key;
import ing.boykiss.gearworks.common.registry.key.Keyed;

import java.util.Collection;

public interface Registry<T> extends Keyed {
    int size();

    T register(Key id, T object);

    T get(Key id);

    Key getId(T object);

    boolean has(Key id);

    Collection<Key> keys();

    Collection<T> values();
}
