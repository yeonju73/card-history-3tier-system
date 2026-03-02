# Requirements Document

## Introduction

이 문서는 3-tier 아키텍처로 구현된 CardLedger 시스템의 전체 아키텍처와 각 계층(Presentation, Application, Data)에 대한 문서화 요구사항을 정의합니다. 시스템은 Master Nginx → Worker Nginx → WAS/Tomcat → MySQL Router → MySQL Cluster로 구성되며, Redis를 통한 세션 공유를 지원합니다.

## Glossary

- **Documentation_System**: 3-tier 아키텍처 문서를 생성하고 관리하는 시스템
- **Architecture_Document**: 전체 시스템 아키텍처를 설명하는 문서
- **Layer_Document**: 특정 계층(Nginx, WAS, MySQL Cluster, Redis)을 설명하는 개별 문서
- **Mermaid_Diagram**: Mermaid 문법으로 작성된 flowChart 다이어그램
- **Configuration_File**: 각 계층의 동작을 제어하는 설정 파일 (.conf, .cnf, web.xml 등)
- **Master_Nginx**: 외부 요청을 Worker Nginx로 부하분산하는 최상위 Nginx (포트 80)
- **Worker_Nginx**: WAS로 요청을 전달하고 부하분산하는 Nginx (2개 인스턴스)
- **WAS**: Web Application Server, Tomcat 기반 애플리케이션 서버 (포트 8080, 8090)
- **MySQL_Router**: MySQL Cluster의 읽기/쓰기 요청을 분리하는 라우터
- **MySQL_Cluster**: Source 1개와 Replica 2개로 구성된 MySQL 클러스터
- **Redis**: WAS 간 세션 공유를 위한 중앙 저장소
- **Filter_Chain**: WAS에서 요청을 처리하기 전 실행되는 필터 체인 (RedisSessionFilter → LoginSessionCheckFilter → EncodingFilter)

## Requirements

### Requirement 1: 전체 아키텍처 문서 생성

**User Story:** As a 개발자, I want 전체 3-tier 아키텍처를 한눈에 파악할 수 있는 문서, so that 시스템의 전체 구조와 요청 흐름을 이해할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create an Architecture_Document that includes a Mermaid_Diagram showing the complete request flow from client to database
2. THE Architecture_Document SHALL describe the overall system architecture in Korean
3. THE Architecture_Document SHALL list all major components (Master_Nginx, Worker_Nginx, WAS, MySQL_Router, MySQL_Cluster, Redis) with their roles
4. THE Architecture_Document SHALL explain the request processing flow through all layers
5. WHEN a component has configuration files, THE Architecture_Document SHALL reference the Layer_Document for detailed information

### Requirement 2: Presentation 계층 문서 생성

**User Story:** As a 개발자, I want Nginx 계층의 구조와 부하분산 방식을 이해하는 문서, so that 트래픽 분산 메커니즘을 파악할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create a Layer_Document for the Presentation layer
2. THE Layer_Document SHALL include a Mermaid_Diagram showing Master_Nginx load balancing to Worker_Nginx instances
3. THE Layer_Document SHALL include a Mermaid_Diagram showing Worker_Nginx load balancing to WAS instances
4. THE Layer_Document SHALL describe the role of Master_Nginx (external request handling, port 80)
5. THE Layer_Document SHALL describe the role of Worker_Nginx (WAS load balancing, 2 instances)
6. THE Layer_Document SHALL list docker/nginx/master-nginx.conf with description "Master Nginx upstream 설정 (Worker Nginx 2개로 부하분산)"
7. THE Layer_Document SHALL list docker/nginx/nginx.conf with description "Worker Nginx upstream 설정 (WAS 2개로 부하분산)"
8. FOR EACH configuration file, THE Layer_Document SHALL include key configuration details (upstream servers, proxy settings)

### Requirement 3: Application 계층 문서 생성

**User Story:** As a 개발자, I want WAS 계층의 구조와 세션 공유 방식을 이해하는 문서, so that 애플리케이션 서버의 동작 원리를 파악할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create a Layer_Document for the Application layer
2. THE Layer_Document SHALL include a Mermaid_Diagram showing WAS instances (8080, 8090) connecting to Redis and MySQL_Router
3. THE Layer_Document SHALL describe the role of WAS (Tomcat-based Java Servlet application)
4. THE Layer_Document SHALL describe the Filter_Chain execution order (RedisSessionFilter → LoginSessionCheckFilter → EncodingFilter)
5. THE Layer_Document SHALL explain Redis-based session sharing between WAS instances
6. THE Layer_Document SHALL list src/main/webapp/WEB-INF/web.xml with description "필터 체인 및 세션 클러스터링 설정"
7. THE Layer_Document SHALL describe how WAS connects to MySQL_Router for database operations
8. THE Layer_Document SHALL list key filter classes (RedisSessionFilter, LoginSessionCheckFilter, EncodingFilter) with their roles

### Requirement 4: Data 계층 문서 생성

**User Story:** As a 개발자, I want MySQL Cluster의 구조와 읽기/쓰기 분리 방식을 이해하는 문서, so that 데이터베이스 아키텍처를 파악할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create a Layer_Document for the Data layer
2. THE Layer_Document SHALL include a Mermaid_Diagram showing MySQL_Router routing to MySQL_Cluster nodes
3. THE Layer_Document SHALL describe the role of MySQL_Router (read/write separation)
4. THE Layer_Document SHALL describe port 6446 as "Source (Write-only)"
5. THE Layer_Document SHALL describe port 6447 as "Replica (Read-only)"
6. THE Layer_Document SHALL describe MySQL Node 1 as "Source (쓰기 전용, 포트 3310)"
7. THE Layer_Document SHALL describe MySQL Node 2 and 3 as "Replica (읽기 전용, 포트 3320, 3330)"
8. THE Layer_Document SHALL list docker/mysql/node1.cnf with description "MySQL Source 노드 설정 (쓰기 전용)"
9. THE Layer_Document SHALL list docker/mysql/node2.cnf with description "MySQL Replica 노드 설정 (읽기 전용)"
10. THE Layer_Document SHALL list docker/mysql/node3.cnf with description "MySQL Replica 노드 설정 (읽기 전용)"
11. FOR EACH configuration file, THE Layer_Document SHALL include key configuration details (server-id, replication settings)

### Requirement 5: Redis 세션 저장소 문서 생성

**User Story:** As a 개발자, I want Redis의 역할과 WAS 간 세션 공유 메커니즘을 이해하는 문서, so that 세션 관리 방식을 파악할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create a Layer_Document for Redis
2. THE Layer_Document SHALL include a Mermaid_Diagram showing Redis connecting to both WAS instances
3. THE Layer_Document SHALL describe the role of Redis (central session store for WAS instances)
4. THE Layer_Document SHALL describe Redis port as 6379
5. THE Layer_Document SHALL explain how RedisSessionFilter manages session synchronization
6. THE Layer_Document SHALL describe the benefit of centralized session storage (session persistence across WAS instances)

### Requirement 6: 문서 형식 통일

**User Story:** As a 개발자, I want 모든 문서가 일관된 형식을 따르는 것, so that 문서를 쉽게 읽고 비교할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL use Markdown format for all documents
2. WHEN creating a Layer_Document, THE Documentation_System SHALL include sections in this order: Mermaid_Diagram, 기능 설명, 핵심 파일 목록, 설정 파일 상세
3. THE Documentation_System SHALL write all document content in Korean
4. THE Documentation_System SHALL use code blocks with language tags for Mermaid diagrams (```mermaid)
5. THE Documentation_System SHALL use code blocks with language tags for configuration file examples
6. FOR EACH file listed, THE Documentation_System SHALL provide the file path and a one-line description in Korean

### Requirement 7: 민감 정보 보호

**User Story:** As a 보안 담당자, I want 문서에 민감한 정보가 포함되지 않는 것, so that 보안 위험을 방지할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL NOT read .sql files
2. IF the Documentation_System detects sensitive information (passwords, credentials, API keys), THEN THE Documentation_System SHALL report it immediately
3. THE Documentation_System SHALL reference .kiroignore for files to exclude from documentation
4. THE Documentation_System SHALL use placeholder values (e.g., [password], [api_key]) when describing configuration examples that require sensitive data

### Requirement 8: 문서 저장 위치

**User Story:** As a 개발자, I want 생성된 문서가 체계적으로 저장되는 것, so that 문서를 쉽게 찾고 관리할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL create a docs/ directory in the project root
2. THE Documentation_System SHALL save the Architecture_Document as docs/architecture-overview.md
3. THE Documentation_System SHALL save the Presentation layer document as docs/presentation-layer.md
4. THE Documentation_System SHALL save the Application layer document as docs/application-layer.md
5. THE Documentation_System SHALL save the Data layer document as docs/data-layer.md
6. THE Documentation_System SHALL save the Redis document as docs/redis-session-store.md

### Requirement 9: Mermaid 다이어그램 품질

**User Story:** As a 개발자, I want Mermaid 다이어그램이 정확하고 읽기 쉬운 것, so that 시스템 구조를 시각적으로 이해할 수 있다

#### Acceptance Criteria

1. THE Documentation_System SHALL use flowChart syntax for all Mermaid_Diagram instances
2. THE Mermaid_Diagram SHALL show component names in Korean
3. THE Mermaid_Diagram SHALL show port numbers for network services
4. THE Mermaid_Diagram SHALL use arrows to indicate request flow direction
5. THE Mermaid_Diagram SHALL use subgraphs to group related components when appropriate
6. WHEN a component has multiple instances, THE Mermaid_Diagram SHALL show all instances separately
7. THE Mermaid_Diagram SHALL be syntactically valid and renderable by Mermaid parsers

### Requirement 10: 설정 파일 상세 설명

**User Story:** As a 개발자, I want 각 설정 파일의 주요 내용을 이해하는 것, so that 시스템 동작을 커스터마이즈할 수 있다

#### Acceptance Criteria

1. WHEN a Layer_Document references a Configuration_File, THE Documentation_System SHALL include key configuration parameters
2. FOR EACH configuration parameter, THE Documentation_System SHALL provide a one-line description in Korean
3. THE Documentation_System SHALL highlight critical settings (upstream servers, ports, replication settings)
4. THE Documentation_System SHALL use code blocks to show configuration file excerpts
5. THE Documentation_System SHALL NOT include the entire configuration file content, only relevant excerpts
