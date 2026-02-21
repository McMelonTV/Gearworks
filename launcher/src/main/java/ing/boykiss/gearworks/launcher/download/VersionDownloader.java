package ing.boykiss.gearworks.launcher.download;

import ing.boykiss.gearworks.launcher.manifest.VersionManifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Consumer;

public class VersionDownloader {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Path versionsDir;

    /**
     * @param versionsDir Root directory where versions are stored.
     *                    Each version gets its own subdirectory: versionsDir/<id>/
     */
    public VersionDownloader(Path versionsDir) {
        this.versionsDir = versionsDir;
    }

    /**
     * Returns the path to the client game JAR for the given version,
     * downloading it first if it doesn't exist locally.
     *
     * @param version          The version to download
     * @param progressCallback Called with values 0.0–1.0 as the download progresses
     */
    public Path resolveClientJar(VersionManifest.Version version, Consumer<Double> progressCallback)
            throws IOException, InterruptedException {
        VersionManifest.Download download = version.getDownloads().getClient();
        return resolveJar(version.getId(), "Gearworks-" + version.getId() + ".jar", download, progressCallback);
    }

    /**
     * Returns the path to the server game JAR for the given version,
     * downloading it first if it doesn't exist locally.
     */
    public Path resolveServerJar(VersionManifest.Version version, Consumer<Double> progressCallback)
            throws IOException, InterruptedException {
        VersionManifest.Download download = version.getDownloads().getServer();
        return resolveJar(version.getId(), "GearworksServer-" + version.getId() + ".jar", download, progressCallback);
    }

    private Path resolveJar(String versionId, String filename, VersionManifest.Download download,
                            Consumer<Double> progressCallback) throws IOException, InterruptedException {
        Path versionDir = versionsDir.resolve(versionId);
        Files.createDirectories(versionDir);

        Path jarPath = versionDir.resolve(filename);

        if (Files.exists(jarPath)) {
            // Verify checksum if present; re-download if corrupted
            if (download.getSha256() != null && !verifySha256(jarPath, download.getSha256())) {
                System.err.println("[Launcher] Checksum mismatch for " + jarPath + ", re-downloading...");
                Files.delete(jarPath);
            } else {
                progressCallback.accept(1.0);
                return jarPath;
            }
        }

        download(download.getUrl(), jarPath, progressCallback);

        if (download.getSha256() != null && !verifySha256(jarPath, download.getSha256())) {
            Files.delete(jarPath);
            throw new IOException("Downloaded file failed SHA-256 verification: " + filename);
        }

        return jarPath;
    }

    private void download(String url, Path destination, Consumer<Double> progressCallback)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        // Use InputStream body handler to report progress
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed with HTTP " + response.statusCode() + ": " + url);
        }

        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

        try (InputStream in = response.body()) {
            long downloaded = 0;
            byte[] buffer = new byte[8192];
            int read;

            try (var out = Files.newOutputStream(destination)) {
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;

                    if (contentLength > 0) {
                        progressCallback.accept((double) downloaded / contentLength);
                    }
                }
            }
        }

        progressCallback.accept(1.0);
    }

    private boolean verifySha256(Path file, String expectedHex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash).equalsIgnoreCase(expectedHex);
        } catch (NoSuchAlgorithmException | IOException e) {
            return false;
        }
    }
}
