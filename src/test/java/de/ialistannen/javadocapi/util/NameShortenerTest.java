package de.ialistannen.javadocapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NameShortenerTest {

  private NameShortener shortener;

  @BeforeEach
  void setUp() {
    shortener = new NameShortener();
  }

  @Test
  void testShortensTypes() {
    assertEquals(
        Map.ofEntries(
            Map.entry("java.lang.String#foo()", "String#foo()"),
            Map.entry("java.lang.String#foo(int)", "String#foo(int)"),
            Map.entry("java.lang.StringBuilder#FOO", "StringBuilder#FOO"),
            Map.entry("java.lang.StringBuffer", "StringBuffer"),
            Map.entry("foo.conflicting.Bar#method(int)", "foo.conflicting.Bar#method(int)"),
            Map.entry("bar.conflicting.Bar#method(String)", "bar.conflicting.Bar#method(String)")
        ),
        shortener.shortenMatches(Set.of(
            new QualifiedName("java.lang.String#foo()"),
            new QualifiedName("java.lang.String#foo(int)"),
            new QualifiedName("java.lang.StringBuilder#FOO"),
            new QualifiedName("java.lang.StringBuffer"),
            new QualifiedName("foo.conflicting.Bar#method(int)"),
            new QualifiedName("bar.conflicting.Bar#method(String)")
        ))
    );
  }

  @Test
  void testShortenList() {
    assertEquals(
        Map.ofEntries(
            Map.entry("java.util.List", "util.List"),
            Map.entry("java.awt.List", "awt.List")
        ),
        shortener.shortenMatches(Set.of(
            new QualifiedName("java.util.List"),
            new QualifiedName("java.awt.List")
        ))
    );
  }

  @Test
  void testShortensWithParams() {
    assertEquals(
        Map.ofEntries(
            Map.entry("java.lang.String#foo()", "String#foo()"),
            Map.entry("java.lang.String#foo(int)", "String#foo(int)"),
            Map.entry("java.lang.String#foo(int, int)", "String#foo(int,int)"),
            Map.entry(
                "java.lang.String#foo(java.lang.String, java.lang.String)",
                "String#foo(String,String)"),
            Map.entry(
                "java.lang.String#foo(java.lang.String, bar.conflict.List)",
                "String#foo(String,bar.conflict.List)"
            ),
            Map.entry(
                "java.lang.String#foo(java.lang.String, foo.conflict.List)",
                "String#foo(String,foo.conflict.List)"
            )
        ),
        shortener.shortenMatches(Set.of(
            new QualifiedName("java.lang.String#foo()"),
            new QualifiedName("java.lang.String#foo(int)"),
            new QualifiedName("java.lang.String#foo(int, int)"),
            new QualifiedName("java.lang.String#foo(java.lang.String, java.lang.String)"),
            new QualifiedName("java.lang.String#foo(java.lang.String, bar.conflict.List)"),
            new QualifiedName("java.lang.String#foo(java.lang.String, foo.conflict.List)")
        ))
    );
  }
}
