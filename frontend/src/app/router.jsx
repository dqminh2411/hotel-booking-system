import { Navigate, Route, Routes } from 'react-router-dom';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import NotFoundPage from '../pages/NotFoundPage';
import RegisterPage from '../pages/RegisterPage';
import VerifyEmailPage from '../pages/VerifyEmailPage';
import HotelSearchPage from "../pages/HotelSearchPage";

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/auth/login" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<NotFoundPage />} />
      <Route path="/hotels" element={<HotelSearchPage />} />
    </Routes>
  );
}
