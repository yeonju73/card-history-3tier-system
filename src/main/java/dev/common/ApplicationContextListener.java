package dev.common;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    private HikariDataSource ds;
    private HikariDataSource masterDataSource;
    private HikariDataSource slaveDataSource;
    

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	System.out.println("컨텍스트 초기화 됨");
        ServletContext ctx = sce.getServletContext();
        
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

        HikariConfig config = new HikariConfig();
        // 필수 설정값(별도의 설정파일로 분리 가능, ex. jdbc.properties)
        config.setJdbcUrl("jdbc:mysql://localhost:3306/card_db?serverTimezone=Asia/Seoul");
        config.setUsername("root");
        config.setPassword("1234");
        
        // 1. Master 설정 (Port 6446)
//        HikariConfig masterConfig = new HikariConfig();
//        masterConfig.setJdbcUrl("jdbc:mysql://localhost:6446/card_db");
//        masterConfig.setUsername("root");
//        masterConfig.setPassword("password");
//        masterConfig.setPoolName("Master-Pool-6446");
//        masterDataSource = new HikariDataSource(masterConfig);

        // 2. Slave 설정 (Port 6447)
//        HikariConfig slaveConfig = new HikariConfig();
//        slaveConfig.setJdbcUrl("jdbc:mysql://localhost:6447/card_db");
//        slaveConfig.setUsername("root");
//        slaveConfig.setPassword("password");
//        slaveConfig.setPoolName("Slave-Pool-6447");
//        slaveDataSource = new HikariDataSource(slaveConfig);

        // 선택 설정값 예시
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(3000);
//        config.setIdleTimeout(600000);
//        config.setMaxLifetime(1800000);

        ds = new HikariDataSource(config);
//        slaveDataSource = new HikariDataSource(slaveConfig);
//        masterDataSource = new HikariDataSource(masterConfig);

        ctx.setAttribute("DATA_SOURCE", ds);
//        ctx.setAttribute("MASTER_DATA_SOURCE", masterDataSource);
//        ctx.setAttribute("SLAVE_DATA_SOURCE", slaveDataSource);
    }

    
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (ds != null) ds.close(); // 애플리케이션 종료 시 커넥션 풀 자원해제
//        if (masterDataSource != null) masterDataSource.close();
//        if (slaveDataSource != null) slaveDataSource.close();
    }

    public static DataSource getDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("DATA_SOURCE");
    }
    
    // 쓰기
//    public static DataSource getMasterDataSource(ServletContext ctx) {
//        return (DataSource) ctx.getAttribute("MASTER_DATA_SOURCE");
//    }
    
    // 읽기
//    public static DataSource getSlaveDataSource(ServletContext ctx) {
//        return (DataSource) ctx.getAttribute("SLAVE_DATA_SOURCE");
//    }
}
