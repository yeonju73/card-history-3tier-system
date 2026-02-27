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

	private HikariDataSource sourceDs;
    private HikariDataSource replicaDs;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	System.out.println("컨텍스트 초기화 됨");
        ServletContext ctx = sce.getServletContext();
        
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

        HikariConfig sourceConfig  = new HikariConfig();
        // 필수 설정값(별도의 설정파일로 분리 가능, ex. jdbc.properties)
        // port : 
        sourceConfig .setJdbcUrl("jdbc:mysql://localhost:6446/card_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
        sourceConfig .setUsername("root");
        sourceConfig .setPassword("root1234");
        sourceConfig.setReadOnly(false);
        sourceConfig.setPoolName("SourcePool");
         
        // 선택 설정값 예시
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(3000);
//        config.setIdleTimeout(600000);
//        config.setMaxLifetime(1800000);

        sourceDs = new HikariDataSource(sourceConfig);
        ctx.setAttribute("SOURCE_DATA_SOURCE", sourceDs);
        
        HikariConfig replicaConfig = new HikariConfig();
        replicaConfig.setJdbcUrl("jdbc:mysql://localhost:6447/card_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
        replicaConfig.setUsername("root");
        replicaConfig.setPassword("root1234");
        replicaConfig.setReadOnly(true);
	    // 조회 요청이 많으므로 풀 사이즈를 더 크게 잡는 것이 유리
        replicaConfig.setMaximumPoolSize(20);
        replicaConfig.setPoolName("ReplicaPool");
        
        replicaDs = new HikariDataSource(replicaConfig);
        ctx.setAttribute("REPLICA_DATA_SOURCE", replicaDs);
        
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    	if (sourceDs  != null) sourceDs.close();
        if (replicaDs != null) replicaDs.close(); // 애플리케이션 종료 시 커넥션 풀 자원해제
    }
    
    public static DataSource getSourceDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("SOURCE_DATA_SOURCE");
    }

    public static DataSource getReplicaDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("REPLICA_DATA_SOURCE");
    }
}
