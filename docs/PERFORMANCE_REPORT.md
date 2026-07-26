# HighFiveBooks V2 Performance Report

작성일: 2026-07-09

## 요약

HighFiveBooks V2의 도서 조회 API를 k6로 측정한 결과, 대용량 도서 데이터에서 목록 조회가 p95 9.03초까지 지연되는 병목을 확인했다. 원인은 `Pageable` 목록 조회에 collection `EntityGraph`가 적용되어 Hibernate가 DB pagination 대신 메모리 pagination을 수행한 것이었다.

`BookRepository.findAll(Pageable)`의 fetch graph를 목록 조회에 필요한 `publisher` 중심으로 축소한 뒤 동일 k6 스크립트로 재측정했고, 목록 조회 p95는 51.96ms로 개선됐다.

## 테스트 환경

```text
대상 서비스: book-server
대상 API:
  GET /api/books/1
  GET /api/books?page=0&size=1

데이터 규모: 도서 157,118건
실행 방식: Docker k6
스크립트: perf/k6/book-read-baseline.js
결과 파일:
  perf/results/book-read-baseline-20260708-234828.json
  perf/results/book-read-optimized-20260708-235147.json
```

## 실행 명령

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:9002 `
  -e DETAIL_VUS=5 `
  -e LIST_VUS=5 `
  -e DETAIL_DURATION=30s `
  -e LIST_DURATION=30s `
  -v "${PWD}\perf:/perf" `
  grafana/k6:0.53.0 run `
  --summary-export "/perf/results/book-read-baseline-20260708-234828.json" `
  "/perf/k6/book-read-baseline.js"
```

개선 후에는 summary export 파일명만 `book-read-optimized-20260708-235147.json`으로 바꿔 같은 시나리오를 재실행했다.

## 측정 결과

| 구분 | 상세 조회 p95 | 목록 조회 p95 | 목록 조회 평균 | 실패율 | 요청 수 |
|---|---:|---:|---:|---:|---:|
| 개선 전 | 254.02ms | 9039.51ms | 6907.78ms | 0% | 160 |
| 개선 후 | 15.24ms | 51.97ms | 44.17ms | 0% | 295 |

목록 조회 p95 기준 개선 폭:

```text
9039.51ms / 51.97ms = 약 173.94배
```

## 문제 원인

개선 전 `BookRepository.findAll(Pageable)`은 다음처럼 collection 연관관계를 즉시 로딩했다.

```java
@EntityGraph(attributePaths = {"bookAuthors", "bookAuthors.author"})
Page<Book> findAll(Pageable pageable);
```

`Pageable`과 collection fetch가 결합되면 Hibernate가 DB의 limit/offset pagination을 온전히 활용하지 못하고, 연관 데이터를 포함한 결과를 메모리에 올린 뒤 pagination을 수행할 수 있다. 이 경우 페이지 크기가 작아도 불필요한 row와 연관 데이터 로딩 비용이 발생한다.

## 개선 내용

목록 조회에서 필요한 즉시 로딩 대상을 `publisher`로 축소했다. 저자 목록은 상세 조회의 `findById`에서만 collection graph를 유지한다.

```java
@EntityGraph(attributePaths = {"publisher"})
Page<Book> findAll(Pageable pageable);

@EntityGraph(attributePaths = {"bookAuthors", "bookAuthors.author"})
Optional<Book> findById(Long id);
```

## 결론

이번 병목은 DB 인덱스 부족이 아니라 ORM fetch 전략과 pagination 조합에서 발생했다. 조회 목적별 fetch graph를 분리해 목록 조회의 불필요한 collection loading을 제거했고, p95 기준 약 174배 개선을 확인했다.

## 포트폴리오 문장

HighFiveBooks V2에서는 AWS RDS에서 확보한 15만 건 이상의 도서 데이터를 로컬 MSA 환경에 복제하고 k6로 도서 목록 API를 측정했습니다. 그 결과 JPA collection EntityGraph와 Pageable 조합으로 Hibernate 메모리 pagination이 발생해 목록 조회 p95가 9.03초까지 지연되는 병목을 확인했습니다. 목록 조회 fetch graph를 `publisher` 중심으로 축소해 p95를 51.96ms로 낮추며 약 174배의 응답 성능 개선을 달성했습니다.

## 남은 보강

- Hibernate warning 로그 캡처
- 같은 시나리오를 더 긴 duration으로 재측정
- 도서 목록 DTO projection 적용 여부 검토
- 검색 API와 Elasticsearch 재색인 성능 별도 측정
