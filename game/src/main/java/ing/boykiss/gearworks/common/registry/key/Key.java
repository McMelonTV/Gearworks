package ing.boykiss.gearworks.common.registry.key;

import org.jspecify.annotations.NonNull;

public record Key(String namespace, String path) {
    @Override
    public @NonNull String toString() {
        return namespace + ":" + path;
    }
}
