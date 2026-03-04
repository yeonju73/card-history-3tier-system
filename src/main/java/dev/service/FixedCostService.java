package dev.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.dao.FixedCostDAO;
import java.sql.SQLException;

@Service
public class FixedCostService {

    @Autowired
    private FixedCostDAO fixedCostDAO;

    public boolean addFixedCost(String seq, String date, String category, long cost) throws SQLException {
        return fixedCostDAO.updateFixedCost(seq, date, category, cost);
    }
}
