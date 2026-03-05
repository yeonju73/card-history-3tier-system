package dev.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import dev.domain.CardTransactionVO;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PaymentDAO {

    @Autowired
    private DataSource dataSource;

    public List<String> findPaymentDates(String seq) throws SQLException {
        String sql = "SELECT BAS_YH FROM CARD_TRANSACTION WHERE SEQ = ? GROUP BY BAS_YH";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, seq);
            ResultSet rs = pstmt.executeQuery();
            List<String> dates = new ArrayList<>();
            while (rs.next()) dates.add(rs.getString("BAS_YH"));
            return dates;
        }
    }

    public CardTransactionVO findMonthlyReport(String seq, String date) throws SQLException {
        String sql = "SELECT FSBZ_AM, AUTO_AM, DIST_AM, TRVL_AM, HOS_AM, TOT_USE_AM "
                   + "FROM CARD_TRANSACTION WHERE SEQ = ? AND BAS_YH = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, seq);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return CardTransactionVO.builder()
                    .totUseAm(rs.getLong("TOT_USE_AM"))
                    .trvlAm(rs.getLong("TRVL_AM"))
                    .fsbzAm(rs.getLong("FSBZ_AM"))
                    .distAm(rs.getLong("DIST_AM"))
                    .autoAm(rs.getLong("AUTO_AM"))
                    .hosAm(rs.getLong("HOS_AM"))
                    .build();
            }
            return null;
        }
    }
}
