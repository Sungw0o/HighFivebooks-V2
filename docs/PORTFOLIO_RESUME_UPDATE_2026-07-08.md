# HighFiveBooks 포트폴리오/이력서 업데이트 초안

작성일: 2026-07-08

## 이력서 프로젝트 문단

### HighFiveBooks V2 - 도서 커머스 MSA 로컬/클라우드 실행 환경 재구성

Spring Boot 기반 도서 커머스 MSA를 로컬 Docker Compose 및 Kubernetes 전환이 가능한 구조로 정리하고, AWS RDS에서 확보한 대용량 도서 데이터를 로컬 통합 테스트 환경에 복제했습니다. 책, 회원, 쿠폰, 결제, 주문 서버와 MySQL, Redis, RabbitMQ, Elasticsearch, MinIO 인프라를 Compose로 통합 기동하고, 전체 서비스 health 및 책 조회 API를 검증했습니다.

## 이력서 bullet

- AWS EC2/RDS 환경에서 도서 데이터 157,118건을 적재하고, `mysqldump` 기반 압축 덤프를 생성해 로컬 MySQL로 복제하는 데이터 이전 흐름을 구성했습니다.
- Docker Compose 기반 로컬 MSA 실행 환경을 정비해 `book`, `member`, `coupon`, `payment`, `order` 서버와 MySQL, Redis, RabbitMQ, Elasticsearch, MinIO를 한 번에 기동하도록 구성했습니다.
- Elasticsearch 한글 검색 안정성을 위해 Nori analyzer 포함 이미지를 Compose에 적용하고, `analysis-nori` 플러그인 및 서비스 health를 검증했습니다.
- 로컬 이메일 인증 미구현 상태에서도 서비스 health가 실패하지 않도록 Mail health check를 비활성화하여 개발/검증 환경의 기동 안정성을 높였습니다.
- payment-server 컨테이너에 healthcheck 의존 도구인 `curl`을 추가해 Docker Compose dependency chain에서 `order-server`가 정상 기동되도록 수정했습니다.
- RDS 덤프를 로컬 MySQL 컨테이너에 재현 가능하게 적재하는 PowerShell 스크립트를 작성해 대용량 책 데이터 초기화 과정을 자동화했습니다.
- 전체 MSA 기동 후 `order-server`까지 healthy 상태를 확인하고, 실제 책 단건/목록 API 조회로 데이터 연동을 검증했습니다.

## 포트폴리오 상세 서술

### 문제 상황

기존 프로젝트는 서버별 실행 환경과 데이터 준비 과정이 분산되어 있어 주문, 쿠폰, 재고, 결제 흐름을 한 번에 검증하기 어려웠습니다. 특히 책 데이터가 충분히 준비되지 않으면 주문/재고 테스트가 현실적인 시나리오로 이어지지 않았고, Elasticsearch 한글 분석기 의존성이나 healthcheck 설정 차이로 로컬 실행 안정성이 떨어질 수 있었습니다.

### 해결 과정

1. AWS EC2에서 RDS MySQL에 연결하고, 기존 도서 CSV 데이터를 정규화해 `book`, `publishers`, `authors`, `book_author` 테이블에 적재했습니다.
2. 적재된 RDS 데이터를 압축 SQL 덤프로 추출하고 로컬 개발 환경으로 복사했습니다.
3. Docker Compose 인프라를 점검하고, Elasticsearch 이미지를 Nori analyzer 포함 이미지로 교체했습니다.
4. 로컬 MySQL에 덤프를 자동 import하는 `scripts/import-book-dump.ps1`를 추가했습니다.
5. member-server의 Mail health check와 payment-server의 curl 누락 문제를 해결해 Compose dependency 기반 기동이 정상 동작하도록 만들었습니다.
6. 전체 MSA를 기동한 뒤 health endpoint와 책 조회 API를 통해 서비스 간 기본 연결과 데이터 적재 상태를 검증했습니다.

### 결과

- 로컬 MySQL 기준 책 데이터 `157,118`건 적재 완료
- `book`, `member`, `coupon`, `payment`, `order` 서버 전체 healthy 확인
- MySQL, Redis, RabbitMQ, Elasticsearch, MinIO 인프라 healthy 확인
- `GET /api/books/1`, `GET /api/books?page=0&size=1` 실제 조회 성공
- 주문/쿠폰/재고/결제 통합 검증을 진행할 수 있는 로컬 MSA 기반 마련

## 면접 답변용 요약

이 프로젝트에서는 단순히 서버를 띄우는 데서 끝내지 않고, 실제 주문 플로우 검증에 필요한 책 데이터를 먼저 확보했습니다. AWS RDS에 15만 건 이상의 도서 데이터를 적재한 뒤 덤프로 추출하고, Docker Compose 기반 로컬 MSA 환경에 복제했습니다. 이후 Elasticsearch Nori analyzer, healthcheck, payment 컨테이너의 curl 누락 같은 실행 환경 문제를 하나씩 해결해 최종적으로 주문 서버까지 healthy 상태로 기동되는 환경을 만들었습니다.

## 강조 키워드

- Spring Boot MSA
- Docker Compose
- Kubernetes 전환 준비
- AWS EC2/RDS
- MySQL 데이터 마이그레이션
- RabbitMQ
- Redis
- Elasticsearch Nori analyzer
- MinIO
- Healthcheck 안정화
- 로컬 통합 테스트 환경

## 다음 포트폴리오 보강 포인트

- 쿠폰 적용 주문 플로우 검증 결과 추가
- 재고 hold, confirm, restore 시나리오 검증 결과 추가
- Toss 결제 승인/실패/취소 시나리오 검증 결과 추가
- 책 목록 API fetch join pagination 성능 개선 사례 추가
- Kubernetes 배포 전환 및 smoke test 결과 추가
