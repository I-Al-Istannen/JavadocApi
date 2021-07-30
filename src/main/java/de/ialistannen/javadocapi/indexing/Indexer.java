package de.ialistannen.javadocapi.indexing;

import de.ialistannen.javadocapi.spoon.JavadocElementExtractor;
import de.ialistannen.javadocapi.spoon.filtering.IndexerFilterChain;
import de.ialistannen.javadocapi.spoon.filtering.ParallelProcessor;
import de.ialistannen.javadocapi.storage.ConfiguredGson;
import de.ialistannen.javadocapi.storage.SqliteStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import spoon.Launcher;
import spoon.OutputType;
import spoon.reflect.CtModel;
import spoon.support.compiler.ProgressLogger;
import spoon.support.compiler.ZipFolder;

public class Indexer {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: Indexer <path to config>");
      System.exit(1);
    }

    String configFileString = Files.readString(Path.of(args[0]));
    IndexerConfig config = ConfiguredGson.create().fromJson(configFileString, IndexerConfig.class);

    System.out.println(heading("Configuring spoon"));
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setShouldCompile(false);
    launcher.getEnvironment().disableConsistencyChecks();
    launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
    launcher.getEnvironment().setSpoonProgress(new ConsoleProcessLogger(launcher));
    launcher.getEnvironment().setCommentEnabled(true);
    launcher.getEnvironment().setComplianceLevel(15);
    for (String path : config.getResourcePaths()) {
      if (path.endsWith(".zip")) {
        launcher.addInputResource(new ZipFolder(new File(path)));
      } else {
        launcher.addInputResource(path);
      }
    }
    System.out.println("Spoon successfully configured\n");

    System.out.println(heading("Building spoon model"));
    CtModel model = launcher.buildModel();
    System.out.println("Model successfully built\n");

    System.out.println(heading("Converting Spoon Model "));
    JavadocElementExtractor extractor = new JavadocElementExtractor();
    ParallelProcessor processor = new ParallelProcessor(
        new IndexerFilterChain(config.getAllowedPackages()).asFilter(),
        Runtime.getRuntime().availableProcessors()
    );
    model.getAllModules()
        .forEach(it -> processor.process(
            it,
            element -> element.accept(extractor))
        );
    processor.shutdown();
    System.out.println("Model successfully converted\n");

    System.out.println(heading("Writing to output database"));
    new SqliteStorage(ConfiguredGson.create(), Path.of(config.getOutputPath()))
        .addAll(extractor.getFoundElements());
  }

  private static String heading(String text) {
    return "\n\033[94;1m==== \033[36;1m" + text + " \033[94;1m====\033[0m";
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
                + " classes so far. Currently working on " + task
        );
      }
    }

    @Override
    public void end(Process process) {
      System.out.println("Phase " + process + " done! Discovered Classes: " + touchedClasses);
    }
  }
}
