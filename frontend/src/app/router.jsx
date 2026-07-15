import { Navigate, Route, Routes } from 'react-router-dom';
import BookingDetailPage from '../pages/BookingDetailPage';
import CheckoutPage from '../pages/CheckoutPage';
import HomePage from '../pages/HomePage';
import HotelDetailPage from '../pages/HotelDetailPage';
import LoginPage from '../pages/LoginPage';
import NotFoundPage from '../pages/NotFoundPage';
import RegisterPage from '../pages/RegisterPage';
import VerifyEmailPage from '../pages/VerifyEmailPage';
import HomePageSearch from "../pages/HomePageSearch.jsx";
import SearchResultsPage from '../pages/SearchResultsPage.jsx';
import Header from '../features/hotel/components/Header';

export default function AppRouter() {
  return (

    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/hotels/:hotelId" element={<HotelDetailPage />} />
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/booking/checkout" element={<Navigate to="/checkout" replace />} />
      <Route path="/bookings/:bookingId" element={<BookingDetailPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/auth/login" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<NotFoundPage />} />
      <Route path="/hotels" element={<HomePageSearch />} />
      <Route path="/hotels/search" element={<SearchResultsPage />} />
    </Routes>
  );
}
