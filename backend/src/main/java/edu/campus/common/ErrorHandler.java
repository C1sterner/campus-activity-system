package edu.campus.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> business(ApiException e) {
        return ResponseEntity.status(e.status).body(ApiResponse.error(e.code, e.getMessage()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> size() {
        return ResponseEntity.status(413).body(ApiResponse.error("FILE_413", "单个文件不能超过20MiB"));
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse> invalid(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_400", "参数格式不正确"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> duplicate() {
        return ResponseEntity.status(409).body(ApiResponse.error("STATE_409", "数据重复或存在关联记录，请刷新后重试"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> other(Exception e) {
        LOG.error("Request failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ApiResponse.error("SERVER_500", "操作失败，请稍后重试"));
    }
}
