# Project Instructions

This project uses `.claude/` for AI agent configuration.

- Project context: [.claude/CLAUDE.md](.claude/CLAUDE.md)
- Coding rules: `.claude/rules/*.md`
- Decision records: `docs/decisions/*.md`

If your tool supports reading these paths, follow them.
Otherwise, the essential constraints are:

- Domain module must have zero external dependencies (pure Java only)
- Application module depends only on domain
- All business logic must be inside domain entities, not in services
- 1 transaction = 1 aggregate, no exceptions
- VO must be `record` with compact constructor self-validation
- Domain Event must be `sealed interface` with 5 mandatory fields
- Input Port interface required for every UseCase — no CRUD bypass
- Output Port 3-way split: Load/Save/Query
- No `@Transactional` in Application — TX via Configuration proxy only
