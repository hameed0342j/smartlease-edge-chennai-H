Execute ROADMAP.md Budget 1, in order. The audit is done — AUDIT_CLAIMS.md, AUDIT_GAPS.md,
JUDGE_QA.md and ROADMAP.md are at the workspace root. Read all four before touching anything.
Judging is tomorrow, 2026-09-13. You have ~8 working hours.

GOVERNING RULE
  You are not adding capability. You are removing every contradiction a judge can find in
  under a minute, and fixing the three things that break during a live demo. A feature gained
  is worth less than a contradiction removed, because a contradiction discounts everything
  said after it. If an item threatens the build, skip it and say so.

HARD RULES
  - One item = one commit. Build and run the unit tests after each. If a commit doesn't
    build, revert it and move on; do not debug past 20 minutes on any single item.
  - No refactors, no reformatting, no dependency changes, no architecture layers, no new
    permissions. INTERNET stays undeclared. Blast radius stays minimal.
  - Touch only: app/, docs/pitch/, README.md, problem.md, solution1.md, summary.md, and
    YOLOV8/unified_defects/data.yaml. Do not touch ml/ otherwise.
  - Never introduce a new placeholder string into UI, into a DB label, or into the PDF. If
    something is unimplemented the surface must name what it is, or say nothing.
  - Documents change in the same commit as the code they describe. If you cannot implement
    a claim, delete the claim — do not soften it.
  - Report progress tersely: item number, files touched, build/test result, one line.

EXPLICITLY OUT OF SCOPE — do not start any of these, do not "prepare" for them
  ExecuTorch / QNN / HTP integration. Re-lowering the .pte. Any on-device LLM. Baseline
  capture, storage, diff, or phone-to-phone transport. Fixture inventory. Wear-and-tear
  rules. Real pinhole area from CameraCharacteristics. Model retraining. SQLCipher. R8.
  Hard-negative collection. If ROADMAP.md's exclusions table names it, it is out.

── TIER A — contradictions (2h15m total, do all of these first) ──

A1. allowBackup (5 min) — AUDIT_GAPS P0-1
    AndroidManifest.xml:23 → android:allowBackup="false". Empty <cloud-backup> in
    data_extraction_rules.xml. Verify: ReportGenerator.kt:94 already prints "no data left
    this phone" — after this commit that sentence is true. Leave it in.

A2. Deck edits (45 min) — P0-3, P0-4, P0-7, and Q9/Q10 in JUDGE_QA.md
    Use python-pptx on docs/pitch/SmartLease-Edge-iQOO-Hackathon-2026.pptx. Before editing,
    dump every slide's text so I can see exactly what you changed.
      - Slide 1, 14: fill [Team name], [Member 1-3], [Student / Professional bucket].
        Leave a visible TODO marker in your output listing what I must supply — do not
        invent names. Slide 15: [email] likewise.
      - Slide 12: move "GenieX Llama 3.2 3B narrative" and "SHA-256 fingerprint + dual
        signature" out of the ✓ column into a "Next" column. If A7 lands, move SHA-256 back.
      - Slide 6: 2106 images (1796/203/107), classes crack / peeling / spalling /
        stain_mould. Delete "580+" and the "hole" and separate "stain"/"mould" classes.
        Delete the "Z from ARCore depth (±10%)" line and the "confidence <0.30 hidden /
        dismissible" line — neither is implemented.
      - Slide 7: 15 taps from 8 recordings, not "100+ own recordings (50 hollow, 50 solid)".
        Keep the grouped-LOGO 80% with its 62–96% CI and the 53% majority baseline — that is
        the strongest honest number in the deck.
      - Slide 8: redraw the "HEXAGON NPU (HTP)" lane as a CPU lane. Add one line:
        "ExecuTorch INT8 lowering compiled and in repo (qnn_android_bundle/); HTP
        integration not yet in the APK."
      - Slides 1 and 4: flag the "Target UI · app build in progress (Days 6–10)" captions
        for me to replace with real screenshots. Do not fabricate screenshots.
      - PITCH_DECK_PROMPT.md:128: delete "Everything you saw ran on the Hexagon NPU."
        Replace with the honest line from JUDGE_QA.md Q1.
      - Slide 17: add one CC BY 4.0 credit line for the four Roboflow datasets (P2-1).

A3. Placeholder purge from shipping surfaces (35 min) — P0-2, P0-6
    - HomeScreen.kt:43 — delete the placeholder card. Replace with a live status line read
      from DefectSegmenter.isTrainedModel and the loaded bundle's trainedTaps.
    - WalkthroughScreen.kt:172-177 — the IR finding string currently writes "placeholder
      pattern, replace with Day 12 capture" into the DB and therefore into the tenant-facing
      PDF. Change to: "IR command transmitted — <brand>, 38 kHz (pattern not verified
      against this unit)". Keep the caveat; delete the word placeholder.
    - grep app/src/ for "Day \d", "placeholder", "Placeholder", "PLACEHOLDER", "TODO",
      "stub". Fix every hit that can reach a user-visible surface or the DB. Source comments
      may stay — they are the project's honesty and a judge reading them is a good outcome.

A4. Gate the Capture button (30 min) — P0-5
    WalkthroughScreen.kt:122 → enabled = !busy && hasCameraPermission. Wrap the capture body
    (:125-148) and the report body in try/catch(Exception) logging to findingsLog instead of
    throwing. Same for the OcrEngine.kt:26-28 path. Add a unit test where one exists to add;
    otherwise state plainly that this one is manual-verify only.
    Manually verify: fresh install → Deny camera → tap Capture → app survives.

A5. Delete dead code and dead assets (20 min) — P1-2, P1-9
    - Delete package com.iqoo.multimodal (6 files) and AndroidManifest.xml:44-48.
    - Delete app/src/main/assets/vision_best.ptl (12.47 MB) and acoustic_cnn.ptl.
    - Remove vision_best.ptl from CANDIDATE_ASSETS (YoloSegDefectSegmenter.kt:114).
    - Make YoloSegDefectSegmenter.create() return null when the loaded module does not
      return a (preds, protos) tuple, so the honest heuristic fallback engages instead of
      the current silent zero-detection mode. Add a test if the seam allows one.
    - Delete the "Dual Entry Points" claim from solution1.md:29-31 and summary.md:64-66.
    - Fix solution1.md:4 and summary.md:68 — vision_best.ptl is NOT the segmentation model.

A6. Delete unused permissions (10 min) — P1-4
    ACCESS_FINE_LOCATION and VIBRATE from AndroidManifest.xml:17,20, and the request at
    MainActivity.kt:37. Neither is used by any code path. Leave the latitude/longitude
    columns on InspectionEntity alone — schema churn is not worth it tonight.

── TIER B — the three demo-critical builds (4h30m) ──

A7. SHA-256 over canonical findings, in the PDF footer (1h30m) — P0-9
    Canonical serialisation of the findings list (stable field order, stable number
    formatting — write the canonicalisation as a pure function and unit-test it).
    MessageDigest.getInstance("SHA-256"). Print the hex digest on every PDF page and show it
    on ReportScreen before generation. Also fold the full session UUID into the digest input
    and stop truncating it at WalkthroughScreen.kt:47 (P1-15).
    Test that must exist and must fail before this commit: same findings → same digest;
    one character changed in one label → different digest.
    Then un-do that half of the slide-12 edit from A2: the ✓ becomes true.

A8. Share the PDF (1h30m) — P0-8
    FileProvider + res/xml/file_paths.xml + a "Share report" button on ReportScreen firing
    ACTION_SEND with FLAG_GRANT_READ_URI_PERMISSION. Today the PDF is written to filesDir
    with no share path, so neither party can receive the document the entire pitch is about.
    Manually verify the share sheet opens and the PDF renders in a viewer.

A9. Get inference off the main thread (1h30m) — P0-10
    - withContext(Dispatchers.Default) around WalkthroughScreen.kt:126 (640×640 forward pass
      + 8400-anchor decode) and :159 (1.2s blocking AudioRecord + FFT).
    - Move DefectSegmenterFactory.create and TrainedTapClassifier.create out of remember { }
      at :51,53 into a LaunchedEffect with an explicit loading state — first launch currently
      copies a 13.7 MB asset and loads a TorchScript module during composition.
    Manually verify: opening Walkthrough shows a spinner, not a freeze; Capture does not
    block the UI.

── TIER C — only if Tier A and B are done and building ──

C1. yolo segment val (30 min) — P1-7
    Fix YOLOV8/unified_defects/data.yaml:5 — it says path: D:\hackathon\... Make it relative.
    Then run, from the local workspace (not the repo mirror — best.pt is not committed):
      yolo segment val model=YOLOV8/best.pt data=YOLOV8/unified_defects/data.yaml split=test
    Write the numbers into a new METRICS.md with the leakage caveat stated up front: video
    frames from the same source appear across splits (DJI_0017: 50 train / 5 valid / 3 test),
    so this figure is inflated by near-duplicates. A number with its caveat beats silence.

C2. Report generation race (30 min) — P1-13
    Make logFinding suspend and await the insert. Pass the built report through instead of
    rebuilding it at MainActivity.kt:69.

C3. Kill or fix "View Past Reports" (5 min to delete) — P0-11
    Default to deleting the button. A missing feature costs nothing; a broken one a judge
    taps costs trust. Only implement the session-list query if everything above is done.

C4. Severity that means something (1h) — P1-14
    Severity.NOTABLE is declared and unreachable. Map defect class + area band → NOTABLE in
    logFinding. Emit a finding on "Set Baseline" so FindingType.AR_BASELINE_ALIGNMENT stops
    being dead. Add ~8 tests for SafetyGate.evaluate() while you are in there (P1-12) — it is
    pitched as the architectural novelty and has zero tests.

C5. README and root docs (1h30m) — P1-16
    README.md:103 still says "Current Status (Day 2 of 12)". Rewrite both status tables
    against what the code does after tonight. Correct solution1.md:44-45 (NPU, Sensing Hub),
    solution1.md:52 (zero-network), summary.md:115 ("Encrypted" — it is plain Room).

── OUTPUT ──

When you stop, write FIXED.md at the workspace root:
  - Table: AUDIT_GAPS ID | fixed / partial / skipped | commit | one-line verification command
    a judge could run (a grep, a file path, an adb command).
  - Then patch JUDGE_QA.md in place: for every question whose honest answer changed tonight,
    rewrite the answer to match the code as it now stands. Mark the ones that did NOT change,
    because those are the ones I have to say out loud tomorrow.
  - Final section: the list of items still open, ranked, with the sentence I should use if
    asked about each. No spin.

Do not narrate plans. Start at A1.