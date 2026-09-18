# Rx roadmap

Living document — update it when the plan changes, don't treat it as a promise
calendar.

## Identity: "Med RX" → "Rx" (Record eXchange) — SHIPPED in 2.2.0-fork.1

The app is renamed to **Rx**, where **Rx stands for "Record eXchange"** — a
personal logbook of *what goes into the body and when*: medicines, symptoms,
events, and the context between them.

How it was done:

- **Launcher name is "Rx"** on phone and Wear, in every language (the brand is
  untranslated by design). The Med RX identity remains in the README, repo
  name, and fork-credit line for discoverability and provenance.
- **Package id stays `com.nukirk.medrx`**: changing the applicationId would
  orphan every existing install — updates would stop matching, and moving data
  would require uninstall + restore. The id is identity plumbing, not branding.

## Translations: Weblate (planned)

Volunteer translation moves to [Weblate](https://weblate.org) once community
translation demand appears (or before the next string-heavy feature). Setup,
when scheduled:

1. Host the project on weblate.org (free for open-source) pointed at
   `nufuturepro/MedRX`, component = `app/src/main/res/values*/strings.xml`.
2. Add a `TRANSLATING.md` contributor guide: where strings live, tone rules per
   language (du / vous / tú / você / вы), and the "brand name is never
   translated" rule.
3. Add a CI check that fails when any locale drifts out of sync with English
   (missing or extra keys), so PRs can't silently break a language.
4. Enable Weblate's auto-PR flow so completed languages arrive as pull
   requests, not raw commits.

Current state: six languages fully in sync (en, de, fr, es, pt, ru — 362/362
keys each); the other twenty upstream locales carry only the original Med
strings and fall back to English for fork features.

## Dosing models: sprays and as-needed meds (scoping)

Two medication shapes the current scheduler doesn't express, to be scoped
before any scheduling-engine work:

### Spray medications

Examples: nasal corticosteroids, sublingual nitroglycerin, throat sprays.

- **Unit model**: doses counted in *sprays* (or puffs), not tablets. A bottle
  has a total spray count (e.g. 120 sprays), not a pill count. Supply tracking
  should decrement per actuation; a "metered vs. non-metered" distinction
  affects whether the ledger can assume exact counts.
- **Schedule shapes**: fixed times (like current daily slots), PRN (see
  below), or both (e.g. twice daily AND as-needed rescue). The med editor
  needs a dose unit selector and a "doses per use" field (some sprays are
  2 sprays per use).
- **Open questions**: does one use = one spray or N? Refill math when the user
  primes the device (priming wastes sprays)? Expiration/after-opening shelf
  life reminders?

### As-needed (PRN) medications

Examples: analgesics, antihistamines, rescue inhalers.

- **No fixed schedule**: the med card should appear without a time slot and
  log *occurrences* (date + time + optionally amount), never generate missed
  doses, and never count against adherence/streaks.
- **Inter-dose guards**: a configurable minimum interval ("no more than one
  dose per 4h") and a daily maximum — surfaced as warnings at log time and in
  the ledger. This connects naturally to the existing skip reason "Double dose
  protection".
- **Stats impact**: PRN usage frequency over time is clinically interesting
  (rescue-medicator use is an asthma/flare signal) — likely a new Stats view
  rather than shoehorning into the adherence calendar.
- **Data model**: extends `MedData` with a dosing kind (scheduled / PRN),
  a per-use amount, and interval/cap rules; occurrences could reuse
  `takenHistory` (it already stores a timestamp per date) but the planner,
  reminders, and Stats must learn to ignore PRN entries.

Suggested sequencing: supply-unit abstraction first (sprays are really a
"unit" problem), PRN scheduling second (a scheduling-model problem), then the
Stats view that treats both correctly.
