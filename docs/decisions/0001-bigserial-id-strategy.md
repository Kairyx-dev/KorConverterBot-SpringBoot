# ADR-0001: BIGSERIAL ID Strategy

## Status
Accepted

## Context
The purist DDD playbook recommends UUIDv7 (RFC 9562) for Aggregate IDs. However, this project:
- Uses Discord Snowflake IDs (long-based) for UserId/ChannelId
- Has existing BIGSERIAL schema
- Operates as single BC + single DB where UUID's distributed benefits are minimal

## Decision
Keep DB BIGSERIAL for IgnoreUserId. IgnoreUserId.UNSAVED (0L) for pre-save state.

## Consequences
- Pro: Simpler schema, no UUID parsing overhead, natural index ordering
- Con: Deviates from playbook UUIDv7 recommendation
- Con: ID not assignable before persistence (UNSAVED sentinel needed)
