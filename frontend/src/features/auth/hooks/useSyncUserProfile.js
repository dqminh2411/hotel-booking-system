import { useEffect } from 'react';
import axiosClient from '../../../shared/api/axiosClient';
import useAuth from './useAuth';

const SYNCED_FLAG_PREFIX = 'hotelhub.profileSynced:';

// Keycloak owns the identity (login/register/Google) but our own backend
// still needs a matching row in the app's user database (fullName, email,
// phone, keycloakId). Rather than relying on a single redirect page to
// fire this exactly once (the verification link can be opened on a
// different device than the one that registered), we upsert the profile
// the first time we see an authenticated session in *any* tab, and only
// once per browser session thereafter.
export default function useSyncUserProfile() {
  const { user, isAuthenticated } = useAuth();

  useEffect(() => {
    if (!isAuthenticated || !user?.id) return;

    const flagKey = `${SYNCED_FLAG_PREFIX}${user.id}`;
    if (sessionStorage.getItem(flagKey)) return;

    let cancelled = false;

    axiosClient
      .post('/api/users', {
        keycloakId: user.id,
        fullName: user.fullName,
        email: user.email,
        phone: user.phone,
      })
      .then(() => {
        if (!cancelled) sessionStorage.setItem(flagKey, '1');
      })
      .catch(() => {
        // Best-effort: if this fails (e.g. backend momentarily down), it
        // will simply retry on the next authenticated mount/session.
      });

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, user]);
}
