package com.papairs.docs.dto.request;

import java.util.List;

public class YjsDocumentRequest {
    private List<Integer> ydoc; // Y.js document state as array of integers

    public List<Integer> getYdoc() {
        return ydoc;
    }

    public void setYdoc(List<Integer> ydoc) {
        this.ydoc = ydoc;
    }
}
