package ing.boykiss.gearworks.common.registry.key;

import org.jspecify.annotations.NonNull;

public record KeyLocation(Key registry, Key key) {
    @Override
    public @NonNull String toString() {
        return registry.toString() + "=" + key.toString();
    }
}
