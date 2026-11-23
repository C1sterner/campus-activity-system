package edu.campus.common;

public class ApiException extends RuntimeException {
    public final int status;
    public final String code;
    public ApiException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public static ApiException bad(String message) { return new ApiException(400, "VALIDATION_400", message); }
    public static ApiException forbidden() { return new ApiException(403, "AUTH_403", "无权操作此数据"); }
    public static ApiException missing() { return new ApiException(404, "RESOURCE_404", "记录不存在"); }
    public static ApiException conflict(String message) { return new ApiException(409, "CONFLICT_409", message); }
}
