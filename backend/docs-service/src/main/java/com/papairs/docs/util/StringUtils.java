package com.papairs.docs.util;

public class StringUtils {

    /**
     * Convert empty/blank strings to null
     * Use this for optional foreign keys {@code (folder_id, parent_folder_id, etc.)}
     * @param value String value that might be empty
     * @return null if blank, otherwise the trimmed value
     */
    public static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}