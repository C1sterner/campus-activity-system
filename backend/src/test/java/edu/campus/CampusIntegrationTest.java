package edu.campus;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.campus.auth.SessionService;
import edu.campus.common.ApiException;
import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import edu.campus.service.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:campus-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}","spring.datasource.username=${TEST_DB_USER:sa}","spring.datasource.password=${TEST_DB_PASSWORD:}","app.seed-demo=false","app.storage-dir=target/test-uploads"})
@AutoConfigureMockMvc
class CampusIntegrationTest {
    @MockitoSpyBean AppMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired RegistrationService registrations;
    @Autowired ReviewService reviewService;
    @Autowired SessionService sessions;
    @Autowired ObjectMapper json;
    @Autowired FileStorage files;
    @Autowired MockMvc mvc;
    User student,teacher,outsider;Activity activity;
    @BeforeEach void fixtures(){
        for(String table:List.of("review_record","registration","operation_log","activity","sys_user"))jdbc.update("DELETE FROM "+table);
        student=user("test_student","student");teacher=user("test_teacher","teacher");outsider=user("test_teacher2","teacher");
        User creator=user("test_activity","activity_admin");
        activity=new Activity();activity.title="TEST 活动";activity.content="测试内容";activity.creatorId=creator.userId;activity.reviewerId=teacher.userId;activity.status="1";
        activity.startTime=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(1);activity.endTime=activity.startTime.plusDays(3);
        activity.formSchema="{\"version\":1,\"fields\":[{\"key\":\"name\",\"label\":\"姓名\",\"type\":\"text\",\"required\":true}]}";mapper.insertActivity(activity);
    }
    User user(String name,String role){User u=new User();u.userName=name;u.realName=name;u.email=name+"@example.test";u.role=role;u.status="0";u.password="test-only-hash";mapper.insertUser(u);return u;}
    Registration submit(){return registrations.save(student,activity.activityId,json.createObjectNode().put("name","测试同学"),false,null);}
    String token(User user){return "Bearer "+sessions.issue(user).get("token");}
    @Test void databaseUniqueIndexRejectsDuplicate(){submit();ApiException error=assertThrows(ApiException.class,this::submit);assertEquals(409,error.status);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM registration",Integer.class));}
    @Test void duplicateConcurrentSubmissionOnlyOneWins() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
        Callable<Boolean> task=()->{go.await();try{submit();return true;}catch(ApiException e){assertEquals(409,e.status);return false;}};
        try{Future<Boolean>a=executor.submit(task),b=executor.submit(task);go.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM registration",Integer.class));}finally{executor.shutdownNow();}
    }
    @Test void concurrentReviewOnlyOneWins() throws Exception {
        Registration r=submit();ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
        Callable<Boolean> task=()->{go.await();try{reviewService.submitReview(teacher,r.regId,"1",null,0);return true;}catch(ApiException e){assertEquals(409,e.status);return false;}};
        try{Future<Boolean>a=executor.submit(task),b=executor.submit(task);go.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals("2",mapper.registration(r.regId).status);assertEquals(1,mapper.reviews(r.regId).size());}finally{executor.shutdownNow();}
    }
    @Test void reviewRecordFailureRollsBackStatus(){
        Registration r=submit();doThrow(new org.springframework.dao.DataAccessResourceFailureException("test-only injected failure")).when(mapper).insertReview(any());
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->reviewService.submitReview(teacher,r.regId,"1",null,0));
        assertEquals("1",mapper.registration(r.regId).status);assertEquals(0,mapper.registration(r.regId).version);assertTrue(mapper.reviews(r.regId).isEmpty());
    }
    @Test void oldDraftCannotOverwriteSubmittedRecord(){
        var data=json.createObjectNode().put("name","草稿");registrations.save(student,activity.activityId,data,true,null);
        Registration r=submit();assertEquals(1,r.version);assertThrows(ApiException.class,()->registrations.save(student,activity.activityId,data,true,null));assertEquals("1",mapper.registration(r.regId).status);assertTrue(mapper.registration(r.regId).formData.contains("测试同学"));
    }
    @Test void otherStudentAndTeacherCannotRead() throws Exception {
        Registration r=submit();User other=user("test_other_student","student");
        mvc.perform(get("/api/my-registrations/"+r.regId).header("Authorization",token(other))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reviews/"+r.regId).header("Authorization",token(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reviews/"+r.regId).header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$.data.registration.version").value(0)).andExpect(jsonPath("$.data.registration.attachmentPath").doesNotExist());
    }
    @Test void studentCannotCallAdmin() throws Exception {mvc.perform(get("/api/admin/users").header("Authorization",token(student))).andExpect(status().isForbidden());}
    @Test void disabledAccountTokenIsRefused() throws Exception {String token=token(student);student.status="1";mapper.updateUser(student);mvc.perform(get("/api/profile").header("Authorization",token)).andExpect(status().isUnauthorized());}
    @Test void deadlineIsCheckedOnServer(){activity.endTime=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusSeconds(1);jdbc.update("UPDATE activity SET end_time=? WHERE activity_id=?",activity.endTime,activity.activityId);assertThrows(ApiException.class,this::submit);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM registration",Integer.class));}
    @Test void fakeImageIsRejected(){MockMultipartFile f=new MockMultipartFile("attachment","proof.png","image/png","%PDF-1.7".getBytes());assertEquals(415,assertThrows(ApiException.class,()->files.save(f)).status);}
    @Test void overTwentyMiBIsRejected(){MockMultipartFile f=new MockMultipartFile("attachment","proof.pdf","application/pdf",new byte[20971521]);assertEquals(413,assertThrows(ApiException.class,()->files.save(f)).status);}
}
