package dev.common;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
// import javax.sql.DataSource;  // [주석처리] Spring이 DataSource를 관리

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

// import com.zaxxer.hikari.HikariConfig;    // [주석처리] SpringConfig로 이동
// import com.zaxxer.hikari.HikariDataSource; // [주석처리] SpringConfig로 이동

import dev.config.SpringConfig;

@WebListener
@Component
public class ApplicationContextListener implements ServletContextListener {

//    private HikariDataSource sourceDs;  // [주석처리] 미사용
//    private HikariDataSource replicaDs; // [주석처리] 미사용
//    private HikariDataSource ds;        // [주석처리] Spring이 관리
    private AnnotationConfigApplicationContext springContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("컨텍스트 초기화 됨");
        ServletContext ctx = sce.getServletContext();

//        try {  // [주석처리] SpringConfig의 DataSource Bean이 드라이버 로딩 처리
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

//        HikariConfig sourceConfig  = new HikariConfig();
//        // 필수 설정값(별도의 설정파일로 분리 가능, ex. jdbc.properties)
//        // port :
//        sourceConfig .setJdbcUrl("jdbc:mysql://mysql-router:6446/card_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
//        sourceConfig .setUsername("root");
//        sourceConfig .setPassword("root1234");
//        sourceConfig.setReadOnly(false);
//        sourceConfig.setPoolName("SourcePool");
//
//        // 선택 설정값 예시
////        config.setMaximumPoolSize(10);
////        config.setMinimumIdle(2);
////        config.setConnectionTimeout(3000);
////        config.setIdleTimeout(600000);
////        config.setMaxLifetime(1800000);
//
//        sourceDs = new HikariDataSource(sourceConfig);
//        ctx.setAttribute("SOURCE_DATA_SOURCE", sourceDs);
//
//        HikariConfig replicaConfig = new HikariConfig();
//        replicaConfig.setJdbcUrl("jdbc:mysql://mysql-router:6447/card_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
//        replicaConfig.setUsername("root");
//        replicaConfig.setPassword("root1234");
//        replicaConfig.setReadOnly(true);
//        // 조회 요청이 많으므로 풀 사이즈를 더 크게 잡는 것이 유리
//        replicaConfig.setMaximumPoolSize(20);
//        replicaConfig.setPoolName("ReplicaPool");
//
//        replicaDs = new HikariDataSource(replicaConfig);
//        ctx.setAttribute("REPLICA_DATA_SOURCE", replicaDs);

//        HikariConfig config = new HikariConfig();  // [주석처리] SpringConfig로 이동
//        config.setJdbcUrl("jdbc:mysql://localhost:3306/card_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8");
//        config.setUsername("root");
//        config.setPassword("1234");
//        ds = new HikariDataSource(config);
//        ctx.setAttribute("DATA_SOURCE", ds);

        springContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        ctx.setAttribute("SPRING_CONTEXT", springContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        if (sourceDs  != null) sourceDs.close();  // [주석처리]
//        if (replicaDs != null) replicaDs.close();  // [주석처리]
//        if (ds != null) ds.close();                // [주석처리]
        if (springContext != null) springContext.close();
    }

//    public static DataSource getSourceDataSource(ServletContext ctx) {  // [주석처리]
//        return (DataSource) ctx.getAttribute("SOURCE_DATA_SOURCE");
//    }

//    public static DataSource getReplicaDataSource(ServletContext ctx) {  // [주석처리]
//        return (DataSource) ctx.getAttribute("REPLICA_DATA_SOURCE");
//    }

//    public static DataSource getDataSource(ServletContext ctx) {  // [주석처리]
//        return (DataSource) ctx.getAttribute("DATA_SOURCE");
//    }

    public static AnnotationConfigApplicationContext getSpringContext(ServletContext ctx) {
        return (AnnotationConfigApplicationContext) ctx.getAttribute("SPRING_CONTEXT");
    }
}
