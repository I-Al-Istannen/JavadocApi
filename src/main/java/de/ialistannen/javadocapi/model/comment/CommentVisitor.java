package de.ialistannen.javadocapi.model.comment;

public interface CommentVisitor<T> {

  T visitLink(JavadocCommentLink link);

  T visitInlineTag(JavadocCommentInlineTag tag);

  T visitText(JavadocCommentText text);
}
