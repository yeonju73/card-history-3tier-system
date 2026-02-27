package dev.controller.servlet.fixed_cost;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/fixedcost/report")
public class ReportFixedCostServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\", \"message\":\"인가되지 않은 사용자입니다.\"}");
            return;
        }
        
        // Service -> DAO (Replica DB 읽기 요청)
        // String jsonData = fixedCostService.getReportData(userId);
        
        out.print("{\"status\":\"success\", \"data\": []}"); // JSON 배열 반환
        out.flush();
    }
}