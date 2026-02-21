package ing.boykiss.gearworks.launcher;

import ing.boykiss.gearworks.launcher.download.VersionDownloader;
import ing.boykiss.gearworks.launcher.manifest.ManifestFetcher;
import ing.boykiss.gearworks.launcher.manifest.VersionManifest;
import ing.boykiss.gearworks.launcher.ui.LauncherUIState;
import ing.boykiss.gearworks.launcher.ui.LauncherWindow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates the launcher: fetches the manifest, shows the Vulkan UI,
 * downloads the selected version, and spawns the game process.
 */
public class LauncherApp {
    /**
     * URL of the version manifest JSON.
     */
    public static final String MANIFEST_URL = "https://gearworks-test.local/versions.json";

    /**
     * Directory where downloaded versions are stored.
     */
    private static final Path VERSIONS_DIR = Path.of(System.getProperty("user.home"), ".gearworks", "versions");

    /**
     * Play button bounds — must match LauncherRenderer constants (pixel space).
     */
    private static final double BTN_X0 = 330, BTN_X1 = 630, BTN_Y0 = 370, BTN_Y1 = 430;

    private final LauncherWindow window = new LauncherWindow();
    private final LauncherUIState uiState = new LauncherUIState();
    private final ManifestFetcher fetcher = new ManifestFetcher(MANIFEST_URL);
    private final VersionDownloader downloader = new VersionDownloader(VERSIONS_DIR);

    private VersionManifest manifest;
    private volatile boolean launchRequested = false;
    private volatile VersionManifest.Version versionToLaunch;
    /**
     * Set at startup if a locally-built game JAR is found; bypasses manifest fetch.
     */
    private Path localGameJar;

    public void run() {
        localGameJar = findLocalGameJar();

        window.init(uiState);

        if (localGameJar != null) {
            // Found a locally-built game JAR — skip manifest and go straight to ready state.
            System.out.println("[Launcher] Found local game JAR: " + localGameJar);
            uiState.setScreen(LauncherUIState.Screen.VERSION_SELECT);
        } else {
            // Fetch manifest on a background thread so the window can open immediately.
            Thread.ofVirtual().start(() -> {
                try {
                    manifest = fetcher.fetch();
                    uiState.setVersions(manifest.getVersions());
                    uiState.setScreen(LauncherUIState.Screen.VERSION_SELECT);
                } catch (Exception e) {
                    uiState.setErrorMessage("Failed to fetch version manifest:\n" + e.getMessage());
                    uiState.setScreen(LauncherUIState.Screen.ERROR);
                }
            });
        }

        window.loop(this::onFrame);
        window.cleanup();

        if (launchRequested) {
            if (localGameJar != null && versionToLaunch == null) {
                try {
                    System.out.println("[Launcher] Launching local game JAR: " + localGameJar);
                    ProcessUtils.launchClient(localGameJar);
                } catch (Exception e) {
                    System.err.println("[Launcher] Launch failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (versionToLaunch != null) {
                launchVersion(versionToLaunch);
            }
        }
    }

    /**
     * Called every frame from the render loop (main thread).
     */
    private void onFrame() {
        double mx = window.mouseX;
        double my = window.mouseY;
        boolean hovered = mx >= BTN_X0 && mx <= BTN_X1 && my >= BTN_Y0 && my <= BTN_Y1;
        uiState.setPlayButtonHovered(hovered);

        // Button is clickable when we have a local JAR ready, or when a remote
        // version list has been loaded.
        boolean canPlay = localGameJar != null
                || (uiState.getScreen() == LauncherUIState.Screen.VERSION_SELECT
                && uiState.getVersions() != null
                && !uiState.getVersions().isEmpty());

        if (window.consumeClick() && hovered && canPlay) {
            // Prefer a selected remote version; fall back to local JAR.
            if (uiState.getVersions() != null && !uiState.getVersions().isEmpty()) {
                versionToLaunch = uiState.getVersions().get(uiState.getSelectedVersionIndex());
            }
            launchRequested = true;
            window.requestClose();
        }
    }

    /**
     * Downloads (if needed) and spawns the game process for the given version.
     * For bundled / dev releases, checks for a local game JAR first.
     * Runs on the main thread after the window has closed.
     */
    private void launchVersion(VersionManifest.Version version) {
        try {
            // Check for a locally bundled game JAR first (release / dev mode)
            Path localJar = findLocalGameJar();
            Path gameJar;
            if (localJar != null) {
                System.out.println("[Launcher] Using local game JAR: " + localJar);
                gameJar = localJar;
            } else {
                System.out.println("[Launcher] Resolving client JAR for " + version.getId() + "...");
                uiState.setScreen(LauncherUIState.Screen.DOWNLOADING);
                gameJar = downloader.resolveClientJar(version, progress -> {
                    uiState.setDownloadProgress(progress);
                    uiState.setDownloadStatus(String.format("Downloading... %.0f%%", progress * 100));
                });
            }

            System.out.println("[Launcher] Launching " + version.getId() + " from " + gameJar);
            ProcessUtils.launchClient(gameJar);
        } catch (Exception e) {
            System.err.println("[Launcher] Launch failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Looks for a bundled game JAR next to the running launcher JAR (release mode).
     * Returns {@code null} if none is found.
     */
    /**
     * Looks for a game JAR (Gearworks-*.jar) in the working directory.
     * In dev/release this is {@code run/launcher/}, where
     * {@code stageClientLauncher} copies both the launcher JAR and the game JAR.
     */
    private Path findLocalGameJar() {
        try (var files = Files.list(Path.of("."))) {
            return files
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("Gearworks-") && name.endsWith(".jar")
                                && !name.contains("Launcher");
                    })
                    .findFirst()
                    .map(p -> p.toAbsolutePath().normalize())
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}