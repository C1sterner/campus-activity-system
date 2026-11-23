package edu.campus.common;

import edu.campus.domain.Models.User;
import java.util.Set;

public final class Access {
    public static final Set<String> ROLES = Set.of("student", "teacher", "activity_admin", "admin");
    private Access() {}
    public static void role(User actor, String role) {
        if (actor == null || !role.equals(actor.role)) throw ApiException.forbidden();
    }
    public static String text(String value, String label, int min, int max) {
        if (value == null || value.trim().length() < min || value.length() > max)
            throw ApiException.bad(label + "长度应为" + min + "至" + max + "字符");
        return value.trim();
    }
}
