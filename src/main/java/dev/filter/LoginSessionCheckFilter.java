package dev.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginSessionCheckFilter implements Filter {

    // 제외할 경로 리스트
	private static final String LOGIN_PAGE         = "/login.html";
    private static final String LOGIN_ENDPOINT     = "/login";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // 1. 제외 대상 확인
        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 세션 및 로그인 여부 확인
        HttpSession session = request.getSession(false);
        if (isAuthenticated(session)) {
            setNoCacheHeaders(response);
            chain.doFilter(request, response);
        } else {
            handleUnauthorized(request, response, session);
        }
    }

    private boolean isExcluded(String path) {
        return path.endsWith(LOGIN_PAGE)
            || path.endsWith(LOGIN_ENDPOINT)
            || path.contains("/static/")
            || path.endsWith(".css")
            || path.endsWith(".js");
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("loggedInUser") != null;
    }

    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        
        // 세션 무효화
        if (session != null) {
            session.invalidate();
        }

        // JSESSIONID 쿠키 삭제 (Path 주의: 컨텍스트 경로에 맞춤)
        deleteCookie(request, response, "JSESSIONID");

        if (isAjaxRequest(request)) {
            sendJsonError(response);
        } else {
            response.sendRedirect(request.getContextPath() + "/login.html");
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        response.addCookie(cookie);
    }

    private void sendJsonError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) { // try-with-resources로 자동 close
            out.print("{\"status\":\"error\", \"message\":\"세션이 만료되었습니다.\"}");
            out.flush();
        }
    }

}
