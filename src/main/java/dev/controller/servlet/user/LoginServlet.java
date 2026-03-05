package dev.controller.servlet.user;

import java.io.IOException;
// import java.sql.Connection;      // [주석처리] Service 계층으로 이동
// import java.sql.PreparedStatement; // [주석처리] Service 계층으로 이동
// import java.sql.ResultSet;        // [주석처리] Service 계층으로 이동
import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
// import javax.sql.DataSource;  // [주석처리] Spring Bean이 관리

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.common.ApplicationContextListener;
import dev.service.LoginService;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String userId = request.getParameter("userId");

		// 기존 세션 확인
		HttpSession existingSession = request.getSession(false);
		if (existingSession != null && existingSession.getAttribute("loggedInUser") != null) {
			String loggedInUser = (String) existingSession.getAttribute("loggedInUser");

			if (loggedInUser.equals(userId)) {
				// 같은 유저 → 세션 재사용, 바로 이동
				response.sendRedirect(request.getContextPath() + "/index.html");
				return;
			} else {
				// 다른 유저 → 기존 세션 폐기
				existingSession.invalidate();
			}
		}

		// 2. Spring Context에서 LoginService 빈 획득
		AnnotationConfigApplicationContext springCtx = ApplicationContextListener.getSpringContext(getServletContext());
		LoginService loginService = springCtx.getBean(LoginService.class);

//		// [주석처리] DataSource 직접 획득 방식
//		DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

//		// [주석처리] 직접 DB 연동 로직
//		String sql = "SELECT SEQ FROM CARD_TRANSACTION WHERE SEQ = ?";
//		try (Connection conn = ds.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
//			pstmt.setString(1, userId);
//			try (ResultSet rs = pstmt.executeQuery()) {
//				if (rs.next()) {
//					String SEQ = rs.getString("SEQ");
//					HttpSession session = request.getSession();
//					session.setAttribute("loggedInUser", SEQ);
//					response.sendRedirect(request.getContextPath() + "/index.html");
//				} else {
//					response.sendRedirect(request.getContextPath() + "/login.html?error=1");
//				}
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "데이터베이스 연결 오류");
//		}

		// 3. 서비스 호출
		try {
			String seq = loginService.login(userId);
			if (seq != null) {
				HttpSession session = request.getSession();
				session.setAttribute("loggedInUser", seq);
				response.sendRedirect(request.getContextPath() + "/index.html");
			} else {
				response.sendRedirect(request.getContextPath() + "/login.html?error=1");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "데이터베이스 연결 오류");
		}
	}
}
