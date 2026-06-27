package dev.vepo.jsonata.functions.data;

/**
 * Registry for the fallback {@link DataInspector} used when no evaluation session is active.
 * <p>
 * Domain types call {@link #defaultInspector()} from {@link Data#inspector()} defaults.
 * Infrastructure bootstraps the default during {@link dev.vepo.jsonata.functions.json.JsonFactory}
 * static initialization; embedders may replace it via {@link #setDefault(DataInspector)} before evaluation.
 */
public final class DataInspectors {

    private static DataInspector defaultInspector;

    private DataInspectors() {}

    /**
     * Returns the registered default inspector.
     *
     * @return active default {@link DataInspector}
     * @throws IllegalStateException if no inspector has been registered yet
     */
    public static DataInspector defaultInspector() {
        if (defaultInspector == null) {
            throw new IllegalStateException("No default DataInspector registered");
        }
        return defaultInspector;
    }

    /**
     * Registers the fallback inspector for {@link #defaultInspector()}.
     * Typically invoked once at startup by infrastructure adapters.
     *
     * @param inspector inspector implementation; must not be {@code null}
     */
    public static void setDefault(DataInspector inspector) {
        defaultInspector = inspector;
    }
}
