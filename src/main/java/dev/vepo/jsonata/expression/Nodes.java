package dev.vepo.jsonata.expression;

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.Node;

public abstract class Nodes {
    private static class EmptyNode implements Node {
        private EmptyNode() {
        }

        @Override
        public String asText() {
            throw new IllegalStateException("Value is empty");
        }

        @Override
        public int asInt() {
            throw new IllegalStateException("Value is empty");
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            throw new IllegalStateException("Value is empty");
        }

        @Override
        public Multi multi() {
            return new Multi() {

                @Override
                public List<String> asText() {
                    return emptyList();
                }

                @Override
                public List<Integer> asInt() {
                    return emptyList();
                }

                @Override
                public List<Boolean> asBoolean() {
                    return emptyList();
                }

            };
        }
    }

    private static class _ArrayNode implements Node {

        private final ArrayNode element;

        private _ArrayNode(ArrayNode element) {
            this.element = element;
        }

        @Override
        public String asText() {
            return element.asText();
        }

        @Override
        public int asInt() {
            return element.asInt();
        }

        @Override
        public boolean asBoolean() {
            return element.asBoolean();
        }

        @Override
        public boolean isNull() {
            return element.isNull();
        }

        @Override
        public boolean isEmpty() {
            return element.isEmpty();
        }

        @Override
        public Multi multi() {
            return new Multi() {

                @Override
                public List<String> asText() {
                    return IntStream.range(0, element.size())
                            .mapToObj(i -> element.get(i))
                            .map(JsonNode::asText)
                            .toList();
                }

                @Override
                public List<Integer> asInt() {
                    return IntStream.range(0, element.size())
                            .mapToObj(i -> element.get(i))
                            .map(JsonNode::asInt)
                            .toList();
                }

                @Override
                public List<Boolean> asBoolean() {
                    return IntStream.range(0, element.size())
                            .mapToObj(i -> element.get(i))
                            .map(JsonNode::asBoolean)
                            .toList();
                }

            };
        }
    }

    private static class GroupNode implements Node {

        private final List<Node> elements;

        private GroupNode(List<Node> elements) {
            this.elements = elements;
        }

        @Override
        public String asText() {
            return elements.stream().map(Node::asText).collect(Collectors.joining(", "));
        }

        @Override
        public int asInt() {
            return elements.stream().mapToInt(Node::asInt).sum();
        }

        @Override
        public boolean asBoolean() {
            return elements.stream().map(Node::asBoolean).reduce((b1, b2) -> b1 && b2).orElse(false);
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public Multi multi() {
            return new Multi() {

                @Override
                public List<String> asText() {
                    return elements.stream().map(Node::asText).toList();
                }

                @Override
                public List<Integer> asInt() {
                    return elements.stream().map(Node::asInt).toList();
                }

                @Override
                public List<Boolean> asBoolean() {
                    return elements.stream().map(Node::asBoolean).toList();
                }

            };
        }
    }

    private static class ObjectNode implements Node {

        private final JsonNode element;

        private ObjectNode(JsonNode element) {
            this.element = element;
        }

        @Override
        public String asText() {
            if (element.isObject()) {
                return element.toString();
            } else {
                return element.asText();
            }
        }

        @Override
        public int asInt() {
            return element.asInt();
        }

        @Override
        public boolean isNull() {
            return element.isNull();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean asBoolean() {
            return element.asBoolean();
        }

        @Override
        public Multi multi() {
            return new Multi() {

                @Override
                public List<String> asText() {
                    return Collections.singletonList(element.asText());
                }

                @Override
                public List<Integer> asInt() {
                    return Collections.singletonList(element.asInt());
                }

                @Override
                public List<Boolean> asBoolean() {
                    return Collections.singletonList(element.asBoolean());
                }

            };
        }

    }

    public static final Node empty() {
        return new EmptyNode();
    }

    public static Node object(JsonNode element) {
        return new ObjectNode(element);
    }

    public static Node array(ArrayNode element) {
        return new _ArrayNode(element);
    }

    public static Node group(List<Node> elements) {
        return new GroupNode(elements);
    }
}