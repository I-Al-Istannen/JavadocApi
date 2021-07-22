package de.ialistannen.javadocapi.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentLink;
import de.ialistannen.javadocapi.model.comment.JavadocCommentText;
import de.ialistannen.javadocapi.model.types.AnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.ListAnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.PrimitiveAnnotationValue;
import de.ialistannen.javadocapi.model.types.AnnotationValue.QualifiedAnnotationValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            (JsonSerializer<QualifiedName>) (name, type, context) -> {
              String asText = name.asString();
              if (name.getModuleName().isPresent()) {
                asText = name.getModuleName().get() + "/" + asText;
              }
              return new JsonPrimitive(asText);
            }
        )
        .registerTypeAdapter(
            QualifiedName.class,
            (JsonDeserializer<QualifiedName>) (element, type, context) -> {
              String moduleName = null;
              String qualifiedName = element.getAsString();

              if (element.getAsString().contains("/")) {
                String[] split = element.getAsString().split("/");
                moduleName = split[0];
                qualifiedName = split[1];
              }
              return new QualifiedName(qualifiedName, moduleName);
            }
        )
        .registerTypeAdapter(
            AnnotationValue.class,
            (JsonSerializer<AnnotationValue>) (value, type, context) -> {
              if (value instanceof PrimitiveAnnotationValue) {
                return new JsonPrimitive(((PrimitiveAnnotationValue) value).getValue());
              }
              if (value instanceof QualifiedAnnotationValue) {
                JsonObject object = new JsonObject();
                object.add("name", context.serialize(((QualifiedAnnotationValue) value).getName()));
                return object;
              }
              if (!(value instanceof ListAnnotationValue)) {
                throw new IllegalArgumentException(
                    "Unknown annotation value " + type + " " + value
                );
              }
              JsonArray array = new JsonArray();

              for (AnnotationValue annotationValue : ((ListAnnotationValue) value).getValues()) {
                array.add(context.serialize(annotationValue));
              }

              return array;
            }
        )
        .registerTypeAdapter(
            AnnotationValue.class,
            (JsonDeserializer<AnnotationValue>) (element, type, context) -> {
              if (element.isJsonPrimitive()) {
                return new PrimitiveAnnotationValue(element.getAsString());
              }
              if (element.isJsonObject()) {
                return new QualifiedAnnotationValue(
                    context.deserialize(
                        element.getAsJsonObject().get("name"),
                        QualifiedName.class
                    )
                );
              }

              List<AnnotationValue> values = new ArrayList<>();
              for (JsonElement jsonElement : element.getAsJsonArray()) {
                values.add(context.deserialize(jsonElement, AnnotationValue.class));
              }

              return new ListAnnotationValue(values);
            }
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

  private enum AnnotationValueType {
    PRIMITIVE(PrimitiveAnnotationValue.class),
    QUALIFIED(QualifiedAnnotationValue.class),
    LIST(ListAnnotationValue.class);

    private final Class<? extends AnnotationValue> valueClass;

    AnnotationValueType(Class<? extends AnnotationValue> elementClass) {
      this.valueClass = elementClass;
    }

    public Class<? extends AnnotationValue> getValueClass() {
      return valueClass;
    }

    public static AnnotationValueType fromValue(AnnotationValue element) {
      return Arrays.stream(values())
          .filter(it -> it.valueClass.isInstance(element))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown annotation type " + element));
    }
  }
}
