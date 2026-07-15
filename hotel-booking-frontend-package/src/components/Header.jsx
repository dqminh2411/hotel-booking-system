import { Link } from 'react-router-dom';

export default function Header() {
    return (
        <header className="bg-booking-blue text-white p-4 flex justify-between items-center shadow-md">
            <div className="container mx-auto flex justify-between items-center">
                <Link to="/" className="text-2xl font-bold">Booking.com Clone</Link>
                <div className="flex gap-4">
                    <button className="bg-white text-booking-blue px-4 py-1 rounded font-semibold">Đăng ký</button>
                    <button className="bg-white text-booking-blue px-4 py-1 rounded font-semibold">Đăng nhập</button>
                </div>
            </div>
        </header>
    );
}