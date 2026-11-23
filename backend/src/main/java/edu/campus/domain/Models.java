package edu.campus.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public final class Models {
    private Models() {}
    public static class User {
        public Long userId;
        public String userName;
        @JsonIgnore public String password;
        public String realName;
        public String role;
        public String email;
        public String phone;
        public String status;
        public LocalDateTime createTime;
        public String loginIp;
        public LocalDateTime loginDate;
    }
    public static class Activity {
        public Integer activityId;
        public String title;
        public String content;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String formSchema;
        public Long reviewerId;
        public String reviewerName;
        public String status;
        public Long creatorId;
        public LocalDateTime createTime;
    }
    public static class Registration {
        public Integer regId;
        public Integer activityId;
        public Long userId;
        public String formData;
        @JsonIgnore public String attachmentPath;
        public String status;
        public Integer version;
        public LocalDateTime submitTime;
        public LocalDateTime updateTime;
        public String activityTitle;
        public String studentName;
        public String latestOpinion;
    }
    public static class ReviewRecord {
        public Integer reviewId;
        public Integer regId;
        public Long reviewerId;
        public String result;
        public String opinion;
        public LocalDateTime reviewTime;
    }
    public record ReviewOutcome(int regId, String status, int version) {}
}
