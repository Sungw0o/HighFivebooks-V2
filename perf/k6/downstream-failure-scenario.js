import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

/**
 * 장애/지연 시나리오 테스트 (우선순위 6)
 *
 * 대상: POST /api/orders  (order-server)
 * 사전 조건: 다운스트림(book/member/coupon) 중 하나를 중단하거나 지연시킨 상태에서 실행한다.
 *   장애 주입 예 (PowerShell):
 *     docker compose --profile apps stop book-server      # 완전 장애
 *     # 또는 toxiproxy/tc 등으로 지연 주입
 *
 * 목적: order-server가 다운스트림 장애를 격리(fast-fail / CircuitBreaker)하고,
 *   무한 대기나 카스케이드 없이 일정 시간 안에 응답하는지 검증한다.
 *   → 실패율 자체보다 "빠른 실패"와 "치명적 지연 없음"이 성공 기준이다.
 *
 * 실행 예 (PowerShell):
 *   docker run --rm `
 *     -e BASE_URL=http://host.docker.internal:9006 `
 *     -e VUS=10 -e DURATION=1m `
 *     -v ${PWD}\perf\k6:/scripts -v ${PWD}\perf\results:/results `
 *     grafana/k6 run --summary-export /results/downstream-failure.json /scripts/downstream-failure-scenario.js
 */

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9006';
const bookId = Number(__ENV.BOOK_ID || 1);
const quantity = Number(__ENV.QUANTITY || 1);

export const options = {
  scenarios: {
    order_under_failure: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '1m',
      exec: 'orderUnderFailure',
    },
  },
  thresholds: {
    // 장애 상황이므로 http_req_failed 는 임계로 두지 않는다.
    // 대신 격리 근거를 본다: 빠른 실패(p95<3s) + 무한 대기 없음(hang<5%)
    order_fail_handled_duration: ['p(95)<3000'],
    order_hang_rate: ['rate<0.05'],
  },
};

const handledDuration = new Trend('order_fail_handled_duration', true);
const hangRate = new Rate('order_hang_rate');
const respondedRate = new Rate('order_responded_rate');

export function orderUnderFailure() {
  const payload = JSON.stringify({
    orderPassword: '1234',
    receiverName: `K6 Fail ${__VU}`,
    receiverAddress: '서울특별시 강남구 테스트로 1',
    orderItems: [{ bookId, quantity }],
  });

  const res = http.post(`${baseUrl}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'order_under_failure' },
    timeout: '15s',
  });

  // status 0 = k6 클라이언트 타임아웃/연결 실패 (= 격리 실패, 무한 대기)
  const responded = res.status !== 0;

  check(res, {
    'responded (no client timeout)': (r) => r.status !== 0,
    'no 10s+ hang': (r) => r.timings.duration < 10000,
    // 다운스트림 장애 시 4xx/5xx 로 명시적으로 실패하거나(격리), 부가 기능이면 2xx
    'explicit status returned': (r) => r.status >= 200 && r.status < 600,
  });

  handledDuration.add(res.timings.duration);
  hangRate.add(res.timings.duration >= 10000);
  respondedRate.add(responded);
  sleep(1);
}
