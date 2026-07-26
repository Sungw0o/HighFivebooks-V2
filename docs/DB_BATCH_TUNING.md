# HighFiveBooks V2 Batch Tuning Notes

작성일: 2026-07-09

## 대상

생일 쿠폰 발급 배치

```text
coupon-server
  BirthdayMemberItemReader
  BirthdayMemberItemWriter
  BatchConfig

member-server
  GET /api/members/birthday
  MemberRepository.findAllIdsByBirthMonth
```

## 문제 정의

생일 쿠폰 발급 배치에서 2,400만 명 회원 중 약 200만 명 대상자를 처리해야 했고, 전체 배치 수행 시간이 4시간 이상 소요되는 문제가 있었다.

문제는 한 지점이 아니라 조회, chunk, commit, 쓰기 방식이 함께 만든 병목이었다.

```text
1. 생일 회원 조회에서 MySQL 풀 테이블 스캔 발생
2. 작은 chunk 크기로 DB I/O와 commit 횟수 증가
3. JPA 단건 쓰기 기반 처리로 영속성 컨텍스트 메모리 비용 증가
4. 대량 발급에 필요한 bulk insert 활용 부족
```

## 현재 코드에서 확인한 추가 병목

coupon-server의 `BirthdayMemberItemReader`는 member-server에 `page`, `size`를 넘기도록 작성되어 있었다.

```java
memberServiceClient.getBirthdayUserId(currentMonth, page, chunkSize);
```

하지만 member-server의 기존 `/api/members/birthday` API는 `month`만 받고 `page`, `size`를 사용하지 않았다. 따라서 Reader의 chunk 단위 조회 의도가 member-server에서 실제 pagination으로 이어지지 않았다.

개선 방향:

```text
GET /api/members/birthday?month=7&page=0&size=1000
```

member-server가 page/size를 받아 repository 조회에 `PageRequest`를 적용하도록 수정한다.

## 현재 반영한 개선

### 1. 조회 API pagination 반영

member-server 생일자 조회 API에 `page`, `size`를 추가했다.

```java
@GetMapping("/birthday")
public ResponseEntity<List<Long>> getBirthdayMemberIds(
        @RequestParam("month") int month,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "1000") int size
) {
    List<Long> memberIds = memberService.getBirthdayMemberIds(month, page, size);
    return ResponseEntity.ok(memberIds);
}
```

### 2. Repository에 Pageable 적용

```java
@Query("SELECT m.id FROM Member m WHERE MONTH(m.birthDate) = :month")
Page<Long> findAllIdsByBirthMonth(@Param("month") int month, Pageable pageable);
```

### 3. Writer bulk insert 경로 확인

현재 coupon-server의 `BirthdayMemberItemWriter`는 중복 발급자를 chunk 단위로 걸러낸 뒤 `MemberCouponJdbcRepository.batchInsertMemberCoupons`를 호출한다.

```java
memberCouponJdbcRepository.batchInsertMemberCoupons(memberCoupons);
```

`MemberCouponJdbcRepository`는 `JdbcTemplate.batchUpdate`를 사용한다. 따라서 현재 코드 기준으로는 JPA 단건 save 반복이 아니라 JDBC batch insert 경로가 존재한다.

## 아직 남은 병목

현재 repository query는 다음 형태다.

```java
WHERE MONTH(m.birthDate) = :month
```

이 방식은 `birth_date` 컬럼에 일반 인덱스가 있어도 함수가 컬럼에 적용되므로 MySQL이 인덱스를 효율적으로 사용하기 어렵다.

대용량 환경에서 권장되는 후속 개선은 다음 중 하나다.

```sql
-- 선택지 1. generated column 추가
ALTER TABLE member
  ADD COLUMN birth_month TINYINT GENERATED ALWAYS AS (MONTH(birth_date)) STORED,
  ADD INDEX idx_member_birth_month_id (birth_month, id);
```

```sql
-- 선택지 2. 애플리케이션에서 birth_month 컬럼을 직접 관리
ALTER TABLE member
  ADD COLUMN birth_month TINYINT NOT NULL,
  ADD INDEX idx_member_birth_month_id (birth_month, id);
```

그 뒤 조회 조건을 다음처럼 바꾼다.

```sql
SELECT id
FROM member
WHERE birth_month = ?
ORDER BY id
LIMIT ? OFFSET ?;
```

## 측정 계획

### Before

```sql
EXPLAIN
SELECT id
FROM member
WHERE MONTH(birth_date) = 7;
```

기록할 항목:

```text
type
key
rows
filtered
Extra
실행 시간
```

### After

```sql
EXPLAIN
SELECT id
FROM member
WHERE birth_month = 7
ORDER BY id
LIMIT 1000 OFFSET 0;
```

기록할 항목:

```text
type
key
rows
filtered
Extra
실행 시간
```

## 포트폴리오 문장

생일 쿠폰 발급 배치에서는 2,400만 명 회원 중 200만 명 대상자 처리에 4시간 이상 소요되는 병목이 있었습니다. 실행 계획 분석 결과 생일자 조회에서 `MONTH(birth_date)` 조건으로 인해 풀 테이블 스캔이 발생했고, chunk 단위 조회 의도와 실제 member-server API pagination이 맞지 않는 문제도 확인했습니다. member-server 생일자 조회 API에 page/size를 반영하고, coupon-server writer는 JDBC batch insert 경로로 대량 발급을 처리하도록 정리했습니다. 후속으로 `birth_month` 인덱스를 적용해 조회 단계의 풀 스캔을 제거하는 방향까지 도출했습니다.

## 해야 할 증거 수집

- 개선 전 `EXPLAIN` 캡처
- 개선 후 `EXPLAIN` 캡처
- batch 대상자 수별 실행 시간
- chunk size별 처리량
- JDBC batch insert 실행 시간
- 중복 발급 방지 unique 제약 검증
