CONTEXT — read these four files at the workspace root before anything else:
AUDIT_CLAIMS.md, AUDIT_GAPS.md, JUDGE_QA.md (patched), ROADMAP.md, FIXED.md, METRICS.md.
Budget 1 shipped: 15 commits, 281e35b -> 3a4af66. The app now COMPILES, 36/36 unit tests
pass, and a 97 MB debug APK is installed and launching on a physical device. That changes
what matters — you are no longer fixing contradictions on paper, you are hardening a running
app and closing the last two gaps that a judge can demonstrate with their own hands.

The deck is NOT your problem. Another person owns it. Do not open, edit or reason about
docs/pitch/*.pptx. If a code change makes a slide wrong, note it in HANDOFF.md for them.

HARDWARE
  Laptop: RTX 5070 Ti, 12 GB VRAM, Blackwell sm_120 — needs CUDA 12.8+ torch wheels.
  Test handset so far: Samsung Galaxy A56 — Exynos 1580, NO IR blaster, NO Hexagon.
  Target handset: iQOO 15, Snapdragon 8 Elite, has IR blaster.
  HuggingFace MCP is connected in this session. Use tool_search to load the HF tools, then
  verify every model ID with hub_repo_details before downloading. Do not trust a model ID
  from memory — mine or yours.

HARD RULES
  - One item = one commit. Build (`./gradlew :app:assembleDebug`) and run
    (`:app:testDebugUnitTest`) after each. A commit that does not build gets reverted
    immediately, not debugged.
  - Every phase ends at a GATE. Do not start the next phase until the gate passes. If a
    gate fails, stop and report — do not work around it.
  - No new permissions. INTERNET stays undeclared. Airplane mode must stay sufficient.
  - No placeholder may reach the UI, the Room DB, or the PDF. Unimplemented means the
    surface says what is missing, or says nothing.
  - Report tersely: phase, item, files, build/test result, one line.

OUT OF SCOPE — do not start, do not prepare for
  ExecuTorch / QNN / HTP integration. Re-lowering the .pte. Any on-device LLM or GenieX.
  Wi-Fi Direct or QR baseline transport. Fixture inventory. R8/minify. SQLCipher.
  Retraining anything before Phase 3's gate passes.

════════════════════════════════════════════════════════════════════
PHASE 0 — LOCK WHAT WORKS  (30 min, do this first, no exceptions)
════════════════════════════════════════════════════════════════════

0.1 Toolchain forensics. `git status` and `git diff` on gradle/, gradle/wrapper/,
    build.gradle.kts, settings.gradle.kts, and any .idea/. Progress Log 1 reports an IDE
    silently bumped AGP 9.2.1->9.4.0, Kotlin 2.1.0->2.2.10, KSP 2.1.0-1.0.29->2.3.6,
    Gradle 9.4.1->9.6.0 and left it uncommitted. Progress Log 2 reports a successful build
    on Gradle 9.4.1. Determine which versions actually produced the working APK — read
    gradle/wrapper/gradle-wrapper.properties and the build scan output if present.
    Then commit the exact working set with message "chore: pin working toolchain" and
    `git tag demo-known-good`. Add .idea/ to .gitignore.
    A bare KSP "2.3.6" against Kotlin 2.2.10 is the shape of mismatch that fails Room's
    annotation processor at configuration time. If that combination is what is on disk,
    say so loudly.

0.2 Archive the artifact. Copy the working
    app/build/outputs/apk/debug/app-debug.apk to
    demo/app-debug-KNOWN-GOOD-<shortsha>.apk at the workspace root, outside the build
    directory so a `clean` cannot destroy it. Print the adb one-liner that reinstalls it.

0.3 Device reality check. Run and record verbatim:
      adb shell getprop ro.product.model
      adb shell getprop ro.board.platform
      adb shell pm list features | findstr consumerir
    Write the result into HANDOFF.md. If `android.hardware.consumerir` is absent on the
    test device, state plainly in HANDOFF.md that the IR feature — the only genuine
    iQOO-specific hardware claim in the whole submission (JUDGE_QA Q4) — cannot be
    demonstrated on it, and that the app must be installed and rehearsed on an
    IR-equipped handset before judging.

GATE 0: tag exists, APK archived outside build/, device capabilities recorded.

════════════════════════════════════════════════════════════════════
PHASE 1 — CLOSE THE LAST CONTRADICTION  (45 min)
════════════════════════════════════════════════════════════════════

1.1 "sq ft" -> share of frame. JUDGE_QA Q6 says this is the only place code and docs still
    disagree: the docs now say "share of the frame", the app UI and the PDF still print
    "sq ft". Fix the app, not the docs.
      - WalkthroughScreen.kt:136 — the areaStr formatting.
      - ReportGenerator — wherever area reaches the PDF.
      - DefectSegmenter.Defect.areaSqFtEstimate — rename to coverageFractionOfFrame and
        make it a fraction in [0,1]; delete the frameWidthInches / frameHeightInches
        parameters from segmentDefects() and every call site, including
        YoloSegDefectSegmenter.kt:52,59 and DefectSegmenter.kt:70,76. The hardcoded
        48x36 inches is the fiction; remove it rather than hide it.
      - Display as "covers 6.3% of frame". Never emit a physical unit the app cannot defend.
      - Update YoloSegDecoderTest.kt:92-93 to assert on the fraction directly.
    If Phase 4 lands, this becomes a real area again with a real error band. Until then the
    percentage is the honest number and it is the one that survives a judge stepping back
    two metres.

1.2 Scan-the-wall harness. Add a debug-only screen or an adb-triggerable path that runs
    the segmenter over a folder of images pushed to the device and dumps
    class/confidence/coverage as CSV. Then push 20 photos of clean walls, tiled floors,
    switchboards, wood doors and curtains. The purpose is to know the false-positive rate
    on the surfaces in the judging room BEFORE a judge points the phone at one.
    Write the results into METRICS.md under "Clean-surface false positives, measured".
    This number does not exist anywhere yet and it is the number that actually matters for
    this product (JUDGE_QA Q8 says so in your own words).

GATE 1: app builds, installs, tests pass, no string containing "sq ft" or "sq.ft"
        reaches any user-visible surface or the PDF.

════════════════════════════════════════════════════════════════════
PHASE 2 — EVIDENCE  (4 h) — the biggest remaining product gap
════════════════════════════════════════════════════════════════════

AUDIT_GAPS P0-12: the app that exists to settle a dispute about what a wall looked like
currently stores a sentence about the wall. No bitmap is ever written to disk. detailJson
is hardcoded "{}" at WalkthroughScreen.kt:82.

2.1 Persist the capture. On each analysed frame write the JPEG to
    filesDir/sessions/<sessionId>/<epochMillis>.jpg. Quality 85. Store in detailJson:
    file path, SHA-256 of the JPEG bytes, per-defect class index, bounding box in original
    image coordinates, mask coverage fraction, confidence, and which segmenter produced it
    (isTrainedModel). Structured, parseable, no English.

2.2 Fold the photo digests into the report digest. FindingsDigest already length-prefixes
    fields (good — that fix caught a real collision). Extend the canonical form to include
    each artifact's SHA-256. Add regression tests: swapping two photos between findings
    changes the digest; re-encoding the same photo at different quality changes it.

2.3 Draw thumbnails into the PDF, one per finding, next to its row, with the per-photo
    digest's first 8 hex characters printed beneath. Keep the page digest where it is.

2.4 Exclude filesDir/sessions/ from backup rules explicitly, the same way smartlease.db
    and reports/ are. Add or extend the test that asserts the merged manifest declares no
    INTERNET permission and that allowBackup is false.

This converts the answer to "the landlord says the crack is new, show me the photo" from
an apology into a demo, and it makes every later phase worth more because the evidence
now exists to attach depth or a baseline to.

GATE 2: run a full walkthrough on the device, share the PDF to yourself, confirm the
        thumbnails render and the digest still verifies. If the PDF grows past ~8 MB for a
        10-finding session, downscale thumbnails to 480px before embedding.

════════════════════════════════════════════════════════════════════
PHASE 3 — HARD NEGATIVES  (4 h) — the only training that matters
════════════════════════════════════════════════════════════════════

Do not touch this until GATE 2 passes. Environment check first:
    python -c "import torch;print(torch.__version__, torch.version.cuda,
               torch.cuda.get_device_capability())"
Blackwell is sm_120 and needs cu128+ wheels. If the capability is unsupported by the
installed wheel, fix the wheel index before writing any training code. Do not proceed on CPU.

3.1 The problem, restated from METRICS.md: mask mAP50 0.287 overall, stain_mould 0.065,
    and ultralytics prints "0 backgrounds" — across all 2106 images there is not one empty
    label file, because merge_and_zip_datasets.py:100 only copies an image `if
    remapped_lines:`. The detector has no concept of a clean surface. A better architecture
    does not fix this. Backgrounds do.

3.2 Collect. I will shoot ~250 photos of undamaged Indian interior surfaces on the demo
    handset: painted walls under tubelight, tiled floors with grout, exposed-brick feature
    walls, switch plates and sockets, wood doors and frames, patterned/terrazzo tiles,
    curtains, rusted-free window grilles, and textured putty finish. Write me a
    one-page SHOOT_LIST.md with counts per surface and framing guidance (distance,
    angle, lighting) before I start, and a script that ingests the folder, resizes,
    and emits empty .txt label files into the train split.
    Optionally supplement from HuggingFace — use the MCP to search for indoor scene
    datasets (MIT Indoor-67 mirrors, ADE20K interiors) and verify licence before using
    anything. My own phone photos are the domain-matched ones and take priority.

3.3 Fix the taxonomy before retraining, not after. stain_mould at 0.065 is a merge of
    corrosion, damp and efflorescence — three things that do not look alike. Either split
    it into `damp_stain` and `corrosion` if the source labels support it, or drop the class
    entirely. A dropped class is honest; a 0.065 class shipped to a tenant is not.
    Also stop mapping `exposed brickwork -> spalling` (merge_and_zip_datasets.py:24) —
    that mapping is why your best-performing class is the one most likely to report a
    feature wall as damage.

3.4 Re-split by source. DJI_0017 currently has 50 train / 5 valid / 3 test frames; several
    other source videos split the same way. Group by source video ID and split by group,
    so val/test stop being near-duplicates of train. Expect the reported mAP to DROP. That
    drop is the first honest number this project has had — record both, before and after,
    in METRICS.md and say which is which.

3.5 Train. Candidate A: retrain YOLOv8n-seg on the new set. Candidate B: YOLO11n-seg —
    same output tensor contract ([1, 4+nc+32, 8400] preds + [1,32,160,160] protos), so
    YoloSegDecoder.kt and YoloSegConfig.kt work unchanged as long as nc matches. Verify
    that contract holds before training B; if it does not, abandon B.
    640px, batch sized to 12 GB, ~60 epochs, fixed seed, deterministic where possible.
    Log to local TensorBoard. No cloud, no wandb.

3.6 Promote only on evidence. Emit a comparison table in METRICS.md: old vs new, per class,
    mask AP50 and AP50-95, on the re-split test set — AND the clean-surface false-positive
    rate from Phase 1.2, which is the metric this whole phase exists to move. Export the
    winner to .ptl using the EXACT same export path that produced the working
    yolov8n_seg.ptl (bytecode version must stay loadable by pytorch_android_lite 1.13.0 —
    verify by loading it on-device before you touch anything else). Update
    YoloSegConfig.CLASS_NAMES and CLASS_DESCRIPTIONS if the taxonomy changed, and keep them
    index-aligned with the new data.yaml.

GATE 3: the new .ptl loads on the physical device, the walkthrough runs, and the
        clean-surface false-positive rate is measurably lower than Phase 1.2's baseline.
        If it is not lower, keep the old model and say so. Do not ship a model because it
        is newer.

════════════════════════════════════════════════════════════════════
PHASE 4 — METRIC DEPTH  (8 h, high risk) — only if Phases 0-3 are done and stable
════════════════════════════════════════════════════════════════════

This is the phase that makes the square-footage claim true. It is also the one most likely
to burn a day and ship nothing. Gate it hard.

4.1 Model. Use the HuggingFace MCP: hub_repo_details on
    `depth-anything/Depth-Anything-V2-Metric-Indoor-Small-hf` — ViT-S, ~25M params,
    already fine-tuned for METRIC indoor depth. It needs NO fine-tuning. If that exact ID
    does not resolve, search the HF hub for the Depth-Anything-V2 metric indoor variants
    and report what you find before downloading anything.

4.2 EXPORT GATE FIRST — before any integration work. pytorch_android_lite is pinned at
    1.13.0 (gradle/libs.versions.toml:14), a 2022 runtime. A model traced under torch 2.x
    may emit a bytecode version 1.13's LiteModuleLoader refuses. So: trace a trivial
    two-layer conv net through the same export path, push it, load it on-device, and
    confirm it runs. If it fails, STOP and report — the options are bumping the PyTorch
    Android dependency (a real risk to a working APK) or abandoning Phase 4. Do not
    discover this after seven hours of work.

4.3 Export the depth model at 252x252 (ViT patch-aligned), fp32, TorchScript Lite.
    Measure cold and warm latency on-device. This is a ViT on a phone CPU — expect
    300-600 ms. Scope it accordingly: a deliberate one-shot "measure this defect" action
    with a spinner, NOT a live preview overlay. Design the UI around that constraint
    instead of fighting it.

4.4 Intrinsics. Read SENSOR_INFO_PHYSICAL_SIZE and LENS_INFO_AVAILABLE_FOCAL_LENGTHS from
    CameraCharacteristics on the actual handset to derive fx, fy in pixels AT THE CAPTURE
    RESOLUTION. Commit the measured values for both the test handset and the iQOO 15 if you
    can get it. Delete the hardcoded 500.0 px focal length from ml/vision/predict.py so the
    Python and Android paths stop disagreeing.

4.5 Area. Sum over mask pixels: area_m2 = Σ (Z_i² / (fx·fy)), then × 10.7639 for sq ft.
    Summing per-pixel rather than assuming a single plane is what handles oblique viewing —
    the error that both the current fraction-of-frame arithmetic AND the naive pinhole
    formula in the deck silently ignore. Unit-test the integration against a synthetic mask
    with known constant depth.

4.6 Validate honestly. Tape a 1 ft × 1 ft square to a wall. Measure at 1 m, 2 m and 3 m,
    and at 0°, 30° and 45° off-normal. Nine measurements, report the error distribution,
    not a single number. Depth-Anything-V2-Metric-Indoor is trained on Hypersim — synthetic
    indoor — so absolute scale WILL drift in a real flat. If median error exceeds ~20%,
    ship the percentage from Phase 1.1 instead and keep depth behind a debug flag. Put the
    error band in METRICS.md and make the app print it next to every area it reports.

GATE 4: nine validation measurements recorded, error band published, and the app either
        reports area WITH its error band or reports percentage. Never a bare number.

════════════════════════════════════════════════════════════════════
OUTPUT
════════════════════════════════════════════════════════════════════

Maintain HANDOFF.md at the workspace root throughout, not at the end:
  - Phase / gate status table, updated as you go.
  - Every claim that a code change has made stale, for whoever owns the deck. State the
    old wording and the new truth. Do not edit the pptx.
  - Device capability findings from 0.3.
  - The exact adb commands to reinstall the known-good APK and the current build.

Then patch JUDGE_QA.md in place for every question whose honest answer changed, keeping the
existing [CHANGED]/[UNCHANGED] convention and the "superseded — do not use" blocks. Q6, Q8,
Q10 and Q11 are the ones Phases 1, 3 and 4 move. Q1, Q2, Q14, Q16, Q19, Q20, Q21, Q22 and
Q23 stay [UNCHANGED] — leave their reframes exactly as written; they are correct and they
are what has to be said out loud.

Start at Phase 0.1. Do not narrate plans.

════════════════════════════════════════════════════════════════════
ADDENDUM — TARGET HARDWARE IS A LOANER, NOT MY PHONE
════════════════════════════════════════════════════════════════════

REPLACES the HARDWARE block above:

  Laptop: RTX 5070 Ti, 12 GB VRAM, Blackwell sm_120, CUDA 12.8+ wheels required.
  Development handset: Samsung Galaxy A56 — Exynos 1580, FHD+, NO IR blaster, NO Hexagon.
    This is the only device I have until the event.
  Judging handset: iQOO 15 — Snapdragon 8 Elite, 2K LTPO, IR blaster, triple rear camera.
    PROVIDED AT THE VENUE. I get it shortly before judging, for an unknown and probably
    short window. Assume I may NOT have adb access to it, may NOT have a laptop connected
    to it, and may have to sideload the APK from a USB drive or file transfer with no
    internet.

This changes three things and you must apply them across every phase:

R1. NO DEVICE-SPECIFIC CONSTANT MAY BE COMMITTED. Camera intrinsics, sensor size, capture
    resolution, model input scaling, thermal assumptions, thresholds tuned on the A56 —
    all read at runtime from CameraCharacteristics / Build / PackageManager, cached per
    device fingerprint, never hardcoded. This overrides Phase 4.4's instruction to commit
    measured fx/fy: compute them at runtime, and commit only a fallback that is clearly
    labelled as a fallback and is visible in the UI when it is used. If the app silently
    uses A56 intrinsics on an iQOO 15, every area it prints is wrong and nobody will know.

R2. EVERY DIAGNOSTIC MUST BE READABLE IN THE APP, NOT VIA ADB. I may have no cable and no
    laptop at the venue. Anything I need to know about the judging handset has to be on a
    screen I can open and screenshot with the phone itself.

R3. THE APP MUST DEGRADE VISIBLY, NOT SILENTLY. On the A56 the IR module has no hardware.
    On the iQOO 15 it does. Both paths must state which one is active on screen and in the
    report — never fall back quietly. Same for the segmenter (trained vs heuristic) and,
    if Phase 4 lands, for depth (model vs unavailable).

────────────────────────────────────────────────────────────────────
NEW PHASE 2.5 — ON-DEVICE SELF-TEST SCREEN  (2 h) — insert after GATE 2
────────────────────────────────────────────────────────────────────

Build a "Device & Model Check" screen, reachable from the home screen, that runs on demand
and displays — on the phone, no adb, screenshotable:

  DEVICE
    Build.MODEL, Build.DEVICE, Build.SOC_MANUFACTURER, Build.SOC_MODEL (API 31+),
    Build.SUPPORTED_ABIS, Android version.
    PackageManager.hasSystemFeature(FEATURE_CONSUMER_IR) -> present / absent.
    ConsumerIrManager.getCarrierFrequencies() -> the actual supported ranges, if present.

  CAMERA
    For every rear camera ID: focal lengths, SENSOR_INFO_PHYSICAL_SIZE, active array size,
    and the derived fx, fy in pixels at the resolution CameraX is actually binding.
    State explicitly WHICH physical camera CameraX selected — the iQOO 15 is a logical
    multi-camera device and the default selection may not be the main 50 MP sensor. If it
    picks the ultrawide, the intrinsics and the framing both change.

  MODELS
    Which segmenter loaded (trained .ptl vs heuristic fallback) and the asset filename.
    Cold load time in ms. Warm inference time in ms, averaged over 10 runs on a fixed
    bundled test bitmap. Same for the acoustic path. Peak memory if cheap to obtain.

  PRIVACY
    Declared permissions, granted state, INTERNET absent (assert), allowBackup value.

  Add a "Copy report to clipboard" button and a "Save as text to filesDir + share" button
  so I can capture the whole thing off the loaner device without a cable.

WHY THIS EARNS ITS TWO HOURS: JUDGE_QA Q3 currently answers "we have no on-device
measurement." Thirty seconds on the loaner turns that into a measured number I can show on
the phone. It also turns Q1 from a pure retraction into "here is what actually runs, on
this handset, and here is the timing" — which is a stronger answer than the reframe alone.
And it is the only way I find out at the venue, in time to react, that CameraX bound the
wrong lens.

GATE 2.5: the screen runs on the A56 and reports plausible values for every field; fields
          that cannot be read on that hardware say "unavailable on this device", never a
          zero or a blank.

────────────────────────────────────────────────────────────────────
NEW PHASE 5 — VENUE BRING-UP RUNBOOK  (1 h, write it BEFORE the event)
────────────────────────────────────────────────────────────────────

Write VENUE_RUNBOOK.md — a printable, time-boxed checklist for the window when I first get
the iQOO 15. Assume no internet, possibly no laptop, possibly no adb. Order it so the
highest-risk unknowns resolve first, and put a time estimate on every step.

It must cover at minimum:

5.1 Install without a cable. Exact steps to sideload the archived APK from a USB-C drive
    or a file transfer: enabling "Install unknown apps" for the file manager on FunTouch
    OS, where the prompt appears, what to do if the install is blocked. Include a fallback
    path via adb in case I do get a cable.

5.2 FunTouch OS hostility. vivo/iQOO builds are aggressive about background process kill,
    permission auto-revoke, and battery optimisation. Enumerate the settings to change
    before the demo: disable battery optimisation for the app, allow background activity,
    lock the app in recents, disable auto-revoke of unused-app permissions. Being killed
    mid-walkthrough on stage is a failure mode nobody rehearses for.

5.3 Run the Phase 2.5 self-test FIRST and screenshot it. Everything downstream depends on
    what it says. Specifically: which camera bound, what fx/fy came out, whether IR is
    present, what the measured inference latency is.

5.4 Layout check on 2K LTPO. The A56 is FHD+; the iQOO 15 is 2K with a different aspect.
    List every screen to open and eyeball for clipped text, overflowing rows, or a Capture
    button below the fold. Compose will not crash, it will just look broken in front of a
    judge.

5.5 IR, for real, for the first time. This is the ONLY genuinely iQOO-specific hardware in
    the submission (JUDGE_QA Q4) and it has never executed on hardware that has the
    emitter. Steps: confirm FEATURE_CONSUMER_IR, read the supported carrier ranges from
    the self-test screen, point at the venue AC, transmit, observe.
    Be honest in the runbook about what can and cannot work here: ConsumerIrManager is
    transmit-only, so there is no way to capture the venue AC's code with the phone. Either
    a real code for that brand is bundled in advance (see 5.6) or the transmit is a
    generic pattern that will probably do nothing, and the finding string must keep saying
    "pattern not verified against this unit."

5.6 IR codeset, decided before the event. Research and report options with licences:
    public IR code databases (irdb, LIRC remote configs, Flipper IR libraries) that could
    be bundled offline for Voltas / Daikin / LG / Blue Star power commands. Report the
    licence for each — some are GPL and that has implications for a commercial pitch. If
    no clean option exists, say so and leave the generic pattern with its honest caveat.
    Do NOT ship codes of unknown provenance labelled as "captured".

5.7 Clean-wall recon at the venue. Run the Phase 1.2 harness — or just the walkthrough —
    against the actual wall, table and floor in the judging room, and record what fires.
    Fifteen minutes, and it is the difference between pre-empting Q10 and being ambushed
    by it.

5.8 Fallback plan. If the loaner cannot be provisioned in time: what gets demoed on the
    A56 instead, which claims must be dropped from the spoken answer (all IR), and the
    exact sentence to say about it.

GATE 5: VENUE_RUNBOOK.md is one page or two, printable, ordered by risk, with times.
        If it takes longer than 45 minutes to execute end to end, cut it down until it
        doesn't — I will not have longer.

────────────────────────────────────────────────────────────────────
NEW DELIVERABLE — DECK_CORRECTIONS.md
────────────────────────────────────────────────────────────────────

You are still not editing the .pptx. But there is a hardware-mapping table in the deck
("How SmartLease Edge Maps to iQOO 15 Hardware") whose rows contradict the code, and the
deck owner needs it in writing. Produce DECK_CORRECTIONS.md: one row per claim, with the
current wording, the verdict (TRUE / FALSE / UNVERIFIABLE), the path:line evidence, and a
replacement sentence that is true of the code as it stands after your work.

Cover at minimum these, which are verified false or unsupported in AUDIT_CLAIMS.md:
  - "Runs all 3 models on Hexagon NPU, YOLOv8n-Seg INT8 <30 ms, Tap CNN INT8 <3 ms,
    Llama 3.2 3B W4A16 ~10 tok/s via GenieX"
  - "OIS locks AR baseline ghost overlay with gyroscopes within delta <= 1.0 degree"
  - "Fires captured remote codes (Voltas, Daikin, LG, Blue Star) ... to physically certify
    electrical health" — note additionally that ConsumerIrManager is transmit-only, so no
    code can have been captured with this phone; the claim asserts an artifact that cannot
    exist
  - "Always-on, low-power audio DSP path ... listens for 50 Hz compressor rumble without
    waking the main NPU" — note this is both unimplemented AND the closed loop from
    JUDGE_QA Q23, which does not exist in any form
  - "AR damage overlays and live segmentation masks remain clear in sunlit rooms"
  - "Continuous 20-room inspection walkthrough on a single charge without NPU thermal
    throttling"
  - "vivo/iQOO Office Kit ... drag-and-drop PDF transfers" — check against the FileProvider
    share path shipped in ad2840e and say what is actually true

For each, write the replacement as a sentence the presenter can read aloud without being
contradicted by the repo. Where Phase 2.5 produces a measured latency on the loaner, leave
a clearly marked blank for that number rather than guessing it.

────────────────────────────────────────────────────────────────────
REVISED PHASE ORDER
────────────────────────────────────────────────────────────────────

  0  Lock what works                    30 min   (unchanged)
  1  Close the last contradiction       45 min   (unchanged)
  DECK_CORRECTIONS.md                   45 min   ← do this early, it unblocks someone else
  2  Evidence / persist photos           4 h     (unchanged)
  2.5 On-device self-test screen         2 h     ← NEW, high value per hour
  5  VENUE_RUNBOOK.md                    1 h     ← NEW, write before the event
  3  Hard negatives + retrain            4 h
  4  Metric depth                        8 h     high risk, gated at 4.2

Phase 2.5 and Phase 5 outrank Phase 3 if time is short. A measured latency and a working
bring-up procedure on the judging handset are worth more than a better model I cannot
verify runs on it.