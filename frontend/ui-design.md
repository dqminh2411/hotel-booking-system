# UI Design Guide for HotelHub Frontend

This document defines the visual and interaction rules for consistent UI generation with Codex.

Project context:

- App name: **HotelHub**
- Purpose: demo frontend for a Hotel Booking SaaS system
- Main focus: demonstrate backend features clearly, not build a production-grade polished UI
- Style direction: **Booking.com-inspired**, web-first, clean, practical, information-dense
- Tech stack: React 18, React Router, Axios, Vite 5, Tailwind CSS, Firebase Messaging, npm, Javascript (no TypeScript)

---

## 1. Core UI Principle

Build a frontend that looks like a real hotel booking platform, but keep implementation simple and maintainable.

Prioritize:

- clear booking flow
- readable hotel information
- obvious calls to action
- fast demo navigation
- loading, empty, and error states
- reusable UI components

Avoid:

- over-engineered animations
- complex custom design systems
- unnecessary UI libraries
- complicated state management
- overly artistic layouts
- dark mode unless explicitly requested

---

## 2. Visual Style

The UI should feel similar to Booking.com:

- blue primary header/search style
- yellow/orange CTA highlights where appropriate
- dense but readable hotel cards
- strong search panel
- practical filters
- clear price and availability information
- simple dashboard pages for staff/admin

Do not copy Booking.com exactly. Use it only as design inspiration.

Design keywords:

- practical
- trustworthy
- clean
- travel booking
- web-first
- demo-friendly
- information-rich

---

## 3. Color Tokens

Use Tailwind CSS utility classes. Do not use inline styles.

### Main Colors

| Purpose | Tailwind Class | Usage |
| --- | --- | --- |
| Primary | `blue-700` | header, primary buttons, active states |
| Primary hover | `blue-800` | hover for primary actions |
| Primary light | `blue-50` | subtle backgrounds |
| Accent CTA | `amber-400` | search button, important booking CTA |
| Accent hover | `amber-500` | hover for accent CTA |
| Success | `green-600` | successful booking/payment status |
| Warning | `amber-600` | pending booking/payment status |
| Error | `red-600` | failed booking/payment/error messages |
| Background | `slate-50` | page background |
| Surface | `white` | cards, forms, panels |
| Border | `slate-200` | card/input/table borders |
| Text primary | `slate-900` | main text |
| Text secondary | `slate-600` | descriptions/meta text |
| Text muted | `slate-500` | helper text |

### Rules

- Use blue for brand identity and navigation.
- Use amber/yellow mainly for high-priority search or booking CTAs.
- Use white cards on `slate-50` background.
- Avoid gradients unless specifically requested.
- Avoid colorful shadows.

---

## 4. Typography

Use default system font or configured project font.

### Text Scale

| Element | Tailwind Class |
| --- | --- |
| Page title | `text-2xl md:text-3xl font-bold text-slate-900` |
| Section title | `text-xl font-semibold text-slate-900` |
| Card title | `text-lg font-semibold text-slate-900` |
| Body | `text-sm text-slate-700` |
| Meta text | `text-xs text-slate-500` |
| Price | `text-xl font-bold text-slate-900` |

### Rules

- Keep text compact but readable.
- Use bold only for title, price, and key status.
- Do not use decorative fonts.
- Use consistent capitalization.

---

## 5. Spacing and Layout

Use Tailwind spacing based on a simple 4px/8px rhythm.

Common spacing:

- page padding: `px-4 md:px-6 lg:px-8`
- section spacing: `py-6 md:py-8`
- card padding: `p-4 md:p-5`
- form gap: `gap-4`
- dense list gap: `gap-3`

### Page Container

Use this for most public pages:

```jsx
<div className="min-h-screen bg-slate-50">
  <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
    ...
  </main>
</div>
```

### Dashboard Container

Use this for staff/admin pages:

```jsx
<div className="min-h-screen bg-slate-50">
  <div className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">
    ...
  </div>
</div>
```

---

## 6. Border Radius and Shadows

| Element | Radius | Shadow |
| --- | --- | --- |
| Button | `rounded-md` | usually none |
| Input | `rounded-md` | none |
| Card | `rounded-lg` | `shadow-sm` |
| Modal/Dialog | `rounded-xl` | `shadow-lg` |
| Badge | `rounded-full` | none |
| Image | `rounded-lg` | none |

Rules:

- Prefer borders over heavy shadows.
- Use `border border-slate-200` for cards.
- Use `shadow-sm` only when a card needs separation.
- Do not use very large border radius everywhere.

---

## 7. Buttons

### Primary Button

Use for main actions like login, save, confirm.

```jsx
<button className="inline-flex items-center justify-center rounded-md bg-blue-700 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-60">
  Continue
</button>
```

### Accent CTA Button

Use for search and booking actions.

```jsx
<button className="inline-flex items-center justify-center rounded-md bg-amber-400 px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-amber-500 disabled:cursor-not-allowed disabled:opacity-60">
  Search
</button>
```

### Secondary Button

```jsx
<button className="inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60">
  Cancel
</button>
```

Rules:

- Buttons should have clear text.
- Avoid icon-only buttons unless the meaning is obvious.
- Use disabled state during API submission.
- Use full-width buttons on mobile where appropriate.

---

## 8. Forms

Form style should be simple and demo-friendly.

### Input

```jsx
<input className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-100" />
```

### Label

```jsx
<label className="mb-1 block text-sm font-medium text-slate-700">
  Destination
</label>
```

### Error Text

```jsx
<p className="mt-1 text-xs text-red-600">Destination is required.</p>
```

Rules:

- Every input must have a label.
- Every form must handle validation errors.
- Use `React Hook Form` only if already installed; otherwise simple controlled forms are acceptable for demo.
- Keep booking and login forms easy to complete during demo.

---

## 9. Header and Navigation

### Public Header

Booking.com-inspired header:

- blue background
- white logo text
- simple navigation
- auth actions on the right

```jsx
<header className="bg-blue-700 text-white">
  <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 md:px-6 lg:px-8">
    <div className="text-xl font-bold">HotelHub</div>
    <nav className="hidden items-center gap-6 text-sm font-medium md:flex">
      <a className="hover:text-blue-100">Stays</a>
      <a className="hover:text-blue-100">Bookings</a>
      <a className="hover:text-blue-100">Support</a>
    </nav>
  </div>
</header>
```

Rules:

- Keep navigation simple.
- Do not build complex mega menus.
- Mobile menu can be simple or omitted for demo if not necessary.

---

## 10. Search Panel

The search panel is the most important public UI element.

Style:

- blue hero section
- white or amber search box
- destination, dates, guests, search button
- clear labels

Layout:

```text
Hero title
Hero subtitle
Search box: destination | check-in | check-out | guests | search
```

Container style:

```jsx
<section className="bg-blue-700 text-white">
  <div className="mx-auto max-w-7xl px-4 py-8 md:px-6 lg:px-8">
    ...
  </div>
</section>
```

Search box style:

```jsx
<div className="grid gap-3 rounded-lg bg-amber-400 p-3 md:grid-cols-[2fr_1fr_1fr_1fr_auto]">
  ...
</div>
```

Rules:

- Search must be visually prominent.
- Use amber/yellow for the search panel/button.
- Keep fields large enough for demo visibility.

---

## 11. Hotel Card

Hotel cards should be information-rich like Booking.com.

Required content:

- hotel image
- hotel name
- location
- price per night
- availability/action button

Layout:

```text
[Image] [Hotel info] [Rating + Price + CTA]
```

Desktop style:

```jsx
<article className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[240px_1fr_220px]">
  ...
</article>
```

Mobile style:

- image on top
- details below
- price/action at bottom

Rules:

- Use real-looking placeholder images if backend image is missing.
- Price must be visually obvious.
- CTA should say `See availability` or `Reserve`.
- Rating badge should use blue background with white text.

---

## 12. Hotel Detail Page

Recommended sections:

1. hotel title and location
2. image gallery
3. overview
4. amenities
5. room types
6. policies
7. booking panel

Layout:

```text
Title
Gallery
Main content + sticky booking summary
Room list
Policies
```

Rules:

- Keep the booking/reserve action visible.
- Room type cards must show capacity, price, available rooms, and CTA.
- For demo, gallery can use one large image and 2-4 smaller images.

---

## 13. Booking Flow Pages

Booking flow must clearly show backend orchestration.

Pages:

- booking checkout page
- booking status page
- payment redirect/status page
- booking success/failure state

Checkout page layout:

```text
Guest details form
Booking summary card
Price summary
Confirm booking button
```

Status page should show:

- booking id
- current status
- payment status
- email notification status if available
- Firebase push notification status if available

Status badge colors:

| Status | Style |
| --- | --- |
| Success / Confirmed | `bg-green-50 text-green-700 border-green-200` |
| Pending / Processing | `bg-amber-50 text-amber-700 border-amber-200` |
| Failed / Cancelled | `bg-red-50 text-red-700 border-red-200` |

---

## 14. Notification UI

Firebase notification UI should be simple.

Use for:

- booking success notification
- payment success/failure
- booking cancellation update

Style:

```jsx
<div className="rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
  Notifications are enabled for booking updates.
</div>
```

Rules:

- Show whether notification permission is granted, denied, or not requested.
- Add a simple button: `Enable notifications`.
- Do not make notification setup block the booking flow.

---

## 15. Dashboard UI

For staff/admin pages, use a clean SaaS dashboard style.

Style direction:

- white cards
- simple tables
- filters at top
- status badges
- summary cards

Dashboard layout:

```text
Sidebar / top nav
Page title
Summary cards
Filter bar
Table
Pagination
```

Table style:

```jsx
<div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
  <table className="min-w-full divide-y divide-slate-200">
    ...
  </table>
</div>
```

Rules:

- Use tables for bookings, hotels, payments, users, promotions.
- Use status badges.
- Keep admin features visually simple for demo.

---

## 16. Loading States

Every API query must have a loading state.

Use skeletons:

```jsx
<div className="animate-pulse rounded-lg border border-slate-200 bg-white p-4">
  <div className="h-40 rounded-md bg-slate-200" />
  <div className="mt-4 h-4 w-2/3 rounded bg-slate-200" />
  <div className="mt-2 h-4 w-1/2 rounded bg-slate-200" />
</div>
```

Rules:

- Hotel search should show multiple skeleton cards.
- Detail page should show gallery/detail skeleton.
- Dashboard tables should show skeleton rows.

---

## 17. Empty States

Every list page must have an empty state.

Structure:

```text
Icon or simple illustration
Title
Description
Optional CTA
```

Example:

```jsx
<div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
  <h3 className="text-base font-semibold text-slate-900">No hotels found</h3>
  <p className="mt-2 text-sm text-slate-600">Try changing your destination, dates, or guest count.</p>
</div>
```

---

## 18. Error States

Every failed API call must show an error state.

Example:

```jsx
<div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
  Something went wrong. Please try again.
</div>
```

Rules:

- Do not show raw backend stack traces.
- Show useful messages when backend returns validation errors.
- Include retry button for important queries.

---

## 19. Responsive Rules

This app is web-first but should not break on mobile.

Breakpoints:

- mobile: default
- tablet: `md`
- desktop: `lg`

Rules:

- Search form stacks on mobile.
- Hotel cards stack on mobile.
- Dashboard tables can scroll horizontally.
- Use `max-w-7xl` for main content.
- Avoid fixed pixel widths except for images/sidebar.

---

## 20. Component Naming

Use clear names.

Examples:

```text
HotelCard.jsx
HotelSearchForm.jsx
HotelFilterSidebar.jsx
HotelGallery.jsx
RoomTypeCard.jsx
BookingSummaryCard.jsx
BookingStatusBadge.jsx
NotificationPermissionCard.jsx
DashboardStatCard.jsx
DataTable.jsx
EmptyState.jsx
ErrorState.jsx
PageHeader.jsx
```

Rules:

- Components should be reusable but not over-abstracted.
- Keep page components thin.
- Move API calls into services/hooks.
- Do not place business logic inside components.

---

## 21. Tailwind Class Rules

Use Tailwind utilities directly.

Rules:

- Do not use inline styles.
- Do not create random colors.
- Do not use arbitrary values unless necessary.
- Prefer semantic reusable components for buttons, cards, badges, and states.
- Keep class names readable.

Allowed arbitrary values only when useful:

```text
md:grid-cols-[240px_1fr_220px]
md:grid-cols-[2fr_1fr_1fr_1fr_auto]
```

---

## 22. Demo Data Rules

For demo fallback data:

- Use realistic hotel names.
- Use realistic Vietnamese or international locations.
- Use realistic prices.
- Clearly separate mock/demo data from API data.
- Do not hardcode mock data inside page components.

Example locations:

```text
Hanoi
Da Nang
Ho Chi Minh City
Nha Trang
Da Lat
Phu Quoc
Bangkok
Singapore
```

---

## 23. Codex Prompt Rules

When asking Codex to generate UI, use this instruction:

```text
Follow docs/ui-design.md exactly.
Use Tailwind CSS only.
Use JavaScript, not TypeScript.
Keep the UI Booking.com-inspired: blue header, amber search CTA, white cards, dense hotel information, clear prices and availability buttons.
Do not introduce new UI libraries.
Do not modify unrelated files.
Keep page components thin and reusable.
Show loading, empty, and error states for every API query.
```

---

## 24. Example Codex Task

```text
Task: Create the hotel search results UI.

Context:
- This is HotelHub, a React Vite JavaScript hotel booking SaaS demo frontend.
- Follow docs/ui-design.md and AGENTS.md.
- Use Booking.com-inspired style.

Requirements:
- Create a search page with blue hero section and amber search form.
- Add destination, check-in, check-out, guests fields.
- Add hotel result cards with image, name, location, rating, amenities, price, and CTA.
- Add loading, empty, and error states.
- Use reusable components.
- Do not call backend directly inside components.
- Do not modify unrelated files.

After coding:
- List changed files.
- Explain assumptions.
```

---

## 25. Final Rule

This frontend is for demonstrating the backend system.

Therefore, UI should make these backend features easy to see:

- hotel search
- hotel detail
- booking orchestration
- payment status
- Firebase notification
- user login/profile
- staff/admin management

A simple, consistent, demo-ready UI is better than a visually complex UI.
