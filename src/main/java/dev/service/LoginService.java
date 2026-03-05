package dev.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.dao.UserDAO;
import java.sql.SQLException;

@Service
public class LoginService {

    @Autowired
    private UserDAO userDAO;

    public String login(String userId) throws SQLException {
        return userDAO.findUserBySeq(userId);
    }
}
