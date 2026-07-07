import { Suspense, lazy, useEffect } from 'react'
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom'
import { StubPage } from './pages/StubPage'

// 라우트 단위 code-split
const HomePage = lazy(() => import('./pages/HomePage').then((m) => ({ default: m.HomePage })))
const BooksPage = lazy(() => import('./pages/BooksPage').then((m) => ({ default: m.BooksPage })))
const BookDetailPage = lazy(() => import('./pages/BookDetailPage').then((m) => ({ default: m.BookDetailPage })))
const CartPage = lazy(() => import('./pages/CartPage').then((m) => ({ default: m.CartPage })))
const CheckoutPage = lazy(() => import('./pages/CheckoutPage').then((m) => ({ default: m.CheckoutPage })))
const OrderCompletePage = lazy(() =>
  import('./pages/OrderCompletePage').then((m) => ({ default: m.OrderCompletePage })),
)
const MyPage = lazy(() => import('./pages/MyPage').then((m) => ({ default: m.MyPage })))
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const SignupPage = lazy(() => import('./pages/SignupPage').then((m) => ({ default: m.SignupPage })))
const AccountFindPage = lazy(() => import('./pages/AccountFindPage').then((m) => ({ default: m.AccountFindPage })))
const GuestOrderPage = lazy(() => import('./pages/GuestOrderPage').then((m) => ({ default: m.GuestOrderPage })))
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })))
const MyCouponsPage = lazy(() => import('./pages/MyCouponsPage').then((m) => ({ default: m.MyCouponsPage })))
const ReviewNewPage = lazy(() => import('./pages/ReviewNewPage').then((m) => ({ default: m.ReviewNewPage })))
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout').then((m) => ({ default: m.AdminLayout })))
const AdminDashboardPage = lazy(() =>
  import('./pages/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })),
)
const AdminBooksPage = lazy(() => import('./pages/admin/AdminBooksPage').then((m) => ({ default: m.AdminBooksPage })))
const AdminBookFormPage = lazy(() =>
  import('./pages/admin/AdminBookFormPage').then((m) => ({ default: m.AdminBookFormPage })),
)
const AdminOrdersPage = lazy(() =>
  import('./pages/admin/AdminOrdersPage').then((m) => ({ default: m.AdminOrdersPage })),
)

/** 페이지 전환 시 scroll-to-top */
function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

function PageFallback() {
  return <div className="min-h-screen bg-ink" aria-busy="true" />
}

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Suspense fallback={<PageFallback />}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/books" element={<BooksPage />} />
          <Route path="/books/:id" element={<BookDetailPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/order/complete" element={<OrderCompletePage />} />
          <Route path="/my" element={<MyPage />} />
          <Route path="/my/profile" element={<ProfilePage />} />
          <Route path="/my/coupons" element={<MyCouponsPage />} />
          <Route path="/review/new" element={<ReviewNewPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/account/find" element={<AccountFindPage />} />
          <Route path="/order/guest" element={<GuestOrderPage />} />
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<AdminDashboardPage />} />
            <Route path="books" element={<AdminBooksPage />} />
            <Route path="books/new" element={<AdminBookFormPage />} />
            <Route path="books/:id/edit" element={<AdminBookFormPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
          </Route>
          <Route path="*" element={<StubPage heading="404" description="페이지를 찾을 수 없습니다." />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
