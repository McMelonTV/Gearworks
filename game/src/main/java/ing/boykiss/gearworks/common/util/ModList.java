package ing.boykiss.gearworks.common.util;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModList {

    @Getter
    private final Collection<ModContainer> allMods = FabricLoader.getInstance().getAllMods();
    @Getter
    private final List<ModContainer> builtins = new ArrayList<>();
    @Getter
    private final List<ModContainer> libraries = new ArrayList<>();
    @Getter
    private final List<ModContainer> mods = new ArrayList<>();

    public ModList() {
        this(Sorter.ALPHABETICAL);
    }

    public ModList(Sorter sorter) {
        for (ModContainer mod : allMods) {
            if (mod.getMetadata().getType().equals("builtin")) {
                builtins.add(mod);
            } else if (mod.getContainingMod().isPresent()) {
                libraries.add(mod);
            } else {
                mods.add(mod);
            }
        }
        sort(sorter);
    }

    private static void topoVisit(ModContainer mod,
                                  Map<String, ModContainer> modMap,
                                  Set<String> visited,
                                  Set<String> visiting,
                                  List<ModContainer> result) {
        String id = mod.getMetadata().getId();
        if (visited.contains(id) || visiting.contains(id)) return;

        visiting.add(id);

        for (ModDependency dep : mod.getMetadata().getDependencies()) {
            if (dep.getKind() == ModDependency.Kind.DEPENDS) {
                ModContainer depMod = modMap.get(dep.getModId());
                if (depMod != null) {
                    topoVisit(depMod, modMap, visited, visiting, result);
                }
            }
        }

        visiting.remove(id);
        visited.add(id);
        result.add(mod);
    }

    public void sort(Sorter sorter) {
        Comparator<ModContainer> comparator = switch (sorter) {
            case ALPHABETICAL -> Comparator.comparing(m -> m.getMetadata().getName());
            case ALPHABETICAL_REVERSED ->
                    Comparator.comparing((ModContainer m) -> m.getMetadata().getName()).reversed();
            case LOAD_ORDER -> buildLoadOrderComparator();
        };
        mods.sort(comparator);
        libraries.sort(comparator);
        builtins.sort(comparator);
    }

    private Comparator<ModContainer> buildLoadOrderComparator() {
        Map<String, ModContainer> modMap = new HashMap<>();
        for (ModContainer mod : allMods) {
            modMap.put(mod.getMetadata().getId(), mod);
        }

        List<ModContainer> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (ModContainer mod : allMods) {
            topoVisit(mod, modMap, visited, visiting, ordered);
        }

        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            indexMap.put(ordered.get(i).getMetadata().getId(), i);
        }

        return Comparator.comparingInt(m -> indexMap.getOrDefault(m.getMetadata().getId(), Integer.MAX_VALUE));
    }

    public enum Sorter {
        ALPHABETICAL,
        ALPHABETICAL_REVERSED,
        LOAD_ORDER
    }
}
