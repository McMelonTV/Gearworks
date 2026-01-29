package ing.boykiss.gearworks.common.registry.key;

import org.jspecify.annotations.NonNull;

public interface Keyed {
    @NonNull Key key();
}
