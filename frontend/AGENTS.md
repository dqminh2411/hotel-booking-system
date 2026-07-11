# AGENTS.md

## Project
Hotel Booking SaaS frontend using React Vite JavaScript. Webname: HotelHub. This frontend is a single-page React application that serves as the user interface for the hotel booking system.

## Rules
- Use JavaScript
- Use feature-based folder structure.
- Do not put business logic inside components.
- Do not call backend directly inside components.
- Use services/hooks under each feature.
- Do not hardcode API URLs; use env variables.
- Follow existing naming conventions.

## Backend APIs
- API Gateway base URL: VITE_API_BASE_URL
- User service handles login/register/profile.
- Hotel service handles hotel search/detail.
- Place Booking service handles booking orchestration.
- Payment service handles payment flow.
- Notification service handles FCM token registration.

## UI Rules
- Web-first responsive design.
- Use reusable components.
- Keep pages thin.
- Show loading, empty, and error states for every API query.
- UI design guidelines in [ui-design.md](ui-design.md).

## Git Rules
- Make small changes.
- Do not rewrite unrelated files.
- Explain what changed after each task.