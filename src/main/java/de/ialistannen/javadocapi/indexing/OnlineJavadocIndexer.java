package de.ialistannen.javadocapi.indexing;

import de.ialistannen.javadocapi.util.ExternalJavadocReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlineJavadocIndexer {

  private final HttpClient client;

  public OnlineJavadocIndexer(HttpClient client) {
    this.client = client;
  }

  /**
   * Fetches the package list from the given URL. Will return an empty list if no index can be
   * found.
   *
   * @param baseUrl the base url of the javadoc to index
   * @return all found packages in the linked javadoc
   * @throws IOException if an error occurs
   * @throws InterruptedException if the send operation is interrupted
   */
  public ExternalJavadocReference fetchPackages(String baseUrl)
      throws IOException, InterruptedException {
    String targetUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "package-list";

    HttpResponse<String> response = client.send(
        HttpRequest.newBuilder(URI.create(targetUrl)).timeout(Duration.ofSeconds(15)).build(),
        BodyHandlers.ofString(StandardCharsets.UTF_8)
    );

    if (response.statusCode() != 200) {
      return fetchPackagesOldSchool(baseUrl);
    }

    return new ExternalJavadocReference(
        baseUrl,
        Set.copyOf(Arrays.asList(response.body().split("\n")))
    );
  }

  private ExternalJavadocReference fetchPackagesOldSchool(String baseUrl)
      throws IOException, InterruptedException {
    String targetUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "allpackages-index.html";

    HttpResponse<String> response = client.send(
        HttpRequest.newBuilder(URI.create(targetUrl)).timeout(Duration.ofSeconds(15)).build(),
        BodyHandlers.ofString(StandardCharsets.UTF_8)
    );

    if (response.statusCode() != 200) {
      return new ExternalJavadocReference(baseUrl, Set.of());
    }

    String body = response.body();
    Matcher matcher = Pattern.compile("<a .+package-summary.html\">(.+?)</a>").matcher(body);

    Set<String> packages = new HashSet<>();
    while (matcher.find()) {
      packages.add(matcher.group(1));
    }
    packages.remove("Package"); // header-link in the top left

    return new ExternalJavadocReference(baseUrl, packages);
  }
}
