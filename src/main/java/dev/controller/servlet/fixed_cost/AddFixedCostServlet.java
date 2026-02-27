package dev.controller.servlet.fixed_cost;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import dev.common.ApplicationContextListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/fixedcost/add")
public class AddFixedCostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	long cost = 0L;
    	
		String userNo = request.getParameter("userNo");
    	cost = Long.parseLong(request.getParameter("cost"));
    	String category = request.getParameter("category");
    	String date = request.getParameter("date");
    
        // 응답 헤더 설정 (JSON 형식 및 인코딩)
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // 데이터 베이스 연결
        DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

        try (Connection conn = ds.getConnection()) {
            String SQL = "UPDATE card_transaction SET "+ category +" = ? WHERE SEQ = ? AND BAS_YH = ?;";
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setLong(1, cost);
            pstmt.setString(2, userNo);
            pstmt.setString(3, date);
            
            int result = pstmt.executeUpdate(); // 영향을 받은 행의 수 반환
            boolean isSuccess = (result == 1 ); // 한 개의 행만이 값이 변경되어야 한다.
            
            out.print(isSuccess); 
            out.flush();
            
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }
}
