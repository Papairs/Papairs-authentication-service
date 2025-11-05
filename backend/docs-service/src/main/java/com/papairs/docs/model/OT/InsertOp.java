package com.papairs.docs.model.OT;

public class InsertOp extends Op {
    public String text;
    
    public InsertOp() { 
        this.type = "insert"; 
    }
}