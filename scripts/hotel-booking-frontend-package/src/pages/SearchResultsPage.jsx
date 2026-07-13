import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import SearchBar from '../components/SearchBar';
import FilterSidebar from '../components/FilterSidebar';
import HotelCard from '../components/HotelCard';
import { hotelService } from '../services/hotel.service';

export default function SearchResultsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [hotels, setHotels] = useState([]);
    
    // Parse URL params to Object
    const currentParams = Object.fromEntries([...searchParams]);

    useEffect(() => {
        const fetchHotels = async () => {
            const data = await hotelService.searchHotels(currentParams);
            setHotels(data);
        };
        fetchHotels();
    }, [searchParams]);

    const handleFilterChange = (key, value) => {
        if (value) {
            searchParams.set(key, value);
        } else {
            searchParams.delete(key);
        }
        setSearchParams(searchParams);
    };

    return (
        <div className="bg-gray-100 min-h-screen">
            <div className="bg-booking-blue py-6 px-4">
                <div className="container mx-auto">
                    <SearchBar initialValues={currentParams} />
                </div>
            </div>
            
            <div className="container mx-auto py-6 px-4 flex flex-col md:flex-row gap-4">
                <div className="w-full md:w-auto">
                    <FilterSidebar onFilterChange={handleFilterChange} />
                </div>
                
                <div className="flex-1">
                    <h2 className="text-2xl font-bold mb-4">
                        {currentParams.locationCode 
                            ? `Tìm thấy ${hotels.length} chỗ nghỉ tại ${currentParams.locationCode}`
                            : `Tất cả ${hotels.length} chỗ nghỉ`}
                    </h2>
                    
                    {hotels.map(hotel => (
                        <HotelCard key={hotel.id} hotel={hotel} />
                    ))}
                    
                    {hotels.length === 0 && (
                        <div className="bg-white p-8 text-center rounded border">
                            Không tìm thấy khách sạn nào phù hợp.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}