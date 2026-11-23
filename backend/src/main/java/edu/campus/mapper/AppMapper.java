package edu.campus.mapper;

import edu.campus.domain.Models.*;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AppMapper {
    @Select("SELECT * FROM sys_user WHERE user_name=#{name}") User userByName(String name);
    @Select("SELECT * FROM sys_user WHERE user_id=#{id}") User userById(long id);
    @Select("SELECT * FROM sys_user WHERE email=#{email}") User userByEmail(String email);
    @Select("SELECT COUNT(*) FROM sys_user") int userCount();
    @Select("SELECT COUNT(*) FROM sys_user WHERE role='admin' AND status='0'") int activeAdminCount();
    @Select("SELECT * FROM sys_user ORDER BY user_id DESC LIMIT 200") List<User> users();
    @Select("SELECT * FROM sys_user WHERE role='teacher' AND status='0' ORDER BY user_id") List<User> reviewers();
    @Insert("INSERT INTO sys_user(user_name,password,real_name,role,email,phone,status) VALUES(#{userName},#{password},#{realName},#{role},#{email},#{phone},#{status})")
    @Options(useGeneratedKeys=true,keyProperty="userId") int insertUser(User user);
    @Update("UPDATE sys_user SET real_name=#{realName},email=#{email},phone=#{phone},password=#{password},role=#{role},status=#{status} WHERE user_id=#{userId}") int updateUser(User user);
    @Update("UPDATE sys_user SET login_ip=#{ip},login_date=CURRENT_TIMESTAMP WHERE user_id=#{id}") int loginInfo(@Param("id") long id,@Param("ip") String ip);
    @Select("SELECT a.*,u.real_name AS reviewer_name FROM activity a LEFT JOIN sys_user u ON a.reviewer_id=u.user_id WHERE a.activity_id=#{id}") Activity activity(int id);
    @Select("SELECT a.*,u.real_name AS reviewer_name FROM activity a LEFT JOIN sys_user u ON a.reviewer_id=u.user_id WHERE a.creator_id=#{creator} ORDER BY a.activity_id DESC") List<Activity> ownedActivities(long creator);
    @Select("SELECT a.*,u.real_name AS reviewer_name FROM activity a LEFT JOIN sys_user u ON a.reviewer_id=u.user_id WHERE a.status='1' ORDER BY a.activity_id DESC") List<Activity> published();
    @Select("SELECT COUNT(*) FROM activity WHERE title=#{title} AND status<>'2' AND activity_id<>#{exclude} AND start_time<#{end} AND end_time>#{start}")
    int conflictingActivity(@Param("title") String title,@Param("start") java.time.LocalDateTime start,@Param("end") java.time.LocalDateTime end,@Param("exclude") int exclude);
    @Insert("INSERT INTO activity(title,content,start_time,end_time,form_schema,reviewer_id,status,creator_id) VALUES(#{title},#{content},#{startTime},#{endTime},#{formSchema},#{reviewerId},#{status},#{creatorId})")
    @Options(useGeneratedKeys=true,keyProperty="activityId") int insertActivity(Activity activity);
    @Update("UPDATE activity SET title=#{title},content=#{content},start_time=#{startTime},end_time=#{endTime},form_schema=#{formSchema},reviewer_id=#{reviewerId},status=#{status} WHERE activity_id=#{activityId} AND creator_id=#{creatorId} AND status='0'") int updateActivity(Activity activity);
    @Update("UPDATE activity SET status='2' WHERE activity_id=#{id} AND creator_id=#{creator} AND status='1'") int offline(@Param("id") int id,@Param("creator") long creator);
    @Delete("DELETE FROM activity WHERE activity_id=#{id} AND creator_id=#{creator} AND status='0'") int deleteDraftActivity(@Param("id") int id,@Param("creator") long creator);
    @Select("SELECT * FROM registration WHERE reg_id=#{id}") Registration registration(int id);
    @Select("SELECT * FROM registration WHERE activity_id=#{activity} AND user_id=#{user}") Registration registrationFor(@Param("activity") int activity,@Param("user") long user);
    @Insert("INSERT INTO registration(activity_id,user_id,form_data,attachment_path,status,version,submit_time) VALUES(#{activityId},#{userId},#{formData},#{attachmentPath},#{status},#{version},#{submitTime})")
    @Options(useGeneratedKeys=true,keyProperty="regId") int insertRegistration(Registration registration);
    @Update("UPDATE registration SET form_data=#{formData},update_time=CURRENT_TIMESTAMP WHERE reg_id=#{regId} AND user_id=#{userId} AND status='0'") int saveDraft(Registration registration);
    @Update("UPDATE registration SET form_data=#{formData},attachment_path=#{attachmentPath},status='1',version=version+1,submit_time=CURRENT_TIMESTAMP,update_time=CURRENT_TIMESTAMP WHERE reg_id=#{regId} AND user_id=#{userId} AND status='0'") int submitDraft(Registration registration);
    @Select("SELECT r.*,a.title AS activity_title,u.real_name AS student_name,(SELECT rr.opinion FROM review_record rr WHERE rr.reg_id=r.reg_id ORDER BY rr.review_id DESC LIMIT 1) AS latest_opinion FROM registration r JOIN activity a ON a.activity_id=r.activity_id JOIN sys_user u ON u.user_id=r.user_id WHERE r.user_id=#{user} ORDER BY r.update_time DESC") List<Registration> mine(long user);
    @Select("SELECT r.*,a.title AS activity_title,u.real_name AS student_name FROM registration r JOIN activity a ON a.activity_id=r.activity_id JOIN sys_user u ON u.user_id=r.user_id WHERE a.reviewer_id=#{teacher} AND r.status='1' ORDER BY r.submit_time") List<Registration> pending(long teacher);
    @Update("UPDATE registration SET status=#{status},version=version+1,update_time=CURRENT_TIMESTAMP WHERE reg_id=#{regId} AND version=#{version} AND status='1'")
    int updateReviewStatus(@Param("regId") int regId,@Param("status") String status,@Param("version") int version);
    @Insert("INSERT INTO review_record(reg_id,reviewer_id,result,opinion) VALUES(#{regId},#{reviewerId},#{result},#{opinion})") int insertReview(ReviewRecord review);
    @Select("SELECT * FROM review_record WHERE reg_id=#{regId} ORDER BY review_id DESC") List<ReviewRecord> reviews(int regId);
    @Select("<script>SELECT r.*,a.title AS activity_title,u.real_name AS student_name FROM registration r JOIN activity a ON a.activity_id=r.activity_id JOIN sys_user u ON u.user_id=r.user_id WHERE r.status&lt;&gt;'0' <if test='activityId != null'>AND r.activity_id=#{activityId}</if> <if test='start != null'>AND r.submit_time&gt;=#{start}</if> <if test='end != null'>AND r.submit_time&lt;=#{end}</if> ORDER BY r.reg_id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Registration> registrations(@Param("activityId") Integer activityId,@Param("start") String start,@Param("end") String end,@Param("offset") int offset,@Param("limit") int limit);
    @Select("<script>SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN r.status='1' THEN 1 ELSE 0 END),0) AS pending,COALESCE(SUM(CASE WHEN r.status='2' THEN 1 ELSE 0 END),0) AS approved,COALESCE(SUM(CASE WHEN r.status='3' THEN 1 ELSE 0 END),0) AS rejected FROM registration r WHERE r.status&lt;&gt;'0' <if test='activityId != null'>AND r.activity_id=#{activityId}</if> <if test='start != null'>AND r.submit_time&gt;=#{start}</if> <if test='end != null'>AND r.submit_time&lt;=#{end}</if></script>")
    Map<String,Object> statistics(@Param("activityId") Integer activityId,@Param("start") String start,@Param("end") String end);
    @Select("SELECT config_key,config_value FROM sys_config ORDER BY config_key") List<Map<String,Object>> configs();
    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key}") String config(String key);
    @Insert("INSERT INTO sys_config(config_key,config_value) VALUES(#{key},#{value})") int insertConfig(@Param("key") String key,@Param("value") String value);
    @Update("UPDATE sys_config SET config_value=#{value},update_time=CURRENT_TIMESTAMP WHERE config_key=#{key}") int updateConfig(@Param("key") String key,@Param("value") String value);
    @Insert("INSERT INTO operation_log(operator_id,op_type,op_content,op_ip) VALUES(#{actor},#{type},#{content},#{ip})") int log(@Param("actor") long actor,@Param("type") String type,@Param("content") String content,@Param("ip") String ip);
    @Select("SELECT l.log_id,l.op_type,l.op_content,l.op_ip,l.op_time,u.user_name FROM operation_log l JOIN sys_user u ON l.operator_id=u.user_id ORDER BY l.log_id DESC LIMIT 100") List<Map<String,Object>> logs();
}
