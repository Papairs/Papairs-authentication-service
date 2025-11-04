package com.papairs.docs.model;

public class DeleteOp extends Op {
    public int length;
    public DeleteOp() { this.type = "delete"; }
}
