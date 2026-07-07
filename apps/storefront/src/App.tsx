import { useEffect } from 'react'
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom'
import { HomePage } from './pages/HomePage'
import { StubPage } from './pages/StubPage'

/** 페이지 전환 시 scroll-to-top */
function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/books" element={<StubPage heading="Books" description="책 목록/검색 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/books/:id" element={<StubPage heading="Detail" description="책 상세 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/cart" element={<StubPage heading="Cart" description="장바구니 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/checkout" element={<StubPage heading="Checkout" description="주문/결제 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/order/complete" element={<StubPage heading="Thank You" description="주문 완료 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/my" element={<StubPage heading="My Page" description="마이페이지 — 다음 단계에서 구현합니다." />} />
        <Route path="/review/new" element={<StubPage heading="Review" description="리뷰 작성 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/login" element={<StubPage heading="Login" description="로그인 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/signup" element={<StubPage heading="Sign Up" description="회원가입 화면 — 다음 단계에서 구현합니다." />} />
        <Route path="/admin/*" element={<StubPage heading="HF Admin" description="관리자 콘솔 — 다음 단계에서 구현합니다." />} />
        <Route path="*" element={<StubPage heading="404" description="페이지를 찾을 수 없습니다." />} />
      </Routes>
    </BrowserRouter>
  )
}
