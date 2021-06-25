package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentLink;
import de.ialistannen.javadocapi.model.comment.JavadocCommentText;
import java.util.Arrays;

public class ConfiguredGson {

  public static Gson create() {
    Gson embeddedHelper = new Gson();

    return new GsonBuilder()
        .registerTypeAdapter(
            JavadocCommentFragment.class,
            (JsonSerializer<JavadocCommentFragment>) (fragment, type, context) -> {
              JsonObject object = new JsonObject();

              object.addProperty("type", FragmentType.fromFragment(fragment).name());
              object.add("value", embeddedHelper.toJsonTree(fragment));

              return object;
            }
        )
        .registerTypeAdapter(
            JavadocCommentFragment.class,
            (JsonDeserializer<JavadocCommentFragment>) (element, type, context) -> {
              JsonObject object = element.getAsJsonObject();

              FragmentType fragmentType = FragmentType.valueOf(
                  object.getAsJsonPrimitive("type").getAsString()
              );

              return embeddedHelper.fromJson(object.get("value"), fragmentType.getElementClass());
            }
        )
        .registerTypeAdapter(
            QualifiedName.class,
            (JsonSerializer<QualifiedName>) (name, type, context) ->
                new JsonPrimitive(name.asString())
        )
        .registerTypeAdapter(
            QualifiedName.class,
            (JsonDeserializer<QualifiedName>) (element, type, context) ->
                new QualifiedName(element.getAsString())
        )
        .create();
  }

  private enum FragmentType {
    LINK(JavadocCommentLink.class),
    INLINE_TAG(JavadocCommentInlineTag.class),
    TEXT(JavadocCommentText.class);

    private final Class<? extends JavadocCommentFragment> elementClass;

    FragmentType(Class<? extends JavadocCommentFragment> elementClass) {
      this.elementClass = elementClass;
    }

    public Class<? extends JavadocCommentFragment> getElementClass() {
      return elementClass;
    }

    public static FragmentType fromFragment(JavadocCommentFragment element) {
      return Arrays.stream(values())
          .filter(it -> it.elementClass.isInstance(element))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown fragment type " + element));
    }
  }
}
