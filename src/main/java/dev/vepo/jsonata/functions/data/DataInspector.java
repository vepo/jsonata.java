package dev.vepo.jsonata.functions.data;

/**
 * Domain port for structural operations on {@link Data} during evaluation.
 * <p>
 * Lives in the domain layer so mappings and built-ins can copy, merge, remove, and
 * replace values without depending on Jackson or other infrastructure types.
 * Concrete adapters (for example {@link dev.vepo.jsonata.functions.json.MutableJacksonDataInspector}
 * and {@link dev.vepo.jsonata.functions.json.ImmutableJacksonDataInspector}) implement
 * this contract for a particular backing store.
 * <p>
 * Mutable implementations update {@code target} in place; immutable implementations
 * return new {@link Data} instances from the functional variants.
 *
 * @see Data#inspector()
 * @see dev.vepo.jsonata.functions.json.JsonFactory
 */
public interface DataInspector {

    /**
     * Whether in-place field updates ({@link #mergeFields}, {@link #removeFields}) are supported.
     *
     * @return {@code true} when callers may mutate backing storage directly
     */
    boolean mutableValues();

    /**
     * Produces a deep copy of {@code data} suitable as a transform working copy.
     *
     * @param data value to copy; may be {@code null}
     * @return deep copy, or {@link EmptyData} when {@code data} is {@code null} or empty
     */
    Data copy(Data data);

    /**
     * Whether {@code data} refers to a mutable object node in the backing store.
     *
     * @param data value to inspect
     * @return {@code true} when in-place object updates are possible on {@code data}
     */
    boolean isMutableObject(Data data);

    /**
     * Merges fields from {@code update} into {@code target} in place.
     *
     * @param target object to update; must be object-shaped for mutable inspectors
     * @param update source object whose fields are merged in
     * @throws dev.vepo.jsonata.exception.JSONataException when {@code target} is not an object
     *         or when the inspector does not allow mutation
     */
    void mergeFields(Data target, Data update);

    /**
     * Removes named fields from {@code target} in place.
     *
     * @param target object to update
     * @param fieldNames field names to remove
     * @throws dev.vepo.jsonata.exception.JSONataException when mutation is not allowed
     */
    void removeFields(Data target, Iterable<String> fieldNames);

    /**
     * Merges {@code update} into {@code target} and returns the result.
     * Mutable inspectors mutate {@code target} and return it; immutable inspectors return a new value.
     *
     * @param target object to update
     * @param update source object whose fields are merged in
     * @return updated data (same instance or a new one, depending on the inspector)
     * @throws dev.vepo.jsonata.exception.JSONataException when merge is not possible
     */
    Data merged(Data target, Data update);

    /**
     * Returns {@code target} without the named fields.
     * Mutable inspectors mutate {@code target}; immutable inspectors return a new value.
     *
     * @param target object to update
     * @param fieldNames field names to remove
     * @return updated data (same instance or a new one, depending on the inspector)
     */
    Data withoutFields(Data target, Iterable<String> fieldNames);

    /**
     * Replaces {@code current} with {@code replacement} within {@code root}, returning a new root value.
     *
     * @param root tree containing {@code current}
     * @param current node to replace (identity match in the backing store)
     * @param replacement substitute value
     * @return root data with the substitution applied
     * @throws dev.vepo.jsonata.exception.JSONataException when {@code current} is not found under {@code root}
     */
    Data replaceNode(Data root, Data current, Data replacement);
}
