package dev.vepo.jsonata;

import java.util.List;

/**
 * Represents a node in an expression tree.
 */
public interface JSONataResult {

    /**
     * Converts the node to its textual representation.
     *
     * @return the textual representation of the node
     */
    String asText();

    /**
     * Converts the node to its integer representation.
     *
     * @return the integer representation of the node
     */
    int asInt();

    /**
     * Converts the node to its boolean representation.
     *
     * @return the boolean representation of the node
     */
    boolean asBoolean();

    /**
     * Checks if the node is null.
     *
     * @return true if the node is null, false otherwise
     */
    boolean isNull();

    /**
     * Checks if the node is empty.
     *
     * @return true if the node is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Retrieves the multi-value representation of the node.
     *
     * @return the multi-value representation of the node
     */
    Multi multi();

    /**
     * Represents a multi-value node in an expression tree.
     */
    public interface Multi {

        /**
         * Converts the multi-value node to a list of textual representations.
         *
         * @return the list of textual representations of the multi-value node
         */
        List<String> asText();

        /**
         * Converts the multi-value node to a list of integer representations.
         *
         * @return the list of integer representations of the multi-value node
         */
        List<Integer> asInt();

        /**
         * Converts the multi-value node to a list of boolean representations.
         *
         * @return the list of boolean representations of the multi-value node
         */
        List<Boolean> asBoolean();
    }
}
