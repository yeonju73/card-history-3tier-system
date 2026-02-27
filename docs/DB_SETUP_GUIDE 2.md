# DB 클러스터 셋업 가이드 (팀원용)

## 사전 준비

- Docker Desktop 설치 및 실행
- MySQL Shell (`mysqlsh`) 설치: `brew install mysql-shell`
- 백업 파일 `card_db_backup.sql`을 홈 디렉토리(`~/`)에 저장

## Step 1. 컨테이너 실행

프로젝트 루트에서:

```bash
docker-compose up -d
```

3개 노드 모두 healthy 될 때까지 대기 (약 30초~1분):

```bash
docker ps
```

STATUS에 `(healthy)` 3개 보이면 다음 단계.

## Step 2. configureInstance

```bash
mysqlsh
```

```javascript
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3310')
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3320')
dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3330')
```

각각 `y` 입력. 완료 후 mysqlsh 종료:

```
\quit
```

## Step 3. 데이터 복원

```bash
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 -e "CREATE DATABASE IF NOT EXISTS card_db;"
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 card_db < ~/card_db_backup.sql
```

## Step 4. 클러스터 생성 + 노드 추가

로컬에 MySQL이 3306에서 실행 중이면, Docker 안에서 mysqlsh를 실행해야 함:

```bash
docker run -it --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 mysqlsh
```

> 로컬 MySQL이 없으면 그냥 터미널에서 `mysqlsh` 실행해도 됨 (이 경우 주소를 `127.0.0.1:3310` 등으로 사용)

Docker 안에서:

```javascript
// 클러스터 생성
shell.connect('clusteradmin:admin1234@mysql-node1:3306')
var cluster = dba.createCluster('cardCluster')

// 노드 추가
cluster.addInstance('clusteradmin:admin1234@mysql-node2:3306', {recoveryMethod: 'incremental'})
cluster.addInstance('clusteradmin:admin1234@mysql-node3:3306', {recoveryMethod: 'incremental'})

// 상태 확인
cluster.status()
```

3대 모두 `ONLINE`, status `"OK"` 나오면 성공.

## Step 5. 검증

mysqlsh 종료 후 터미널에서:

```bash
# 라우터 쓰기 포트 확인 (PRIMARY 노드 반환)
mysql -u root -proot1234 -h 127.0.0.1 -P 6446 -e "SELECT @@hostname;"

# 라우터 읽기 포트 확인 (SECONDARY 노드 반환)
mysql -u root -proot1234 -h 127.0.0.1 -P 6447 -e "SELECT @@hostname;"

# 데이터 복제 확인 (SECONDARY에서 조회)
mysql -u root -proot1234 -h 127.0.0.1 -P 3320 -e "SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;"
# → 5,380,000건 이상 나오면 정상
```

## 접속 정보 요약

| 용도 | 호스트 | 포트 | 계정 | 비밀번호 |
|------|--------|------|------|----------|
| 쓰기 (Router) | 127.0.0.1 | 6446 | root | root1234 |
| 읽기 (Router) | 127.0.0.1 | 6447 | root | root1234 |
| node1 직접 접속 | 127.0.0.1 | 3310 | root | root1234 |
| node2 직접 접속 | 127.0.0.1 | 3320 | root | root1234 |
| node3 직접 접속 | 127.0.0.1 | 3330 | root | root1234 |
| 클러스터 관리 | - | - | clusteradmin | admin1234 |

## 종료 / 재시작

```bash
# 종료 (데이터 유지)
docker-compose down

# 종료 (데이터 삭제, 처음부터 다시)
docker-compose down -v

# 재시작
docker-compose up -d
```

> 주의: `docker-compose down -v`하면 볼륨이 삭제되어 클러스터 + 데이터가 모두 초기화됨. Step 1부터 다시 해야 함.
