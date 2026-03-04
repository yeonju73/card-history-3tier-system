package dev.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.dao.PaymentDAO;
import dev.domain.CardTransactionVO;
import java.sql.SQLException;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentDAO paymentDAO;

    public List<String> getPaymentDates(String seq) throws SQLException {
        return paymentDAO.findPaymentDates(seq);
    }

    public CardTransactionVO getMonthlyReport(String seq, String date) throws SQLException {
        return paymentDAO.findMonthlyReport(seq, date);
    }
}
