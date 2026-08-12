import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:9006';
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '1m';
const PAGE = Number(__ENV.PAGE || 0);
const SIZE = Number(__ENV.SIZE || 20);
const STATUS = __ENV.STATUS || '';
const USER_ID = __ENV.USER_ID || '';
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || 1);

export const options = {
  scenarios: {
    admin_order_list: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    order_list_duration: ['p(95)<800'],
    order_list_success: ['rate>0.99'],
  },
};

const orderListDuration = new Trend('order_list_duration', true);
const orderListSuccess = new Rate('order_list_success');

function targetUrl() {
  const params = [`page=${PAGE}`, `size=${SIZE}`];

  if (STATUS) {
    params.push(`status=${encodeURIComponent(STATUS)}`);
  }

  if (USER_ID) {
    return `${BASE_URL}/api/orders?${params.join('&')}`;
  }

  return `${BASE_URL}/api/admin/orders?${params.join('&')}`;
}

export default function () {
  const params = {
    tags: {
      api: USER_ID ? 'member-order-list' : 'admin-order-list',
    },
    headers: {},
  };

  if (USER_ID) {
    params.headers['X-USER-ID'] = USER_ID;
  }

  const response = http.get(targetUrl(), params);
  orderListDuration.add(response.timings.duration);

  const ok = check(response, {
    'order list status is 200': (r) => r.status === 200,
    'order list has data array': (r) => {
      try {
        return Array.isArray(r.json('data'));
      } catch (_) {
        return false;
      }
    },
    'order list has totalElements': (r) => {
      try {
        return Number(r.json('totalElements')) >= 0;
      } catch (_) {
        return false;
      }
    },
  });

  orderListSuccess.add(ok);
  sleep(THINK_TIME_SECONDS);
}
