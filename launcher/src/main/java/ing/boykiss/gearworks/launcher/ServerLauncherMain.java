package ing.boykiss.gearworks.launcher;

import ing.boykiss.gearworks.launcher.download.VersionDownloader;
import ing.boykiss.gearworks.launcher.manifest.ManifestFetcher;
import ing.boykiss.gearworks.launcher.manifest.VersionManifest;
import net.fabricmc.loader.impl.launch.knot.KnotServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for GearworksServerLauncher.jar.
 * <p>
 * Usage:
 * java -jar GearworksServerLauncher.jar [options] [-- <knot args>]
 * <p>
 * Options:
 * --version <id>       Version to launch (default: latest release from manifest)
 * --manifest <url>     Override the manifest URL
 * --versions-dir <dir> Directory for downloaded versions (default: ~/.gearworks/versions)
 * <p>
 * Two modes (same as client):
 * - Normal:       Resolves/downloads the server JAR, then spawns the game process.
 * - Knot launch:  {@code --knot-launch} flag — skips download and hands off to KnotServer.
 */
public class ServerLauncherMain {
    private static final Path DEFAULT_VERSIONS_DIR =
            Path.of(System.getProperty("user.home"), ".gearworks", "versions");

    public static void main(String[] args) throws Exception {
        if (hasFlag(args, ProcessUtils.KNOT_LAUNCH_FLAG)) {
            String[] knotArgs = Arrays.stream(args)
                    .filter(a -> !a.equals(ProcessUtils.KNOT_LAUNCH_FLAG))
                    .toArray(String[]::new);
            KnotServer.main(knotArgs);
            return;
        }

        // ---- Parse CLI options --------------------------------------------
        String versionId = null;
        String manifestUrl = LauncherApp.MANIFEST_URL;
        Path versionsDir = DEFAULT_VERSIONS_DIR;
        List<String> extraArgs = new ArrayList<>();
        boolean pastSeparator = false;

        for (int i = 0; i < args.length; i++) {
            if (pastSeparator) {
                extraArgs.add(args[i]);
                continue;
            }
            switch (args[i]) {
                case "--" -> pastSeparator = true;
                case "--version" -> versionId = args[++i];
                case "--manifest" -> manifestUrl = args[++i];
                case "--versions-dir" -> versionsDir = Path.of(args[++i]);
                default -> extraArgs.add(args[i]);
            }
        }

        // ---- Resolve version ----------------------------------------------
        System.out.println("[ServerLauncher] Fetching manifest from " + manifestUrl);
        ManifestFetcher fetcher = new ManifestFetcher(manifestUrl);
        VersionManifest manifest = fetcher.fetch();

        VersionManifest.Version version;
        if (versionId != null) {
            final String id = versionId;
            version = manifest.getVersions().stream()
                    .filter(v -> v.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Version not found in manifest: " + id));
        } else {
            String latestId = manifest.getLatest().getRelease();
            version = manifest.getVersions().stream()
                    .filter(v -> v.getId().equals(latestId))
                    .findFirst()
                    .orElse(manifest.getVersions().get(0));
            System.out.println("[ServerLauncher] No --version specified, using latest: " + version.getId());
        }

        // ---- Download if needed -------------------------------------------
        System.out.println("[ServerLauncher] Resolving server JAR for " + version.getId() + "...");
        VersionDownloader downloader = new VersionDownloader(versionsDir);
        Path serverGameJar = downloader.resolveServerJar(version, progress ->
                System.out.printf("\r[ServerLauncher] Downloading... %.0f%%", progress * 100));
        System.out.println();

        // ---- Spawn game process -------------------------------------------
        // Re-invoke this same JAR with --knot-launch so KnotServer runs with
        // the correct class path and game JAR.
        System.out.println("[ServerLauncher] Launching " + version.getId() + "...");

        Path selfJar = ProcessUtils.currentJar();
        Process process = ProcessUtils.launchServer(
                selfJar,
                serverGameJar,
                extraArgs.toArray(String[]::new));

        int exitCode = process.waitFor();
        System.exit(exitCode);
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) if (arg.equals(flag)) return true;
        return false;
    }
}
