import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9004';
const couponId = Number(__ENV.COUPON_ID || 1);
const userIdStart = Number(__ENV.USER_ID_START || 1000000);
const userStride = Number(__ENV.USER_STRIDE || 1000000);
const thinkTimeSeconds = Number(__ENV.THINK_TIME_SECONDS || 0);

export const options = {
  scenarios: {
    coupon_issue_unique_users: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 100),
      duration: __ENV.DURATION || '30s',
      exec: 'issueCouponWithUniqueUser',
    },
  },
  thresholds: {
    coupon_issue_unexpected_failure: ['rate<0.01'],
    coupon_issue_duration: ['p(95)<1000'],
  },
};

const issueDuration = new Trend('coupon_issue_duration', true);
const unexpectedFailure = new Rate('coupon_issue_unexpected_failure');
const issuedCount = new Counter('coupon_issue_success_count');
const soldOutCount = new Counter('coupon_issue_sold_out_count');
const duplicateCount = new Counter('coupon_issue_duplicate_count');

function nextUserId() {
  return userIdStart + ((__VU - 1) * userStride) + __ITER;
}

function classifyResponse(res) {
  if (res.status === 201) {
    return 'issued';
  }

  const body = res.body || '';
  if (res.status === 409 || body.includes('DUPLICATE') || body.includes('Duplicate')) {
    return 'duplicate';
  }

  if ((res.status >= 400 && res.status < 500) || body.includes('sold') || body.includes('exhaust')) {
    return 'sold_out_or_rejected';
  }

  return 'unexpected';
}

export function issueCouponWithUniqueUser() {
  const userId = nextUserId();
  const payload = JSON.stringify({ couponId });

  const res = http.post(`${baseUrl}/api/coupons/issue`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-USER-ID': String(userId),
    },
    tags: {
      endpoint: 'coupon_issue',
    },
  });

  const result = classifyResponse(res);
  const ok = check(res, {
    'coupon issue response is classified': () => result !== 'unexpected',
  });

  if (result === 'issued') {
    issuedCount.add(1);
  } else if (result === 'duplicate') {
    duplicateCount.add(1);
  } else if (result === 'sold_out_or_rejected') {
    soldOutCount.add(1);
  }

  issueDuration.add(res.timings.duration);
  unexpectedFailure.add(!ok);

  if (thinkTimeSeconds > 0) {
    sleep(thinkTimeSeconds);
  }
}
