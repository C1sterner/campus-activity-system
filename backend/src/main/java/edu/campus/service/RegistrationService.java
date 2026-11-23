package edu.campus.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.campus.common.Access;
import edu.campus.common.ApiException;
import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final AppMapper mapper;
    private final FormService forms;
    public RegistrationService(AppMapper mapper, FormService forms) { this.mapper=mapper; this.forms=forms; }
    public Activity openActivity(int id) {
        Activity a=mapper.activity(id);
        if(a==null || !"1".equals(a.status)) throw ApiException.bad("活动不存在或尚未发布");
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if(now.isBefore(a.startTime)||!now.isBefore(a.endTime))throw ApiException.bad("当前不在报名时间内");
        return a;
    }
    @Transactional
    public Registration save(User actor,int activityId,JsonNode data,boolean draft,String attachment) {
        Access.role(actor,"student");
        Activity a=openActivity(activityId);
        JsonNode schema=forms.parse(a.formSchema);
        forms.validateData(schema,data,draft);
        if(!draft&&schema.path("attachmentRequired").asBoolean()&&attachment==null)throw ApiException.bad("请上传佐证材料");
        Registration old=mapper.registrationFor(activityId,actor.userId);
        if(old!=null&&!"0".equals(old.status))throw duplicate();
        Registration r=old==null?new Registration():old;
        r.activityId=activityId;r.userId=actor.userId;r.formData=forms.stringify(data);
        r.attachmentPath=attachment;
        if(old==null) {
            r.status=draft?"0":"1";r.version=0;r.submitTime=draft?null:LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
            try { mapper.insertRegistration(r); } catch(DuplicateKeyException e) { throw duplicate(); }
        } else {
            int changed=draft?mapper.saveDraft(r):mapper.submitDraft(r);
            if(changed!=1)throw duplicate();
        }
        return mapper.registration(r.regId);
    }
    private ApiException duplicate(){return new ApiException(409,"DUPLICATE_409","您已报名该活动，请勿重复提交");}
    public Registration readable(User actor,int regId) {
        Registration r=mapper.registration(regId);
        if(r==null)throw ApiException.missing();
        Activity a=mapper.activity(r.activityId);
        boolean owner="student".equals(actor.role)&&actor.userId.equals(r.userId);
        boolean reviewer="teacher".equals(actor.role)&&actor.userId.equals(a.reviewerId)&&!"0".equals(r.status);
        boolean activityAdmin="activity_admin".equals(actor.role)&&!"0".equals(r.status);
        if(!owner&&!reviewer&&!activityAdmin)throw ApiException.forbidden();
        return r;
    }
}
