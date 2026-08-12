# JPA 성능 개선 재현 및 측정 조건

작성일: 2026-08-12

## 문제 → 선택 → 해결 → 검증 → 한계

- **문제**: 도서 157,118건에서 collection `EntityGraph`와 `Pageable`을 함께 사용한 목록 조회 p95가 9,039.51ms까지 증가했다.
- **선택**: DTO projection·native query보다 변경 범위가 작은 조회 목적별 fetch graph 분리를 택했다.
- **해결**: 목록 `findAll(Pageable)`은 `publisher`만 즉시 로딩하고, `bookAuthors.author`는 상세 `findById`에 유지했다.
- **검증**: 동일 k6 스크립트에서 목록 p95 9,039.51ms → 51.97ms, 평균 6,907.78ms → 44.17ms, HTTP 실패율 0%를 기록했다.
- **한계**: 전후 요청 수가 160건과 295건으로 다르고 짧은 30초 측정이며, DB buffer pool·JVM warm-up·호스트 부하를 고정한 반복 실험은 아니다.

검증 상태: **검증 완료(저장된 k6 결과 기준)**

## 고정해야 할 재현 조건

| 항목 | 조건 |
|---|---|
| 코드 | 전후 commit SHA를 각각 기록 |
| 데이터 | 동일 MySQL dump, `book` 157,118건, 관련 저자/출판사 row count 기록 |
| API | `GET /api/books?page=0&size=1`, `GET /api/books/1` |
| 부하 | detail 5 VU + list 5 VU, 각 30초, `sleep(1)` 포함 |
| 도구 | `grafana/k6:0.53.0`, `perf/k6/book-read-baseline.js` |
| 애플리케이션 | 동일 Java/Spring Boot 설정, 동일 컨테이너 CPU·메모리 제한 |
| DB | 동일 MySQL 버전, buffer pool 크기, connection pool, 인덱스 |
| 워밍업 | 측정 전 동일 API를 60초 호출하고 첫 실행 결과는 버림 |
| 반복 | 전후 각 5회, 실행 순서를 교차(ABBAA/BAABB) |
| 호스트 | Docker Desktop 리소스와 백그라운드 프로세스 고정 |

## 수집 지표

- k6: 요청 수, 처리량, 평균, median, p90, p95, max, 실패율
- JVM: heap, GC pause, CPU, thread 수
- DB: 실행 SQL, rows examined, query time, buffer hit, connection 수
- Hibernate: `HHH90003004`(collection fetch + pagination) warning 유무
- 결과: 반복별 JSON과 median/p95의 중앙값·최솟값·최댓값

## 실행 예시

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:9002 `
  -e DETAIL_VUS=5 -e LIST_VUS=5 `
  -e DETAIL_DURATION=30s -e LIST_DURATION=30s `
  -v "${PWD}\perf:/perf" `
  grafana/k6:0.53.0 run `
  --summary-export "/perf/results/book-read-<variant>-<run>.json" `
  "/perf/k6/book-read-baseline.js"
```

## 판정 기준

성능 개선 주장은 다섯 번 반복한 p95 중앙값이 baseline보다 낮고, 실패율이 동일하게 1% 미만이며, Hibernate collection pagination warning이 개선 후 사라졌을 때 확정한다. 현재 173.94배 수치는 저장된 1회 전후 결과의 비율이므로 면접에서는 **“1회 동일 시나리오 측정 기준 약 174배”**라고 한정한다.
