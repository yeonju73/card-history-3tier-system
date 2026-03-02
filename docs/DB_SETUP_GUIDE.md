# DB 클러스터 셋업 가이드 (팀원용)

## 사전 준비

- Docker Desktop 설치 및 실행
- MySQL Shell 설치
  - Mac: `brew install mysql-shell`
  - Windows: [공식 다운로드](https://dev.mysql.com/downloads/shell/) 에서 MSI 설치
- 백업 파일 `card_db_backup.sql`을 프로젝트 루트 폴더(docker-compose.yml이 있는 곳)에 저장

## Step 1. 컨테이너 실행

```bash
docker-compose up -d
```

`docker ps`에서 3개 노드 모두 `(healthy)` 될 때까지 대기 (약 30초~1분)

## Step 2. configureInstance

```bash
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3310', {restart: true})"
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3320', {restart: true})"
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3330', {restart: true})"
```

실행 후 `docker ps`에서 3개 노드 다시 `(healthy)` 확인

## Step 3. 데이터 복원

```bash
docker exec mysql-node1 mysql -u root -proot1234 -e "CREATE DATABASE IF NOT EXISTS card_db;"
docker cp ./card_db_backup.sql mysql-node1:/backup.sql
docker exec mysql-node1 bash -c "mysql -u root -proot1234 card_db < /backup.sql"
```

> 538만건이라 수 분 소요될 수 있음

## Step 4. 클러스터 생성 + 노드 추가

```bash
mysqlsh --no-wizard -e "
shell.connect('clusteradmin:admin1234@127.0.0.1:3310');
var cluster = dba.createCluster('cardCluster');
cluster.addInstance('clusteradmin:admin1234@127.0.0.1:3320', {recoveryMethod: 'incremental'});
cluster.addInstance('clusteradmin:admin1234@127.0.0.1:3330', {recoveryMethod: 'incremental'});
print(cluster.status());
"
```

3대 모두 `ONLINE`, status `"OK"` 나오면 성공.

> 로컬에 MySQL이 3306에서 실행 중이면 addInstance에서 에러날 수 있음. 이 경우 로컬 MySQL을 중지하거나, Step 4만 Docker 안에서 실행:
> ```bash
> docker run -it --network <네트워크이름>_cluster-net mysql/mysql-server:8.0 mysqlsh
> ```
> 네트워크 이름은 `docker network ls`에서 확인

## Step 5. 검증

```bash
docker exec mysql-node1 mysql -u root -proot1234 -h mysql-router -P 6446 -e "SELECT @@hostname;"
docker exec mysql-node1 mysql -u root -proot1234 -h mysql-router -P 6447 -e "SELECT @@hostname;"
docker exec mysql-node1 mysql -u root -proot1234 -e "SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;"
```

- 6446: PRIMARY 노드 이름 반환
- 6447: SECONDARY 노드 이름 반환
- 538만건 이상 나오면 정상

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

## 트러블슈팅

### configureInstance 후 노드가 안 올라옴
- `docker ps`에서 healthy 확인 후 다음 단계 진행
- 안 뜨면 `docker-compose down -v` 후 Step 1부터 다시

### addInstance에서 Access denied
- 로컬 MySQL이 3306에서 실행 중인지 확인
- 실행 중이면 `brew services stop mysql` 후 재시도
- 또는 Docker 안에서 mysqlsh 실행 (Step 4 참고)

### 데이터 복원 실패
- `card_db_backup.sql` 파일이 프로젝트 루트에 있는지 확인
- `docker ps`에서 mysql-node1이 healthy인지 확인
