const REDIRECT_KEY = 'hotelhub.postAuthRedirect';
const CHECKOUT_DRAFT_KEY = 'hotelhub.postAuthCheckoutDraft';

// Keycloak's hosted login/register pages do a full-page redirect away from
// the SPA and back, so any React Router `location.state` we had (e.g. where
// to send the user afterwards, or a pending checkout draft) would otherwise
// be lost. We stash it in sessionStorage right before leaving and read it
// back once Keycloak returns control to the app.

export function storePostAuthRedirect(locationState) {
  const redirectTo = locationState?.from || '/';
  sessionStorage.setItem(REDIRECT_KEY, redirectTo);

  if (locationState?.checkoutDraft) {
    sessionStorage.setItem(CHECKOUT_DRAFT_KEY, JSON.stringify(locationState.checkoutDraft));
  } else {
    sessionStorage.removeItem(CHECKOUT_DRAFT_KEY);
  }
}

export function consumePostAuthRedirect() {
  const redirectTo = sessionStorage.getItem(REDIRECT_KEY) || '/';
  const draftRaw = sessionStorage.getItem(CHECKOUT_DRAFT_KEY);
  sessionStorage.removeItem(REDIRECT_KEY);
  sessionStorage.removeItem(CHECKOUT_DRAFT_KEY);

  let checkoutDraft;
  try {
    checkoutDraft = draftRaw ? JSON.parse(draftRaw) : undefined;
  } catch {
    checkoutDraft = undefined;
  }

  return {
    redirectTo,
    state: checkoutDraft ? { checkoutDraft } : undefined,
  };
}
