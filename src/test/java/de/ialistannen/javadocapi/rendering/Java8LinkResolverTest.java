package de.ialistannen.javadocapi.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.ialistannen.javadocapi.model.QualifiedName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Java8LinkResolverTest {

  private Java8LinkResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new Java8LinkResolver();
  }

  @Test
  void className() {
    String link = resolver.resolveLink(
        new QualifiedName("java.lang.String"),
        "https://foo.com/"
    );

    assertEquals("https://foo.com/java/lang/String.html", link);
  }

  @Test
  void field() {
    String link = resolver.resolveLink(
        new QualifiedName("java.lang.String#CASE_INSENSITIVE_ORDER"),
        "https://foo.com/"
    );

    assertEquals("https://foo.com/java/lang/String.html#CASE_INSENSITIVE_ORDER", link);
  }

  @Test
  void methodNoParams() {
    String link = resolver.resolveLink(
        new QualifiedName("java.lang.String#length()"),
        "https://foo.com/"
    );

    assertEquals("https://foo.com/java/lang/String.html#length--", link);
  }

  @Test
  void methodNoOneParam() {
    String link = resolver.resolveLink(
        new QualifiedName("java.lang.String#charAt(int)"),
        "https://foo.com/"
    );

    assertEquals("https://foo.com/java/lang/String.html#charAt-int-", link);
  }

  @Test
  void methodNoTwoParams() {
    String link = resolver.resolveLink(
        new QualifiedName("java.lang.String#codePointCount(int,int)"),
        "https://foo.com/"
    );

    assertEquals("https://foo.com/java/lang/String.html#codePointCount-int-int-", link);
  }
}
