package ing.boykiss.gearworks.launcher.manifest;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ManifestFetcher {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String manifestUrl;

    public ManifestFetcher(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    /**
     * Fetches and parses the version manifest from the configured URL.
     *
     * @throws IOException          on network error
     * @throws InterruptedException if the request is interrupted
     */
    public VersionManifest fetch() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(manifestUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Manifest fetch failed with HTTP " + response.statusCode() + ": " + manifestUrl);
        }

        return GSON.fromJson(response.body(), VersionManifest.class);
    }
}
