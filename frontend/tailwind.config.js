/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        'booking-blue': '#003b95',
        'booking-yellow': '#febb02',
      },
    }
  },
  plugins: []
};

