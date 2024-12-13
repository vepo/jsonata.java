package dev.vepo.jsonata;

import java.util.List;

public interface NodeList extends Node {
    List<String> asText();
}
