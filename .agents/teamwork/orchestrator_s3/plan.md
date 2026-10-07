# Plan — Sprint S3 Orchestration

## Scope
Deliver Sprint S3 of Stellar Odyssey:
1. **Front A (@antigravity)**:
   - Tarea A1: Mejorar `StarMapScreen.java` con fondo animado de galaxia procedural realista y rotación de planetas.
   - Tarea A2: Extender `ClientRocketFlightHandler` y `WarpTunnelRenderer` con un HUD de telemetría de vuelo (altitud Y, velocidad, aceleración G), shake de cámara e interacción visual al salir a la atmósfera/espacio.
2. **Front B (@opencode)**:
   - Tarea O1 & O2: Crear `SpaceSoundAttenuationHandler.java` para amortiguar sonidos en vacío y registrar en `ModSoundEvents` los rugidos de motor T1-T3 y reentrada atmosférica.
3. **Front C (@deepseek)**:
   - Tarea D1 & D2: Vincular Celidium, Astralite y Verdantite en las recetas de `AssemblyLogic.java` para Cohetes T1-T3 y crear suite de pruebas JUnit `RocketPassengerTeleportTest.java`.
4. **Integration & Deployment**:
   - Compilación limpia con `./gradlew :fabric:build` y ejecución del 100% de tests con `./gradlew test`.
   - Copiar JAR modificado a Prism Launcher: `C:\Users\amaro\Downloads\Prism Launcher\instances\Stellar Odyssey\minecraft\mods\`.
   - Actualizar notas en Obsidian Vault: `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes\STATUS.md`, `HANDOFFS/antigravity.md`, `INBOX.md`.

## Execution Steps
1. **Survey (Phase 0)**:
   - Dispatch 3 Explorers in parallel:
     - Explorer 1 (Front A): Examine `StarMapScreen.java`, `ClientRocketFlightHandler`, `WarpTunnelRenderer`, client HUD & rendering architecture.
     - Explorer 2 (Front B): Examine sound system, `ModSoundEvents`, audio event registrations, dimensions/vacuum status, sound attenuation mechanics.
     - Explorer 3 (Front C): Examine `AssemblyLogic.java`, materials (Celidium, Astralite, Verdantite), rocket item/entity tiers T1-T3, existing tests and test runner.
2. **Implementation (Phase 1)**:
   - Worker A (Front A): Implement StarMap enhancements & Telemetry HUD / Flight camera shake.
   - Worker B (Front B): Implement SpaceSoundAttenuationHandler & ModSoundEvents sound registration.
   - Worker C (Front C): Implement AssemblyLogic recipes & RocketPassengerTeleportTest suite.
3. **Verification & Audit (Phase 2)**:
   - Reviewers & Challengers to inspect side-safety (client vs common), syntax, logic, and test coverage.
   - Forensic Auditor (`teamwork_preview_auditor`) to verify zero cheating / genuine implementations.
4. **Build, Deployment & Documentation (Phase 3)**:
   - Build worker to execute `./gradlew test`, `./gradlew :fabric:build`, copy JAR to Prism Launcher, and update Obsidian Vault notes.
5. **Handoff (Phase 4)**:
   - Write handoff.md and send final report to Sentinel.
