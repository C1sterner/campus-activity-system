package edu.campus.common;

public record ApiResponse(boolean success, String msg, Object data, String code) {
    public static ApiResponse ok(Object data) { return new ApiResponse(true, "操作成功", data, "OK"); }
    public static ApiResponse error(String code, String msg) { return new ApiResponse(false, msg, null, code); }
}
