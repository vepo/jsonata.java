package dev.vepo.jsonata;

import static java.util.Collections.emptyList;

import java.util.List;

public class NodeEmpty implements NodeList {

    static final NodeEmpty EMPTY = new NodeEmpty();

    @Override
    public List<String> asText() {
        return emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public NodeObject asObject() {
        throw new IllegalStateException("This is not an object!");
    }

    @Override
    public boolean hasField(String field) {
        return false;
    }

    @Override
    public Node get(String field) {
        return EMPTY;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public int lenght() {
        throw new IllegalStateException("Node is not defined!");
    }

    @Override
    public Node at(int index) {
        return this;
    }

    @Override
    public NodeList asList() {
        return this;
    }

}
