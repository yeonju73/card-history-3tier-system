package dev.controller.servlet.payment_amount;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import dev.common.ApplicationContextListener;
import dev.repository.CardTransactionDAO;

@WebServlet("/payment/months")
public class ReportMonthsServlet extends HttpServlet {
		
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	String userNo = request.getParameter("userNo");
    	String date = request.getParameter("date");
    	
    	System.out.println(userNo+" "+date);
    
        // 응답 헤더 설정 (JSON 형식 및 인코딩)
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        
        // 데이터 베이스 연결
        DataSource ds = ApplicationContextListener.getDataSource(getServletContext());

        try (Connection conn = ds.getConnection()) {
            // 여러 행을 가져오기 위해 LIMIT 제거 혹은 조정 가능
            String SQL = "SELECT * FROM card_transaction WHERE SEQ = ? AND BAS_YH = ?";
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, userNo);
            pstmt.setString(2, date);
            
            ResultSet rs = pstmt.executeQuery();
            
            if(rs.next()) {
                // Builder 패턴을 사용하여 객체 생성 및 값 매핑
                CardTransactionDAO transaction = CardTransactionDAO.builder()
                    // 2. 고객 거래 정보 (long 매핑)
                    .totUseAm(rs.getLong("TOT_USE_AM"))
                    .crdslUseAm(rs.getLong("CRDSL_USE_AM"))
                    .cnfUseAm(rs.getLong("CNF_USE_AM"))
                    
                    // 3. 대분류 이용 정보
                    .interiorAm(rs.getLong("INTERIOR_AM"))
                    .insuhosAm(rs.getLong("INSUHOS_AM"))
                    .offeduAm(rs.getLong("OFFEDU_AM"))
                    .trvlAm(rs.getLong("TRVL_AM"))
                    .fsbzAm(rs.getLong("FSBZ_AM"))
                    .svcarcAm(rs.getLong("SVCARC_AM"))
                    .distAm(rs.getLong("DIST_AM"))
                    .plsanitAm(rs.getLong("PLSANIT_AM"))
                    .clothgdsAm(rs.getLong("CLOTHGDS_AM"))
                    .autoAm(rs.getLong("AUTO_AM"))
                    
                    // 4. 중분류 이용 정보
                    .funitrAm(rs.getLong("FUNITR_AM"))
                    .applncAm(rs.getLong("APPLNC_AM"))
                    .hlthfsAm(rs.getLong("HLTHFS_AM"))
                    .bldmngAm(rs.getLong("BLDMNG_AM"))
                    .architAm(rs.getLong("ARCHIT_AM"))
                    .opticAm(rs.getLong("OPTIC_AM"))
                    .agrictrAm(rs.getLong("AGRICTR_AM"))
                    .leisureSAm(rs.getLong("LEISURE_S_AM"))
                    .leisurePAm(rs.getLong("LEISURE_P_AM"))
                    .cultureAm(rs.getLong("CULTURE_AM"))
                    .sanitAm(rs.getLong("SANIT_AM"))
                    .insuAm(rs.getLong("INSU_AM"))
                    .offcomAm(rs.getLong("OFFCOM_AM"))
                    .bookAm(rs.getLong("BOOK_AM"))
                    .rprAm(rs.getLong("RPR_AM"))
                    .hotelAm(rs.getLong("HOTEL_AM"))
                    .goodsAm(rs.getLong("GOODS_AM"))
                    .trvlAm(rs.getLong("TRVL_AM"))
                    .fuelAm(rs.getLong("FUEL_AM"))
                    .svcAm(rs.getLong("SVC_AM"))
                    .distbnpAm(rs.getLong("DISTBNP_AM"))
                    .distbpAm(rs.getLong("DISTBP_AM"))
                    .groceryAm(rs.getLong("GROCERY_AM"))
                    .hosAm(rs.getLong("HOS_AM"))
                    .clothAm(rs.getLong("CLOTH_AM"))
                    .restrntAm(rs.getLong("RESTRNT_AM"))
                    .automntAm(rs.getLong("AUTOMNT_AM"))
                    .autoslAm(rs.getLong("AUTOSL_AM"))
                    .kitwrAm(rs.getLong("KITWR_AM"))
                    .fabricAm(rs.getLong("FABRIC_AM"))
                    .acdmAm(rs.getLong("ACDM_AM"))
                    .mbrshopAm(rs.getLong("MBRSHOP_AM"))
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