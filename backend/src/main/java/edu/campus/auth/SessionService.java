package edu.campus.auth;

import edu.campus.common.ApiException;
import edu.campus.domain.Models.User;
import edu.campus.mapper.AppMapper;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private record Session(long userId, Instant expires) {}
    private record Captcha(String answer, Instant expires) {}
    private final Map<String,Session> sessions = new ConcurrentHashMap<>();
    private final Map<String,Captcha> captchas = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AppMapper mapper;
    private final int hours;
    public SessionService(AppMapper mapper, @Value("${app.token-hours:2}") int hours) {
        this.mapper = mapper; this.hours = hours;
    }
    public Map<String,Object> captcha() {
        Instant now = Instant.now();
        captchas.entrySet().removeIf(e -> e.getValue().expires.isBefore(now));
        if (captchas.size() > 5000) throw new ApiException(429, "RATE_429", "请求较多，请稍后重试");
        String answer = String.format("%04d", random.nextInt(10000));
        String id = UUID.randomUUID().toString();
        captchas.put(id, new Captcha(answer, now.plusSeconds(300)));
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='130' height='44'><rect width='130' height='44' rx='6' fill='#eef6f2'/><path d='M5 20 L120 30 M10 35 L115 8' stroke='#a3c4b5'/><text x='20' y='32' font-size='29' font-family='monospace' letter-spacing='4' fill='#225e4b'>" + answer + "</text></svg>";
        return Map.of("captchaId", id, "image", "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)));
    }
    public void checkCaptcha(String id, String answer) {
        Captcha saved = id == null ? null : captchas.remove(id);
        if (saved == null || saved.expires.isBefore(Instant.now()) || !saved.answer.equals(answer))
            throw ApiException.bad("验证码错误或已过期，请刷新验证码");
    }
    public Map<String,Object> issue(User user) {
        sessions.entrySet().removeIf(e -> e.getValue().expires.isBefore(Instant.now()));
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = Instant.now().plusSeconds(hours * 3600L);
        sessions.put(token, new Session(user.userId, expiry));
        return Map.of("token", token, "expiresAt", expiry.toString(), "user", user);
    }
    public User authenticate(String header) {
        String token = token(header);
        Session session = sessions.get(token);
        if (session == null || session.expires.isBefore(Instant.now())) {
            sessions.remove(token); throw new ApiException(401, "AUTH_401", "登录已失效，请重新登录");
        }
        User user = mapper.userById(session.userId);
        if (user == null || !"0".equals(user.status)) {
            sessions.remove(token); throw new ApiException(401, "AUTH_401", "账号已停用，请联系系统管理员");
        }
        return user;
    }
    private String token(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new ApiException(401, "AUTH_401", "请先登录");
        return header.substring(7);
    }
    public void logout(String header) { sessions.remove(token(header)); }
    public void invalidate(long user) { sessions.entrySet().removeIf(e -> e.getValue().userId == user); }
}
