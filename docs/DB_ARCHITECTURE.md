# DB 아키텍처 정리

## 1. 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Java 애플리케이션 (Tomcat)               │
│                                                          │
│   Write DataSource          Read DataSource              │
│   (INSERT/UPDATE/DELETE)    (SELECT)                     │
│        ↓                         ↓                       │
└────────┼─────────────────────────┼───────────────────────┘
         ↓                         ↓
┌────────────────────────────────────────────┐
│           MySQL Router (Docker)             │
│                                             │
│   포트 6446 (R/W)       포트 6447 (R/O)      │
│        ↓                    ↓                │
└────────┼────────────────────┼────────────────┘
         ↓                    ↓
┌─────────────────────────────────────────────┐
│          InnoDB Cluster (Group Replication)   │
│                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  node1   │  │  node2   │  │  node3   │   │
│  │ PRIMARY  │  │SECONDARY │  │SECONDARY │   │
│  │  R/W     │  │  R/O     │  │  R/O     │   │
│  │ :3310    │  │ :3320    │  │ :3330    │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       ↕              ↕              ↕         │
│       └──── 자동 동기화 (복제) ──────┘         │
└───────────────────────────────────────────────┘
```

## 2. 각 구성요소의 역할

### MySQL Node (3대)
- **node1 (PRIMARY)**: 읽기 + 쓰기 가능. 모든 데이터 변경은 여기서 발생
- **node2, node3 (SECONDARY)**: 읽기만 가능. node1의 데이터가 자동으로 복제됨
- **Single-Primary 모드**: 쓰기는 항상 1대에서만 (데이터 충돌 방지)

### MySQL Router
- 앱과 DB 사이의 **중간 다리**
- 6446 포트: 쓰기 요청 → 현재 PRIMARY(node1)로 전달
- 6447 포트: 읽기 요청 → SECONDARY(node2, node3)에 분산 전달
- 앱은 node1/2/3 주소를 몰라도 됨. 라우터 주소만 알면 됨

### InnoDB Cluster (Group Replication)
- 3대의 MySQL이 하나의 클러스터로 묶임
- node1에서 INSERT하면 → node2, node3에 **자동 복제**
- 합의 프로토콜(Paxos 기반): 데이터 변경 시 과반수(2대 이상) 동의해야 커밋 승인

## 3. 데이터 복제 원리

```
1. 앱이 INSERT 실행
   → Router(6446) → node1

2. node1이 트랜잭션 실행
   → binlog(바이너리 로그)에 기록

3. Group Replication이 binlog를 node2, node3에 전파
   → 과반수(2/3) 이상 수신 확인 → 커밋 완료

4. node2, node3가 받은 binlog를 자기 DB에 적용
   → 동일한 데이터 상태 유지
```

**GTID (Global Transaction ID)**: 모든 트랜잭션에 고유 번호가 붙어서, 어떤 트랜잭션이 어디까지 복제됐는지 정확히 추적 가능

## 4. 장애 대응 (자동 Failover)

```
정상 상태:
  node1(PRIMARY) ← 쓰기
  node2(SECONDARY) ← 읽기
  node3(SECONDARY) ← 읽기

node1 장애 발생:
  node1(DOWN) ✘
  node2 → 자동으로 PRIMARY 승격 ← 쓰기
  node3(SECONDARY) ← 읽기

  Router가 자동 감지:
    6446(쓰기) → node2로 변경
    6447(읽기) → node3만 사용

앱 코드 변경 없음! Router가 알아서 처리.
```

- 3대 중 **1대까지 장애 허용** (과반수 2대 생존 필요)
- 2대 동시에 죽으면 클러스터 중단 (과반수 불가)

## 5. Read/Write 분리 원리

```
쓰기 (INSERT/UPDATE/DELETE):
  앱 → Router:6446 → PRIMARY(node1) → 복제 → node2, node3

읽기 (SELECT):
  앱 → Router:6447 → SECONDARY(node2 또는 node3)
```

**왜 분리하나?**
- 쓰기는 PRIMARY 1대에서만 가능 (데이터 일관성)
- 읽기는 SECONDARY 2대에 분산 → PRIMARY 부하 감소
- 카드 내역 조회 서비스 특성상 **읽기가 90% 이상** → 분리하면 효과 큼

## 6. 인덱스와 조회 성능

```sql
-- CARD_TRANSACTION 테이블 인덱스
PRIMARY KEY (ID)                    -- 기본키
KEY idx_seq_bas_yh (SEQ, BAS_YH)   -- 고객번호 + 분기
KEY idx_bas_yh (BAS_YH)            -- 분기
```

**로그인 시** (SEQ 존재 확인):
```sql
SELECT 1 FROM CARD_TRANSACTION WHERE SEQ = 'ABC123' LIMIT 1;
-- idx_seq_bas_yh 인덱스 사용 → 538만건에서 즉시 검색
```

**카드 내역 조회**:
```sql
SELECT * FROM CARD_TRANSACTION WHERE SEQ = 'ABC123' AND BAS_YH = '2023q1';
-- idx_seq_bas_yh (SEQ, BAS_YH) 복합 인덱스 → 두 조건 모두 인덱스로 검색
```

인덱스 없으면 500만건 풀스캔, 있으면 B-Tree로 약 20번 비교만에 찾음.

## 7. Docker 동작 원리

### Docker란?

격리된 가상 환경. 내 맥에 MySQL을 직접 설치하는 대신, 독립된 상자(컨테이너) 안에서 실행하는 것.

```
내 맥 (macOS)
├── 로컬 MySQL (3306) — brew로 설치한 거
│
└── Docker Desktop
    └── 가상 리눅스 환경
        ├── 컨테이너: mysql-node1 (내부 3306)
        ├── 컨테이너: mysql-node2 (내부 3306)
        ├── 컨테이너: mysql-node3 (내부 3306)
        └── 컨테이너: mysql-router (내부 6446, 6447)
```

각 컨테이너는 독립된 리눅스 서버처럼 동작. 서로 영향 안 줌.

### 컨테이너 vs 가상머신

```
일반 가상머신 (VM):                  Docker 컨테이너:
┌──────────────────┐               ┌──────────────────┐
│     앱 (MySQL)   │               │     앱 (MySQL)    │
│   게스트 OS 전체   │                │   필요한 파일만     │
│  (Ubuntu 전체)    │               │  (MySQL 바이너리)  │
│   가상 하드웨어     │               │                  │
└──────────────────┘               └──────────────────┘
    수 GB, 부팅 수 분                  수백 MB, 시작 수 초
```

VM은 OS 전체를 띄우지만, 컨테이너는 앱 실행에 필요한 것만 담아서 가볍고 빠름.

### 이미지 vs 컨테이너

```
이미지 (mysql/mysql-server:8.0)     →  설계도 (클래스)
    ↓ docker-compose up
컨테이너 (mysql-node1, node2...)    →  실제 실행 (인스턴스)
```

- **이미지**: MySQL이 설치된 리눅스 환경의 스냅샷. 변경 불가.
- **컨테이너**: 이미지를 기반으로 실행된 프로세스. 같은 이미지로 여러 컨테이너 생성 가능.
- 우리는 `mysql/mysql-server:8.0` 이미지 하나로 node1, node2, node3 3개 컨테이너를 만든 것.

### 포트 매핑

```
내 맥                         Docker 컨테이너
┌──────────┐                 ┌──────────────┐
│          │   3310 → 3306   │  mysql-node1 │
│          ├────────────────→│  (내부 3306)  │
│          │                 └──────────────┘
│          │                 ┌──────────────┐
│  호스트    │   3320 → 3306   │  mysql-node2 │
│          ├────────────────→│  (내부 3306)  │
│          │                 └──────────────┘
│          │                 ┌──────────────┐
│          │   3330 → 3306   │  mysql-node3 │
│          ├────────────────→│  (내부 3306)  │
└──────────┘                 └──────────────┘
```

- 컨테이너 안에서는 전부 3306 포트
- 내 맥에서 구분하려면 포트를 다르게 매핑 (3310, 3320, 3330)
- `mysql -P 3310` → Docker가 node1의 3306으로 전달

### Docker 네트워크 (cluster-net)

```
┌─── cluster-net (가상 네트워크) ────────────────┐
│                                                │
│  mysql-node1 ←→ mysql-node2 ←→ mysql-node3    │
│      ↑              ↑              ↑           │
│      └──── hostname으로 서로 통신 ────┘           │
│                                                │
│  mysql-router → node1, node2, node3 접속       │
└────────────────────────────────────────────────┘
```

- 같은 네트워크 안에 있으면 컨테이너 이름으로 통신 가능
- node1에서 `mysql-node2:3306`으로 접속 → Docker DNS가 node2 IP로 해석
- 외부(호스트)에서는 이 네트워크에 직접 접근 불가 → 포트 매핑으로 접속

### 볼륨 (데이터 영구 저장)

```
컨테이너 삭제해도 데이터 유지:
  mysql-node1-data (볼륨) ←→ /var/lib/mysql (컨테이너 안 MySQL 데이터)

docker-compose down      → 컨테이너 삭제, 볼륨 유지 (데이터 살아있음)
docker-compose down -v   → 컨테이너 + 볼륨 삭제 (데이터 초기화)
docker-compose up -d     → 볼륨 있으면 기존 데이터로 시작
```

### docker-compose up 하면 일어나는 일

```
1. Docker가 이미지(mysql/mysql-server:8.0) 확인
   → 없으면 자동 다운로드

2. 컨테이너 4개 생성
   → 각각 독립된 리눅스 환경에서 MySQL 실행

3. 볼륨 연결
   → 이전 데이터가 있으면 그대로 사용

4. 설정 파일 마운트
   → node1.cnf → /etc/my.cnf (컨테이너 안에 넣어줌)
   → init-root.sql → /docker-entrypoint-initdb.d/ (최초 실행 시 자동 실행)

5. 네트워크 연결
   → 4개 컨테이너를 cluster-net에 연결

6. healthcheck 시작
   → 5초마다 mysqladmin ping

7. 3개 노드 healthy 되면 → Router 시작
   → depends_on 조건 충족
```

### Docker를 쓰는 이유

1. **환경 통일**: 팀원 누구나 `docker-compose up`으로 동일한 DB 환경
2. **격리**: 로컬 MySQL과 충돌 없이 3대 클러스터 운영
3. **재현성**: 문제 생기면 `down -v` → `up -d`로 깨끗하게 재시작
4. **배포 용이**: docker-compose.yml + cnf 파일만 공유하면 됨

### 컨테이너 구성 요약

| 컨테이너 | 이미지 | 호스트 포트 | 역할 |
|----------|--------|------------|------|
| mysql-node1 | mysql/mysql-server:8.0 | 3310 | PRIMARY (R/W) |
| mysql-node2 | mysql/mysql-server:8.0 | 3320 | SECONDARY (R/O) |
| mysql-node3 | mysql/mysql-server:8.0 | 3330 | SECONDARY (R/O) |
| mysql-router | mysql/mysql-router:8.0 | 6446, 6447 | 라우팅 |

- 4개 컨테이너가 `cluster-net` Docker 네트워크로 연결
- 컨테이너 간 통신: 내부 포트 3306 (Docker DNS로 hostname 해석)
- 호스트에서 접속: 매핑된 포트 (3310, 3320, 3330, 6446, 6447)

## 8. Docker 설정 파일 상세

### 프로젝트 파일 구조

```
docker/
├── mysql/
│   ├── node1.cnf        # node1 MySQL 설정
│   ├── node2.cnf        # node2 MySQL 설정
│   ├── node3.cnf        # node3 MySQL 설정
│   └── init-root.sql    # 계정 초기화 스크립트
├── setup-cluster.sh     # 클러스터 자동 셋업 스크립트
docker-compose.yml       # 전체 컨테이너 정의
```

### docker-compose.yml 상세

#### MySQL 노드 (node1 기준, node2/node3도 동일 구조)

```yaml
mysql-node1:
  image: mysql/mysql-server:8.0
  container_name: mysql-node1
  hostname: mysql-node1
  ports:
    - "3310:3306"
  environment:
    MYSQL_ROOT_PASSWORD: root1234
    MYSQL_ROOT_HOST: '%'
  volumes:
    - mysql-node1-data:/var/lib/mysql
    - ./docker/mysql/node1.cnf:/etc/my.cnf
    - ./docker/mysql/init-root.sql:/docker-entrypoint-initdb.d/init-root.sql
  networks:
    - cluster-net
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot1234"]
    interval: 5s
    timeout: 3s
    retries: 20
    start_period: 30s
```

각 항목의 의미:

| 항목 | 설정값 | 왜 이렇게 했나 |
|------|--------|----------------|
| `image` | mysql/mysql-server:8.0 | Oracle 공식 MySQL 8.0 이미지. InnoDB Cluster 지원 |
| `container_name` | mysql-node1 | `docker ps`, `docker exec`에서 사용할 이름 |
| `hostname` | mysql-node1 | 컨테이너 내부 hostname. 노드끼리 이 이름으로 통신 |
| `ports: 3310:3306` | 호스트:컨테이너 | 호스트에서 3310으로 접속 → 컨테이너의 3306으로 전달. 3대를 구분하기 위해 3310/3320/3330 사용 |
| `MYSQL_ROOT_PASSWORD` | root1234 | root@'%' 계정 비밀번호. 이 값으로 외부에서 접속 |
| `MYSQL_ROOT_HOST: '%'` | % | root 계정을 어디서든 접속 가능하게. 없으면 root@'localhost'만 생성돼서 외부 접속 불가 |
| `volumes: mysql-node1-data` | Named Volume | MySQL 데이터 영구 저장. 컨테이너 재시작해도 데이터 유지 |
| `volumes: node1.cnf:/etc/my.cnf` | 설정 파일 마운트 | InnoDB Cluster에 필요한 MySQL 설정을 컨테이너에 주입 |
| `volumes: init-root.sql` | 초기화 스크립트 마운트 | `/docker-entrypoint-initdb.d/`에 넣으면 최초 1회 자동 실행 |
| `networks: cluster-net` | Docker 네트워크 | 4개 컨테이너가 같은 네트워크에서 hostname으로 통신 |
| `healthcheck` | mysqladmin ping | 5초마다 MySQL 살아있는지 확인. `docker ps`에 (healthy) 표시 |
| `start_period: 30s` | 30초 | MySQL 초기화에 시간이 걸리니 30초간은 실패해도 무시 |

#### MySQL Router

```yaml
mysql-router:
  image: mysql/mysql-router:8.0
  container_name: mysql-router
  ports:
    - "6446:6446"
    - "6447:6447"
  environment:
    MYSQL_HOST: mysql-node1
    MYSQL_PORT: "3306"
    MYSQL_USER: root
    MYSQL_PASSWORD: root1234
  depends_on:
    mysql-node1:
      condition: service_healthy
    mysql-node2:
      condition: service_healthy
    mysql-node3:
      condition: service_healthy
  networks:
    - cluster-net
  restart: on-failure
```

| 항목 | 설정값 | 왜 이렇게 했나 |
|------|--------|----------------|
| `ports: 6446` | R/W 포트 | 쓰기 요청 → 현재 PRIMARY 노드로 전달 |
| `ports: 6447` | R/O 포트 | 읽기 요청 → SECONDARY 노드들에 분산 전달 |
| `MYSQL_HOST: mysql-node1` | 부트스트랩 대상 | Router 시작 시 node1에서 클러스터 구성 정보를 가져옴 |
| `depends_on: service_healthy` | 시작 순서 제어 | 3개 노드가 모두 healthy된 후에 Router 시작. 아직 초기화 중인데 Router가 먼저 뜨면 실패하니까 |
| `restart: on-failure` | 자동 재시작 | Router가 비정상 종료되면 Docker가 자동으로 다시 시작 |

#### volumes / networks

```yaml
volumes:
  mysql-node1-data:    # node1 데이터 저장소
  mysql-node2-data:    # node2 데이터 저장소
  mysql-node3-data:    # node3 데이터 저장소

networks:
  cluster-net:
    driver: bridge     # Docker 기본 네트워크 드라이버
```

- **Named Volume**: Docker가 관리하는 저장소. `docker-compose down`해도 유지, `down -v`하면 삭제
- **bridge 네트워크**: 같은 네트워크의 컨테이너끼리 hostname으로 통신 가능

### nodeN.cnf (MySQL 설정 파일)

node1.cnf 기준 (node2/node3는 server-id와 report-host만 다름):

```ini
[mysqld]
# === MySQL 기본 설정 ===
skip-host-cache                # hostname 캐시 비활성화 (Docker 환경에서 DNS 변경 즉시 반영)
skip-name-resolve              # DNS 역방향 조회 비활성화 (접속 속도 향상)
datadir=/var/lib/mysql         # 데이터 저장 경로
socket=/var/lib/mysql/mysql.sock
secure-file-priv=/var/lib/mysql-files
user=mysql
pid-file=/var/run/mysqld/mysqld.pid

# === InnoDB Cluster 필수 설정 ===
server-id=1                    # 노드 고유 번호 (node2=2, node3=3). 복제 시 노드 식별용
log-bin=mysql-bin              # 바이너리 로그 활성화. 모든 데이터 변경을 기록 → 복제의 기반
gtid-mode=ON                   # GTID 활성화. 트랜잭션마다 고유 ID 부여 → 복제 위치 추적
enforce-gtid-consistency=ON    # GTID 일관성 강제. GTID와 호환 안 되는 SQL 차단
binlog-checksum=NONE           # Group Replication 요구사항. 체크섬 비활성화
log-slave-updates=ON           # SECONDARY도 binlog 기록. 체이닝 복제 지원
binlog-format=ROW              # 행 단위 복제. SQL문이 아닌 실제 변경된 행 데이터를 복제 → 정확성 보장
report-host=mysql-node1        # 클러스터에서 이 노드를 식별하는 hostname
disabled-storage-engines=MyISAM,BLACKHOLE,FEDERATED,ARCHIVE,MEMORY
                               # InnoDB만 사용 강제. 다른 엔진은 복제 미지원
```

왜 `/etc/my.cnf`에 직접 마운트하나?
- `mysql/mysql-server:8.0` 이미지는 `/etc/mysql/conf.d/`나 `/etc/my.cnf.d/` 경로를 포함하지 않음
- `/etc/my.cnf`를 통째로 덮어쓰는 방식이라 MySQL 기본 설정도 함께 포함해야 함

### init-root.sql (계정 초기화 스크립트)

```sql
-- root@'localhost' 비밀번호를 root@'%'와 동일하게 맞춤
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root1234';

-- 클러스터 관리 전용 계정 생성
CREATE USER IF NOT EXISTS 'clusteradmin'@'%' IDENTIFIED BY 'admin1234';
GRANT ALL PRIVILEGES ON *.* TO 'clusteradmin'@'%' WITH GRANT OPTION;
CREATE USER IF NOT EXISTS 'clusteradmin'@'localhost' IDENTIFIED BY 'admin1234';
GRANT ALL PRIVILEGES ON *.* TO 'clusteradmin'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

왜 이 스크립트가 필요한가?

1. **root@'localhost' 비밀번호 문제**
   - `MYSQL_ROOT_PASSWORD`는 `root@'%'`만 설정함
   - `root@'localhost'`는 별도 계정으로 비밀번호가 다를 수 있음
   - 클러스터 작업 시 localhost 접속에서 인증 실패 발생 → 비밀번호 통일로 해결

2. **clusteradmin 계정**
   - root 대신 클러스터 관리 전용 계정 사용
   - `@'%'`와 `@'localhost'` 둘 다 생성 → 외부/내부 접속 모두 가능
   - MySQL Shell에서 `dba.configureInstance()`, `cluster.addInstance()` 등에 이 계정 사용

3. **실행 시점**
   - `/docker-entrypoint-initdb.d/`에 있는 .sql 파일은 컨테이너 최초 생성 시에만 실행됨
   - 데이터 디렉토리가 비어있을 때만 동작 (두 번째 시작부터는 실행 안 됨)
   - 그래서 `docker-compose down -v`로 볼륨 삭제 후 재생성하면 다시 실행됨

### 계정 정리

| 계정 | 비밀번호 | 용도 |
|------|----------|------|
| root@'%' | root1234 | 외부(호스트)에서 DB 접속, 데이터 조회/복원 |
| root@'localhost' | root1234 | 컨테이너 내부 접속, healthcheck |
| clusteradmin@'%' | admin1234 | 클러스터 생성/관리 (외부에서) |
| clusteradmin@'localhost' | admin1234 | 클러스터 내부 작업 (clone, recovery 등) |


## 9. 클러스터 구축 과정

### Step 1. Docker 컨테이너 실행
```bash
docker-compose up -d
```
- mysql-node1(3310), mysql-node2(3320), mysql-node3(3330), mysql-router(6446/6447) 실행
- 각 노드는 `docker/mysql/nodeN.cnf` 설정 파일 사용 (server-id, gtid, binlog 등)
- `docker/mysql/init-root.sql`로 컨테이너 시작 시 root@localhost 비밀번호 + clusteradmin 계정 자동 생성

### Step 2. configureInstance (3대 전부)
```bash
mysqlsh
```
```javascript
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3310')
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3320')
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3330')
```
- InnoDB Cluster에 필요한 MySQL 설정값(binlog_transaction_dependency_tracking 등)을 자동 세팅

### Step 3. 클러스터 생성
```javascript
shell.connect('clusteradmin:admin1234@127.0.0.1:3310')
var cluster = dba.createCluster('cardCluster')
```
- node1을 PRIMARY로 클러스터 생성

### Step 4. 데이터 복원
```bash
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 -e "CREATE DATABASE IF NOT EXISTS card_db;"
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 card_db < ~/card_db_backup.sql
```
- CARD_TRANSACTION 테이블: 538만건 (PK + 인덱스 포함)
- 백업 파일: `~/card_db_backup.sql`

### Step 5. 노드 추가
mysqlsh를 Docker 네트워크 안에서 실행 (로컬 MySQL 3306 포트 충돌 방지):
```bash
docker run -it --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 mysqlsh
```
```javascript
shell.connect('clusteradmin:admin1234@mysql-node1:3306')
var cluster = dba.getCluster()
cluster.addInstance('clusteradmin:admin1234@mysql-node2:3306', {recoveryMethod: 'incremental'})
cluster.addInstance('clusteradmin:admin1234@mysql-node3:3306', {recoveryMethod: 'incremental'})
```
- incremental recovery: 클러스터의 GTID 차이분만 동기화
- 추가 완료 후 node2, node3에 538만건 자동 복제 확인

### Step 6. 검증

**클러스터 상태 확인:**
```javascript
cluster.status()
// status: "OK", 3대 모두 ONLINE, "can tolerate up to ONE failure"
```

**Router 동작 확인:**
```bash
# 쓰기 포트 → PRIMARY(node1)로 연결
mysql -u root -proot1234 -h 127.0.0.1 -P 6446 -e "SELECT @@hostname;"
# → mysql-node1

# 읽기 포트 → SECONDARY(node2 또는 node3)로 연결
mysql -u root -proot1234 -h 127.0.0.1 -P 6447 -e "SELECT @@hostname;"
# → mysql-node2 (또는 mysql-node3, 로드밸런싱)
```

**자동 Failover 테스트:**
```bash
# PRIMARY 강제 중지
docker stop mysql-node1

# 10~20초 후 확인 → 다른 노드가 PRIMARY로 자동 승격
mysql -u root -proot1234 -h 127.0.0.1 -P 6446 -e "SELECT @@hostname;"
# → mysql-node2 (자동 승격됨)

# node1 복구 → SECONDARY로 복귀
docker start mysql-node1
```

## 10. 트러블슈팅 기록

### cnf 마운트 경로 문제
- `mysql/mysql-server:8.0` 이미지는 `/etc/mysql/conf.d/`나 `/etc/my.cnf.d/`를 포함하지 않음
- `/etc/my.cnf`에 직접 마운트해야 함 (기본 MySQL 설정도 포함 필요)

### root@'%' vs root@'localhost' 비밀번호 불일치
- `MYSQL_ROOT_PASSWORD`는 `root@'%'`만 설정
- `root@'localhost'`는 별도 계정으로 비밀번호가 다를 수 있음
- `init-root.sql`로 컨테이너 시작 시 자동으로 비밀번호 통일 + clusteradmin 계정 생성

### mysqlsh 호스트 실행 시 포트 충돌
- 로컬 MySQL이 3306에서 실행 중이면, mysqlsh가 노드의 report-host(mysql-node2:3306)를 /etc/hosts → 127.0.0.1:3306(로컬 MySQL)로 해석
- 해결: mysqlsh를 Docker 컨테이너 안에서 실행하여 Docker DNS 사용