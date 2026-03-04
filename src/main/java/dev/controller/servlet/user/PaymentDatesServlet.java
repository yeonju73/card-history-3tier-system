package dev.controller.servlet.user;

import java.io.IOException;
import java.io.PrintWriter;
// import java.sql.Connection;      // [주석처리] Service 계층으로 이동
// import java.sql.PreparedStatement; // [주석처리] Service 계층으로 이동
// import java.sql.ResultSet;        // [주석처리] Service 계층으로 이동
import java.sql.SQLException;
// import java.util.ArrayList;       // [주석처리] Service 계층으로 이동
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// import javax.sql.DataSource;  // [주석처리] Spring Bean이 관리

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.gson.Gson;

import dev.common.ApplicationContextListener;
import dev.domain.CardTransactionVO;
import dev.service.PaymentService;

@WebServlet("/paymentDates")
public class PaymentDatesServlet extends HttpServlet {
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession(false);
    	String userNo = (String) session.getAttribute("loggedInUser");
    	String date = request.getParameter("date");

        // 응답 헤더 설정 (JSON 형식 및 인코딩)
    	response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

		// Spring Context에서 PaymentService 빈 획득
		AnnotationConfigApplicationContext springCtx = ApplicationContextListener.getSpringContext(getServletContext());
		PaymentService paymentService = springCtx.getBean(PaymentService.class);

//      // [주석처리] DataSource 직접 획득 방식
//      DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

//      // [주석처리] 직접 DB 연동 로직
//      try (Connection conn = ds.getConnection()) {
//          String SQL = "SELECT BAS_YH FROM CARD_TRANSACTION WHERE SEQ = ? group by BAS_YH";
//          PreparedStatement pstmt = conn.prepareStatement(SQL);
//          pstmt.setString(1, userNo);
//          ResultSet rs = pstmt.executeQuery();
//          List<String> dates = new ArrayList<String>();
//          while(rs.next()) {
//              dates.add(rs.getString("BAS_YH"));
//          }
//          out.print(gson.toJson(dates));
//      } catch (SQLException e) {
//          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//          out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
//          e.printStackTrace();
//      } finally {
//          out.flush();
//      }

        try {
            List<String> dates = paymentService.getPaymentDates(userNo);
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
