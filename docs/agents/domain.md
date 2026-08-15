# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

This repo diverges from the skill defaults in one place: **ADRs live in `docs/decisions/`, not `docs/adr/`.** Everywhere a skill says `docs/adr/`, read and write `docs/decisions/`.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root (does not exist yet — see below).
- **`docs/decisions/`** — read ADRs that touch the area you're about to work in. Start from `docs/decisions/index.md`, which lists every ADR with its status and date.
- **`.claude/rules/*.md`** — this repo's coding rules (domain, application, adapter, naming, cqrs, validation, scaffold). These are architectural constraints, not style preferences; treat a rule violation the same way you'd treat a contradicted ADR.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo. One `CONTEXT.md` at the root, one ADR directory:

```
/
├── CONTEXT.md                            ← not created yet; /domain-modeling adds it when needed
├── docs/decisions/
│   ├── index.md                          ← the ADR index
│   ├── 0001-bigserial-id-strategy.md
│   └── 0002-jooq-generated-location.md
├── .claude/rules/                        ← coding rules, loaded by path
└── korConverter/                         ← 6 Gradle modules (hexagonal)
```

The 6 Gradle modules (`domain`, `application`, `adapter-bot`, `adapter-persistence`, `configuration`, `boot`) are architectural layers of a **single** bounded context — a Discord 영타→한글 converter bot — not separate contexts. Don't split them into per-context `CONTEXT.md` files.

## Writing a new ADR

- Number sequentially from the highest existing file in `docs/decisions/`.
- Filename: `NNNN-kebab-case-title.md`.
- **Always update `docs/decisions/index.md`** in the same change — the index is the entry point and a stale index makes an ADR invisible.
- This repo has `/docs:adr` (Nygard format) and `/docs:madr` (MADR 4.0) skills; prefer them over hand-rolling the structure.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

Until `CONTEXT.md` exists, `.claude/rules/naming.md` is the closest thing this repo has to a vocabulary contract — its layer/object naming table is binding.

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0002 (jOOQ generated code location) — but worth reopening because…_
