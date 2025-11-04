package com.papairs.docs.model;

public class Message {
    public String action; // "join" | "op"
    public String docId;
    public Op op;         // present when action == "op"
}
