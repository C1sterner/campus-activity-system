package edu.campus.service;

import edu.campus.common.ApiException;

public final class ReviewValidation {
    private ReviewValidation() {}
    public static void check(String result,String opinion) {
        if (!"1".equals(result) && !"2".equals(result)) {
            throw ApiException.bad("审核结果只能是通过或驳回");
        }
        if (opinion != null && opinion.length() > 500) {
            throw ApiException.bad("审核意见不能超过500字符");
        }
        if ("2".equals(result) && (opinion == null || opinion.isBlank())) {
            throw ApiException.bad("驳回时必须填写审核原因");
        }
    }
}
