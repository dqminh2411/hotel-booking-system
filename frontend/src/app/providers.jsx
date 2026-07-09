import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../features/auth/context/AuthContext';

export default function AppProviders({ children }) {
  return (
    <BrowserRouter>
      <AuthProvider>{children}</AuthProvider>
    </BrowserRouter>
  );
}
