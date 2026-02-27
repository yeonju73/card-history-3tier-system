#!/bin/bash

# Docker 네트워크 이름 자동 감지
NETWORK=$(docker network ls --format '{{.Name}}' | grep cluster-net)
MYSQL_IMG="mysql/mysql-server:8.0"

if [ -z "$NETWORK" ]; then
  echo "ERROR: cluster-net 네트워크를 찾을 수 없습니다. docker-compose up -d 를 먼저 실행하세요."
  exit 1
fi

echo "========================================="
echo " InnoDB Cluster 자동 셋업"
echo " 네트워크: $NETWORK"
echo "========================================="

# Docker 네트워크 안에서 mysqlsh 실행하는 함수
run_mysqlsh() {
  docker run --rm --network "$NETWORK" $MYSQL_IMG mysqlsh --no-wizard -e "$1"
}

# 1. configureInstance
echo ""
echo "[Step 1] configureInstance (3대)..."
run_mysqlsh "dba.configureInstance('clusteradmin:admin1234@mysql-node1:3306', {restart: true})"
run_mysqlsh "dba.configureInstance('clusteradmin:admin1234@mysql-node2:3306', {restart: true})"
run_mysqlsh "dba.configureInstance('clusteradmin:admin1234@mysql-node3:3306', {restart: true})"
echo "[Step 1] 완료"

# 2. 데이터 복원
echo ""
echo "[Step 2] 데이터 복원..."

BACKUP_FILE="$HOME/card_db_backup.sql"
if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: $BACKUP_FILE 파일을 찾을 수 없습니다."
  echo "홈 디렉토리(~/)에 card_db_backup.sql 파일을 넣어주세요."
  exit 1
fi

docker run --rm --network "$NETWORK" $MYSQL_IMG mysql -u root -proot1234 -h mysql-node1 -P 3306 -e "CREATE DATABASE IF NOT EXISTS card_db;"
docker run --rm -i --network "$NETWORK" -v "$BACKUP_FILE":/backup.sql $MYSQL_IMG mysql -u root -proot1234 -h mysql-node1 -P 3306 card_db -e "source /backup.sql"
echo "[Step 2] 완료"

# 3. 클러스터 생성 + 노드 추가
echo ""
echo "[Step 3] 클러스터 생성 + 노드 추가..."
run_mysqlsh "
shell.connect('clusteradmin:admin1234@mysql-node1:3306');
var cluster = dba.createCluster('cardCluster');
cluster.addInstance('clusteradmin:admin1234@mysql-node2:3306', {recoveryMethod: 'incremental'});
cluster.addInstance('clusteradmin:admin1234@mysql-node3:3306', {recoveryMethod: 'incremental'});
print(cluster.status());
"
echo "[Step 3] 완료"

# 4. 검증
echo ""
echo "[Step 4] 검증..."
echo "쓰기 포트(6446):"
docker run --rm --network "$NETWORK" $MYSQL_IMG mysql -u root -proot1234 -h mysql-router -P 6446 -e "SELECT @@hostname;"
echo "읽기 포트(6447):"
docker run --rm --network "$NETWORK" $MYSQL_IMG mysql -u root -proot1234 -h mysql-router -P 6447 -e "SELECT @@hostname;"
echo "데이터 건수:"
docker run --rm --network "$NETWORK" $MYSQL_IMG mysql -u root -proot1234 -h mysql-router -P 6446 -e "SELECT COUNT(*) AS total_rows FROM card_db.CARD_TRANSACTION;"

echo ""
echo "========================================="
echo " 셋업 완료!"
echo "========================================="
