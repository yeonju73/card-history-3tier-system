#!/bin/bash

echo "========================================="
echo " InnoDB Cluster 자동 셋업"
echo "========================================="

# 1. configureInstance
echo ""
echo "[Step 1] configureInstance (3대)..."
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3310', {restart: true})"
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3320', {restart: true})"
mysqlsh --no-wizard -e "dba.configureInstance('clusteradmin:admin1234@127.0.0.1:3330', {restart: true})"
echo "[Step 1] 완료"

# 2. 데이터 복원
echo ""
echo "[Step 2] 데이터 복원..."
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 -e "CREATE DATABASE IF NOT EXISTS card_db;"
mysql -u root -proot1234 -h 127.0.0.1 -P 3310 card_db < ~/card_db_backup.sql
echo "[Step 2] 완료"

# 3. 클러스터 생성 + 노드 추가 (Docker 네트워크 안에서 실행)
echo ""
echo "[Step 3] 클러스터 생성 + 노드 추가..."
docker run --rm --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 mysqlsh --no-wizard -e "
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
mysql -u root -proot1234 -h 127.0.0.1 -P 6446 -e "SELECT @@hostname;"
echo "읽기 포트(6447):"
mysql -u root -proot1234 -h 127.0.0.1 -P 6447 -e "SELECT @@hostname;"
echo "데이터 건수:"
mysql -u root -proot1234 -h 127.0.0.1 -P 6446 -e "SELECT COUNT(*) AS total_rows FROM card_db.CARD_TRANSACTION;"

echo ""
echo "========================================="
echo " 셋업 완료!"
echo "========================================="
