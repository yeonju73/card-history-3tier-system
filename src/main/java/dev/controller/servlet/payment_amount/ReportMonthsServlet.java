package dev.controller.servlet.payment_amount;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import dev.common.ApplicationContextListener;
import dev.domain.CardTransactionVO;

@WebServlet("/paymentmonths")
public class ReportMonthsServlet extends HttpServlet {
		
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	HttpSession session = request.getSession(false); // 기존 세션이 있는지 확인
    	String userNo = (String) session.getAttribute("loggedInUser");
    	String date = request.getParameter("date");
    
        // 응답 헤더 설정 (JSON 형식 및 인코딩)
    	response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        
        // 데이터 베이스 연결
        DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

        try (Connection conn = ds.getConnection()) {
            // 여러 행을 가져오기 위해 LIMIT 제거 혹은 조정 가능
            String SQL = "SELECT FSBZ_AM, AUTO_AM, DIST_AM,TRVL_AM, HOS_AM ,TOT_USE_AM  FROM card_transaction WHERE SEQ = ? AND BAS_YH = ?";
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, userNo);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();
            
            if(rs.next()) {
                // Builder 패턴을 사용하여 객체 생성 및 값 매핑
                CardTransactionVO transaction = CardTransactionVO.builder()
                    // 2. 고객 거래 정보 (long 매핑)
                    .totUseAm(rs.getLong("TOT_USE_AM"))
                    
                    .trvlAm(rs.getLong("TRVL_AM"))
                    .fsbzAm(rs.getLong("FSBZ_AM"))
                    .distAm(rs.getLong("DIST_AM"))
                    .autoAm(rs.getLong("AUTO_AM"))
                    .hosAm(rs.getLong("HOS_AM"))

                    .build();
                
                // 최종 JSON 출력
                out.print(gson.toJson(transaction));
            }
        	
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }
}
