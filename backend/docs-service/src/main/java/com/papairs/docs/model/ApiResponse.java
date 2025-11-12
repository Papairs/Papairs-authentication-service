package com.papairs.docs.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private boolean success;
    private String status;
    private String message;
    private Object data;

    public ApiResponse() {}

    public ApiResponse(String status, String message, Object data) {
        this.success = true;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String status, String message) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, null, message);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}