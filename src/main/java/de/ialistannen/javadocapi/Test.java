package de.ialistannen.javadocapi;

import com.google.gson.Gson;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentTag;
import de.ialistannen.javadocapi.rendering.CommentRenderer;
import de.ialistannen.javadocapi.rendering.HtmlCommentRender;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.spoon.JavadocElementExtractor;
import de.ialistannen.javadocapi.storage.ConfiguredGson;
import de.ialistannen.javadocapi.storage.SqliteStorage;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import spoon.Launcher;
import spoon.reflect.CtModel;

/**
 * Hello world!
 *
 * @author Peter
 * @version 1.1.1
 */
public class Test {

  @Deprecated(forRemoval = true, since = "Yo")
  public static <T extends String> T getGeneric(T t, JavadocComment comment)
      throws IllegalArgumentException {
    return null;
  }

  public static class Uffa<T extends CharSequence & Serializable> {

  }

  @Deprecated(forRemoval = true)
  public static class Uffa2 extends Uffa<String> {

  }

  /**
   * The main method.
   * <p>
   * You can find <em>many</em> things here.
   *
   * @param args the arguments of type {@linkplain String hello} and a cool link {@link
   *     JavadocComment#equals(Object)} {@link JavadocComment#equals(Object other)}
   * @throws Exception an exception!
   */
  public static void main(String[] args) throws Exception {
    Path databasePath = Path.of(
        Test.class.getProtectionDomain().getCodeSource().getLocation().toURI()
    )
        .resolveSibling("Test.db");

    List<JavadocElement> elements;
    Gson gson = ConfiguredGson.create();
    if (Files.exists(databasePath)) {
      elements = new SqliteStorage(gson).load(databasePath);
    } else {
      elements = buildElements();
      new SqliteStorage(gson).store(elements, databasePath);
    }

    printExampleElement(elements);
  }

  private static List<JavadocElement> buildElements() {
    Launcher launcher = new Launcher();
    launcher.addInputResource(
        "/home/i_al_istannen/Programming/Discord/JavadocApi/src/main/java/"
    );
    launcher.getEnvironment().setCommentEnabled(true);
    CtModel model = launcher.buildModel();
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();

    JavadocElementExtractor extractor = new JavadocElementExtractor();
    extractor.visitCtPackage(model.getRootPackage());
    return extractor.getFoundElements();
  }

  private static void printExampleElement(List<JavadocElement> elements) {
    elements = elements
        .stream()
        .filter(it -> it.getQualifiedName()
            .asString()
            .contains("de.ialistannen.javadocapi.Test#main(")
        )
        .collect(Collectors.toList());

    render(elements, new HtmlCommentRender());
    System.out.println();
    System.out.println();
    render(elements, new MarkdownCommentRenderer());
  }

  private static void render(List<JavadocElement> elements, CommentRenderer renderer) {
    for (JavadocElement element : elements) {
      System.out.println(element.getDeclaration(DeclarationStyle.SHORT));
      JavadocComment comment = element.getComment().orElseThrow();
      System.out.println("<br>");
      System.out.println(renderer.render(comment.getShortDescription()));
      System.out.println("<br>");
      System.out.println("<br>");
      System.out.println(renderer.render(comment.getLongDescription()));
      System.out.println("<br>");
      System.out.println("<br>");
      for (JavadocCommentTag tag : comment.getTags()) {
        System.out.println("<br>");
        System.out.println("\n\033[94;1m" + tag.getTagName() + " \033[0m- " + tag.getArgument());
        System.out.println(renderer.render(tag.getContent()));
      }
    }
  }
}
