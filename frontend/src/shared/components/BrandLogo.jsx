import { Link } from 'react-router-dom';

export default function BrandLogo({ inverse = false }) {
  return (
    <Link
      to="/"
      className={`inline-flex items-center gap-2 text-xl font-bold ${
        inverse ? 'text-white' : 'text-sky-700'
      }`}
      aria-label="HotelHub - Trang chủ"
    >
      <span
        className={`grid h-9 w-9 place-items-center rounded-md ${
          inverse ? 'bg-white text-sky-700' : 'bg-sky-700 text-white'
        }`}
        aria-hidden="true"
      >
        H
      </span>
      HotelHub
    </Link>
  );
}
