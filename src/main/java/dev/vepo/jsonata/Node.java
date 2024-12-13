package dev.vepo.jsonata;

public interface Node {

    public static Node emptyNode() {
        return NodeEmpty.EMPTY;
    }

    boolean isEmpty();

    boolean hasField(String field);

    Node get(String field);

    boolean isNull();

    boolean isArray();

    int lenght();

    Node at(int index);

    NodeList asList();

    NodeObject asObject();
}
