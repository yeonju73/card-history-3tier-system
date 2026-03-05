package dev.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class FixedCostDAO {

    @Autowired
    private DataSource dataSource;

    public boolean updateFixedCost(String seq, String date, String category, long cost) throws SQLException {
        String sql = "UPDATE CARD_TRANSACTION SET " + category + " = ? WHERE SEQ = ? AND BAS_YH = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, cost);
            pstmt.setString(2, seq);
            pstmt.setString(3, date);
            return pstmt.executeUpdate() == 1;
        }
    }
}
