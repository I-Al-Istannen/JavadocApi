package de.ialistannen.javadocapi.classpath;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

public class PomClasspathDiscoverer {

  /**
   * Tries to find the full classpath needed for a given maven project.
   *
   * @param pomPath the path to the pom to analyze
   * @param mavenHome the maven.home location
   * @return all extra jars that should be on the classpath
   * @throws MavenInvocationException if an error occurs
   */
  public List<Path> findClasspath(Path pomPath, Path mavenHome) throws MavenInvocationException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    InvocationRequest invocationRequest = new DefaultInvocationRequest();
    invocationRequest.setPomFile(pomPath.toFile());
    invocationRequest.setGoals(List.of("dependency:resolve", "dependency:build-classpath"));
    invocationRequest.setOutputHandler(
        new PrintStreamHandler(new PrintStream(outputStream), false)
    );

    Invoker invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome.toFile());
    InvocationResult result = invoker.execute(invocationRequest);

    String output = outputStream.toString(StandardCharsets.UTF_8);
    if (result.getExitCode() != 0) {
      throw new MavenInvocationException(
          "Non-zero exit code\n" + output,
          result.getExecutionException()
      );
    }

    Matcher matcher = Pattern.compile("Dependencies classpath:\n(.+)").matcher(output);

    if (output.contains("No dependencies found.")) {
      return List.of();
    }

    if (!matcher.find()) {
      throw new MavenInvocationException("Could not parse output\n" + output);
    }

    return Arrays.stream(matcher.group(1).split(":"))
        .map(Path::of)
        .collect(Collectors.toList());
  }
}
