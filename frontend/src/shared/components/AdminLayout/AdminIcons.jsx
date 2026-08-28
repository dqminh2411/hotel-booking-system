/**
 * Bo icon SVG toi gian dung rieng cho khu vuc Admin (sidebar/header).
 * Du an hien khong dung thu vien icon nao (khong co lucide/react-icons trong
 * package.json), nen o day tu ve bang duong net co ban (stroke, currentColor)
 * de dong bo mau sac voi Tailwind (vi du: text-slate-400, text-sky-300...).
 */
const base = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
};

export function DashboardIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="3.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="3.5" width="7" height="7" rx="1.5" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1.5" />
      <rect x="13.5" y="13.5" width="7" height="7" rx="1.5" />
    </svg>
  );
}

export function UsersIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <circle cx="9" cy="8" r="3" />
      <path d="M3.5 20c0-3.6 2.9-6 5.5-6s5.5 2.4 5.5 6" />
      <circle cx="17" cy="8.5" r="2.3" />
      <path d="M15.5 12.3c1.9.4 4 2 4 5.2" />
    </svg>
  );
}

export function TenantsIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="4" y="7" width="10" height="13" rx="1" />
      <path d="M14 20V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v15" />
      <path d="M7 10.5h1M10.5 10.5h1M7 14h1M10.5 14h1M17 8.5h1M17 12h1M17 15.5h1" />
    </svg>
  );
}

export function HotelsIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M3.5 19.5V9l8.5-5.5L20.5 9v10.5" />
      <path d="M3.5 19.5h17" />
      <rect x="9" y="13" width="6" height="6.5" />
    </svg>
  );
}

export function BookingsIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="3.5" y="5" width="17" height="15" rx="1.5" />
      <path d="M3.5 9.5h17" />
      <path d="M7.5 3v4M16.5 3v4" />
      <path d="M7.5 13.2h2.5M14 13.2h2.5M7.5 16.7h2.5M14 16.7h2.5" />
    </svg>
  );
}

export function PaymentsIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="3" y="5.5" width="18" height="13" rx="1.8" />
      <path d="M3 9.5h18" />
      <path d="M6.5 14.5h4M6.5 16.3h2" />
    </svg>
  );
}

export function LogoutIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M9.5 4H6a1.5 1.5 0 0 0-1.5 1.5v13A1.5 1.5 0 0 0 6 20h3.5" />
      <path d="M13 8l4 4-4 4" />
      <path d="M17 12H9.5" />
    </svg>
  );
}

export function MenuIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M4 6.5h16M4 12h16M4 17.5h16" />
    </svg>
  );
}

export function CloseIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M5 5l14 14M19 5L5 19" />
    </svg>
  );
}

export function ChevronDownIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

export function SearchIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="M20 20l-4.5-4.5" />
    </svg>
  );
}

export function LockIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="5" y="10.5" width="14" height="9" rx="1.5" />
      <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
    </svg>
  );
}

export function UnlockIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <rect x="5" y="10.5" width="14" height="9" rx="1.5" />
      <path d="M8 10.5V7.5a4 4 0 0 1 7.4-2" />
    </svg>
  );
}

export function TrashIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M4.5 7h15" />
      <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
      <path d="M6.5 7l.7 12a1.5 1.5 0 0 0 1.5 1.4h6.6a1.5 1.5 0 0 0 1.5-1.4L18 7" />
      <path d="M10 11v6M14 11v6" />
    </svg>
  );
}

export function PlusIcon({ className }) {
  return (
    <svg {...base} className={className} aria-hidden="true">
      <path d="M12 5v14M5 12h14" />
    </svg>
  );
}

// Map key trong ADMIN_NAV_ITEMS (shared/constants/adminNav.js) -> icon component.
// Dung chung boi AdminSidebar va AdminDashboardPage de khong lap lai mapping.
export const ADMIN_ICON_BY_KEY = {
  dashboard: DashboardIcon,
  users: UsersIcon,
  tenants: TenantsIcon,
  hotels: HotelsIcon,
  bookings: BookingsIcon,
  payments: PaymentsIcon,
};