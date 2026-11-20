import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

/**
 * 검색 API 부하 테스트 (우선순위 2)
 *
 * 대상: GET /api/search?keyword=&sort=POPULAR&page=0&size=20  (book-server, Elasticsearch + Nori)
 * 목적: 한글 형태소 검색 API의 동시 사용자 안정성과 p95 응답시간 측정.
 *
 * 실행 예 (PowerShell):
 *   docker run --rm `
 *     -e BASE_URL=http://host.docker.internal:9002 `
 *     -e VUS=10 -e DURATION=1m `
 *     -v ${PWD}\perf\k6:/scripts -v ${PWD}\perf\results:/results `
 *     grafana/k6 run --summary-export /results/book-search-vu10.json /scripts/book-search-load-test.js
 */

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:9002';
const sort = __ENV.SORT || 'POPULAR';
const size = Number(__ENV.SIZE || 20);

// 회전 검색어 (Nori 형태소 분석 대상). KEYWORDS 환경변수로 덮어쓸 수 있다.
const keywords = (
  __ENV.KEYWORDS ||
  '자바,스프링,코틀린,파이썬,데이터,알고리즘,리팩토링,클린코드,네트워크,운영체제,머신러닝,디자인패턴'
).split(',');

export const options = {
  scenarios: {
    book_search: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '1m',
      exec: 'bookSearch',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    book_search_failure: ['rate<0.01'],
    // p95 1000ms 초과 시 병목 후보로 정리 (ES 쿼리/인덱스/샤드)
    book_search_duration: ['p(95)<1000'],
  },
};

const searchDuration = new Trend('book_search_duration', true);
const searchFailure = new Rate('book_search_failure');

export function bookSearch() {
  const keyword = keywords[(__ITER + __VU) % keywords.length];
  const url = `${baseUrl}/api/search?keyword=${encodeURIComponent(keyword)}&sort=${sort}&page=0&size=${size}`;

  const res = http.get(url, { tags: { endpoint: 'book_search' } });

  const ok = check(res, {
    'search status is 200': (r) => r.status === 200,
    'search has content field': (r) => r.body.includes('"content"'),
  });

  searchDuration.add(res.timings.duration);
  searchFailure.add(!ok);
  sleep(1);
}
