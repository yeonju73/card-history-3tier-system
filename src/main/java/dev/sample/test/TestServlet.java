package dev.sample.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/test") 
public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	// Nginx에서 보낸 헤더 읽기
    	String nginxName = req.getHeader("X-Nginx-Name");
        int wasPort = req.getLocalPort(); // 8080 또는 8090
        System.out.println("호출 됨.");
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        out.println("<h1>우리은행 3-Tier 시스템 확인</h1>");
        if (nginxName != null) {
            out.println("<h2 style='color:blue;'>경유한 웹 서버: " + nginxName + "</h2>");
        } else {
            out.println("<h2 style='color:red;'>접속 방식: 직접 접속 (헤더를 찾을 수 없음)</h2>");
        }
        out.println("<h2>처리한 앱 서버 포트: " + wasPort + "</h2>");
    }

}