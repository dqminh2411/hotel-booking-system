import AppRouter from './app/router';
import useSyncUserProfile from './features/auth/hooks/useSyncUserProfile';

export default function App() {
  useSyncUserProfile();
  return <AppRouter />;
}
