import { useEffect, useRef, useState } from 'react';

const GOOGLE_SCRIPT_URL = 'https://accounts.google.com/gsi/client';

function loadGoogleScript() {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve();
      return;
    }

    const existingScript = document.querySelector(`script[src="${GOOGLE_SCRIPT_URL}"]`);
    if (existingScript) {
      existingScript.addEventListener('load', resolve, { once: true });
      existingScript.addEventListener('error', reject, { once: true });
      return;
    }

    const script = document.createElement('script');
    script.src = GOOGLE_SCRIPT_URL;
    script.async = true;
    script.defer = true;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

export default function GoogleSignInButton({ onCredential, disabled }) {
  const containerRef = useRef(null);
  const [loadError, setLoadError] = useState('');
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    if (!clientId || !containerRef.current) return undefined;
    let active = true;

    loadGoogleScript()
      .then(() => {
        if (!active || !containerRef.current) return;
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: ({ credential }) => onCredential(credential),
        });
        containerRef.current.replaceChildren();
        window.google.accounts.id.renderButton(containerRef.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          width: containerRef.current.clientWidth,
          locale: 'vi',
        });
      })
      .catch(() => {
        if (active) setLoadError('Không thể tải Google Sign-In. Vui lòng thử lại sau.');
      });

    return () => {
      active = false;
    };
  }, [clientId, onCredential]);

  if (!clientId) {
    return (
      <div>
        <button
          type="button"
          disabled
          className="flex h-11 w-full cursor-not-allowed items-center justify-center gap-3 rounded-md border border-slate-300 bg-white text-sm font-semibold text-slate-400"
        >
          <span className="font-bold">G</span>
          Tiếp tục với Google
        </button>
        <p className="mt-2 text-xs text-amber-700">
          Thêm VITE_GOOGLE_CLIENT_ID để bật đăng nhập Google.
        </p>
      </div>
    );
  }

  return (
    <div className={disabled ? 'pointer-events-none opacity-60' : ''}>
      <div ref={containerRef} className="min-h-11 w-full" aria-label="Đăng nhập với Google" />
      {loadError && <p className="mt-2 text-xs text-red-600">{loadError}</p>}
    </div>
  );
}
