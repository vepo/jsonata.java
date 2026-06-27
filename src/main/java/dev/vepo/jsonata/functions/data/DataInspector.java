package dev.vepo.jsonata.functions.data;

public interface DataInspector {

    boolean mutableValues();

    Data copy(Data data);

    boolean isMutableObject(Data data);

    void mergeFields(Data target, Data update);

    void removeFields(Data target, Iterable<String> fieldNames);

    Data merged(Data target, Data update);

    Data withoutFields(Data target, Iterable<String> fieldNames);

    Data replaceNode(Data root, Data current, Data replacement);
}
