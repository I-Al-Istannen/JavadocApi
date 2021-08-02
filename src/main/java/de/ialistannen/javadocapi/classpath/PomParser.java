package de.ialistannen.javadocapi.classpath;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomParser {

  public Pom parsePom(String pom) {
    try {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      Model model = reader.read(new StringReader(pom));

      List<Repository> repositories = model.getRepositories()
          .stream()
          .map(it -> new Repository(it.getUrl()))
          .collect(Collectors.toList());

      List<Dependency> dependencies = mapDependencies(model.getDependencies());
      List<Dependency> dependencyManagement = mapDependencies(
          model.getDependencyManagement().getDependencies()
      );

      return new Pom(
          Set.copyOf(dependencies),
          Set.copyOf(dependencyManagement),
          Set.copyOf(repositories)
      );
    } catch (IOException | XmlPullParserException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Dependency> mapDependencies(List<org.apache.maven.model.Dependency> dependencies) {
    return dependencies.stream()
        .map(it -> new Dependency(
            it.getGroupId(),
            it.getArtifactId(),
            it.getVersion(),
            it.getType().equals("pom")
        ))
        .collect(Collectors.toList());
  }
}
