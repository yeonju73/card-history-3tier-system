package dev.controller.servlet.user;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import dev.common.ApplicationContextListener;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String userId = request.getParameter("userId");
		System.out.println(userId);
		

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

		// 2. Listener 를 통해 DataSource 획득
		DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

		// 3. DB 연동 로직
		String sql = "SELECT SEQ FROM card_transaction WHERE SEQ = ?";

		try (Connection conn = ds.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, userId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					// 로그인 성공
					String SEQ = rs.getString("SEQ");
					
					// 세션 생성 및 정보 저장
					HttpSession session = request.getSession(); // true가 기본값
					session.setAttribute("loggedInUser", SEQ);

					// 메인 화면으로 리다이렉트
					response.sendRedirect(request.getContextPath() + "/");
				} else {
					// 로그인 실패 (ID 일치하는 고객 없음)
					response.sendRedirect(request.getContextPath() + "/login.html?error=1");
					System.out.println("else");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "데이터베이스 연결 오류");
			System.out.println("catch");
		}
	}
}
