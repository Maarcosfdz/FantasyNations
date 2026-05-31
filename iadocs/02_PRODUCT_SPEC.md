# Product Spec

## Project name

Fantasy Nations

## Goal

Fantasy Nations is a private fantasy football application for an international national-team tournament.

The app is designed mainly for friend groups. Users can create private leagues, join leagues, build squads, buy and sell players, pay release clauses, compete in rankings and follow league activity.

The goal is to build a fun fantasy experience inspired by apps like Marca Fantasy, but adapted to national teams and private leagues.

---

## Product principles

- Private leagues first.
- Web-first, but responsive for mobile, tablet and desktop.
- Mobile experience is important.
- Fun and competitive between friends.
- Easy to add players and teams manually.
- Easy to replace images, logos and sensitive assets.
- Easy to replace the data source.
- MVP first, advanced rules later.
- No real-money betting.
- No payments in MVP.

---

## Public landing page

The app has a public main page that presents the project.

The landing page should include:

- a header;
- the Fantasy Nations name;
- a hero/cover section;
- a 3D World-Cup-style trophy or generic international cup object;
- a football stadium background layer;
- a short description of the app;
- smooth scroll sections;
- an About section;
- GitHub link;
- LinkedIn link;
- login/register modal.

Important legal/design note:

The trophy must not copy an official FIFA World Cup trophy design. It should be a generic fantasy/international football cup.

---

## Header

The landing header should include navigation to:

- Home;
- About;
- Login / Sign in.

The About section should include:

- short project description;
- GitHub: https://github.com/Maarcosfdz
- LinkedIn: https://www.linkedin.com/in/marcos-romay-82b16036a/

---

## Authentication

Users must be able to sign in using:

- Google OAuth;
- email and password.

Users must be able to:

- register;
- log in;
- log out;
- recover password using a forgot-password flow;
- change/reset password.

User profile data:

- nickname;
- email;
- password if using email/password auth;
- optional profile image/avatar.

The nickname is the public display name inside leagues.

Passwords must never be stored in plain text.

---

## Authenticated area

After login, users should be redirected to the "My Leagues" screen.

---

## My Leagues screen

The My Leagues screen shows:

- leagues the user has joined;
- button to create a league;
- button to join a league.

Each league card should show useful information such as:

- league name;
- number of members;
- user ranking if available;
- last activity if available.

---

## Create league

Users can create a league.

League creation should allow:

- league name;
- initial money/budget;
- money earned based on points;
- release clause protection time;
- market configuration;
- other rules added later.

Rules should be stored in a configurable way so more league settings can be added in the future.

---

## Join league

Users can join a league using an invite code.

---

## League layout

Inside a league, the main navigation should be placed as a bottom selector/footer.

The central/default tab is:

- Ranking

Tabs from left to right:

1. Team
2. Lineup
3. Ranking
4. Market
5. Activity

Meaning:

- Team: full squad and team management.
- Lineup: select starting 11 on a football pitch background.
- Ranking: league standings.
- Market: available players refreshed every 24 hours.
- Activity: league history from the last month.

The layout must work well on mobile first, then adapt to tablet and desktop.

---

## Team tab

The Team tab shows all players owned by the user.

Users should be able to:

- view squad players;
- see player names and images;
- see player value;
- see player release clause;
- increase release clause;
- sell players if implemented;
- manage squad details.

---

## Lineup tab

The Lineup tab allows users to select their starting 11.

Visual requirement:

- use a football pitch background;
- allow selecting/placing players on the pitch;
- show player images or safe fallbacks;
- support mobile interaction.

The lineup should be easy and visual.

---

## Ranking tab

The Ranking tab shows the league standings.

It should show:

- position;
- user nickname;
- user avatar if available;
- total points;
- optionally money, squad value or recent points.

---

## Market tab

The Market tab shows available players.

Market behavior:

- market refreshes every 24 hours;
- players can be bought from the market;
- player images and names are shown;
- market players are taken from available tournament players;
- players should be easy to add manually or through data imports.

---

## Activity tab

The Activity tab shows the league history.

It should show activity from the last month.

Examples:

- player bought;
- player sold;
- release clause paid;
- user joined league;
- lineup changed;
- market refreshed;
- league rule changed.

---

## Player images and team data

The app may show:

- player names;
- player images;
- national team names;
- stadium names;
- user images.

These assets must be isolated and easy to disable or replace.

Do not hardcode image URLs directly in components.

---

## Data source

The app should support scraped/imported sports data.

Data source logic must be isolated from the main application.

The main app should interact with the data source as if it were an internal API.

Initial idea:

- Python scripts can be used for scraping;
- scraped data can come from sports/betting websites;
- data must be normalized before entering the app;
- app logic must not depend directly on a specific scraped website.

Important:

Scraping must be replaceable by manual data, CSV or a paid API later.

---

## Player values and points

Player points and values should change based on performance data.

A future algorithm should calculate:

- player fantasy points;
- market value changes;
- release clause changes;
- money earned by users based on points.

The first MVP can use a simple algorithm, but it must be easy to improve later.

---

## Not MVP

Do not implement unless explicitly requested:

- real-money betting;
- payments;
- public global leagues;
- native mobile app;
- chat;
- push notifications;
- advanced admin dashboard;
- complex social feed;
- multiple sports;
- complex AI recommendations;
- production-grade scraping system from day one.

---

## MVP priority

The first useful version should include:

1. landing page;
2. login/register;
3. my leagues;
4. create/join league;
5. league tabs;
6. basic player data;
7. market;
8. buy players;
9. lineup selection;
10. ranking;
11. league activity;
12. safe asset architecture.