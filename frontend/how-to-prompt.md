# What to do
1. Set up project architecture
2. Add shared axios client
3. Add auth flow (register/login/google-login)
4. Add hotel search
5. Add hotel detail page
6. Add room selection
7. Add booking creation
8. Add payment redirect/status
9. Add FCM notification registration
10. Add staff/admin dashboard

# Prompt Template
Task: [specific feature]

Context:
- This is a React Vite JavaScript hotel booking SaaS frontend.
- Backend base URL comes from VITE_API_BASE_URL.
- Use existing project conventions.

Requirements:
- ...
- ...

Constraints:
- Do not modify unrelated files.
- Do not introduce new libraries unless necessary.
- Keep components small.
- Add loading/error/empty states.

After coding:
- Run build.
- Tell me changed files.
- Tell me any assumptions.

# Prompt Example
Task: Implement booking creation page.

Context:
The backend booking flow goes through place-booking-service as orchestrator.

Requirements:
- Route: /booking/checkout
- User selects hotelId, roomTypeList, checkIn, checkOut, guest info
- Create BookingCheckoutPage
- Submit to POST /api/place-bookings
- Show pending state while booking is being processed

- Do not call booking-service directly from frontend

Constraints:
- Keep page component thin
- Put API call in features/booking/api
- Put hooks in features/booking/hooks