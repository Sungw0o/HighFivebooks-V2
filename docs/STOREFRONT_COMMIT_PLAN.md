# Storefront 커밋 지시서 (2026-07-08 작업분)

브랜치: `frontend/storefront` (main에서 분기 권장)
공통 규칙: CLAUDE.md 커밋 규칙 준수 — `<gitmoji> <type>: <한글 요약>`, 마침표 금지, 한 커밋 한 의도.
`node_modules`, `dist`, `.env`는 절대 포함하지 않는다 (`apps/storefront/.gitignore` 확인됨).
중간 커밋은 빌드가 깨질 수 있음(App.tsx가 마지막 커밋) → 빌드 검증은 마지막 커밋 후 1회만.

## 커밋 순서

1. `📝 docs: 스토어프론트 API 계약 분석 문서 추가`
   - `docs/STOREFRONT_API_CONTRACT.md`, `docs/STOREFRONT_COMMIT_PLAN.md`

2. `🎉 feat: storefront 앱 스캐폴드 및 디자인 토큰 추가`
   - `apps/storefront/package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig*.json`,
     `index.html`, `.env.example`, `.gitignore`, `src/main.tsx`, `src/vite-env.d.ts`, `src/styles/index.css`

3. `✨ feat: 백엔드 계약 기반 API 레이어 추가`
   - `apps/storefront/src/api/**` (contracts, port, http, tokenStore, mock/, real/, index)

4. `✨ feat: 공용 컴포넌트와 모션 유틸 추가`
   - `apps/storefront/src/components/**`, `src/lib/format.ts`

5. `✨ feat: 홈 화면 구현`
   - `src/pages/HomePage.tsx`, `src/pages/home/**`, `src/pages/StubPage.tsx`

6. `✨ feat: 도서 목록·상세·장바구니 화면 구현`
   - `src/pages/BooksPage.tsx`, `BookDetailPage.tsx`, `CartPage.tsx`

7. `✨ feat: 주문 결제·완료 화면 구현`
   - `src/pages/CheckoutPage.tsx`, `OrderCompletePage.tsx`

8. `✨ feat: 마이페이지·리뷰 작성 화면 구현`
   - `src/pages/MyPage.tsx`(반품 모달 포함), `ReviewNewPage.tsx`

9. `✨ feat: 로그인·회원가입·계정 찾기 화면 구현`
   - `src/pages/LoginPage.tsx`, `SignupPage.tsx`, `AccountFindPage.tsx`

10. `✨ feat: 관리자 콘솔 화면 구현`
    - `src/pages/admin/**`

11. `✨ feat: 라우터 연결 및 코드 스플릿 적용`
    - `src/App.tsx`

## 커밋 전 확인
```powershell
git status                # node_modules/dist/.env 미포함 확인
cd apps/storefront
npm ci; npm run build     # 마지막 커밋 후 통과 확인 (경고 없이 청크 분리 출력)
```

## 주의
- 백엔드(services/**), k8s, 루트 설정은 이번 작업에서 변경 없음 — diff에 있으면 커밋하지 말고 보고
- 파일 저장 중 동기화 이슈가 있었으므로 커밋 전 `npm run build` 결과를 반드시 신뢰 기준으로 삼을 것
