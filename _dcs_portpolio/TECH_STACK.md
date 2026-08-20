# 기술 스택

## Backend
- **Java 8 / 21**, **Spring Boot 2.7**
- Spring MVC, Spring Security (폼 로그인, CSRF), Spring Data JPA
- Thymeleaf (서버사이드 렌더링 + 부분 AJAX 갱신)
- RESTful API 설계 (API 키 기반 서비스 간 인증)
- Raw TCP 소켓 프로그래밍 (`Socket`, `DataInputStream`/`DataOutputStream` 기반 커스텀 바이너리 프로토콜)
- 스레드풀 기반 비동기 처리 (`ExecutorService`, `@Scheduled`, `@Async` 패턴)
- Python (Flask) — 별도 사이드 프로젝트형 관리 도구

## 데이터베이스
- MariaDB, Spring Data JPA / Hibernate

## 인프라 / 운영
- Docker, Docker Compose (멀티 서비스 오케스트레이션)
- 컨테이너 레지스트리 운영(사설 레지스트리 push/pull)
- Linux 서버 운영 (CentOS 계열), systemd, cron, LVM/XFS 스토리지 이해
- 방화벽(firewalld) 설정, 포트/네트워크 트러블슈팅

## 모니터링 / 관측성
- Prometheus (PromQL 쿼리 작성, 집계 함수 트러블슈팅)
- Grafana (대시보드 설계, 패널/트랜스폼 API를 통한 프로그래매틱 대시보드 수정)
- cAdvisor, node_exporter (+ textfile collector로 커스텀 지표 확장)

## 외부 연동
- OAuth2 스타일 토큰 발급/갱신 플로우 (액세스/리프레시 토큰, 토큰 회전 대응)
- 외부 메신저 플랫폼 알림 API 연동

## 협업 / 형상관리
- Git / GitHub (커밋 단위 설계, 시크릿 유출 대응 — `commit --amend` + force push로 즉시 스크럽)
- 요구사항 검토 → 구현 → 실제 서버 배포 → end-to-end 검증까지 전 과정을 반복적으로 수행하는
  워크플로우에 익숙

## 문제해결 접근 방식
- 로그/메트릭 기반 근거 있는 디버깅 (추측보다 실제 증거 우선)
- 재현 어려운 버그는 실제 사용자 환경(브라우저, 접속 로그)까지 파고들어 확인
- 운영 중인 시스템에 새 기능을 얹을 때 기존 데이터/트래픽에 대한 영향도를 먼저 검토
