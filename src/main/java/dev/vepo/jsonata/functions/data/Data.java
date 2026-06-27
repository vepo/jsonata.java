package dev.vepo.jsonata.functions.data;

import static dev.vepo.jsonata.functions.json.JsonFactory.fromString;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.regex.RegExp;

/**
 * Domain representation of a JSON value during expression evaluation.
 * <p>
 * {@link Mapping#map(Data, Data)} threads two values through the evaluator:
 * <em>original</em> ({@code $$}, the root input document) and <em>current</em>
 * ({@code $}, the focus at the current step). Methods on this interface operate on
 * the receiver — typically the <em>current</em> context passed into a mapping step.
 * Callers that need the root document hold it separately and pass it alongside
 * {@code current} to nested mappings.
 * <p>
 * <strong>Empty and absent values.</strong> JSONata's empty sequence (no match,
 * not JSON {@code null}) is represented by {@link EmptyData}. Navigation that finds
 * nothing ({@link #get(String)} on a missing field, {@link #at(int)} out of range on
 * some subtypes) returns empty data; {@link #isEmpty()} is {@code true} only for
 * that sentinel. JSON {@code null} is a scalar {@link ObjectData} and is not empty.
 * {@link #toJson()} may return {@code null} for values with no JSON projection
 * (empty sequence, functions, tail-call thunks).
 * <p>
 * Immutability depends on the concrete subtype and the session {@link DataInspector};
 * structural updates go through {@link #inspector()}, not through this interface directly.
 *
 * @see Mapping#map(Data, Data)
 * @see EmptyData
 * @see DataInspector
 */
public interface Data {

    /**
     * Parses a JSON text document into domain {@link Data}.
     *
     * @param contents JSON text; must be valid JSON
     * @return root value as {@link ObjectData} or {@link ArrayData}
     * @throws dev.vepo.jsonata.exception.JSONataException if {@code contents} is not valid JSON
     */
    public static Data load(String contents) {
        return fromString(contents);
    }

    /**
     * Collects all immediate property values of an object context (wildcard {@code *} semantics).
     * On non-objects returns {@code this} unchanged.
     *
     * @return grouped sequence of child values, or {@code this} when not applicable
     */
    Data all();

    /**
     * Selects the element at a zero-based index in an array or grouped sequence.
     *
     * @param index zero-based position
     * @return element at {@code index}, or {@link EmptyData} when indexing is not defined for this value
     */
    Data at(int index);

    /**
     * Invokes {@code action} for each immediate child used in descendant navigation.
     * No-op when this value has no navigable children.
     *
     * @param action consumer invoked once per child; must not be {@code null}
     */
    void forEachChild(Consumer<Data> action);

    /**
     * Navigates to a named field on an object, or projects that field across array elements.
     *
     * @param fieldName property name
     * @return field value, grouped multi-match result, or {@link EmptyData} when the field is absent
     */
    Data get(String fieldName);

    /**
     * Reports whether a named field is present on this value (or on any element of an array).
     *
     * @param fieldName property name to test
     * @return {@code true} if the field exists for navigation purposes
     */
    boolean hasField(String fieldName);

    /**
     * Whether this value is a JSON array ({@link ArrayData}).
     *
     * @return {@code true} for array-backed data
     */
    default boolean isArray() {
        return false;
    }

    /**
     * Whether this value is a grouped multi-match sequence ({@link GroupedData}).
     *
     * @return {@code true} for list-shaped grouped results
     */
    default boolean isList() {
        return false;
    }

    /**
     * Whether this value is the JSONata empty sequence ({@link EmptyData}).
     *
     * @return {@code true} only for the empty sentinel, not for JSON {@code null}
     */
    default boolean isEmpty() {
        return false;
    }

    /**
     * Whether this value is object-shaped ({@link ObjectData}).
     *
     * @return {@code true} for object-backed data
     */
    default boolean isObject() {
        return false;
    }

    /**
     * Whether this value is a regular-expression literal ({@link RegexData}).
     *
     * @return {@code true} for regex literals
     */
    default boolean isRegex() {
        return false;
    }

    /**
     * Sequence length: array element count, grouped match count, or {@code 1} for a scalar object.
     *
     * @return non-negative length in JSONata navigation terms
     */
    int length();

    /**
     * Jackson projection of this value for adapters and serialization.
     *
     * @return backing {@link JsonNode}, or {@code null} when this value has no JSON representation
     */
    JsonNode toJson();

    /**
     * Caller-facing evaluation result for this value.
     *
     * @return {@link JSONataResult} suitable for {@code JSONata.evaluate} consumers
     */
    JSONataResult toNode();

    /**
     * Compiles this value as a JSONata regular expression.
     *
     * @return compiled {@link RegExp} for {@link RegexData}
     * @throws UnsupportedOperationException when this value is not a regex literal
     */
    default RegExp asRegex() {
        throw new UnsupportedOperationException("Unimplemented method 'asRegex'");
    }

    /**
     * Maps each navigable element through {@code function}, producing a new grouped sequence.
     *
     * @param function transforms each element's {@link JsonNode}; must not be {@code null}
     * @return mapped {@link Data}, often {@link GroupedData}
     */
    Data map(Function<JsonNode, Data> function);

    /**
     * Streams the sequence of values represented by this data (self for scalars, elements for arrays/groups).
     *
     * @return stream of constitutent {@link Data} values
     */
    default Stream<Data> stream() {
        return Stream.of(this);
    }

    /**
     * Returns the {@link DataInspector} associated with this value's backing store.
     *
     * @return inspector used for copy, merge, and replace during transform and similar operations
     */
    default DataInspector inspector() {
        return DataInspectors.defaultInspector();
    }
}
