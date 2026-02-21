package ing.boykiss.gearworks.server;

import ing.boykiss.gearworks.common.Greeting;
import ing.boykiss.gearworks.common.util.ModList;
import ing.boykiss.gearworks.server.engine.GameEngine;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerMain {
    private static GameEngine gameEngine;

    static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> System.out.println(Greeting.getGoodbye())));

        System.out.println(Greeting.getGreeting());

        FabricLoader loader = FabricLoader.getInstance();
        System.out.println("Running " + loader.getEnvironmentType().name() + " v" + loader.getRawGameVersion() + " " + (loader.isDevelopmentEnvironment() ? "(Dev)" : "(Release)"));

        ModList modList = new ModList();

        System.out.println(modList.getAllMods().size() + " loaded mods.");

        Map<String, List<ModContainer>> categories = new LinkedHashMap<>();
        categories.put("Mods", modList.getMods());
        categories.put("Libraries", modList.getLibraries());
        categories.put("Core", modList.getBuiltins());

        categories.forEach((label, list) -> {
            if (list.isEmpty()) return;
            System.out.println(label + " (" + list.size() + ")" + ":");
            list.forEach(mod -> {
                ModMetadata meta = mod.getMetadata();
                System.out.println("\t" + meta.getName() + " v" + meta.getVersion() + " (" + meta.getId() + ")");
            });
        });

        gameEngine = new GameEngine();
        gameEngine.start();

        while (true) {
            // NOOP to prevent shutting down this thread, might not be the proper way to handle this but whatever
        }
    }
}
