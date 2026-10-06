# Task Assignment: Forensic Integrity Audit for Milestone M1 (Oxygen & Atmosphere)

## Context
You are auditor_oxygen_m1, a teamwork_preview_auditor subagent performing forensic integrity verification on Milestone M1 for the Stellar Odyssey mod.
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/auditor_oxygen_m1

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (read verbatim, section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md`.
- Implementation: files touched by worker_oxygen_m1.

## Forensic Integrity Checks
Audit the M1 implementation with zero tolerance:
1. Hardcoded / Dummy Logic Check: Verify that `AtmosphereHelper`, `LifeSupportManager`, `OxygenSealerBlockEntity`, and `OxygenRefillerBlock` contain genuine physical algorithms and not mock/stub/noop bypasses.
2. Circular Dependency & Side Safety Check: Ensure no imports of client-only classes in common and no circular package references.
3. Test Authenticity Check: Inspect `LifeSupportSystemTest.java`. Ensure tests genuinely exercise the production classes and do not assert tautologies (`assertTrue(true)`), mock away the core logic, or bypass the real code.
4. Run `./gradlew test` to verify execution results.

## Deliverables
- Write `handoff.md` with sections:
  - Observation
  - Logic Chain
  - Caveats
  - Conclusion with explicit binary verdict: **CLEAN** or **INTEGRITY VIOLATION**
  - Verification Method
- Notify parent orchestrator via `send_message`.

## 2026-10-06T18:14:41Z
You are auditor_oxygen_m1.
Your working directory is: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/auditor_oxygen_m1
Read your task instructions at: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/auditor_oxygen_m1/DISPATCH.md
MANDATORY: You MUST read the authoritative user request at c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md before starting work.
Also read c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md.
Perform forensic integrity verification of M1 implementation, check for facades/dummy logic/tautological tests, write handoff.md with CLEAN or INTEGRITY VIOLATION verdict, and notify me with send_message.
