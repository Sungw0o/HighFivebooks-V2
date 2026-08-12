import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9002';

export const options = {
  vus: Number(__ENV.VUS || 1),
  duration: __ENV.DURATION || '60s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    book_list_failure: ['rate<0.01'],
  },
};

const bookListDuration = new Trend('book_list_duration', true);
const bookListFailure = new Rate('book_list_failure');

export default function () {
  const response = http.get(`${baseUrl}/api/books?page=0&size=1`, {
    tags: { endpoint: 'book_list' },
  });

  const succeeded = check(response, {
    'book list status is 200': (result) => result.status === 200,
    'book list has content': (result) => result.body.includes('"content"'),
  });

  bookListDuration.add(response.timings.duration);
  bookListFailure.add(!succeeded);
  sleep(1);
}
