package com.papairs.docs.model;

public class CursorPosition {
    public int from;
    public int to;
    public String userId;
    public String userName;
    
    public CursorPosition() {}
    
    public CursorPosition(int from, int to, String userId, String userName) {
        this.from = from;
        this.to = to;
        this.userId = userId;
        this.userName = userName;
    }
}
