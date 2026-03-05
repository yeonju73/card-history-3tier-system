package dev.controller.servlet.fixed_cost;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// import javax.sql.DataSource;  // [주석처리] Spring Bean이 관리

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.common.ApplicationContextListener;
import dev.service.FixedCostService;

import java.io.IOException;
import java.io.PrintWriter;
// import java.sql.Connection;        // [주석처리] Service 계층으로 이동
// import java.sql.PreparedStatement;  // [주석처리] Service 계층으로 이동
// import java.sql.ResultSet;          // [주석처리] Service 계층으로 이동
import java.sql.SQLException;
// import java.util.ArrayList;  // [주석처리] 미사용
// import java.util.List;       // [주석처리] 미사용

@WebServlet("/addfixedcost")
public class AddFixedCostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	long cost = 0L;

    	HttpSession session = request.getSession(false); // 기존 세션이 있는지 확인
    	String userNo = (String) session.getAttribute("loggedInUser");

    	cost = Long.parseLong(request.getParameter("cost"));
    	String category = request.getParameter("category");
    	String date = request.getParameter("date");

        // 응답 헤더 설정 (JSON 형식 및 인코딩)
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Spring Context에서 FixedCostService 빈 획득
        AnnotationConfigApplicationContext springCtx = ApplicationContextListener.getSpringContext(getServletContext());
        FixedCostService fixedCostService = springCtx.getBean(FixedCostService.class);

//      // [주석처리] DataSource 직접 획득 방식
//      DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

//      // [주석처리] 직접 DB 연동 로직
//      try (Connection conn = ds.getConnection()) {
//          String SQL = "UPDATE CARD_TRANSACTION SET "+ category +" = ? WHERE SEQ = ? AND BAS_YH = ?;";
//          PreparedStatement pstmt = conn.prepareStatement(SQL);
//          pstmt.setLong(1, cost);
//          pstmt.setString(2, userNo);
//          pstmt.setString(3, date);
//          int result = pstmt.executeUpdate();
//          boolean isSuccess = (result == 1);
//          out.print(isSuccess);
//          out.flush();
//      } catch (SQLException e) {
//          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//          out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
//          e.printStackTrace();
//      } finally {
//          out.flush();
//      }

        try {
            boolean isSuccess = fixedCostService.addFixedCost(userNo, date, category, cost);
            out.print(isSuccess);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }
}
