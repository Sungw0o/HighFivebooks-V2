# Coupon Concurrency and Performance Plan - 2026-07-09

## Goal

HighFiveBooks has a stronger large-traffic story in the coupon domain than in the already-tested order-create path.

Recommended portfolio topic:

> Redis Lua based first-come coupon issue concurrency control and Spring Batch based bulk coupon issue optimization.

## Current Code Evidence

### Real-time first-come coupon issue

- API: `POST /api/coupons/issue`
- Controller: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/controller/MemberCouponController.java`
- Service: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/service/impl/MemberCouponServiceImpl.java`
- Lua script: `services/coupon-server/src/main/resources/lua/issue-coupon.lua`
- DB unique constraint: `member_coupon(user_id, coupon_id)`

Current flow:

1. Load coupon metadata from DB.
2. Validate issue period and policy status.
3. If `issueCount` exists, execute Redis Lua script.
4. Lua checks duplicate issue with `SISMEMBER`.
5. Lua checks remaining stock with `GET coupon:count:{couponId}`.
6. Lua decrements stock with `DECR` and adds user to issued set with `SADD`.
7. Service persists final issue record to DB with JPA `save`.

### Bulk scheduled coupon issue

- Scheduler: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/config/BirthdayScheduler.java`
- Batch config: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/config/BatchConfig.java`
- Writer: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/batch/BirthdayMemberItemWriter.java`
- JDBC repository: `services/coupon-server/src/main/java/com/nhnacademy/coupon_server/repository/membercoupon/impl/MemberCouponJdbcRepository.java`

Current flow:

1. Scheduler starts birthday coupon batch.
2. ShedLock prevents duplicate scheduler execution in multi-instance deployment.
3. Reader fetches target member IDs.
4. Writer loads birthday coupon once and caches it for the step.
5. Writer checks already-issued users with one `IN` query per chunk.
6. Writer inserts target `member_coupon` rows with `JdbcTemplate.batchUpdate`.

## What Is Already Strong

- First-come issue has real Redis Lua atomic section.
- Duplicate issue has two layers of defense: Redis set and DB unique constraint.
- Bulk issue already avoids repeated JPA `save` and uses JDBC batch insert.
- Scheduler already has a good multi-instance story through ShedLock.

## Current Gap

- There is no k6 result for coupon issue yet.
- Real-time issue DB persistence still uses JPA `save`.
- Redis stock is decremented before DB persistence. A retry by the same user can recover the DB row because Redis returns duplicate and the service double-checks DB, but if the user never retries after a DB failure, stock can remain consumed in Redis.

## Test Script Added

- `perf/k6/coupon-issue-concurrency.js`
- `perf/scripts/prepare-coupon-issue-test.ps1`
- `perf/scripts/get-coupon-issue-evidence.ps1`

## Test Scenario

Recommended first run:

- Coupon issue count: `100`
- k6 users: `100`
- Duration: `30s`
- User IDs: generated uniquely per VU and iteration

Expected result:

- DB issued count should not exceed `100`.
- Redis remaining stock should be `0`.
- Redis issued user set size should be close to or equal to DB issued count.
- Duplicate user count should be `0`.
- k6 unexpected failure rate should be below `1%`.

Docker k6 command:

Prepare a fresh coupon:

```powershell
powershell -ExecutionPolicy Bypass -File .\perf\scripts\prepare-coupon-issue-test.ps1 -IssueCount 100
```

Use the printed `couponId` in the k6 command:

```powershell
docker run --rm -e BASE_URL=http://host.docker.internal:9004 -e COUPON_ID=<printed-coupon-id> -e VUS=100 -e DURATION=30s -e USER_ID_START=1000000 -v C:\Users\SSAFY\Desktop\HighFivebooks-V2\perf\k6:/scripts -v C:\Users\SSAFY\Desktop\HighFivebooks-V2\perf\results:/results grafana/k6 run --summary-export /results/coupon-issue-concurrency.json /scripts/coupon-issue-concurrency.js
```

Evidence command after k6:

```powershell
powershell -ExecutionPolicy Bypass -File .\perf\scripts\get-coupon-issue-evidence.ps1 -CouponId <printed-coupon-id>
```

## Recommended Improvement Path

### Step 1 - Baseline

Run the current implementation as-is:

- Redis Lua concurrency control
- JPA `memberCouponRepository.save`
- DB unique constraint

Capture:

- total requests
- success count
- sold-out/rejected count
- p95 response time
- DB issued count
- Redis remaining stock
- duplicate user count

### Step 2 - Hot Path Persistence Optimization

Add a dedicated single-row JDBC insert method for real-time issue, or batch asynchronous persistence only if eventual consistency is explicitly designed.

Conservative first improvement:

- Keep Redis Lua for concurrency.
- Replace only the final real-time issue insert with a focused JDBC insert.
- Keep JPA for normal CRUD and coupon read/use/cancel flows.

### Step 3 - Compare

Compare JPA-save baseline and JDBC-insert version:

- p95 response time
- throughput
- DB connection pressure
- correctness: no over-issue, no duplicate issue

## Resume-Safe Wording Before Running k6

> Coupon-server already separates real-time first-come coupon issue and scheduled bulk coupon issue. The real-time path uses Redis Lua for atomic stock decrement and duplicate prevention, while the scheduled birthday coupon path uses Spring Batch, ShedLock, chunk processing, and JDBC batch insert.

## Resume Wording After Running k6

> Verified first-come coupon issue concurrency with k6. Redis Lua prevented over-issue and duplicate issue under concurrent requests, and DB evidence confirmed issued count, duplicate count, and remaining stock consistency.
