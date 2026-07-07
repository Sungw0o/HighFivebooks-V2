# Storefront 프론트 잔여 작업 지시서

대상: `apps/storefront` (React 19 + TS strict + Vite 6 + Tailwind v4 + Framer Motion)
전제: 루트 `AGENTS.md`·`CLAUDE.md`를 먼저 읽을 것. 백엔드 코드는 읽기만 하고 수정 금지.

## 시작 전 파악할 구조 (10분)
- `src/api/contracts.ts` — 백엔드 실제 DTO 타입 (근거: `docs/STOREFRONT_API_CONTRACT.md`)
- `src/api/port.ts` — `StorefrontApi` 인터페이스. **mock(`api/mock/`)과 real(`api/real/`)이 반드시 동일 시그니처로 구현**
- `src/api/index.ts` — `VITE_API_ADAPTER`(mock|real)로 adapter 선택. 화면은 `api.도메인.메서드()`만 사용
- 디자인 토큰: `src/styles/index.css` (`--color-ink #0C0C0C`, `--color-body #D7E2EA`, `--color-accent #BE4C00`,
  `.text-gradient-heading`, `.btn-cta`, `.btn-ghost`, `.skeleton`)
- 공용: `SlimHeader`, `SiteFooter`, `BookCover`, `BookCard`, `states.tsx`(skeleton/빈/에러), `motion/`(FadeIn·Magnet·AnimatedText), `lib/format.ts`
- 라우팅: `src/App.tsx` — 전 페이지 `React.lazy`. 새 페이지도 같은 패턴으로 추가

## 작업 목록 (우선순위 순)

### 1. 쿠폰함 화면 `/my/coupons`
- API (port에 이미 있음): `api.coupons.getIssuableCoupons(page,size)` = `GET /api/coupons/templates`,
  `api.coupons.issueCoupon(couponId)` = `POST /api/coupons/issue`, `api.coupons.getMyCoupons(page,size)` = `GET /api/coupons/members`
- UI: 탭 2개 — "보유 쿠폰"(MemberCouponResponseDto: couponName·condition·discountValue/Type·expiredAt·daysRemaining·status pill),
  "발급받기"(CouponResponseDto: couponName·description·remainingCount·issueEndAt + [발급받기] 버튼, 발급 성공 시 보유 탭 갱신)
- 로그인 가드(MyPage 패턴 복사), 빈/로딩/에러 상태 필수. MyPage에 진입 링크 추가
- mock adapter의 `getIssuableCoupons`가 빈 배열이므로 데모용 2~3건 채울 것 (contracts 타입 준수)

### 2. 카트 배지 전역 상태
- 현재: `SlimHeader`가 마운트마다 `GET /api/cart`로 종수 조회 → 담기 직후 미갱신
- 개선: `src/store/cartBadge.ts` 같은 초소형 store(Context 또는 이벤트 기반)로
  `cart.addItem/updateItem/removeItem/clear` 후 배지 갱신. **외부 상태 라이브러리 추가 금지** (의존성 최소 원칙)
- 장바구니 담기 직후 헤더 배지가 즉시 변하면 완료

### 3. 모바일 반응형 점검 (디자인 레퍼런스: 와이어프레임 모바일 섹션)
- 홈 히어로: 16.5vw 타이포/340×470 표지가 ~390px 폭에서 깨지지 않게 (표지 축소, 태그라인/CTA 세로 스택)
- SlimHeader: 좁은 폭에서 검색바 축소 or 아이콘화, pill 줄바꿈 정리
- 목록: 필터 사이드바를 모바일에서 접기(details/summary 또는 토글)
- 관리자: 사이드바 230px 고정 → 모바일에서 상단 바 or 햄버거로 전환
- 체크아웃/마이페이지: 2열 grid가 1열로 자연스럽게 떨어지는지 확인
- `prefers-reduced-motion` 동작 유지

### 4. 우편번호 검색 연동 (선택)
- Daum 우편번호 서비스(스크립트 embed, key 불요) — `CheckoutPage` 배송지, `ProfilePage` 배송지 폼에 [주소 검색] 버튼
- 외부 스크립트는 lazy load, 실패 시 수기 입력 fallback 유지

## 금지/규칙
- 임의 endpoint 생성 금지 — 백엔드에 없는 API가 필요하면 구현하지 말고 `docs/STOREFRONT_API_CONTRACT.md`의 '추가 필요' 목록에 추가 후 보고
- `any` 남발 금지, mock과 real 코드 혼합 금지, `node_modules`/`dist`/`.env` 커밋 금지
- 정가/할인율·재고·리뷰 제목 등 백엔드 미지원 항목을 프론트에서 임의 구현하지 말 것 (대기 중인 백엔드 작업)

## 검증 (작업 후 필수)
```powershell
cd apps/storefront
npm run build   # tsc strict + vite, 통과 필수
```

## 커밋
`docs/STOREFRONT_COMMIT_PLAN.md` 규칙 준수. 이번 지시서 작업은 항목당 1커밋:
- `✨ feat: 쿠폰함 화면 추가`
- `✨ feat: 장바구니 배지 전역 상태 적용`
- `💄 style: 모바일 반응형 레이아웃 보완`
- `✨ feat: 우편번호 검색 연동`

참고: 직전 세션 미커밋분(GuestOrderPage, ProfilePage, SlimHeader 개선, AccountFindPage, 반품 모달, code-split)은
커밋 플랜 9~11번 커밋에 포함하거나 `✨ feat: 비회원 주문 조회·프로필·계정 찾기 화면 추가` 별도 커밋으로 처리.

## 완료 보고 양식 (CLAUDE.md 준수)
변경 파일 / 검증 명령 / 검증 결과 / 백엔드 API 계약 영향 / 남은 위험 / 다음 작업
