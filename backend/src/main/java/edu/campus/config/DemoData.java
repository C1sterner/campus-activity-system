package edu.campus.config;

import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoData implements ApplicationRunner {
    private final AppMapper mapper;private final BCryptPasswordEncoder passwords;private final boolean seed;private final String initialPassword;
    public DemoData(AppMapper mapper,BCryptPasswordEncoder passwords,@Value("${app.seed-demo}") boolean seed,@Value("${INIT_ADMIN_PASSWORD:}") String initialPassword){this.mapper=mapper;this.passwords=passwords;this.seed=seed;this.initialPassword=initialPassword;}
    @Override public void run(ApplicationArguments args){
        Map.of("upload.max_size_bytes","20971520","upload.allowed_extensions","jpg,png,pdf","export.max_rows","50000","registration.draft_interval_seconds","30").forEach((key,value)->{if(mapper.config(key)==null)mapper.insertConfig(key,value);});
        if(mapper.userCount()!=0)return;
        if(!seed){
            if(!initialPassword.isBlank()){
                if(initialPassword.length()<12||initialPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("INIT_ADMIN_PASSWORD must be 12 to 72 UTF-8 bytes");
                user("admin","系统管理员","admin",initialPassword);
            }
            return;
        }
        String pass="Demo1234!";
        user("admin1","系统管理员","admin",pass);
        User teacher=user("teacher1","林老师","teacher",pass);
        User activityAdmin=user("activity1","活动管理员","activity_admin",pass);
        user("student1","同学甲","student",pass);
        Activity a=new Activity();a.title="校园创新分享会";a.content="一起分享课程项目与创新点子。面向全校学生，填写基本信息后由指导教师审核。此活动用于课程演示。";
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai"));a.startTime=now.minusDays(1);a.endTime=now.plusDays(30);
        a.creatorId=activityAdmin.userId;a.reviewerId=teacher.userId;a.status="1";
        a.formSchema="{\"version\":1,\"attachmentRequired\":false,\"fields\":[{\"key\":\"studentNo\",\"label\":\"学号\",\"type\":\"text\",\"required\":true,\"maxLength\":32},{\"key\":\"college\",\"label\":\"学院\",\"type\":\"text\",\"required\":true,\"maxLength\":50},{\"key\":\"grade\",\"label\":\"年级\",\"type\":\"select\",\"required\":true,\"options\":[\"大一\",\"大二\",\"大三\",\"大四\"]},{\"key\":\"reason\",\"label\":\"参与理由\",\"type\":\"textarea\",\"required\":false,\"maxLength\":500}]}";
        mapper.insertActivity(a);
    }
    private User user(String name,String realName,String role,String password){User u=new User();u.userName=name;u.realName=realName;u.role=role;u.email=name+"@example.test";u.status="0";u.password=passwords.encode(password);mapper.insertUser(u);return u;}
}
