package dev.vepo.jsonata.functions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.EmptyData;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.results.JSONataResults;

class TransformWithAlternateBackingTest {

  private static final Data SAMPLE = sample();

  @Test
  void mergesAndDeletesUsingNonJacksonBacking() {
    var transform = new Transform(
        (original, current) -> current.get("items"),
        (original, match) -> MapData.object(Map.of("updated", true)),
        Optional.of((original, match) -> MapData.text("remove")));

    var result = EvaluationContext.call(MapDataInspector.INSTANCE, () -> transform.map(SAMPLE, SAMPLE));

    assertThat(result.get("items").at(0).get("updated").toJson().asBoolean()).isTrue();
    assertThat(result.get("items").at(0).hasField("remove")).isFalse();
    assertThat(result.get("items").at(0).get("name").toJson().asText()).isEqualTo("first");
    assertThat(result.get("label").toJson().asText()).isEqualTo("keep");
    assertThat(SAMPLE.get("items").at(0).hasField("remove")).isTrue();
  }

  @Test
  void deepCopyDoesNotMutateOriginalMapBacking() {
    var transform = new Transform(
        (original, current) -> current.get("items"),
        (original, match) -> MapData.object(Map.of("updated", true)),
        Optional.empty());

    EvaluationContext.call(MapDataInspector.INSTANCE, () -> transform.map(SAMPLE, SAMPLE));

    assertThat(SAMPLE.get("items").at(0).hasField("updated")).isFalse();
  }

  private static Data sample() {
    var item1 = new LinkedHashMap<String, Object>();
    item1.put("name", "first");
    item1.put("remove", true);
    item1.put("score", 1);
    var item2 = new LinkedHashMap<String, Object>();
    item2.put("name", "second");
    item2.put("remove", true);
    item2.put("score", 2);
    var root = new LinkedHashMap<String, Object>();
    root.put("items", new ArrayList<>(List.of(item1, item2)));
    root.put("label", "keep");
    root.put("untouched", new LinkedHashMap<>(Map.of("value", 99)));
    return new MapData(root);
  }

  private static final class MapDataInspector implements DataInspector {

    static final MapDataInspector INSTANCE = new MapDataInspector();

    private MapDataInspector() {}

    @Override
    public boolean mutableValues() {
      return true;
    }

    @Override
    public Data copy(Data data) {
      if (data == null || data.isEmpty()) {
        return new EmptyData();
      }
      if (!(data instanceof MapData mapData)) {
        throw new JSONataException("Unsupported data backing for copy");
      }
      return mapData.deepCopy();
    }

    @Override
    public boolean isMutableObject(Data data) {
      return data instanceof MapData mapData && mapData.value instanceof Map<?, ?>;
    }

    @Override
    public void mergeFields(Data target, Data update) {
      if (!(target instanceof MapData targetMap) || !(targetMap.value instanceof Map<?, ?> targetObject)) {
        throw new JSONataException("Transform update target must be an object");
      }
      if (!(update instanceof MapData updateMap) || !(updateMap.value instanceof Map<?, ?> updateObject)) {
        return;
      }
      @SuppressWarnings("unchecked")
      var mutableTarget = (Map<String, Object>) targetObject;
      updateObject.forEach((key, value) -> mutableTarget.put(String.valueOf(key), value));
    }

    @Override
    public void removeFields(Data target, Iterable<String> fieldNames) {
      if (target instanceof MapData mapData && mapData.value instanceof Map<?, ?> targetObject) {
        @SuppressWarnings("unchecked")
        var mutableTarget = (Map<String, Object>) targetObject;
        fieldNames.forEach(mutableTarget::remove);
      }
    }

    @Override
    public Data merged(Data target, Data update) {
      mergeFields(target, update);
      return target;
    }

    @Override
    public Data withoutFields(Data target, Iterable<String> fieldNames) {
      removeFields(target, fieldNames);
      return target;
    }

    @Override
    public Data replaceNode(Data root, Data current, Data replacement) {
      if (root == current) {
        return ((MapData) replacement).deepCopy();
      }
      if (!(root instanceof MapData rootMap) || !(rootMap.value instanceof Map<?, ?> rootObject)) {
        throw new JSONataException("Cannot replace node outside root tree");
      }
      @SuppressWarnings("unchecked")
      var mutableRoot = (Map<String, Object>) ((MapData) rootMap.deepCopy()).value;
      var path = findMapPath(mutableRoot, ((MapData) current).value);
      applyReplacement(mutableRoot, path, ((MapData) replacement).value);
      return new MapData(mutableRoot);
    }

    private static List<Object> findMapPath(Map<String, Object> root, Object target) {
      if (root == target) {
        return List.of();
      }
      for (var entry : root.entrySet()) {
        var value = entry.getValue();
        if (value == target) {
          return List.of(entry.getKey());
        }
        if (value instanceof Map<?, ?> childMap) {
          @SuppressWarnings("unchecked")
          var childPath = findMapPath((Map<String, Object>) childMap, target);
          if (!childPath.isEmpty()) {
            var path = new java.util.ArrayList<>();
            path.add(entry.getKey());
            path.addAll(childPath);
            return path;
          }
        }
        if (value instanceof List<?> list) {
          for (int i = 0; i < list.size(); i++) {
            var element = list.get(i);
            if (element == target) {
              return List.of(entry.getKey(), i);
            }
            if (element instanceof Map<?, ?> elementMap) {
              @SuppressWarnings("unchecked")
              var childPath = findMapPath((Map<String, Object>) elementMap, target);
              if (!childPath.isEmpty()) {
                var path = new java.util.ArrayList<>();
                path.add(entry.getKey());
                path.add(i);
                path.addAll(childPath);
                return path;
              }
            }
          }
        }
      }
      throw new JSONataException("Cannot replace node outside root tree");
    }

    @SuppressWarnings("unchecked")
    private static void applyReplacement(Map<String, Object> root, List<Object> path, Object replacement) {
      if (path.isEmpty()) {
        throw new JSONataException("Cannot replace root via nested path");
      }
      Object current = root;
      for (int i = 0; i < path.size() - 1; i++) {
        var step = path.get(i);
        if (step instanceof String name) {
          current = ((Map<String, Object>) current).get(name);
        } else {
          current = ((List<Object>) current).get((Integer) step);
        }
      }
      var last = path.getLast();
      if (last instanceof String name) {
        ((Map<String, Object>) current).put(name, MapData.copyValue(replacement));
      } else {
        ((List<Object>) current).set((Integer) last, MapData.copyValue(replacement));
      }
    }
  }

  private static final class MapData implements Data {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private final Object value;
    private final DataInspector inspector;

    private MapData(Object value) {
      this.value = value;
      this.inspector = MapDataInspector.INSTANCE;
    }

    static MapData object(Map<String, Object> value) {
      return new MapData(new LinkedHashMap<>(value));
    }

    static MapData text(String value) {
      return new MapData(value);
    }

    MapData deepCopy() {
      return new MapData(deepCopyValue(value));
    }

    static Object copyValue(Object value) {
      return deepCopyValue(value);
    }

    private static Object deepCopyValue(Object value) {
      if (value instanceof Map<?, ?> map) {
        var copy = new LinkedHashMap<String, Object>();
        map.forEach((key, entryValue) -> copy.put(String.valueOf(key), deepCopyValue(entryValue)));
        return copy;
      }
      if (value instanceof List<?> list) {
        return list.stream().map(MapData::deepCopyValue).toList();
      }
      return value;
    }

    @Override
    public Data all() {
      return this;
    }

    @Override
    public Data at(int index) {
      if (value instanceof List<?> list) {
        return new MapData(list.get(index));
      }
      return Mapping.empty();
    }

    @Override
    public void forEachChild(Consumer<Data> action) {
      if (value instanceof Map<?, ?> map) {
        map.values().stream()
            .filter(Map.class::isInstance)
            .map(MapData::new)
            .forEach(action);
      }
    }

    @Override
    public Data get(String fieldName) {
      if (!(value instanceof Map<?, ?> map)) {
        return Mapping.empty();
      }
      var child = map.get(fieldName);
      if (child == null) {
        return Mapping.empty();
      }
      if (child instanceof List<?> list) {
        return new GroupedData(list.stream().map(MapData::new).map(Data.class::cast).toList());
      }
      return new MapData(child);
    }

    @Override
    public boolean hasField(String fieldName) {
      return value instanceof Map<?, ?> map && map.containsKey(fieldName);
    }

    @Override
    public boolean isObject() {
      return value instanceof Map<?, ?>;
    }

    @Override
    public boolean isArray() {
      return value instanceof List<?>;
    }

    @Override
    public boolean isList() {
      return value instanceof List<?>;
    }

    @Override
    public int length() {
      if (value instanceof List<?> list) {
        return list.size();
      }
      return 1;
    }

    @Override
    public JsonNode toJson() {
      if (value instanceof String text) {
        return NODE_FACTORY.textNode(text);
      }
      if (value instanceof Boolean booleanValue) {
        return NODE_FACTORY.booleanNode(booleanValue);
      }
      if (value instanceof Number number) {
        return NODE_FACTORY.numberNode(number.intValue());
      }
      if (value instanceof Map<?, ?> map) {
        var objectNode = NODE_FACTORY.objectNode();
        map.forEach((key, entryValue) -> objectNode.set(String.valueOf(key), new MapData(entryValue).toJson()));
        return objectNode;
      }
      if (value instanceof List<?> list) {
        var arrayNode = NODE_FACTORY.arrayNode();
        list.stream().map(MapData::new).map(MapData::toJson).forEach(arrayNode::add);
        return arrayNode;
      }
      throw new JSONataException("Unsupported map-backed value type: " + value.getClass());
    }

    @Override
    public JSONataResult toNode() {
      if (value instanceof Map<?, ?>) {
        return JSONataResults.object(toJson());
      }
      return JSONataResults.empty();
    }

    @Override
    public Data map(Function<JsonNode, Data> function) {
      return function.apply(toJson());
    }

    @Override
    public Stream<Data> stream() {
      if (value instanceof List<?> list) {
        return IntStream.range(0, list.size()).mapToObj(i -> new MapData(list.get(i)));
      }
      return Stream.of(this);
    }

    @Override
    public DataInspector inspector() {
      return inspector;
    }
  }
}
