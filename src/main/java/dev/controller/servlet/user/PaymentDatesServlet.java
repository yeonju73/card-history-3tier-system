package dev.controller.servlet.user;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.google.gson.Gson;

import dev.common.ApplicationContextListener;
import dev.domain.CardTransactionVO;

@WebServlet("/paymentDates")
public class PaymentDatesServlet  extends HttpServlet{
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false); // 기존 세션이 있는지 확인
    	String userNo = (String) session.getAttribute("loggedInUser");
    
        // 응답 헤더 설정 (JSON 형식 및 인코딩)
    	response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        
        // 데이터 베이스 연결
        DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

        try (Connection conn = ds.getConnection()) {
            String SQL = "SELECT BAS_YH FROM card_transaction WHERE SEQ = ? group by BAS_YH";
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, userNo);
            
            ResultSet rs = pstmt.executeQuery();
            
            List<String> dates = new ArrayList<String>();
            while(rs.next()) {
            	dates.add(rs.getString("BAS_YH"));
            }
            
            out.print(gson.toJson(dates));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }
}
