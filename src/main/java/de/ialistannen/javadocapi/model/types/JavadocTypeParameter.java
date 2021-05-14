package de.ialistannen.javadocapi.model.types;

import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import java.util.regex.Pattern;

public class JavadocTypeParameter {

  private final Pattern QUALIFIED = Pattern.compile(
      "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+([\\p{L}_$][\\p{L}\\p{N}_$]+)"
  );

  private final String asString;

  public JavadocTypeParameter(String asString) {
    this.asString = asString;
  }

  public String getDeclaration(DeclarationStyle style) {
    if (style == DeclarationStyle.QUALIFIED) {
      return asString;
    }
    return QUALIFIED.matcher(asString).replaceAll("$2");
  }
}
