package dev.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserDAO {

    @Autowired
    private DataSource dataSource;

    public String findUserBySeq(String userId) throws SQLException {
        String sql = "SELECT SEQ FROM CARD_TRANSACTION WHERE SEQ = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("SEQ") : null;
            }
        }
    }
}
