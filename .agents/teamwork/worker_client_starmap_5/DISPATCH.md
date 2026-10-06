## 2026-10-06T04:27:56Z
You are Worker 5 (StarMap GUI & Emissive Client Engineer).
Your working directory: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_client_starmap_5
Project root: c:/Users/amaro/Documents/antigravity/blissful-lavoisier

MANDATORY: Read the original user request at:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md
Read the project master plan at:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/PROJECT.md
Read the surveyor reports at:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_survey_2/handoff.md

FILE OWNERSHIP (Exclusive):
- common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/**
- common/src/main/java/com/amaro/stellarodyssey/client/renderer/layer/EmissiveModelLayer.java
- common/src/main/java/com/amaro/stellarodyssey/client/renderer/StarshipEntityRenderer.java

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Tasks:
1. Implement `com.amaro.stellarodyssey.satellites.starmap.StarMapSatellite` implementing `SatelliteModule`.
   - Module id: "starmap", priority 0.
   - Guard client-only execution (`onClientSetup()`) so dedicated server runtimes never crash.
2. Implement `com.amaro.stellarodyssey.satellites.starmap.screen.StarMapScreen` (extends Screen):
   - Fullscreen celestial navigation UI querying `ICelestialCatalog` without direct worldgen/mob dependencies.
3. Implement `com.amaro.stellarodyssey.satellites.starmap.screen.StarMapCoordinatesWidget`:
   - Interactive UI widget displaying galactic sector coordinates and planetary hazard levels.
4. Implement `com.amaro.stellarodyssey.satellites.starmap.render.StarMapSkyRenderer`:
   - Orbital sector projector and star map grid rendering.
5. Implement `com.amaro.stellarodyssey.client.renderer.layer.EmissiveModelLayer`:
   - Emissive model layer rendering using MC 26.3 `SubmitNodeCollector` and full-bright coordinates (`0x00F000F0` / LightTexture.FULL_BRIGHT) without ambient attenuation.
6. Update `StarshipEntityRenderer` to invoke `EmissiveModelLayer.submitEmissive` with `starship_emissive.png`.
7. Run `.\gradlew.bat compileJava` to ensure compilation succeeds.
8. Write your handoff report to:
   c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_client_starmap_5/handoff.md
9. Send a message to orchestrator when finished.
