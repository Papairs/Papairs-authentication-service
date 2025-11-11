package com.papairs.docs.util;

import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;

public class OtTransform {
    private static int tiebreak(String aClient, String aId, String bClient, String bId){
        String a = (aClient==null?"":aClient)+":"+(aId==null?"":aId);
        String b = (bClient==null?"":bClient)+":"+(bId==null?"":bId);
        return a.compareTo(b);
    }

    public static Op transform(Op A, Op B){
        if (A == null || B == null) return A;

        if ("insert".equals(A.type) && "insert".equals(B.type)) {
            InsertOp a = (InsertOp)A; InsertOp b = (InsertOp)B;
            if (a.pos > b.pos || (a.pos == b.pos && tiebreak(a.clientId,a.opId,b.clientId,b.opId) > 0)) {
                a.pos += (b.text != null ? b.text.length() : 0);
            }
            return a;
        }

        if ("insert".equals(A.type) && "delete".equals(B.type)) {
            InsertOp a = (InsertOp)A; DeleteOp b = (DeleteOp)B;
            if (a.pos > b.pos) {
                a.pos -= Math.min(b.length, a.pos - b.pos);
            }
            return a;
        }

        if ("delete".equals(A.type) && "insert".equals(B.type)) {
            DeleteOp a = (DeleteOp)A; InsertOp b = (InsertOp)B;
            if (a.pos >= b.pos) {
                a.pos += (b.text != null ? b.text.length() : 0);
            }
            return a;
        }

        if ("delete".equals(A.type) && "delete".equals(B.type)) {
            DeleteOp a = (DeleteOp)A; DeleteOp b = (DeleteOp)B;
            if (b.pos + b.length <= a.pos) {
                a.pos -= b.length;
            } else if (b.pos >= a.pos + a.length) {
            } else {
                int overlapStart = Math.max(a.pos, b.pos);
                int overlapEnd = Math.min(a.pos + a.length, b.pos + b.length);
                int overlap = Math.max(0, overlapEnd - overlapStart);
                a.length -= overlap;
                if (b.pos < a.pos) a.pos = b.pos;
            }
            return a;
        }

        return A;
    }
}
