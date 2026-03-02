package dev.filter;

import redis.clients.jedis.Jedis;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * Servlet 코드를 수정하지 않고 세션을 Redis로 위임하는 필터입니다.
 * (JedisPool 에러 제거, Gson 에러 제거, Java 기본 직렬화 적용 완료 버전)
 */
@WebFilter("/*")
public class RedisSessionFilter implements Filter {
    
    // Redis 접속 정보 (도커 환경의 localhost 포워딩 포트)
    private static final String REDIS_HOST = "redis-session";
    private static final int REDIS_PORT = 6379;
    private static final String COOKIE_NAME = "REDIS_SESSION_ID";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 정적 파일(css, js, 이미지 등)은 세션 처리를 생략하여 속도를 높이고 로그 도배를 막습니다.
        String uri = httpRequest.getRequestURI();
        if (uri.matches(".*\\.(css|png|jpg|jpeg|gif|ico)$")) {
            chain.doFilter(request, response);
            return;
        }
        
        // 원본 request를 Redis Wrapper로 교체하여 서블릿으로 넘깁니다.
        RedisSessionRequestWrapper wrappedRequest = new RedisSessionRequestWrapper(httpRequest, httpResponse);
        
        // 서블릿 로직 실행 (사용자의 기존 코드가 여기서 실행됨)
        chain.doFilter(wrappedRequest, response);

        // 서블릿 실행이 끝난 후, 세션이 한 번이라도 사용되었다면 Redis에 동기화(저장) 합니다.
        if (wrappedRequest.isSessionInitialized()) {
            wrappedRequest.saveSession();
        }
    }

    @Override
    public void destroy() {}

    // =================================================================
    // 1. Request Wrapper 클래스 (request.getSession()을 가로채는 역할)
    // =================================================================
    class RedisSessionRequestWrapper extends HttpServletRequestWrapper {
        private RedisSession session;
        private HttpServletResponse response;
        private boolean initialized = false;

        public RedisSessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        public boolean isSessionInitialized() { 
            return initialized; 
        }

        @Override
        public HttpSession getSession(boolean create) {
            if (session != null) return session;

            // 쿠키에서 세션 ID 확인
            String sid = getSessionIdFromCookie();

            // 쿠키가 없다면 새로 만들고 브라우저에 구워줌
            if (sid == null) {
                if (!create) return null;
                sid = java.util.UUID.randomUUID().toString();
                addSessionCookie(sid);
                System.out.println("[REDIS LOG] 신규 사용자를 위한 세션 ID 발급: " + sid);
            }

            // Redis 세션 객체를 만들고 데이터를 로드함
            session = new RedisSession(sid, getServletContext());
            session.loadFromRedis();
            initialized = true;
            return session;
        }

        @Override
        public HttpSession getSession() { 
            return getSession(true); 
        }

        public void saveSession() {
            if (session != null) session.saveToRedis();
        }
        
        // 쿠키 추출 기능
        private String getSessionIdFromCookie() {
            Cookie[] cookies = getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (COOKIE_NAME.equals(c.getName())) return c.getValue();
                }
            }
            return null;
        }
        
        // 쿠키 추가 기능
        private void addSessionCookie(String sid) {
            Cookie c = new Cookie(COOKIE_NAME, sid);
            c.setPath("/");
            response.addCookie(c);
        }
    }

    // =================================================================
    // 2. Redis Session 클래스 (실제 데이터를 담고 Redis와 통신하는 역할)
    // =================================================================
    class RedisSession implements HttpSession {
        private String id;
        private Map<String, Object> attributes = new HashMap<>();
        private ServletContext context;
        private long creationTime;
        private long lastAccessedTime;
        private int maxInactiveInterval = 1800; // 30분 만료
        private boolean isInvalidated = false;

        public RedisSession(String id, ServletContext context) {
            this.id = id;
            this.context = context;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessedTime = this.creationTime;
        }

        // Redis에서 데이터 읽기 (자바 객체 역직렬화로 타입 유지)
        @SuppressWarnings("unchecked")
        public void loadFromRedis() {
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                String base64Data = jedis.get("session:" + id);
                if (base64Data != null) {
                    byte[] data = Base64.getDecoder().decode(base64Data);
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                         ObjectInputStream ois = new ObjectInputStream(bais)) {
                        attributes = (Map<String, Object>) ois.readObject();
                    }
                }
            } catch (Exception e) {
                System.err.println("[REDIS ERROR] 데이터 로드 실패: " + e.getMessage());
            }
        }

        // Redis에 데이터 저장하기 (자바 객체 직렬화로 타입 유지)
        public void saveToRedis() {
            if (isInvalidated) return;
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(attributes);
                    String base64Data = Base64.getEncoder().encodeToString(baos.toByteArray());
                    jedis.setex("session:" + id, maxInactiveInterval, base64Data);
                }
            } catch (Exception e) {
                System.err.println("[REDIS ERROR] 데이터 저장 실패: " + e.getMessage());
            }
        }

        @Override public void setAttribute(String name, Object value) { attributes.put(name, value); }
        @Override public Object getAttribute(String name) { return attributes.get(name); }
        @Override public void removeAttribute(String name) { attributes.remove(name); }
        @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
        
        @Override 
        public void invalidate() {
            isInvalidated = true;
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) { 
                jedis.del("session:" + id); 
            } catch (Exception e) {}
            attributes.clear();
        }

        @Override public long getCreationTime() { return creationTime; }
        @Override public String getId() { return id; }
        @Override public long getLastAccessedTime() { return lastAccessedTime; }
        @Override public ServletContext getServletContext() { return context; }
        @Override public void setMaxInactiveInterval(int interval) { this.maxInactiveInterval = interval; }
        @Override public int getMaxInactiveInterval() { return maxInactiveInterval; }
        @Override public boolean isNew() { return false; }

        // --- 필수 구현 완료 부분 ---
        @Override public Object getValue(String name) { return getAttribute(name); }
        @Override public String[] getValueNames() { return attributes.keySet().toArray(new String[0]); }
        @Override public void putValue(String name, Object value) { setAttribute(name, value); }
        @Override public void removeValue(String name) { removeAttribute(name); }
        @SuppressWarnings("deprecation")
        @Override public HttpSessionContext getSessionContext() { return null; }
    }
}