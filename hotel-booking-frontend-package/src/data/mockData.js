const cities = ['Hà Nội', 'Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Nha Trang', 'Đà Lạt', 'Huế', 'Vũng Tàu', 'Phú Quốc', 'Cần Thơ'];

const amenitiesList = [
    { id: 'a1', name: 'Hồ bơi' },
    { id: 'a2', name: 'Bữa sáng miễn phí' },
    { id: 'a3', name: 'Wifi miễn phí' },
    { id: 'a4', name: 'Bãi đậu xe' },
    { id: 'a5', name: 'Spa' },
    { id: 'a6', name: 'Phòng gym' }
];

const generateMockHotels = () => {
    let hotels = [];
    let idCounter = 1;
    cities.forEach(city => {
        for (let i = 1; i <= 10; i++) {
            hotels.push({
                id: idCounter.toString(),
                name: `Khách sạn ${city} ${i} Sao`,
                address: `Trung tâm ${city}`,
                locationCode: city,
                stars: Math.floor(Math.random() * 3) + 3, // 3 - 5 sao
                rating: (Math.random() * 2 + 7).toFixed(1), // 7.0 - 9.9
                topAmenities: [amenitiesList[0], amenitiesList[2]],
                lowestPrice: Math.floor(Math.random() * 2000000) + 500000,
                cheapestRoomType: 'Phòng Tiêu Chuẩn',
                image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500&q=80'
            });
            idCounter++;
        }
    });
    return hotels;
};

export const mockHotels = generateMockHotels();
export const mockCities = cities;
export const mockAmenities = amenitiesList;