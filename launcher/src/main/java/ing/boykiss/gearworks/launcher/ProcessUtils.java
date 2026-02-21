package ing.boykiss.gearworks.launcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for spawning game/server processes.
 * <p>
 * The launcher JAR doubles as the game launcher: when started with
 * {@code --knot-launch}, LauncherMain bypasses the UI and directly
 * calls KnotClient (or KnotServer). ProcessUtils builds the command
 * that triggers this mode with the correct game JAR path.
 */
public class ProcessUtils {
    /**
     * Flag that, when present, causes LauncherMain to skip the UI and hand off to Knot.
     */
    public static final String KNOT_LAUNCH_FLAG = "--knot-launch";

    private ProcessUtils() {
    }

    /**
     * Spawns the client game process.
     * Blocks until the game exits if {@code wait} is true.
     *
     * @param gameJar   Absolute path to the game JAR (Gearworks-x.x.x.jar)
     * @param extraArgs Additional arguments forwarded to KnotClient
     */
    public static Process launchClient(Path gameJar, String... extraArgs) throws IOException {
        Path launcherJar = currentJar();
        String java = currentJavaBin();

        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        cmd.add("--enable-native-access=ALL-UNNAMED");
        cmd.add("-Dfabric.gameJarPath=" + gameJar.toAbsolutePath());
        cmd.add("-jar");
        cmd.add(launcherJar.toAbsolutePath().toString());
        cmd.add(KNOT_LAUNCH_FLAG);
        cmd.addAll(List.of(extraArgs));

        return new ProcessBuilder(cmd)
                .inheritIO()
                .start();
    }

    /**
     * Spawns the server game process.
     *
     * @param serverLauncherJar Absolute path to the server launcher JAR
     * @param serverGameJar     Absolute path to the server game JAR
     * @param extraArgs         Additional arguments forwarded to KnotServer
     */
    public static Process launchServer(Path serverLauncherJar, Path serverGameJar, String... extraArgs)
            throws IOException {
        String java = currentJavaBin();

        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        cmd.add("--enable-native-access=ALL-UNNAMED");
        cmd.add("-Dfabric.gameJarPath=" + serverGameJar.toAbsolutePath());
        cmd.add("-jar");
        cmd.add(serverLauncherJar.toAbsolutePath().toString());
        cmd.add(KNOT_LAUNCH_FLAG);
        cmd.addAll(List.of(extraArgs));

        return new ProcessBuilder(cmd)
                .inheritIO()
                .start();
    }

    /**
     * Returns the Path of the JAR this class was loaded from.
     */
    public static Path currentJar() {
        try {
            return Path.of(ProcessUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not determine launcher JAR path", e);
        }
    }

    /**
     * Returns the path to the java binary of the running JVM.
     */
    private static String currentJavaBin() {
        return ProcessHandle.current().info().command()
                .orElseThrow(() -> new RuntimeException("Could not determine java binary path"));
    }
}
