package de.ialistannen.javadocapi.model.comment;

import de.ialistannen.javadocapi.model.QualifiedName;
import java.util.Optional;

public class JavadocCommentLink extends JavadocCommentInlineTag {

  private final String label;
  private final QualifiedName target;
  private final boolean plain;

  public JavadocCommentLink(QualifiedName target, String label, boolean plain) {
    super(
        plain ? Type.LINKPLAIN : Type.LINK,
        target.asString() + (label != null ? " " + label.strip() : "")
    );
    this.label = label != null ? label.strip() : null;
    this.target = target;
    this.plain = plain;
  }

  public Optional<String> getLabel() {
    return Optional.ofNullable(label);
  }

  public boolean isPlain() {
    return plain;
  }

  public QualifiedName getTarget() {
    return target;
  }

  @Override
  public <T> T accept(CommentVisitor<T> visitor) {
    return visitor.visitLink(this);
  }
}
