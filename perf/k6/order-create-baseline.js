import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:9006';
const BOOK_ID = Number(__ENV.BOOK_ID || 1);
const QUANTITY = Number(__ENV.QUANTITY || 1);
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '1m';
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || 1);

export const options = {
  scenarios: {
    order_create: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    order_create_duration: ['p(95)<800'],
    order_create_success: ['rate>0.99'],
  },
};

const orderCreateDuration = new Trend('order_create_duration', true);
const orderCreateSuccess = new Rate('order_create_success');

function orderPayload() {
  const unique = `${__VU}-${__ITER}-${Date.now()}`;

  return JSON.stringify({
    userId: null,
    orderPassword: '1234',
    receiverName: `k6-${unique}`,
    receiverAddress: '서울특별시 성능구 테스트로 1',
    requestDeliveryDate: null,
    couponId: null,
    usedPoint: 0,
    orderItems: [
      {
        bookId: BOOK_ID,
        quantity: QUANTITY,
        wrapperId: null,
      },
    ],
  });
}

export default function () {
  const response = http.post(`${BASE_URL}/api/orders`, orderPayload(), {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      api: 'order-create',
    },
  });

  orderCreateDuration.add(response.timings.duration);

  const ok = check(response, {
    'order create status is 201': (r) => r.status === 201,
    'order create has orderId': (r) => {
      try {
        return Boolean(r.json('orderId'));
      } catch (_) {
        return false;
      }
    },
    'order create has orderKey': (r) => {
      try {
        return Boolean(r.json('orderKey'));
      } catch (_) {
        return false;
      }
    },
    'order create has totalAmount': (r) => {
      try {
        return Number(r.json('totalAmount')) >= 0;
      } catch (_) {
        return false;
      }
    },
  });

  orderCreateSuccess.add(ok);
  sleep(THINK_TIME_SECONDS);
}
