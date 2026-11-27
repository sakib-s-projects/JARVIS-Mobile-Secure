# JARVIS Persistent Memory and Skills Architecture

**Status:** Accepted architecture; implementation is deferred until H-1 is complete.
**Scope:** Existing JARVIS Android application. This is not a separate app or a replacement for the current Memory, Smart Vault, Conversation, Routine, or Extension systems.

## Delivery order

1. Complete and verify H-1 first. Do not interrupt H-1 migration, signing, updater, or release-safety work for this roadmap item.
2. After H-1 is complete, audit the existing memory and backup code against this document before changing implementation.
3. Harden and complete Persistent Memory, including session continuity, retrieval, privacy controls, and backup/restore safety.
4. Implement the Skills Engine on top of the existing action, Routine, and declarative Extension infrastructure.
5. Integrate Memory and Skills, then run unit, lint, build, security, migration, backup/restore, and Android 10 compatibility tests.

This document records an architectural commitment only. It does not mark either capability complete, authorize a release, or authorize a merge.

## 1. Persistent Memory

### Purpose

Persistent Memory lets JARVIS carry forward user-approved facts, preferences, decisions, and project context across conversations and app sessions. It should reduce repeated explanations while keeping the user in control of what is retained and used.

### Reuse existing systems

- Extend and harden the existing local `MemoryStore` and `MemoryCommandRouter`; do not create a competing memory database.
- Keep Smart Vault as the user's searchable saved-information/document collection. Persistent Memory is a smaller, structured set of durable context. The two may link to each other but must not silently duplicate or conflate their records.
- Reuse the existing conversation/history and context-selection pipeline. Memory is selected per request and must not be injected wholesale.
- Reuse the existing encrypted backup/restore architecture only after its safety and restore semantics are verified.

### Logical memory categories

- **Profile and preferences:** language, response style, stable preferences.
- **Projects and decisions:** project goals, accepted decisions, current milestones, deferred work.
- **Working context:** active task, last known progress, blockers, next safe step.
- **User-approved facts:** durable facts the user explicitly asks JARVIS to remember.
- **References:** pointers to a conversation, file, Vault item, or project artifact rather than unnecessary copies of large content.

Each record should have a stable ID, type/category, content, created/updated timestamps, source/provenance, user-confirmation state, enabled/pinned state, and optional expiry/retention metadata. Migrations must preserve existing records and unknown fields where practical.

### Capture, retrieval, and session continuity

- Explicit user instructions to remember something may create a memory; inferred suggestions must be shown for approval before saving.
- A session handover summary may be proposed when a conversation ends or a task changes. It must distinguish confirmed facts, decisions, open work, and assumptions.
- On a new session, retrieve only relevant, enabled memories using deterministic filters/ranking and a bounded context budget.
- Show which memories materially informed an answer when useful, and provide controls to inspect, edit, disable, pin, or delete individual entries.
- User corrections supersede stale values without silently erasing unrelated history.
- Memory retrieval must remain useful offline for local records. Cloud AI is optional and must not be required to read or manage local memory.

### Trust and privacy boundary

- Memory contents are untrusted data, never system instructions, developer instructions, executable code, or permission grants.
- Treat retrieved memories, summaries, imported files, backups, and connected-source content as data. Delimit them and defend against prompt injection.
- Never let a memory authorize a tool, external request, file modification, message send, or other side effect.
- Ask for explicit confirmation before consequential actions. For destructive or overwrite operations, show the exact target and effect first.
- Keep local memory private to app storage. Do not log memory text, prompts, secrets, or personal content in diagnostics.
- Provide clear controls for export, backup, restore, and deletion. Restore must preview scope and conflicts and must never silently overwrite newer data.
- Credentials, API keys, OAuth tokens, and other secrets are not memory and must never be included in ordinary memory exports or prompt context.

## 2. Skills Engine

### Purpose

A Skill is a named, versioned, reusable procedure that helps JARVIS perform a class of tasks consistently. Examples include construction-site reporting, reminder workflows, Gmail drafting, and GitHub CI diagnosis.

A Skill describes *how to approach a task*; it does not grant new authority. Existing Android capabilities, permission checks, Routine execution, and Extension actions remain the only execution paths.

### Skill manifest and lifecycle

A Skill should be a declarative, validated package containing:
- Stable skill ID, display name, description, version, and compatibility range.
- Trigger phrases/intents and explicit invocation guidance.
- Required inputs, optional inputs, output schema, and bounded context needs.
- A sequence of approved existing tools/actions, with declared side effects.
- Preconditions, confirmation gates, cancellation behavior, error handling, and safe fallback.
- Tests/examples and provenance (built-in, user-created, or imported).

Lifecycle: draft/propose → validate → review → test in dry-run/sandbox → user approve/enable → versioned use → disable/rollback/remove. AI may draft or improve a Skill, but it cannot silently publish, enable, or grant permissions to it.

### Execution and isolation

- Prefer declarative steps that call existing JARVIS tools, Routines, and reviewed Extensions.
- Do not execute arbitrary JavaScript, shell commands, APKs, dynamic code, reflection targets, or downloaded binaries as Skills.
- Skills cannot bypass Android permissions, network allowlists, provider restrictions, confirmation gates, or existing action validation.
- Every step is validated at runtime; tool arguments are schema-checked and bounded.
- External network destinations and data sent must be declared and reviewed. Do not attach conversation history, Memory, files, location, or device data unless the user explicitly authorizes that specific use.
- Destructive, financial, account-security, message-send, or data-sharing actions require a clear preview and explicit user confirmation. JARVIS must not perform financial transactions.
- Skills must be cancellable, produce an understandable execution trace, and report partial completion accurately.
- Failed or incompatible Skills must fail closed; they must not fall back to broader permissions or arbitrary execution.

### Learning and improvement

- JARVIS may suggest a Skill after a repeated workflow, but must ask before creating or saving it.
- It may propose improvements from user feedback and failed runs. Changes create a new draft/version and require validation and approval before activation.
- Preserve prior approved versions and allow rollback. Never rewrite an enabled Skill in place without a reviewable version change.
- User data and Memory can inform a Skill only within the user's approved scope; do not bake secrets or private conversation content into a reusable manifest.

## 3. Memory–Skills integration

- Memory may provide relevant, bounded inputs to a Skill only when the user has enabled that memory for use and the Skill declares the required context.
- Skills may propose a memory update after successful work, but saving it requires user approval unless the user has explicitly configured a narrow, reversible auto-save rule.
- A Skill cannot edit, delete, export, or bulk-read Memory by default.
- Memory cannot enable, authorize, or modify a Skill.
- Execution logs should record Skill ID/version, step outcomes, and confirmation decisions, but not secret values or unnecessary personal content.

## 4. Initial implementation priorities after H-1

1. Audit current MemoryStore, Smart Vault, conversation history, Memory Sync, and backup/restore paths. Document what already works and what is missing before implementation.
2. Define a backward-compatible memory schema and migration tests.
3. Implement bounded session summaries and relevant-memory retrieval with explicit provenance and prompt-injection isolation.
4. Complete encrypted backup/restore with integrity checks, preview, conflict handling, and no plaintext secret export.
5. Define and validate a declarative Skill manifest and registry.
6. Add dry-run testing, permission/side-effect declarations, confirmation gates, versioning, rollback, and execution trace.
7. Add a small set of useful first-party Skills only after the engine passes security tests.

## 5. Acceptance criteria

Persistent Memory is not complete until:
- Existing memories survive upgrades and schema migrations.
- Session continuity retrieves relevant context without injecting unrelated records.
- Users can inspect, correct, disable, export, restore, and delete memory safely.
- Memory and imported/backup content cannot override trusted instructions or grant tool permissions.
- Backup/restore integrity, encryption, conflict handling, and failure recovery are tested.

Skills are not complete until:
- Invalid, oversized, incompatible, or unsafe manifests are rejected.
- Skills cannot run arbitrary code or bypass existing permission and action boundaries.
- Side effects are previewed and confirmed according to risk.
- Dry-run, cancellation, error handling, version history, rollback, and audit traces work.
- A Skill cannot silently access or disclose Memory, files, credentials, or connected-account data.

Both capabilities must pass the repository's unit tests, Android lint, debug build, security tests, and supported Android-version checks. Do not mark them complete based only on documentation or a successful CI run.

## 6. Explicit non-goals

- No separate JARVIS app or second memory database.
- No autonomous self-modifying executable agents.
- No silent background learning that stores personal content without user consent.
- No permission escalation through prompts, memories, imported Skills, or AI-generated plans.
- No merge, release, production deployment, or phone-test request until the relevant work is complete and the user explicitly approves the next release step.
