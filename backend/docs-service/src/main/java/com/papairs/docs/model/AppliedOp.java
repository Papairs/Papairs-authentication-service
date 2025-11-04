package com.papairs.docs.model;

public class AppliedOp {
    public String type;     // insert | delete | snapshot
    public int version;     // server version after apply
    public String clientId; // source
    public String opId;     // echoes client op for ack
    public int pos;
    public String text;     // for insert
    public int length;      // for delete
    public String content;  // for snapshot
}
