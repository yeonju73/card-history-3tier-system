 # CardLedger (3-Tier Architecture)

> 결제 내역을 한눈에 파악하는 카드 지출 관리 서비스

순수 Java Servlet 기반의 3티어 아키텍처를 직접 설계하고 구현한 프로젝트입니다.

---

## 서비스 소개

CardLedger는 고객 번호로 간편하게 로그인하고, 카드 결제 내역을 분기별로 분석해주는 웹 애플리케이션입니다.

### 주요 기능

**1. 로그인**
<img width="1911" height="857" alt="스크린샷 2026-02-27 103435" src="https://github.com/user-attachments/assets/cc27be11-a97e-4882-8c36-8f93346d3eb2" />
- 고객 번호 기반 간편 로그인
- 세션 관리 및 중복 로그인 처리
- 로그아웃 시 세션 완전 폐기 (쿠키 삭제 포함)

**2. 월별 소비 패턴**
<img width="1882" height="861" alt="image" src="https://github.com/user-attachments/assets/f15e6806-2f79-4ca1-9d74-6a25c497e00d" />
<img width="1901" height="331" alt="image" src="https://github.com/user-attachments/assets/2e587a73-2bbc-435b-b976-81b9c318a198" />
- 분기별 총 지출 조회
- 카테고리별 지출 현황 및 비율 시각화 (쇼핑, 식비, 의료, 교통 등)
- 고정지출 포함/제외 토글
- 최다 지출 카테고리 하이라이트

**3. 고정지출 등록**
<img width="1901" height="855" alt="image" src="https://github.com/user-attachments/assets/f58e83aa-251a-4180-89db-70a90d236533" />
- 매달 반복되는 지출 항목 등록 (주거, 보험/의료, 교육, 자동차 등)
- 세부 항목 선택 및 금액 입력
- 적용 월(분기) 지정
- 실시간 합계 및 등록 항목 미리보기

---


## 아키텍처 개요
<img width="1919" height="518" alt="image" src="https://github.com/user-attachments/assets/3be14842-4ff4-4361-bb9e-315f9dbb120b" />

```
클라이언트
 (웹 브라우저 / API 클라이언트)
        │
        │ HTTP 요청
        ▼
┌─────────────────────┐
│  Presentation 계층   │  Nginx (리버스 프록시 / 로드밸런서)
└─────────────────────┘
         │ 부하분산
   ┌─────┴─────┐
   ▼           ▼
┌──────┐    ┌──────┐
│ WAS1 │    │ WAS2 │   Application 계층 (Tomcat + Servlets & JSP)
│ 8080 │    │ 8090 │
└──┬───┘    └───┬──┘
   │  HikariCP  │
   ▼            ▼
┌──────────────────────────────────────────┐
│              Data 계층                    │
│                                          │
│          MySQL Router (Docker)           │
│     6446 (R/W)        6447 (R/O)         │
│          ↓                 ↓             │
│  ┌────────────────────────────────────┐  │
│  │       InnoDB Cluster (Docker)      │  │
│  │                                    │  │
│  │  node1(PRIMARY)    node2  node3    │  │
│  │    (R/W)         (SECONDARY, R/O)  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

---

## 계층별 구성

### Presentation 계층 — Nginx
- 클라이언트 요청을 받아 WAS로 **리버스 프록시**
- **라운드로빈 로드밸런싱**으로 WAS1, WAS2에 트래픽 분산
- 정적 리소스(HTML, CSS, JS) 직접 서빙으로 WAS 부하 절감

```nginx
upstream backend {
    server was1:8080;
    server was2:8090;
}

server {
    listen 80;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

> ⚠️ Nginx 단일 장애점(SPOF) 문제 인식 — 추후 이중화 고려

---

### Application 계층 — Tomcat WAS × 2
- **순수 Java Servlet / JSP** 기반 구현 (프레임워크 미사용)
- **Tomcat 세션 클러스터링**으로 WAS 간 세션 공유
  - WAS1 로그인 → WAS2에서도 세션 유지
  - WAS1 로그아웃 → WAS2에도 세션 invalidate 전파
- **HikariCP** 커넥션 풀로 DB 연결 관리
- `ApplicationContextListener`를 통해 DataSource를 서블릿 컨텍스트에 등록

```java
// 읽기 요청 → Replica
DataSource ds = ApplicationContextListener.getReplicaDataSource(getServletContext());

// 쓰기 요청 → Source
DataSource ds = ApplicationContextListener.getSourceDataSource(getServletContext());
```

---

### Data 계층 — MySQL InnoDB Cluster (Docker)
- **InnoDB Cluster** 3노드 구성 
  - node1 (Source): Read/Write
  - node2, node3 (Replica): Read Only
- **MySQL Router**가 R/W 자동 라우팅
  - 6446 포트 → Source (쓰기)
  - 6447 포트 → Replica (읽기, 로드밸런싱)
- **자동 Failover**: Source 장애 시 Replica가 자동 승격, Router가 즉시 감지하여 라우팅 변경
- **HikariCP** DataSource 2개로 코드 단에서 R/W 분리
  - `SELECT` → Router 6447 (Replica)
  - `INSERT` / `UPDATE` / `DELETE` → Router 6446 (Source)
- CARD_TRANSACTION 테이블 538만건, B-Tree 인덱스로 조회 최적화

```
┌──────────────────────────────────────────┐
│           MySQL Router (Docker)          │
│   포트 6446 (R/W)       포트 6447 (R/O)    │
└────────┬────────────────────┬────────────┘
         ↓                    ↓
┌──────────────────────────────────────────┐
│     InnoDB Cluster (Group Replication)   │
│                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐  │ 
│  │  node1   │ │  node2   │ │  node3   │  │
│  │ PRIMARY  │ │SECONDARY │ │SECONDARY │  │
│  │  :3310   │ │  :3320   │ │  :3330   │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘  │
│       └─── 자동 동기화 (복제) ──────┘        │
└──────────────────────────────────────────┘
```

---

## 주요 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java |
| Web | Servlet, JSP |
| WAS | Apache Tomcat × 2 |
| Proxy / LB | Nginx |
| DB Connection | HikariCP |
| Database | MySQL InnoDB Cluster |
| DB Routing | MySQL Router |
| Session | Tomcat Cluster Replication |
| Infra | Docker |

---

## 핵심 설계 포인트

### 1. 세션 클러스터링
프레임워크 없이 순수 Tomcat 클러스터링으로 WAS 간 세션을 공유합니다. 어느 WAS로 요청이 가더라도 동일한 세션을 보장합니다.

### 2. DB Read/Write 분리
InnoDB Cluster 3노드 + MySQL Router로 DB를 이중화합니다. Router가 쓰기(6446)는 PRIMARY로, 읽기(6447)는 SECONDARY로 자동 라우팅합니다. HikariCP DataSource를 2개 등록하여 코드 단에서도 R/W를 분리합니다.

### 3. 자동 Failover
Source 서버 장애 시 InnoDB Cluster가 Replica를 자동 승격하고, MySQL Router가 즉시 감지하여 라우팅을 변경합니다. 애플리케이션 코드 변경 없이 무중단 운영이 가능합니다.

### 4. 로드밸런싱
Nginx 라운드로빈으로 트래픽을 균등 분산합니다. 세션 클러스터링이 되어있어 Sticky Session 없이도 정상 동작합니다.

---

## 개선 가능한 부분
- Nginx SPOF 해소 (Active-Passive HA 구성)
- WAS 캐시 도입
- HTTPS 적용
