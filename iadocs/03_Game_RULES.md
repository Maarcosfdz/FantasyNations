# Game Rules

## Goal

The game is a fantasy football experience for private leagues.

Users compete by buying players, building a squad, selecting a lineup, earning points and managing money.

The rules are inspired by fantasy football apps with market and release clause mechanics.

---

## League

Each league has:

- name;
- owner;
- members;
- invite code;
- rules;
- market;
- activity log.

The league owner can configure rules.

---

## Configurable league rules

League rules should be stored in a flexible way.

Initial rules:

- starting money;
- money earned per point;
- release clause protection time;
- market refresh interval;
- number of players in market;
- maximum players per squad;
- minimum lineup players;
- lineup formation rules if enabled;
- market value change rules.

More rules may be added later.

---

## Squad

Each user has a squad inside each league.

A squad contains players owned by that user.

Players can be acquired by:

- buying from the market;
- paying another user's release clause.

Players can leave the squad by:

- being sold;
- being bought by another user through release clause.

---

## Lineup

Users select a starting 11.

The lineup should be displayed over a football pitch.

The system should validate:

- correct number of players;
- valid positions if formation rules are active;
- user owns the selected players.

---

## Market

The market refreshes every 24 hours by default.

The market shows available players that can be bought.

Market configuration may include:

- refresh interval;
- number of players shown;
- price rules;
- whether owned players can appear;
- whether market refresh is automatic or manual.

---

## Buying from market

A user can buy a player from the market if:

- the player is available;
- the user has enough money;
- the player is not already owned if league rules prevent duplicates;
- the purchase is valid according to league rules.

After purchase:

- user money decreases;
- player joins squad;
- activity entry is created.

---

## Release clauses

Users can buy players from other users by paying the release clause.

A release clause is the price required to take a player from another squad.

When a release clause is paid:

- buyer pays the clause amount;
- seller receives money according to league rules;
- player moves to buyer squad;
- protection time may apply;
- activity entry is created.

---

## Release clause protection

After a player is bought, a protection time may prevent immediate release-clause purchases.

This rule should be configurable per league.

Example:

- after buying a player, he is protected for X hours.

---

## Release clause changes

Users can increase a player's release clause.

The release clause may also change automatically based on market value.

Initial rule idea:

- release clause can go up or down depending on market value;
- if user manually increases it, the change should be respected according to league rules.

This system can be improved later.

---

## Player points

Players earn points based on match performance.

The exact scoring system can be defined later.

Possible inputs:

- goals;
- assists;
- clean sheets;
- minutes played;
- cards;
- penalties;
- own goals;
- saves;
- team result;
- advanced stats if available.

The scoring engine must be deterministic and testable.

---

## User money

Users can earn money based on points.

Initial configurable rule:

```txt
moneyEarned = points * moneyPerPoint