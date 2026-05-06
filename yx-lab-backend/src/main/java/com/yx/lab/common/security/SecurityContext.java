package com.yx.lab.common.security;

public final class SecurityContext {

    private static final ThreadLocal<CurrentUser> USER_HOLDER = new ThreadLocal<>();

    private SecurityContext() {
    }

    public static void setCurrentUser(CurrentUser currentUser) {
        USER_HOLDER.set(currentUser);
    }

    public static CurrentUser getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
