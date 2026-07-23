import AppRouter from './app/router';
import useSyncUserProfile from './features/auth/hooks/useSyncUserProfile';
import GlobalNotificationToast from './shared/components/GlobalNotificationToast';

export default function App() {
  useSyncUserProfile();
  return (
    <>
      <GlobalNotificationToast />
      <AppRouter />
    </>
  );
}
