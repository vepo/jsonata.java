module jsonata.java {
    requires com.fasterxml.jackson.databind;
    requires org.antlr.antlr4.runtime;
    requires org.apache.commons.text;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires java.scripting;
    requires org.openjdk.nashorn;

    exports dev.vepo.jsonata;
}