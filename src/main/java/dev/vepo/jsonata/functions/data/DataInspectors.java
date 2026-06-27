package dev.vepo.jsonata.functions.data;

public final class DataInspectors {

    private static DataInspector defaultInspector;

    private DataInspectors() {}

    public static DataInspector defaultInspector() {
        if (defaultInspector == null) {
            throw new IllegalStateException("No default DataInspector registered");
        }
        return defaultInspector;
    }

    public static void setDefault(DataInspector inspector) {
        defaultInspector = inspector;
    }
}
