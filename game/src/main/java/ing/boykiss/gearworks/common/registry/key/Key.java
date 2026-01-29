package ing.boykiss.gearworks.common.registry.key;

import org.intellij.lang.annotations.Pattern;
import org.jspecify.annotations.NonNull;

// TODO: make sure the regexes are correct and document them
public record Key(@NonNull @Pattern("^[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?$") String namespace,
                  @NonNull @Pattern("^[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?(/[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?)*$") String path) {
    @Override
    public @NonNull String toString() {
        return namespace + ":" + path;
    }
}
