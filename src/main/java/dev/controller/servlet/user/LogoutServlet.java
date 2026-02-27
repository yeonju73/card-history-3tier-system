package dev.controller.servlet.user;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet{
	private static final long serialVersionUID = 1L;

    private static final String LOGIN_PAGE = "/login.html";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. 뒤로가기 방지 - 캐시 완전 차단
        setNoCacheHeaders(response);

        // 2. 세션 무효화
        invalidateSession(request);

        // 3. 브라우저 쿠키 즉시 삭제
        expireAllCookies(request, response);

        // 4. 로그인 페이지로 리다이렉트
        response.sendRedirect(request.getContextPath() + LOGIN_PAGE);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    // ── 뒤로가기 방지 헤더 ────────────────────────────────────────────────────
    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    // ── 세션 무효화 ───────────────────────────────────────────────────────────
    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    // ── 브라우저 쿠키 전체 파기 ───────────────────────────────────────────────
    private void expireAllCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;

        for (Cookie cookie : cookies) {
            Cookie expired = new Cookie(cookie.getName(), "");
            expired.setMaxAge(0);               // 즉시 만료
            expired.setPath(request.getContextPath());
            expired.setHttpOnly(true);          // JS 접근 차단
            response.addCookie(expired);
        }
    }
}
