package com.papairs.docs.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.papairs.docs.util.SanitizingDeserializer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a {@link String} field should be sanitized during JSON deserialization <p>
 * This annotation works in conjunction with {@link SanitizingDeserializer} to provide
 * configurable, declarative sanitization for incoming data transfer objects (DTOs).
 * It allows for common data cleaning operations like trimming whitespace and converting
 * blank values to {@code null} <p>
 * The sanitization operations are always performed in a consistent, logical order:
 * first trimming, then blank-to-null conversion.
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class CreateUserRequest {
 *
 *     // Trim whitespace but do not allow a blank value.
 *     @Sanitize(trim = true)
 *     @NotBlank
 *     private String username;
 *
 *     // Allow an optional nickname. If provided as an empty or whitespace string,
 *     // it will be stored as null.
 *     @Sanitize(trim = true, blankToNull = true)
 *     private String nickname;
 * }
 * }</pre>
 *
 * @see SanitizingDeserializer
 * @see JsonDeserialize
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = SanitizingDeserializer.class)
public @interface Sanitize {

    /**
     * If true, trims leading and trailing whitespace from the string
     * This is applied before blankToNull
     */
    boolean trim() default false;

    /**
     * If true, converts any blank string (null, empty or whitespace-only) to a null value
     */
    boolean blankToNull() default false;
}