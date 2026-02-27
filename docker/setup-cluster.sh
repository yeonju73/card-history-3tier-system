#!/bin/bash

NETWORK="card-history-3tier-system_cluster-net"
MYSQL_IMG="mysql/mysql-server:8.0"

# Docker 네트워크 안에서 mysqlsh 실행하는 함수
run_mysqlsh() {
  docker run --rm --network $NETWORK $MYSQL_IMG mysqlsh --no-wizard -e "$1"
}

# Docker 네트워크 안에서 mysql 실행하는 함수
run_mysql() {
  docker run --rm --network $NETWORK $MYSQL_IMG mysql -u root -proot1234 -h "$1" -P 3306 "${@:2}"
}

echo "========================================="
echo " InnoDB Cluster 자동 셋업"
echo "========================================="

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
run_mysql mysql-node1 -e "CREATE DATABASE IF NOT EXISTS card_db;"
docker run --rm --network $NETWORK -v ~/card_db_backup.sql:/backup.sql $MYSQL_IMG mysql -u root -proot1234 -h mysql-node1 -P 3306 card_db -e "source /backup.sql"
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
run_mysql mysql-router -P 6446 -e "SELECT @@hostname;"
echo "읽기 포트(6447):"
run_mysql mysql-router -P 6447 -e "SELECT @@hostname;"
echo "데이터 건수:"
run_mysql mysql-router -P 6446 -e "SELECT COUNT(*) AS total_rows FROM card_db.CARD_TRANSACTION;"

echo ""
echo "========================================="
echo " 셋업 완료!"
echo "========================================="
