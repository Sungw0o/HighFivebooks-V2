# HighFiveBooks Cloud Portfolio Section

작성일: 2026-07-09

이 문서는 HighFiveBooks V2의 클라우드/인프라 경험을 포트폴리오와 이력서에 먼저 반영하기 위한 초안이다. 실제 완료된 근거와 아직 캡처가 필요한 항목을 분리한다.

## 포트폴리오 제목 후보

### AWS RDS 기반 대용량 도서 데이터 확보 및 로컬 MSA 재현 환경 구축

## 한 줄 요약

AWS EC2/RDS를 최소 비용으로 사용해 15만 건 이상의 도서 데이터를 확보하고, Docker Compose 기반 로컬 MSA 환경으로 복제해 성능 테스트와 주문 통합 검증이 가능한 재현 환경을 구축했습니다.

## 문제 정의

기존 프로젝트는 서버별 실행 방식과 작은 seed 데이터에 의존해 주문, 재고, 쿠폰, 결제 흐름을 실제 데이터 규모로 검증하기 어려웠습니다. 특히 도서 데이터가 작으면 JPA pagination, Elasticsearch 한글 분석기, DB I/O 병목이 드러나지 않았고, 각 개발자가 같은 환경을 재현하기도 어려웠습니다.

또한 전체 서비스를 클라우드에 계속 띄우기에는 프리티어 비용과 운영 부담이 컸습니다. 그래서 클라우드는 데이터 확보와 배포 구조 검증에 사용하고, 반복 성능 실험은 로컬 Docker Compose에서 수행하는 방향이 더 낫다고 판단했습니다.

## 해결 과정

1. EC2에서 RDS MySQL에 연결해 도서 데이터를 적재하고, 로컬에서 재사용할 수 있도록 dump 흐름을 정리했습니다.
2. RDS에서 확보한 도서 데이터를 로컬 MySQL 컨테이너로 복제하는 `scripts/import-book-dump.ps1`를 작성했습니다.
3. MySQL, Redis, RabbitMQ, Elasticsearch, MinIO와 5개 백엔드 서버를 Docker Compose로 통합 기동하도록 정리했습니다.
4. Elasticsearch는 한글 검색 안정성을 위해 Nori analyzer 포함 이미지를 사용하도록 맞췄습니다.
5. payment-server healthcheck에 필요한 `curl` 누락과 mail health indicator로 인한 로컬 기동 실패를 정리했습니다.
6. Docker Compose 전체 서비스가 healthy 상태로 올라오는지 확인하고, k6 주문 목록 smoke 테스트로 API 접근까지 검증했습니다.

## 결과

| 항목 | 결과 |
|---|---:|
| 로컬 MySQL 도서 데이터 | 157,118건 |
| 도서 목록 조회 개선 전 p95 | 9039.51ms |
| 도서 목록 조회 개선 후 p95 | 51.97ms |
| Docker Compose 서비스 | 5개 백엔드 + MySQL/Redis/RabbitMQ/Elasticsearch/MinIO |
| 주문 목록 smoke 요청 수 | 10 |
| 주문 목록 smoke checks | 100% |
| 주문 목록 smoke 실패율 | 0.00% |
| 주문 목록 smoke p95 | 15.28ms |

## 이력서 bullet

- AWS EC2/RDS를 활용해 157,118건 도서 데이터를 확보하고, dump 기반 로컬 MySQL 복제 흐름을 구성해 실제 데이터 규모의 성능 테스트 환경을 마련했습니다.
- Docker Compose로 `book`, `member`, `coupon`, `payment`, `order` 서버와 MySQL, Redis, RabbitMQ, Elasticsearch, MinIO를 통합 기동하도록 정리해 MSA 로컬 재현성을 확보했습니다.
- Elasticsearch 한글 검색 환경 일관성을 위해 Nori analyzer 포함 이미지를 Compose/K8s 전환 기준 이미지로 정리했습니다.
- healthcheck 의존 도구 누락과 외부 mail health indicator 문제를 정리해 Compose dependency 기반 서비스 기동 안정성을 개선했습니다.
- k6 smoke 테스트를 통해 Docker Compose 로컬 MSA의 주문 목록 API가 checks 100%, 실패율 0%, p95 15.28ms로 응답하는 것을 확인했습니다.

## 포트폴리오 본문

HighFiveBooks V2에서는 프리티어 비용 제약을 고려해 전체 서비스를 클라우드에 상시 배포하기보다, AWS EC2/RDS를 데이터 확보와 배포 구조 검증에 집중적으로 사용했습니다. RDS에 적재한 157,118건 도서 데이터를 로컬 MySQL 컨테이너로 복제하고, Docker Compose로 5개 백엔드 서비스와 MySQL, Redis, RabbitMQ, Elasticsearch, MinIO를 통합 기동하는 구조를 만들었습니다.

이 환경을 기반으로 k6 성능 테스트를 수행한 결과, 도서 목록 조회에서 JPA collection EntityGraph와 Pageable 조합으로 p95가 9039.51ms까지 증가하는 병목을 확인했습니다. 목록 조회 fetch graph를 분리한 뒤 p95는 51.97ms로 낮아졌고, 클라우드에서 확보한 실제 규모 데이터가 성능 병목을 드러내는 데 핵심 역할을 했습니다.

또한 Elasticsearch 한글 검색 안정성을 위해 Nori analyzer 포함 이미지를 사용하고, payment-server healthcheck용 `curl` 누락과 mail health indicator 문제를 정리해 로컬 통합 환경의 기동 안정성을 높였습니다. Docker Compose 전체 서비스가 healthy로 올라온 뒤 주문 목록 API에 대해 k6 smoke 테스트를 실행해 checks 100%, 실패율 0%, p95 15.28ms를 확인했습니다.

## 면접 답변

프리티어 환경이라 전체 MSA를 클라우드에서 계속 운영하기보다는, 클라우드는 실제 규모 데이터를 확보하는 용도로 사용하고 반복 실험은 로컬 Docker Compose에서 수행하는 전략을 선택했습니다. RDS에서 15만 건 이상의 도서 데이터를 확보해 로컬 MySQL로 복제했고, 이 데이터 덕분에 작은 seed 데이터에서는 보이지 않던 JPA pagination 병목을 발견할 수 있었습니다. 이후 Compose로 5개 백엔드와 인프라를 함께 띄워 주문, 결제, 쿠폰, 재고 흐름을 같은 환경에서 검증할 수 있게 만들었습니다.

## 증거 파일

- `docs/PERFORMANCE_REPORT.md`
- `docs/PERFORMANCE_TEST_PROGRESS_2026-07-09.md`
- `docs/LOCAL_REPRODUCIBILITY.md`
- `docs/k8s-transition-runbook.md`
- `docs/k8s-smoke-evidence.md`
- `scripts/import-book-dump.ps1`
- `scripts/local-up.ps1`
- `docker-compose.yml`
- `perf/results/order-list-smoke-20260709-220928.json`

## 아직 보강하면 좋은 캡처

- RDS dump 생성 명령과 파일 크기
- 로컬 import 후 `book` 테이블 count 출력
- Docker Compose 전체 healthy 화면
- RabbitMQ queue/DLQ 화면
- Elasticsearch Nori plugin 확인 API
- `kubectl kustomize k8s/base` 결과
- 실제 Kubernetes context에서 smoke 성공 화면

## 주의해서 말할 것

- Cloudflare + S3 + EC2 Ingress + RDS 구조는 아직 실제 배포 완료가 아니라 설계와 비용 판단 단계로 말한다.
- K8s는 manifest, runbook, smoke script, 렌더링 검증까지 완료됐고 실제 클러스터 성공 캡처는 별도 보강이 필요하다고 말한다.
- "운영 배포 완료"보다 "프리티어 제약에서 데이터 확보, 로컬 재현성, K8s 전환 가능성을 검증했다"로 표현하는 것이 정확하다.
