package ing.boykiss.gearworks.launcher.manifest;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

/**
 * Represents the version manifest JSON served from the custom manifest URL.
 * <p>
 * Example manifest format:
 * <code>
 * {
 * "latest": { "release": "0.0.1", "beta": "0.0.2-dev" },
 * "versions": [
 * {
 * "id": "0.0.1",
 * "type": "release",
 * "releaseTime": "2026-02-21T00:00:00Z",
 * "downloads": {
 * "client": { "url": "https://...", "sha256": "..." },
 * "server": { "url": "https://...", "sha256": "..." }
 * }
 * }
 * ]
 * }
 * </code>
 */
@Getter
public class VersionManifest {
    private Latest latest;
    private List<Version> versions;

    @Getter
    public static class Latest {
        private String release;
        private String beta;
    }

    @Getter
    public static class Version {
        private String id;
        private String type;
        private String releaseTime;
        private Downloads downloads;

        public boolean isRelease() {
            return "release".equals(type);
        }

        public boolean isBeta() {
            return "beta".equals(type);
        }
    }

    @Getter
    public static class Downloads {
        private Download client;
        private Download server;
    }

    @Getter
    public static class Download {
        private String url;
        @SerializedName("sha256")
        private String sha256;
    }
}
