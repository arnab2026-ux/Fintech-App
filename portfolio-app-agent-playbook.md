# Building a Portfolio Tracker with a Claude Code Agent Fleet

A prompt-by-prompt playbook. Prototype scope, Indian retail assets (MF, FD, equity, bonds, EPF), tax answers and swap suggestions.

---

## The shape of the system

Map your roles onto Claude Code primitives rather than trying to make one primitive do everything:

| Your role | Claude Code primitive | Where it lives |
|---|---|---|
| Product manager | Spec + ADR documents, regenerated on demand | `/docs/spec.md`, `/docs/adr/` |
| UX designer | Subagent + a design-system skill | `.claude/agents/ux-designer.md` |
| Android engineer | Subagent | `.claude/agents/android-engineer.md` |
| Backend engineer | Subagent | `.claude/agents/api-engineer.md` |
| Data engineer | Subagent | `.claude/agents/data-engineer.md` |
| QA tester | Subagent + a scheduled task | `.claude/agents/qa-engineer.md` |
| The manager coordinating them | **Agent team** with a lead | `.claude/teams/` |
| House rules everyone obeys | CLAUDE.md + skills | `CLAUDE.md`, `.claude/skills/` |
| Keeping parallel agents off each other's files | Git worktrees | one branch per stream |

The single most important idea: **role consistency comes from files, not from running processes.** A subagent that re-derives your conventions each session will drift. A subagent that reads a two-page skill will not.

**Suggested stack for the prototype:** Kotlin + Jetpack Compose, Room for local cache, Hilt, Ktor client. Supabase for Postgres + auth + edge functions on the backend — you already have that muscle memory, and it removes a week of backend scaffolding. Tax and valuation logic in a pure-Kotlin module with zero Android dependencies so it's testable without an emulator.

---

## Step 0 — Repo and scaffold

Do this yourself, not through an agent. Agents are bad at initial project generation and good at filling in a structure that exists.

```
Set up a new Android project with these modules, and nothing else yet:

  :app                  Compose UI, DI wiring
  :core:model           pure Kotlin data classes, no Android deps
  :core:money           BigDecimal money type + formatting, no Android deps
  :core:database        Room entities and DAOs
  :core:network         Ktor client + Supabase client
  :feature:portfolio    portfolio dashboard
  :domain:valuation     pure Kotlin, no Android deps
  :domain:tax           pure Kotlin, no Android deps

Kotlin 2.x, Compose BOM, Hilt, version catalog in gradle/libs.versions.toml.
Make it build and run with a blank screen. Do not write any features.
```

Verify it builds before going further. Everything after this assumes a green build.

---

## Step 1 — The product manager pass

One session, high effort, no agents. You are the other half of this conversation.

```
/effort high

You are acting as the product manager for a personal portfolio tracker
for Indian retail investors. Before writing anything, interview me.

Ask me ONE question at a time, wait for my answer, then ask the next.
Cover, in this order:
  1. Who this is for and what the one job-to-be-done is
  2. Which asset classes are in the v1 scope and which are deferred
  3. How data gets in for each asset class (manual entry, statement
     upload, scraping, API) — probe me on each one separately
  4. What the user sees on the home screen, ranked
  5. Which tax questions the app must answer
  6. What "suggest a swap" means concretely — what input, what output
  7. What is explicitly out of scope for v1

Do not propose solutions during the interview. When we're done, write
/docs/spec.md with: problem statement, personas, v1 scope, v2 backlog,
per-asset-class data flow, and a glossary of every domain term we used.
```

Then:

```
Read /docs/spec.md. Write /docs/adr/ as one file per decision, using the
standard ADR format (context, decision, consequences). Cover at minimum:
persistence strategy, offline behaviour, how NAV and price data is
sourced and refreshed, how we model a transaction vs a holding, how
corporate actions are handled, auth model, and how tax rules are
versioned by financial year.

For each ADR, give me the two strongest alternatives you rejected and
why. Where you are uncertain, write UNRESOLVED and ask me.
```

Read these. This is the highest-leverage hour of the whole project — every agent downstream inherits whatever is wrong here.

---

## Step 2 — House rules

```
Read /docs/spec.md and every file in /docs/adr/. Write CLAUDE.md for
this repo. It must be under 200 lines and contain only rules an agent
would otherwise get wrong:

  - module boundaries and which module may depend on which
  - the money rule: all amounts are BigDecimal, stored as paise in Long
    at the persistence layer, never Float or Double, never == comparison
  - date rule: LocalDate for trade dates, Instant for sync timestamps,
    all financial-year logic goes through FinancialYear in :domain:tax
  - naming conventions for entities vs domain models vs UI state
  - the test requirement: every function in :domain:* needs unit tests,
    UI needs at least one Compose test per screen
  - what an agent must NEVER do: add a dependency without asking, edit
    the version catalog, change a Room schema without a migration, touch
    files outside its assigned module

Write it as terse imperatives. No prose, no explanation of why.
```

Then the skills. These are what your "roles" actually read:

```
Create these skills in .claude/skills/, each with a SKILL.md under 150
lines:

  money-arithmetic/   — the money type, rounding rules (always
                        HALF_EVEN, 2dp for display, full precision
                        internally), currency formatting for INR with
                        lakh/crore grouping, XIRR and CAGR
                        implementations with their edge cases
  compose-conventions/— our state hoisting pattern, our
                        UiState/UiEvent shape, our theming tokens, what
                        goes in a ViewModel vs a use case
  room-migrations/    — how we version and migrate schemas, the test
                        we require for every migration
  tax-rules-fy/       — how tax rules are keyed by financial year, how
                        to add a new FY without touching old logic
  supabase-patterns/  — our table naming, RLS policy shape, how edge
                        functions are structured and deployed

Each skill should be written so an agent that reads only that skill can
do the work correctly without reading the rest of the repo.
```

---

## Step 3 — Define the fleet

```
Create subagents in .claude/agents/. For each one give it: a one-line
description, a tool allowlist, the skills it should load, the modules it
owns, and an explicit "you do not touch" list.

  ux-designer
    Owns: /docs/design/, the theme and component library in :app
    Produces: screen specs as markdown with component breakdowns and
    state tables. Does NOT write feature code.

  android-engineer
    Owns: :app, :feature:*
    Loads: compose-conventions, money-arithmetic
    Does NOT touch: :domain:*, :core:database schemas, backend

  api-engineer
    Owns: Supabase schema, edge functions, :core:network
    Loads: supabase-patterns
    Does NOT touch: any Android UI code

  data-engineer
    Owns: ingestion pipelines, parsers (CAS statements, broker CSVs),
    NAV/price refresh jobs, :core:database
    Loads: room-migrations, money-arithmetic
    Does NOT touch: UI or tax logic

  domain-engineer
    Owns: :domain:valuation, :domain:tax, :core:model, :core:money
    Loads: money-arithmetic, tax-rules-fy
    Writes pure Kotlin only. No Android imports. Every public function
    gets tests in the same commit.

  qa-engineer
    Owns: all test source sets
    May read everything, may write only tests. Never fixes production
    code — files a report instead.

  code-reviewer
    Read-only plus comment. Reviews against CLAUDE.md and the skills.
    Reports findings ranked by severity; does not edit.
```

Sanity check one before trusting the set:

```
Show me .claude/agents/domain-engineer.md and explain what it would and
would not do if I asked it to "add LTCG calculation for equity".
```

---

## Step 4 — UX pass before any feature code

```
Use the ux-designer subagent. Read /docs/spec.md. Produce
/docs/design/screens.md covering every v1 screen: portfolio dashboard,
asset-class detail, holding detail, add/import flow, tax centre, and
the swap-suggestion screen.

For each screen give: purpose, the data it needs, every state
(loading / empty / partial data / error / populated), the component
breakdown, and the navigation edges in and out. Include an explicit
empty-state and first-run design — this app is useless until the user
has data in it, so onboarding is the hardest screen.

No code. Markdown and ASCII wireframes only.
```

Then the design system:

```
Use the ux-designer subagent. Implement the theme and shared components
from /docs/design/screens.md in :app — colour tokens, type scale,
spacing scale, and the shared components (AmountText, AssetRow,
DeltaChip, SectionCard, EmptyState). Compose previews for every one,
light and dark. Do not build any screen yet.
```

---

## Step 5 — Data model and backend, in one coordinated pass

This is the first place two roles must agree, so run it as an agent team rather than two separate sessions.

```
Start an agent team with domain-engineer as lead and api-engineer and
data-engineer as peers.

Goal: land the complete data model for all five asset classes at once.

  domain-engineer defines the domain models in :core:model first —
    Holding, Transaction, Instrument, Valuation, and the asset-class
    subtypes. Everything else waits on this.
  data-engineer then writes the Room entities, DAOs, and mappers to and
    from the domain models.
  api-engineer then writes the Supabase schema and migrations to mirror
    it, with RLS policies scoped to the authenticated user.

Rules: no UI, no business logic beyond mapping, no partial commits. The
lead must reject any entity where an amount is not stored as Long paise.
When all three are done, the lead writes /docs/adr/0010-data-model.md
recording what was decided.
```

Review the domain models yourself before you let anyone build on them. Changing `Transaction` after five modules depend on it is the expensive mistake in this project.

---

## Step 6 — One vertical slice, done properly

Do mutual funds end-to-end and review it closely. The fleet copies this pattern later, so its quality sets the ceiling.

```
Build the mutual fund vertical slice end to end. Work through it in this
order, and stop for my review at each arrow:

  data-engineer: MF instrument catalogue + NAV refresh from AMFI's daily
    NAV file, cached in Room with a staleness policy →
  domain-engineer: valuation for an MF holding — units, average cost,
    current value, absolute and XIRR returns, with tests covering
    partial redemptions, SIPs, dividend reinvestment, and switches →
  api-engineer: sync endpoints and RLS for MF holdings →
  android-engineer: the MF list and detail screens per
    /docs/design/screens.md →
  qa-engineer: full test pass, report gaps →
  code-reviewer: review the whole slice against CLAUDE.md

Use one git worktree for this slice. Commit at every arrow.
```

When it's done:

```
Read the mutual fund slice end to end and write
/docs/patterns/asset-class-module.md: the canonical structure every
other asset class must follow, with the file layout, the naming, the
test expectations, and the three things that were hardest to get right.
Write it as instructions to another engineer, not as a description.
```

That document is what makes the parallel phase work.

---

## Step 7 — Now go wide

Four asset classes, four worktrees, genuinely independent. This is the one place a dynamic workflow is a clean fit, because the units are parallel and identical in shape:

```
use a workflow to implement four asset-class modules — fixed_deposit,
equity, bonds, and epf — following /docs/patterns/asset-class-module.md
and the mutual fund slice exactly. One agent per module, each working in
its own isolated copy. Each module: domain model usage, Room entities,
DAO, valuation service, ingestion path, and unit tests. Then run an
independent reviewer agent per module that diffs its structure against
the mutual fund module and reports every divergence before anything is
merged.
```

Set `/config workflowSizeGuideline=medium` first, and run it on `fixed_deposit` alone before all four.

Alternatively, if you want to stay hands-on and interruptible, run it as a team instead:

```
Start an agent team with four peers, one per asset class
(fixed_deposit, equity, bonds, epf), each in its own git worktree, each
following /docs/patterns/asset-class-module.md. Lead agent reviews each
module against the mutual fund reference before merging to main and
resolves conflicts in shared files by asking me.
```

Use the team version if the modules will touch shared files. Use the workflow version if they truly won't.

---

## Step 8 — The tax engine

Build this separately and carefully. It is the part users will trust or not trust.

```
Use the domain-engineer subagent. Build :domain:tax.

First write /docs/tax-rules.md: every rule the engine implements, stated
in plain English with the FY it applies to and the source. Cover STCG
and LTCG for listed equity and equity MF, debt MF taxation post-April
2023, FD interest as income from other sources with TDS, EPF interest
taxability above the contribution threshold, bond interest and capital
gains, set-off and carry-forward of losses, and the 80C/80D deductions
relevant to these instruments.

Then implement it as pure functions keyed by FinancialYear, per the
tax-rules-fy skill. Every rule in the doc maps to one named function.
Every function gets a test table with at least one boundary case.
```

Then the harvesting and swap logic, which are both queries over the same engine:

```
Use the domain-engineer subagent. Implement two analysers on top of
:domain:tax and :domain:valuation:

  HarvestAnalyser — given a portfolio and a target date, find lots whose
  sale would realise gains within the LTCG exemption or losses that
  offset existing gains. Output ranked candidates with the exact tax
  effect, the units to sell, and what re-entry costs.

  SwapAnalyser — given the portfolio, flag holdings that are weak on
  objective, checkable grounds: expense ratio above category median,
  3y/5y return below category median, overlap above a threshold with
  another holding, exit load window expired, category drift from the
  stated mandate. Output the reason and the evidence, plus the tax cost
  of switching. Rank by expected benefit net of tax.

Both must return their reasoning as structured data, not prose. The UI
renders the reasons; the analyser never writes sentences.
```

That last constraint matters: it keeps the explanation auditable and makes the feature easy to soften later if you ever do take it public.

---

## Step 9 — Adversarial QA

Two things worth running as workflows, because they're fan-out shaped:

```
use a workflow to generate property-based tests for every public
function in :domain:tax and :domain:valuation. One agent per function
writes the tests. A second, independent agent per function reads only
/docs/tax-rules.md and the function signature, then tries to construct
an input where the implementation and the stated rule disagree. Report
every disagreement with the failing input.
```

```
use a workflow to audit every file under :app and :feature: for
hardcoded strings, amounts rendered without our INR formatter, Double or
Float used for money, network calls off a main-safe dispatcher, and
missing empty or error states. Verify each finding against CLAUDE.md
before reporting.
```

And a standing loop:

```
Create a scheduled task that runs each night: build, run the full test
suite, run the qa-engineer subagent over anything that changed that day,
and open a PR with fixes for anything mechanical. Anything requiring a
judgement call goes into /docs/qa-backlog.md instead of a PR.
```

---

## Daily operating rhythm

Once the scaffolding exists, the loop is:

1. You pick the next feature from the spec backlog.
2. `ux-designer` produces or updates the screen spec.
3. Agent team builds it — domain first, then data, then API, then UI.
4. `qa-engineer` and `code-reviewer` run before merge.
5. You review the PR. You are the only approver.

Keep your review at the *interface* level — domain models, function signatures, screen specs. Reviewing every line defeats the point; reviewing none of the contracts guarantees rework.

---

## Cost and safety controls

- Run the first instance of anything parallel on one item before scaling it. Watch `/workflows` for token totals.
- `workflowSizeGuideline=medium` by default; `large` only for the asset-class fan-out.
- One worktree per parallel stream, always. Merge conflicts between agents are the failure mode that wastes the most time.
- Commit at every arrow in a sequence, so a bad agent run costs one step, not one day.
- Keep `:domain:*` free of Android dependencies. It means the tax and valuation tests run in milliseconds, which is what makes the adversarial QA loops cheap enough to run nightly.
- Ask for a smaller model on mechanical stages (test scaffolding, mappers) and keep the strongest model for domain and tax work.

---

## The three things most likely to go wrong

**Data ingestion.** MF and EPF data in India is not casually available. For a prototype, plan on CAS statement parsing (CAMS/KFintech PDF), broker CSV export, and manual entry — and have the data-engineer build a clean import abstraction so a real Account Aggregator path can slot in later. Ask Claude to research the current options before designing this layer; it moves.

**Money arithmetic drift.** Agents will independently reach for `Double`. The skill plus a lint rule plus the audit workflow is three layers of defence, and you want all three.

**Spec rot.** After a few weeks `/docs/spec.md` will no longer describe the app. Schedule a weekly prompt: *"Diff /docs/spec.md against what the code actually does and list every divergence."* An agent fleet building against a stale spec is the quiet way this goes wrong.
