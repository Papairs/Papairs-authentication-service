package com.papairs.docs.model.OT;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InsertOp.class, name = "insert"),
    @JsonSubTypes.Type(value = DeleteOp.class, name = "delete"),
    @JsonSubTypes.Type(value = HtmlUpdateOp.class, name = "html-update")
})
public abstract class Op {
    public String type;
    public int pos;
    public int baseVersion;
    public String clientId;
    public String opId;
    public String htmlContent; // Full HTML content for HTML-based operations
}