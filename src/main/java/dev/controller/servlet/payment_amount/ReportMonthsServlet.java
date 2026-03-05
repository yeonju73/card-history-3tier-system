package dev.controller.servlet.payment_amount;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// import javax.sql.DataSource;  // [주석처리] Spring Bean이 관리
import java.io.IOException;
import java.io.PrintWriter;
// import java.sql.Connection;        // [주석처리] Service 계층으로 이동
// import java.sql.PreparedStatement;  // [주석처리] Service 계층으로 이동
// import java.sql.ResultSet;          // [주석처리] Service 계층으로 이동
import java.sql.SQLException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.gson.Gson;

import dev.common.ApplicationContextListener;
import dev.domain.CardTransactionVO;
import dev.service.PaymentService;

@WebServlet("/paymentmonths")
public class ReportMonthsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	HttpSession session = request.getSession(false); // 기존 세션이 있는지 확인
    	String userNo = (String) session.getAttribute("loggedInUser");
    	String date = request.getParameter("date");

    	System.out.println(userNo+" "+date);

        // 응답 헤더 설정 (JSON 형식 및 인코딩)
    	response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        // Spring Context에서 PaymentService 빈 획득
        AnnotationConfigApplicationContext springCtx = ApplicationContextListener.getSpringContext(getServletContext());
        PaymentService paymentService = springCtx.getBean(PaymentService.class);

//      // [주석처리] DataSource 직접 획득 방식
//      DataSource ds = ApplicationContextListener.getReplicaDataSource(getServletContext());
//      DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

//      // [주석처리] 직접 DB 연동 로직
//      try (Connection conn = ds.getConnection()) {
//          // 여러 행을 가져오기 위해 LIMIT 제거 혹은 조정 가능
//          String SQL = "SELECT FSBZ_AM, AUTO_AM, DIST_AM,TRVL_AM, HOS_AM ,TOT_USE_AM  FROM CARD_TRANSACTION WHERE SEQ = ? AND BAS_YH = ?";
//          PreparedStatement pstmt = conn.prepareStatement(SQL);
//          pstmt.setString(1, userNo);
//          pstmt.setString(2, date);
//          System.out.println();
//          ResultSet rs = pstmt.executeQuery();
//          if(rs.next()) {
//              CardTransactionVO transaction = CardTransactionVO.builder()
//                  .totUseAm(rs.getLong("TOT_USE_AM"))
//                  .trvlAm(rs.getLong("TRVL_AM"))
//                  .fsbzAm(rs.getLong("FSBZ_AM"))
//                  .distAm(rs.getLong("DIST_AM"))
//                  .autoAm(rs.getLong("AUTO_AM"))
//                  .hosAm(rs.getLong("HOS_AM"))
//                  .build();
//              out.print(gson.toJson(transaction));
//          }
//      } catch (SQLException e) {
//          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//          out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
//          e.printStackTrace();
//      } finally {
//          out.flush();
//      }

        try {
            CardTransactionVO transaction = paymentService.getMonthlyReport(userNo, date);
            if (transaction != null) out.print(gson.toJson(transaction));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }
}
