# Frontend Stack and Skills

## Goal

The frontend should be modern, visual, responsive and easy to maintain.

The app is web-first, mobile-friendly and should support a strong landing page with 3D/animated visuals.

Do not add all libraries at once. Add dependencies only when needed.

---

## Core stack

Use:

- Next.js
- TypeScript
- Tailwind CSS
- shadcn/ui
- pnpm

---

## UI components

Use `shadcn/ui` for reusable UI primitives.

Recommended components:

- button
- card
- dialog
- sheet
- dropdown-menu
- tabs
- form
- input
- select
- avatar
- badge
- table
- toast/sonner

Do not create a large custom design system before the visual direction is finalized.

---

## Animations

Use `motion` for normal UI animations.

Use it for:

- modal transitions
- page section reveal
- card hover/tap effects
- tab transitions
- ranking/market animations
- landing text animations

Do not use Motion for 3D scenes.

GSAP is optional and should only be added for advanced landing-page animation or complex scroll-based effects.

---

## 3D

Use:

- three
- @react-three/fiber
- @react-three/drei

Use these for:

- generic 3D trophy/cup
- interactive rotation
- floating hero object
- lighting
- 3D presentation controls

The 3D trophy must be generic and must not copy official tournament trophies.

Recommended components/helpers:

- Canvas
- PresentationControls
- OrbitControls if needed
- Float
- Environment
- ContactShadows
- useGLTF

---

## Forms

Use:

- react-hook-form
- zod
- @hookform/resolvers

Use forms for:

- login
- register
- forgot password
- create league
- join league
- league settings
- profile settings

All forms should validate null, empty and invalid values.

---

## Server state

Use `@tanstack/react-query` for server state.

Use it for:

- user leagues
- league details
- market players
- ranking
- activity log
- profile
- mutations such as buying players or joining leagues

Do not use global client state for server data.

---

## Client UI state

Use `zustand` only for small UI state.

Examples:

- theme
- menu open/closed
- selected league
- modal state
- local UI preferences

Do not use Redux.

---

## Icons

Use `lucide-react`.

Use icons for:

- menu
- user
- trophy
- ranking
- market
- activity
- settings
- theme switch

---

## Charts

Use `recharts` only when chart features are needed.

Potential charts:

- points evolution
- team value evolution
- money evolution
- historical ranking

Do not add chart dependencies before charts are implemented.

---

## Lineup interaction

Initial lineup interaction should be simple and mobile-friendly.

Preferred MVP:

- tap/click player;
- tap/click pitch position;
- assign player.

Drag and drop is optional later.

If drag and drop is needed, use:

- @dnd-kit/core
- @dnd-kit/sortable
- @dnd-kit/utilities

Do not add dnd-kit until lineup drag and drop is actually implemented.

---

## Asset rules

Do not hardcode player image URLs or team logo URLs in components.

Use asset resolvers from:

```txt
frontend/src/shared/assets/

hay un stack recomendado,
puede instalarlo,
puede proponer/añadir librerías si aportan valor,
pero debe justificarlo y no mezclar librerías UI sin motivo.

For dev dependencies:

pnpm add -D package-name


Luego añade esta sección de librerías ampliadas:

```md
---

## Approved frontend libraries

The agent may install these libraries when needed.

### Core UI

Preferred UI system:

```txt
shadcn/ui