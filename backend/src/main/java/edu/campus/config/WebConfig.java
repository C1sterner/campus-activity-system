package edu.campus.config;

import edu.campus.auth.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SessionService sessions;
    public WebConfig(SessionService sessions) { this.sessions = sessions; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                req.setAttribute("currentUser", sessions.authenticate(req.getHeader("Authorization")));
                return true;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/login", "/api/register", "/api/captcha", "/api/health");
    }
    @Bean public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
    @Bean public Jackson2ObjectMapperBuilderCustomizer dateFormat() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return builder -> builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(fmt))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(fmt));
    }
}
