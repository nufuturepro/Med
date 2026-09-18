# Med RX roadmap

Living document — update it when the plan changes, don't treat it as a promise
calendar.

## Identity: "Med RX" → "Rx" (planned)

A future version rebrands the app to **Rx**, where **Rx stands for "Record
eXchange"** — reframing the product from a medication reminder into a personal
logbook of *what goes into the body and when*: medicines, symptoms, events,
and the context between them.

What that implies when it happens:

- **Name**: launcher label "Rx" everywhere (phone + Wear). The Med RX identity
  remains in the README, repo name, and fork-credit line for discoverability
  and provenance.
- **Package id stays `com.nukirk.medrx`**: changing the applicationId would
  orphan every existing install — updates would stop matching, and moving data
  would require uninstall + restore. The id is identity plumbing, not branding.
- **What "Record eXchange" means for the product**: the logbook already points
  this way — medicines with supply tracking, symptoms with severity, events,
  the skip ledger, and the doctor report. Future work leans into the *exchange*
  half: export/import in open formats (CSV today; standardized health formats
  later) so your records move where you need them.
- **versionName**: the rebrand lands as a base-version step per
  `docs/RELEASING.md` (e.g. `2.2.0-fork.1`), not a fork-patch bump — it's a
  product identity change, not a maintenance release.
- **Strings**: the app name appears in very few user-visible strings; those get
  reworded across all six languages as part of the rebrand. The name is
  intentionally NOT translated anywhere — "Rx" is the brand in every locale.

## After the rebrand

- Reword the What's New dialog copy for the release that ships it.
- Update release-listing text; keep the upstream credit intact — it names the
  fork lineage, which the rename does not change.
