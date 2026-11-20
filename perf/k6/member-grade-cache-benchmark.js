import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:9006';
const MEMBER_ID = Number(__ENV.MEMBER_ID || 1);
const BOOK_ID = Number(__ENV.BOOK_ID || 1);
const VUS = Number(__ENV.VUS || 32);
const ITERATIONS = Number(__ENV.ITERATIONS || 320);
const WARM_CACHE = (__ENV.WARM_CACHE || 'true').toLowerCase() === 'true';

export const options = {
  scenarios: {
    member_order_create: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: '2m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    member_order_create_success: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const orderDuration = new Trend('member_order_create_duration', true);
const orderSuccess = new Rate('member_order_create_success');

function orderPayload(label) {
  return JSON.stringify({
    userId: MEMBER_ID,
    orderPassword: null,
    receiverName: `cache-${label}`,
    receiverAddress: '서울특별시 성능구 테스트로 1',
    requestDeliveryDate: null,
    couponId: null,
    usedPoint: 0,
    orderItems: [
      {
        bookId: BOOK_ID,
        quantity: 1,
        wrapperId: null,
      },
    ],
  });
}

function createOrder(label, measured) {
  const response = http.post(`${BASE_URL}/api/orders`, orderPayload(label), {
    headers: { 'Content-Type': 'application/json' },
    tags: { api: measured ? 'member-order-measured' : 'member-order-warmup' },
  });

  if (measured) {
    orderDuration.add(response.timings.duration);
    orderSuccess.add(check(response, {
      'member order create status is 201': (r) => r.status === 201,
      'member order create returns orderId': (r) => {
        try {
          return Boolean(r.json('orderId'));
        } catch (_) {
          return false;
        }
      },
    }));
  }

  return response;
}

export function setup() {
  if (WARM_CACHE) {
    const response = createOrder(`warmup-${Date.now()}`, false);
    if (response.status !== 201) {
      throw new Error(`cache warmup failed: status=${response.status}, body=${response.body}`);
    }
  }
}

export default function () {
  createOrder(`${__VU}-${__ITER}-${Date.now()}`, true);
}
