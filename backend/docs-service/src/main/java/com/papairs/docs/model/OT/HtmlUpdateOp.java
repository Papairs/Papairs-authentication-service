package com.papairs.docs.model.OT;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HTML-based operation that carries the full HTML content.
 * This preserves formatting by avoiding text-based transformations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HtmlUpdateOp extends Op {
    public long timestamp;  // Optional timestamp from frontend
    
    public HtmlUpdateOp() {
        this.type = "html-update";
    }
}
