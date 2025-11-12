package com.papairs.docs.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.papairs.docs.annotation.Sanitize;

import java.io.IOException;

public class SanitizingDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    private final boolean trim;
    private final boolean blankToNull;

    public SanitizingDeserializer() {
        this.trim = false;
        this.blankToNull = false;
    }

    public SanitizingDeserializer(boolean trim, boolean blankToNull) {
        this.trim = trim;
        this.blankToNull = blankToNull;
    }

    /**
     * Creates a contextualized instance of the deserializer <p>
     * Jackson calls this method to obtain a configured deserializer instance for each
     * specific field that uses it. This method inspects the field for a {@link Sanitize}
     * annotation and creates a new {@code SanitizingDeserializer} instance with the
     * settings from that annotation.
     *
     * @param context The deserialization context
     * @param property The property (field or method) being deserialized
     * @return A configured {@link JsonDeserializer} instance to be used for the property
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        Sanitize sanitize = property.getAnnotation(Sanitize.class);
        if (sanitize != null) {
            return new SanitizingDeserializer(sanitize.trim(), sanitize.blankToNull());
        }
        return this;
    }

    /**
     * Deserializes and sanitizes a JSON string value <p>
     * This method applies the sanitization rules (trimming and blank-to-null conversion)
     * that were configured for this instance during the contextualization phase
     * @param p The JsonParser used for reading JSON content
     * @param context The DeserializationContext for the current deserialization operation
     * @return The sanitized string, which may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext context) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }

        // If neither flag is set, no sanitization is needed
        if (!trim && !blankToNull) {
            return value;
        }

        // 1. Trim the string. Trimming is necessary for a reliable blank check,
        // so we perform it if either 'trim' or 'blankToNull' is enabled
        String processedValue = value.trim();

        // 2. Convert to null if the result is blank
        if (blankToNull && processedValue.isEmpty()) {
            return null;
        }

        return processedValue;
    }
}