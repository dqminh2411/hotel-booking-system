import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mockCities } from '../data/mockData';

export default function SearchBar({ initialValues }) {
    const [location, setLocation] = useState(initialValues?.locationCode || '');
    const [checkin, setCheckin] = useState(initialValues?.checkinDate || '');
    const [checkout, setCheckout] = useState(initialValues?.checkoutDate || '');
    const [guests, setGuests] = useState(initialValues?.guestNum || 2);
    const [rooms, setRooms] = useState(initialValues?.roomNum || 1);
    const navigate = useNavigate();

    const handleSearch = () => {
        if (checkin && checkout && new Date(checkin) >= new Date(checkout)) {
            alert('Ngày trả phòng phải sau ngày nhận phòng!');
            return;
        }

        const params = new URLSearchParams();
        if (location) params.append('locationCode', location);
        if (checkin) params.append('checkinDate', checkin);
        if (checkout) params.append('checkoutDate', checkout);
        params.append('guestNum', guests);
        params.append('roomNum', rooms);

        navigate(`/search?${params.toString()}`);
    };

    return (
        <div className="bg-booking-yellow p-1 rounded-md shadow-lg flex flex-col md:flex-row gap-1">
            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded">
                <input 
                    type="text" 
                    placeholder="Bạn muốn đến đâu?" 
                    className="w-full outline-none text-gray-700"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    list="cities"
                />
                <datalist id="cities">
                    {mockCities.map(city => <option key={city} value={city} />)}
                </datalist>
            </div>
            
            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded gap-2">
                <input 
                    type="date" 
                    className="w-full outline-none text-gray-700" 
                    value={checkin}
                    onChange={(e) => setCheckin(e.target.value)}
                />
                <span>-</span>
                <input 
                    type="date" 
                    className="w-full outline-none text-gray-700"
                    value={checkout}
                    onChange={(e) => setCheckout(e.target.value)}
                />
            </div>

            <div className="flex-1 bg-white flex items-center px-3 py-2 rounded">
                <span className="text-gray-700 w-full">
                    {guests} người lớn · 0 trẻ em · {rooms} phòng
                </span>
            </div>

            <button 
                onClick={handleSearch}
                className="bg-booking-blue text-white px-8 py-2 rounded font-bold text-lg hover:bg-blue-800 transition"
            >
                Tìm
            </button>
        </div>
    );
}