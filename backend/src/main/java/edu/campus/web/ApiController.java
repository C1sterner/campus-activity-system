package edu.campus.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.campus.auth.SessionService;
import edu.campus.common.*;
import edu.campus.domain.Models.*;
import edu.campus.mapper.AppMapper;
import edu.campus.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AppMapper mapper;
    private final SessionService sessions;
    private final BCryptPasswordEncoder passwords;
    private final FormService forms;
    private final RegistrationService registrations;
    private final ReviewService reviews;
    private final FileStorage files;
    private final AuditService audit;
    private final ObjectMapper json;
    private final boolean demo;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public ApiController(AppMapper mapper,SessionService sessions,BCryptPasswordEncoder passwords,FormService forms,
                         RegistrationService registrations,ReviewService reviews,FileStorage files,AuditService audit,
                         ObjectMapper json,@Value("${app.seed-demo}") boolean demo) {
        this.mapper=mapper;this.sessions=sessions;this.passwords=passwords;this.forms=forms;this.registrations=registrations;
        this.reviews=reviews;this.files=files;this.audit=audit;this.json=json;this.demo=demo;
    }
    private String str(JsonNode body,String key){return body.path(key).asText("");}
    private User actor(HttpServletRequest request){return (User)request.getAttribute("currentUser");}
    private void log(HttpServletRequest req,String type,String detail){audit.record(actor(req),type,detail,req.getRemoteAddr());}
    private void password(String value){
        if(value==null||value.length()<8||value.length()>64||value.getBytes(StandardCharsets.UTF_8).length>72
                ||!value.matches("(?s).*[A-Za-z].*")||!value.matches("(?s).*[0-9].*"))
            throw ApiException.bad("密码须为8至64字符，包含字母与数字，UTF-8不超过72字节");
    }
    private String email(String value){
        value=Access.text(value,"邮箱",3,100).toLowerCase(Locale.ROOT);
        if(!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw ApiException.bad("邮箱格式不正确");return value;
    }
    private String phone(String value){if(value!=null&&!value.isBlank()&&!value.matches("[+0-9 ()-]{6,20}"))throw ApiException.bad("手机号格式不正确");return value;}
    private LocalDateTime date(String value){try{return LocalDateTime.parse(value,DATE);}catch(Exception e){throw ApiException.bad("时间格式应为yyyy-MM-dd HH:mm:ss");}}
    private Map<String,Object> activityView(Activity a){
        Map<String,Object> view=json.convertValue(a,new com.fasterxml.jackson.core.type.TypeReference<>(){});
        view.put("formSchema",forms.parse(a.formSchema));return view;
    }
    private Map<String,Object> registrationView(Registration r){
        Map<String,Object> view=json.convertValue(r,new com.fasterxml.jackson.core.type.TypeReference<>(){});
        view.put("formData",forms.parse(r.formData));view.put("hasAttachment",r.attachmentPath!=null);return view;
    }
    @GetMapping("/health") public ApiResponse health(){return ApiResponse.ok(Map.of("status","UP","demo",demo));}
    @GetMapping("/captcha") public ApiResponse captcha(){return ApiResponse.ok(sessions.captcha());}
    @PostMapping("/login") public ApiResponse login(@RequestBody JsonNode body,HttpServletRequest req){
        sessions.checkCaptcha(str(body,"captchaId"),str(body,"captcha"));
        User user=mapper.userByName(str(body,"account"));
        if(user==null||!passwords.matches(str(body,"password"),user.password))throw ApiException.bad("账号或密码错误");
        if(!"0".equals(user.status))throw ApiException.bad("账号已被停用，请联系系统管理员");
        mapper.loginInfo(user.userId,req.getRemoteAddr());
        return ApiResponse.ok(sessions.issue(mapper.userById(user.userId)));
    }
    @PostMapping("/register") public ApiResponse register(@RequestBody JsonNode body){
        sessions.checkCaptcha(str(body,"captchaId"),str(body,"captcha"));
        String name=Access.text(str(body,"userName"),"账号",3,50);
        if(!name.matches("[A-Za-z0-9_]{3,50}"))throw ApiException.bad("账号仅支持字母、数字和下划线");
        String mail=email(str(body,"email")),pass=str(body,"password");password(pass);
        if(!pass.equals(str(body,"confirmPassword")))throw ApiException.bad("两次输入的密码不一致");
        if(mapper.userByName(name)!=null||mapper.userByEmail(mail)!=null)throw ApiException.bad("账号或邮箱已被注册");
        User u=new User();u.userName=name;u.email=mail;u.password=passwords.encode(pass);u.realName=name;u.role="student";u.status="0";
        mapper.insertUser(u);return ApiResponse.ok(Map.of("userId",u.userId));
    }
    @PostMapping("/logout") public ApiResponse logout(HttpServletRequest req){sessions.logout(req.getHeader("Authorization"));return ApiResponse.ok(null);}
    @GetMapping("/profile") public ApiResponse profile(HttpServletRequest req){return ApiResponse.ok(actor(req));}
    @PutMapping("/profile") public ApiResponse profile(@RequestBody JsonNode body,HttpServletRequest req){
        User u=actor(req);u.realName=Access.text(str(body,"realName"),"姓名",1,50);u.email=email(str(body,"email"));u.phone=phone(str(body,"phone"));
        User other=mapper.userByEmail(u.email);if(other!=null&&!other.userId.equals(u.userId))throw ApiException.bad("邮箱已被占用");
        String pass=str(body,"newPassword");
        if(!pass.isBlank()){
            if(!passwords.matches(str(body,"oldPassword"),u.password))throw ApiException.bad("原密码不正确");
            password(pass);u.password=passwords.encode(pass);
        }
        mapper.updateUser(u);if(!pass.isBlank())sessions.invalidate(u.userId);log(req,"UPDATE_PROFILE","修改本人资料");return ApiResponse.ok(mapper.userById(u.userId));
    }
    @GetMapping("/activities") public ApiResponse activities(HttpServletRequest req){Access.role(actor(req),"activity_admin");return ApiResponse.ok(mapper.ownedActivities(actor(req).userId).stream().map(this::activityView).toList());}
    @GetMapping("/reviewers") public ApiResponse reviewers(HttpServletRequest req){Access.role(actor(req),"activity_admin");return ApiResponse.ok(mapper.reviewers());}
    @PostMapping("/activities") public ApiResponse writeActivity(@RequestBody JsonNode body,HttpServletRequest req){
        User actor=actor(req);Access.role(actor,"activity_admin");String op=str(body,"opType");int id=body.path("activityId").asInt(0);
        Activity old=id==0?null:mapper.activity(id);
        if(id!=0&&(old==null||!actor.userId.equals(old.creatorId)))throw ApiException.forbidden();
        if(Set.of("OFFLINE","DELETE").contains(op)){
            if(old==null)throw ApiException.missing();
            int changed="OFFLINE".equals(op)?mapper.offline(id,actor.userId):mapper.deleteDraftActivity(id,actor.userId);
            if(changed!=1)throw ApiException.conflict("当前活动状态不允许此操作");log(req,op,"活动#"+id);return ApiResponse.ok(id);
        }
        if(!Set.of("SAVE_DRAFT","PUBLISH").contains(op))throw ApiException.bad("活动操作类型不正确");
        if(old!=null&&!"0".equals(old.status))throw ApiException.conflict("发布后表单已冻结，请下线后新建活动");
        Activity a=old==null?new Activity():old;
        a.title=Access.text(str(body,"title"),"活动名称",1,100);a.content=Access.text(str(body,"content"),"活动介绍",1,5000);
        a.startTime=date(str(body,"startTime"));a.endTime=date(str(body,"endTime"));
        if(!a.endTime.isAfter(a.startTime))throw ApiException.bad("报名结束时间须晚于开始时间");
        a.formSchema=forms.stringify(forms.validateSchema(body.get("formSchema")));
        a.reviewerId=body.hasNonNull("reviewerId")?body.get("reviewerId").asLong():null;
        User reviewer=a.reviewerId==null?null:mapper.userById(a.reviewerId);
        if(a.reviewerId!=null&&(reviewer==null||!"teacher".equals(reviewer.role)||!"0".equals(reviewer.status)))throw ApiException.bad("请选择正常状态的审核教师");
        if("PUBLISH".equals(op)&&reviewer==null)throw ApiException.bad("请为活动指定审核教师");
        if(mapper.conflictingActivity(a.title,a.startTime,a.endTime,id)>0)throw ApiException.conflict("已存在时间重叠的同名活动");
        a.creatorId=actor.userId;a.status="PUBLISH".equals(op)?"1":"0";
        if(old==null)mapper.insertActivity(a);else if(mapper.updateActivity(a)!=1)throw ApiException.conflict("活动已被其他操作修改");
        log(req,op,"活动#"+a.activityId);return ApiResponse.ok(activityView(mapper.activity(a.activityId)));
    }
    @GetMapping("/published-activities") public ApiResponse published(HttpServletRequest req){Access.role(actor(req),"student");return ApiResponse.ok(mapper.published().stream().map(this::activityView).toList());}
    @GetMapping("/activities/{id}/detail") public ApiResponse activityDetail(@PathVariable int id,HttpServletRequest req){
        Activity a=mapper.activity(id);User u=actor(req);if(a==null)throw ApiException.missing();
        boolean allowed=("student".equals(u.role)&&"1".equals(a.status))||("teacher".equals(u.role)&&u.userId.equals(a.reviewerId))||("activity_admin".equals(u.role)&&u.userId.equals(a.creatorId));
        if(!allowed)throw ApiException.forbidden();return ApiResponse.ok(activityView(a));
    }
    @GetMapping("/activities/{id}/draft") public ApiResponse draft(@PathVariable int id,HttpServletRequest req){
        User u=actor(req);Access.role(u,"student");Registration r=mapper.registrationFor(id,u.userId);return ApiResponse.ok(r!=null&&"0".equals(r.status)?registrationView(r):null);
    }
    @PostMapping("/registrations/draft") public ApiResponse saveDraft(@RequestBody JsonNode body,HttpServletRequest req){
        return ApiResponse.ok(registrationView(registrations.save(actor(req),body.path("activityId").asInt(),body.path("formData"),true,null)));
    }
    @PostMapping(value="/registrations",consumes="multipart/form-data") public ApiResponse submit(@RequestParam int activityId,@RequestParam String formData,@RequestParam(required=false) MultipartFile attachment,HttpServletRequest req) throws IOException {
        Access.role(actor(req),"student");String key=null;
        try{key=files.save(attachment);Registration r=registrations.save(actor(req),activityId,forms.parse(formData),false,key);log(req,"SUBMIT_REGISTRATION","报名#"+r.regId);return ApiResponse.ok(registrationView(r));}
        catch(RuntimeException|IOException e){files.discard(key);throw e;}
    }
    @GetMapping("/my-registrations") public ApiResponse mine(HttpServletRequest req){Access.role(actor(req),"student");return ApiResponse.ok(mapper.mine(actor(req).userId).stream().map(this::registrationView).toList());}
    @GetMapping({"/my-registrations/{id}","/reviews/{id}"}) public ApiResponse registration(@PathVariable int id,HttpServletRequest req){
        Registration r=registrations.readable(actor(req),id);return ApiResponse.ok(Map.of("registration",registrationView(r),"activity",activityView(mapper.activity(r.activityId)),"reviews",mapper.reviews(id)));
    }
    @GetMapping({"/registrations/{id}/attachment","/reviews/{id}/attachment","/my-registrations/{id}/attachment"}) public void attachment(@PathVariable int id,HttpServletRequest req,HttpServletResponse response) throws IOException {
        Registration r=registrations.readable(actor(req),id);var path=files.read(r.attachmentPath);String ext=r.attachmentPath.substring(r.attachmentPath.lastIndexOf('.')+1);
        response.setContentType(FileStorage.contentType(ext));response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Content-Disposition","attachment; filename=material-"+id+"."+ext);Files.copy(path,response.getOutputStream());
    }
    @GetMapping("/reviews/pending") public ApiResponse pending(HttpServletRequest req){Access.role(actor(req),"teacher");return ApiResponse.ok(mapper.pending(actor(req).userId).stream().map(this::registrationView).toList());}
    @PostMapping("/reviews") public ApiResponse review(@RequestBody JsonNode body,HttpServletRequest req){
        int id=body.path("regId").asInt(-1),version=body.path("version").asInt(-1);if(id<1||version<0)throw ApiException.bad("报名编号或版本号无效");
        var result=reviews.submitReview(actor(req),id,str(body,"result"),body.hasNonNull("opinion")?str(body,"opinion"):null,version);
        log(req,"REVIEW","审核报名#"+id);return ApiResponse.ok(result);
    }
    private void filter(Integer id,String start,String end){if(id!=null&&id<1)throw ApiException.bad("活动编号无效");if(start!=null)date(start);if(end!=null)date(end);if(start!=null&&end!=null&&date(start).isAfter(date(end)))throw ApiException.bad("筛选时间不合法");}
    @GetMapping("/reg-stat") public ApiResponse stat(@RequestParam(required=false) Integer activityId,@RequestParam(required=false) String startTime,@RequestParam(required=false) String endTime,HttpServletRequest req){
        Access.role(actor(req),"activity_admin");filter(activityId,startTime,endTime);return ApiResponse.ok(mapper.statistics(activityId,startTime,endTime));
    }
    @GetMapping("/registration-data") public ApiResponse regData(@RequestParam(required=false) Integer activityId,@RequestParam(required=false) String startTime,@RequestParam(required=false) String endTime,HttpServletRequest req){
        Access.role(actor(req),"activity_admin");filter(activityId,startTime,endTime);return ApiResponse.ok(mapper.registrations(activityId,startTime,endTime,0,200).stream().map(this::registrationView).toList());
    }
    @GetMapping("/reg-export") public void export(@RequestParam(required=false) Integer activityId,@RequestParam(required=false) String startTime,@RequestParam(required=false) String endTime,HttpServletRequest req,HttpServletResponse response) throws IOException {
        Access.role(actor(req),"activity_admin");filter(activityId,startTime,endTime);
        int count=((Number)mapper.statistics(activityId,startTime,endTime).get("total")).intValue();
        int max=Integer.parseInt(Optional.ofNullable(mapper.config("export.max_rows")).orElse("50000"));
        if(count>max)throw new ApiException(422,"EXPORT_413","数据量超限，请缩小筛选范围");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=registrations.xlsx");
        SXSSFWorkbook workbook=new SXSSFWorkbook(100);
        try {
            var sheet=workbook.createSheet("报名汇总");String[] headers={"编号","活动","姓名","状态","提交时间","表单内容"};
            var header=sheet.createRow(0);for(int i=0;i<headers.length;i++)header.createCell(i).setCellValue(headers[i]);int rowNum=1;
            for(int offset=0;offset<count;offset+=500){
                for(Registration r:mapper.registrations(activityId,startTime,endTime,offset,500)){
                    var row=sheet.createRow(rowNum++);String[] values={r.regId.toString(),r.activityTitle,r.studentName,Map.of("1","待审核","2","通过","3","驳回").get(r.status),r.submitTime==null?"":DATE.format(r.submitTime),r.formData};
                    for(int i=0;i<values.length;i++)row.createCell(i).setCellValue(values[i]==null?"":values[i]);
                }
            }
            for(int i=0;i<headers.length;i++)sheet.setColumnWidth(i,i==5?14000:6000);
            workbook.write(response.getOutputStream());
        } finally {workbook.close();workbook.dispose();}
        log(req,"EXPORT","导出报名汇总，条数="+count);
    }
    @GetMapping("/admin/users") public ApiResponse users(HttpServletRequest req){Access.role(actor(req),"admin");return ApiResponse.ok(mapper.users());}
    @PostMapping("/admin/users") public ApiResponse user(@RequestBody JsonNode body,HttpServletRequest req){
        User actor=actor(req);Access.role(actor,"admin");long id=body.path("userId").asLong(0);User u=id==0?new User():mapper.userById(id);if(u==null)throw ApiException.missing();
        String role=str(body,"role"),status=str(body,"status");if(!Access.ROLES.contains(role)||!Set.of("0","1").contains(status))throw ApiException.bad("角色或状态不正确");
        if(id!=0&&u.userId.equals(actor.userId)&&(!role.equals(u.role)||!status.equals("0")))throw ApiException.bad("不能停用自己或修改自身角色");
        if(id!=0&&"admin".equals(u.role)&&"0".equals(u.status)&&(!"admin".equals(role)||!"0".equals(status))&&mapper.activeAdminCount()<=1)throw ApiException.bad("不能停用或降权最后一名管理员");
        if(id==0){u.userName=Access.text(str(body,"userName"),"账号",3,50);if(!u.userName.matches("[A-Za-z0-9_]{3,50}")||mapper.userByName(u.userName)!=null)throw ApiException.bad("账号不合法或已存在");}
        u.realName=Access.text(str(body,"realName"),"姓名",1,50);u.email=email(str(body,"email"));u.phone=phone(str(body,"phone"));
        User other=mapper.userByEmail(u.email);if(other!=null&&!other.userId.equals(u.userId))throw ApiException.bad("邮箱已被占用");
        String pass=str(body,"password");if(id==0||!pass.isBlank()){password(pass);u.password=passwords.encode(pass);}
        u.role=role;u.status=status;if(id==0)mapper.insertUser(u);else {mapper.updateUser(u);sessions.invalidate(id);}
        log(req,"MANAGE_USER","维护账号#"+u.userId);return ApiResponse.ok(mapper.userById(u.userId));
    }
    @GetMapping("/admin/config") public ApiResponse configs(HttpServletRequest req){Access.role(actor(req),"admin");return ApiResponse.ok(mapper.configs());}
    @PutMapping("/admin/config/{key}") public ApiResponse config(@PathVariable String key,@RequestBody JsonNode body,HttpServletRequest req){
        Access.role(actor(req),"admin");String value=str(body,"value");long n;
        try{n=Long.parseLong(value);}catch(Exception e){throw ApiException.bad("配置值应为整数");}
        boolean valid=switch(key){case "upload.max_size_bytes"->n>=1024&&n<=20971520;case "export.max_rows"->n>=1&&n<=50000;case "registration.draft_interval_seconds"->n>=30&&n<=300;default->false;};
        if(!valid)throw ApiException.bad("配置项不允许修改或数值超限");mapper.updateConfig(key,value);log(req,"CONFIG","修改配置 "+key);return ApiResponse.ok(null);
    }
    @GetMapping("/client-config") public ApiResponse clientConfig(){return ApiResponse.ok(Map.of("uploadMaxBytes",Long.parseLong(mapper.config("upload.max_size_bytes")),"draftIntervalSeconds",Integer.parseInt(mapper.config("registration.draft_interval_seconds"))));}
    @GetMapping("/admin/logs") public ApiResponse logs(HttpServletRequest req){Access.role(actor(req),"admin");return ApiResponse.ok(mapper.logs());}
}
