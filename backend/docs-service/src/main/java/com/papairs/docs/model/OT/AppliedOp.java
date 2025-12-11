package com.papairs.docs.model.OT;

import com.papairs.docs.model.CursorPosition;
import java.util.Map;

public class AppliedOp {
    public String type;
    public int version;
    public String clientId;
    public String opId;
    public String content;
    public String htmlContent;
    public CursorPosition cursor;
    public Map<String, CursorPosition> cursors;
}