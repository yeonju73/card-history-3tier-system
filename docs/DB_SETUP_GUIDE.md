# DB 클러스터 셋업 가이드 (팀원용)

## 사전 준비

- Docker Desktop 설치 및 실행
- 백업 파일 `card_db_backup.sql`을 홈 디렉토리(`~/`)에 저장

> MySQL Shell, MySQL Client 등 별도 설치 불필요. 모든 명령이 Docker 안에서 실행됨.

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

## Step 2. 클러스터 자동 셋업

```bash
./docker/setup-cluster.sh
```

이 스크립트가 아래 작업을 자동으로 수행:
1. configureInstance (3대)
2. 데이터 복원 (`~/card_db_backup.sql`)
3. 클러스터 생성 + 노드 추가
4. 라우터/데이터 검증

완료 시 "셋업 완료!" 메시지가 출력됨.

## Step 3. 검증 (선택)

스크립트가 자동으로 검증하지만, 직접 확인하고 싶으면:

```bash
# 쓰기 포트 확인 (PRIMARY 노드 반환)
docker run --rm --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 \
  mysql -u root -proot1234 -h mysql-router -P 6446 -e "SELECT @@hostname;"

# 읽기 포트 확인 (SECONDARY 노드 반환)
docker run --rm --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 \
  mysql -u root -proot1234 -h mysql-router -P 6447 -e "SELECT @@hostname;"

# 데이터 건수 확인
docker run --rm --network card-history-3tier-system_cluster-net mysql/mysql-server:8.0 \
  mysql -u root -proot1234 -h mysql-router -P 6446 -e "SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;"
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

## 트러블슈팅

### 스크립트 실행 권한 없음
```bash
chmod +x ./docker/setup-cluster.sh
```

### 스크립트 실행 중 에러
- 컨테이너가 healthy 상태인지 `docker ps`로 확인
- healthy가 아니면 잠시 기다린 후 다시 실행
- 그래도 안 되면 `docker-compose down -v` 후 Step 1부터 다시
