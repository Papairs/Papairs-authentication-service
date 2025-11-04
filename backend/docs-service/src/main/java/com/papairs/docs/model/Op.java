package com.papairs.docs.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InsertOp.class, name = "insert"),
    @JsonSubTypes.Type(value = DeleteOp.class, name = "delete")
})
public abstract class Op {
    public String type;      // "insert" | "delete"
    public int pos;          // position in doc
    public int baseVersion;  // client's known doc version
    public String clientId;  // sender id
    public String opId;      // client-unique op id
}
