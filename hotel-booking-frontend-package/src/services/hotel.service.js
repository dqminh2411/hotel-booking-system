import { mockHotels } from '../data/mockData';

export const hotelService = {
    searchHotels: async (params) => {
        // GIẢ LẬP LỜI GỌI API
        // Về sau: return await axiosClient.get('/api/hotels', { params }).then(res => res.data);
        return new Promise((resolve) => {
            setTimeout(() => {
                let filtered = [...mockHotels];

                if (params.locationCode) {
                    filtered = filtered.filter(h => h.locationCode.toLowerCase() === params.locationCode.toLowerCase());
                }

                if (params.minPrice) {
                    filtered = filtered.filter(h => h.lowestPrice >= parseInt(params.minPrice));
                }

                if (params.maxPrice) {
                    filtered = filtered.filter(h => h.lowestPrice <= parseInt(params.maxPrice));
                }

                if (params.sortBy) {
                    if (params.sortBy === 'price_asc') {
                        filtered.sort((a, b) => a.lowestPrice - b.lowestPrice);
                    } else if (params.sortBy === 'price_desc') {
                        filtered.sort((a, b) => b.lowestPrice - a.lowestPrice);
                    }
                }

                resolve(filtered);
            }, 500); // Giả lập độ trễ mạng 500ms
        });
    }
};