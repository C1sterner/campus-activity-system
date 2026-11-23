package edu.campus.service;

import edu.campus.common.ApiException;
import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final AppMapper mapper;
    public ReviewService(AppMapper mapper) { this.mapper=mapper; }

    @Transactional
    public ReviewOutcome submitReview(User actor, int regId, String result, String opinion, int version) {
        ReviewValidation.check(result, opinion);
        Registration registration = mapper.registration(regId);
        if (registration == null) {
            throw ApiException.missing();
        }
        Activity activity = mapper.activity(registration.activityId);
        if (actor == null || !"teacher".equals(actor.role) || activity == null
                || !actor.userId.equals(activity.reviewerId)) {
            throw ApiException.forbidden();
        }
        String status;
        if ("1".equals(result)) {
            status = "2";
        } else {
            status = "3";
        }
        int changed = mapper.updateReviewStatus(regId, status, version);
        if (changed != 1) {
            throw ApiException.conflict("该报名记录已被处理，请刷新列表查看最新状态");
        }
        ReviewRecord record = new ReviewRecord();
        record.regId=regId; record.reviewerId=actor.userId;
        record.result=result; record.opinion=opinion == null ? null : opinion.trim();
        mapper.insertReview(record);
        return new ReviewOutcome(regId, status, version + 1);
    }
}
