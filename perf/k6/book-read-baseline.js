import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9002';

export const options = {
  scenarios: {
    book_detail: {
      executor: 'constant-vus',
      vus: Number(__ENV.DETAIL_VUS || 10),
      duration: __ENV.DETAIL_DURATION || '1m',
      exec: 'bookDetail',
    },
    book_list: {
      executor: 'constant-vus',
      vus: Number(__ENV.LIST_VUS || 10),
      duration: __ENV.LIST_DURATION || '1m',
      exec: 'bookList',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    book_detail_duration: ['p(95)<500'],
    book_list_duration: ['p(95)<2000'],
  },
};

const bookDetailDuration = new Trend('book_detail_duration', true);
const bookListDuration = new Trend('book_list_duration', true);
const bookDetailFailure = new Rate('book_detail_failure');
const bookListFailure = new Rate('book_list_failure');

export function bookDetail() {
  const res = http.get(`${baseUrl}/api/books/1`, {
    tags: { endpoint: 'book_detail' },
  });

  const ok = check(res, {
    'book detail status is 200': (r) => r.status === 200,
    'book detail has isbn': (r) => r.body.includes('9791156759270'),
  });

  bookDetailDuration.add(res.timings.duration);
  bookDetailFailure.add(!ok);
  sleep(1);
}

export function bookList() {
  const res = http.get(`${baseUrl}/api/books?page=0&size=1`, {
    tags: { endpoint: 'book_list' },
  });

  const ok = check(res, {
    'book list status is 200': (r) => r.status === 200,
    'book list has content': (r) => r.body.includes('"content"'),
  });

  bookListDuration.add(res.timings.duration);
  bookListFailure.add(!ok);
  sleep(1);
}
