package de.ialistannen.javadocapi.indexing;

import de.ialistannen.javadocapi.spoon.JavadocElementExtractor;
import de.ialistannen.javadocapi.storage.ConfiguredGson;
import de.ialistannen.javadocapi.storage.SqliteStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import spoon.Launcher;
import spoon.OutputType;
import spoon.reflect.CtModel;
import spoon.support.compiler.ProgressLogger;

public class Indexer {

  public static void main(String[] args) throws IOException, SQLException {
    if (args.length != 1) {
      System.err.println("Usage: Indexer <path to config>");
      System.exit(1);
    }

    String configFileString = Files.readString(Path.of(args[0]));
    IndexerConfig config = ConfiguredGson.create().fromJson(configFileString, IndexerConfig.class);

    System.out.println("==== Configuring spoon ====");
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setShouldCompile(false);
    launcher.getEnvironment().disableConsistencyChecks();
    launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
    launcher.getEnvironment().setSpoonProgress(new ConsoleProcessLogger(launcher));
    launcher.getEnvironment().setCommentEnabled(true);
    config.getResourcePaths().forEach(launcher::addInputResource);
    System.out.println("\n\nSpoon successfully configured\n");

    System.out.println("==== Building spoon model ====");
    CtModel model = launcher.buildModel();
    System.out.println("\n\nModel successfully built\n");

    System.out.println("==== Converting Spoon Model  ====");
    JavadocElementExtractor extractor = new JavadocElementExtractor(config.getAllowedPackages());
    extractor.visitCtPackage(model.getRootPackage());
    System.out.println("\n\nModel successfully converted\n");

    System.out.println("==== Writing to output database ====");
    new SqliteStorage(ConfiguredGson.create()).store(
        extractor.getFoundElements(), Path.of(config.getOutputPath())
    );
  }

  private static class ConsoleProcessLogger extends ProgressLogger {

    public int touchedClasses;

    public ConsoleProcessLogger(Launcher launcher) {
      super(launcher.getEnvironment());
      touchedClasses = 0;
    }

    @Override
    public void start(Process process) {
      System.out.println("Starting phase " + process);
      touchedClasses = 0;
    }

    @Override
    public void step(Process process, String task, int taskId, int nbTask) {
      touchedClasses++;
      if (touchedClasses % 1000 == 0) {
        System.out.println(
            "Phase " + process + " has discovered " + touchedClasses
                + " so far. Currently working on " + task
        );
      }
    }

    @Override
    public void end(Process process) {
      System.out.println("Phase " + process + "done! Discovered Classes: " + touchedClasses);
    }
  }
}
