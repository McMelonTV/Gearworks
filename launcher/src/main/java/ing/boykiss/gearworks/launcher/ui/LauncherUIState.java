package ing.boykiss.gearworks.launcher.ui;

import ing.boykiss.gearworks.launcher.manifest.VersionManifest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Mutable UI state passed to LauncherRenderer each frame.
 * All fields may be mutated freely from the main thread.
 */
@Getter
@Setter
public class LauncherUIState {
    private Screen screen = Screen.LOADING;
    // VERSION_SELECT
    private List<VersionManifest.Version> versions;
    private int selectedVersionIndex = 0;
    // DOWNLOADING
    private String downloadStatus = "";
    private double downloadProgress = 0.0;
    // ERROR
    private String errorMessage = "";
    // INPUT
    private boolean playButtonHovered;

    public enum Screen {LOADING, VERSION_SELECT, DOWNLOADING, ERROR}
}
