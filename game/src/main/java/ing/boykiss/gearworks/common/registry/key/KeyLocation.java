package ing.boykiss.gearworks.common.registry.key;

import org.jspecify.annotations.NonNull;

public record KeyLocation(@NonNull Key registry, @NonNull Key key) {
    @Override
    public @NonNull String toString() {
        // TODO: change the joiner to some other character
        return registry + "=" + key;
    }
}
