/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        'booking-blue': '#003580',
        'booking-yellow': '#feba02',
      }
    },
  },
  plugins: [],
}