# SmartLease Edge — Claude Code Brief

Three paste-ready prompts. Run them in order, in `C:\Users\shj08\Downloads\project-1\IQOO Hackathon`.
Prompt 0 is read-only. Do not skip it.

---

## PROMPT 0 — Adversarial repo audit (READ ONLY, write no code)

```
You are auditing a live hackathon submission the day before judging. Your job is to find
everything a hostile technical judge would find. Be unsparing. Do not encourage me.

WORKSPACE
  Root: C:\Users\shj08\Downloads\project-1\IQOO Hackathon
    problem.md, solution1.md, summary.md   <- the CLAIMS
    smartlease-edge-chennai-master\        <- the CODE (Android + Python ML)
    YOLOV8\                                <- training + QNN export, best.pt, dataset.zip
  Mirror: https://github.com/muhammedsayeedurrahman/smartlease-edge-chennai

EVENT CONTEXT
  iQOO Hackathon 2026, Chennai City Battle, "Smart Living" track.
  Mandate: phone-first, hardware-integrated, 100% on-device, no cloud APIs.
  Target device: iQOO 15 / Snapdragon 8 Elite (Hexagon HTP NPU, IR blaster, triple 50MP,
  Sensing Hub). Judged on hardware integration, on-device AI, and demo credibility.

HARD RULES FOR THIS PASS
  1. Write NO code, create NO files except the four reports listed below.
  2. Every finding needs a `path:line` citation. A finding you cannot cite is deleted.
  3. Treat problem.md / solution1.md / summary.md / docs/pitch/*.md as UNTRUSTED CLAIMS.
     Verify each against source. Never let a doc stand in as evidence for itself.
  4. Where doc and code disagree, the code wins and the doc is a CLAIM RISK.

STEP 1 — Build a claim ledger
  Extract every falsifiable technical assertion from the three root .md files, the README,
  and docs/pitch/PITCH_DECK_PROMPT.md (plus the .pptx if you can read it). For each, record:
  claim text | doc:line | status {TRUE / PARTIAL / FALSE / UNVERIFIABLE} | code evidence path:line.
  Pay specific attention to, and independently verify, each of these:
    - "runs on Hexagon NPU / HTP" — grep app/ for executorch, .pte, QnnHtp, NNAPI, Vulkan,
      any delegate at all. Check app/build.gradle.kts and gradle/libs.versions.toml for the
      actual inference dependency and its version. State exactly which silicon runs each model.
    - "<30 ms inference", "<3 ms tap CNN" — find the measurement. If none exists, say so.
    - "pinhole metric engine converts mask pixels to sq ft using fx, fy, Z" — find the
      on-device implementation. Report the exact arithmetic the Android app actually performs
      to produce areaSqFtEstimate, and where the depth Z and the focal lengths come from.
    - "tamper-evident signed PDF", "dual-party signature capture" — grep for MessageDigest,
      KeyStore, Signature, sha256, any signature-capture UI. Quote what ReportGenerator.kt
      says about itself.
    - "IR verifies appliance functional status" — find the transmitted pattern and the
      carrier frequency source. Identify the feedback channel that closes the loop. If there
      is none, state that the feature proves only that the phone's emitter fired.
    - "Qualcomm Sensing Hub continuously listens" — find the API call. Note whether that
      API is even reachable by a non-privileged third-party app.
    - "zero network architecture / data never leaves the handset" — read AndroidManifest.xml,
      res/xml/backup_rules.xml, res/xml/data_extraction_rules.xml. Enumerate every path the
      app writes evidence to (photos, audio clips, PDFs, DB) and state, per path, whether
      Android Auto Backup will exfiltrate it. Check allowBackup. Check every declared
      permission against the privacy claim.
    - "on-device narrative generation" — find the model. Quote the code if it is templated.

STEP 2 — Placeholder and dead-code sweep
  Grep the whole app module for: placeholder, stub, TODO, FIXME, "not implemented", dummy,
  hardcode, "Day \d+". For each hit report: what is fake, what the user sees, and whether it
  is reachable in the demo path. Flag separately any placeholder string that renders in the
  SHIPPING UI. Also list every class/package not reachable from the launcher activity.

STEP 3 — Evidence-integrity threat model
  Write the attack list against the report artifact, assuming a motivated tenant AND a
  motivated landlord, each with root-free access to their own phone:
  device clock rollback, photo substitution, re-generation of the PDF with edited findings,
  repudiation ("I never signed that"), selective omission of a room, EXIF forgery.
  For each: current defence in code (cite it), and the minimum change that would stop it.

STEP 4 — Data and model forensics
  - Count actual training/val/test samples for BOTH models. Inspect ml/data/audio/* and
    YOLOV8/unified_defects. Report raw file counts, not documented intentions.
  - Find any recorded metric: results.csv, mAP, confusion matrix, PR curve, val loss,
    INT8-vs-FP32 accuracy delta, on-device latency log. List what exists and what does not.
  - Check data.yaml and every script for absolute/non-portable paths and un-reproducible steps.
  - Report dataset provenance and licence for every source, and assess domain match to
    "Indian rental flat interior, phone camera, indoor lighting."
  - List the false-positive sources this class set will trip on in a real room (shadows,
    grout lines, wall art, cables, sockets, patterned tiles, wood grain) and whether any
    hard negatives exist in the data.

STEP 5 — Problem-statement coverage gap
  The stated problem is landlord/tenant security-deposit disputes settled by comparing
  MOVE-IN state to MOVE-OUT state, offline, with both parties present.
  Walk the code and answer concretely:
    - Where is the move-in baseline captured, stored, and retrieved?
    - How does the baseline get from the landlord's phone to the tenant's phone with no
      internet? Cite the transport, or state that none exists.
    - Where is the move-out vs move-in diff computed?
    - Where is "fair wear and tear" distinguished from "tenant damage"?
    - Where are missing or broken fixtures (fan, tubelight, tap, geyser, switch, curtain rod)
      inventoried against the baseline?
    - Where do both parties co-sign, and what binds a signature to the specific findings?
  Then list every capability the problem statement implies that the repo does not contain.

STEP 6 — Android build and shipping hygiene
  Release signing config, minify/shrink, APK size, duplicate or unused model assets in
  app/src/main/assets (state which are loaded and which are dead weight), inference library
  EOL status, number of launcher/exported activities, test coverage, crash paths when
  permissions are denied or hardware is absent, behaviour on a device with no IR blaster.

OUTPUT — exactly four files at the workspace root, nothing else
  AUDIT_CLAIMS.md   The claim ledger table from Step 1, worst first.
  AUDIT_GAPS.md     All findings, each as:
                      ID | Severity {P0 kills the demo, P1 loses points, P2 polish}
                      What is wrong (path:line)
                      The exact question a judge asks
                      The honest answer if unfixed
                      Minimal fix + realistic hours
                    Sorted by severity, then by hours ascending.
  JUDGE_QA.md       The 25 hardest questions, with the answer I should give TOMORROW given
                    the code as it stands. No spin. Where the honest answer is bad, say so
                    and give the one-line reframe that is still true.
  ROADMAP.md        A ranked fix list under three budgets: 8h, 24h, 72h. Each item states
                    what it buys in judging terms. Assume one developer. Be realistic;
                    if something cannot be done in the budget, exclude it and say why.

Finish with a 10-line verdict: the three things that will actually lose this hackathon,
and the single highest-leverage fix. Do not soften it.
```

---

## PROMPT 1 — Remediation (run after reading AUDIT_GAPS.md)

```
Implement the P0 items from AUDIT_GAPS.md, in ROADMAP.md's 24h order. Constraints:

  - No new permissions. INTERNET stays undeclared. Everything works in airplane mode.
  - Kotlin/Compose, existing package structure, no new architecture layer.
  - Every change ships with a unit test or an instrumented test that would fail before it.
  - No new placeholder may reach the UI. If something is not implemented, the UI must say
    what it is and the report must mark that section "not assessed", never a fake value.
  - Update problem.md / solution1.md / summary.md / the pitch notes in the SAME commit as
    the code, so no claim outruns the implementation. If you cannot implement a claim,
    delete the claim.

Work item by item. After each, run the build and the tests, and print a one-line diff summary.
Stop and ask me before any change that alters the demo flow's screen order.

Priority order unless the audit contradicts it:

  1. EVIDENCE CHAIN. Hash every artifact (photo bytes, audio clip, mask PNG, findings JSON)
     with SHA-256. Chain them into a Merkle root per inspection session. Sign the root with
     an EC P-256 key generated in Android Keystore with setUserAuthenticationRequired(false),
     setAttestationChallenge(sessionNonce), StrongBox when available. Embed root, per-artifact
     digests, public key and attestation chain in the PDF as a metadata block plus a QR code
     on the last page. Ship a standalone verifier (Python, no network) under tools/verify/
     that takes the PDF + evidence folder and prints PASS/FAIL per artifact. Record
     SystemClock.elapsedRealtimeNanos alongside wall-clock time and flag any session where
     the wall clock moved backwards relative to the monotonic clock. Be explicit in the
     report about what offline signing can and cannot prove — no RFC 3161 claim.

  2. BASELINE + OFFLINE TRANSFER. Add a move-in baseline session type. Export it as a single
     signed bundle. Transport with no internet, in this preference order: (a) rotating QR
     chunk stream displayed on one phone and scanned by the other for the manifest, with
     photos moved by a system share-sheet file copy; (b) Wi-Fi Direct. Implement (a) first;
     it always works and demos well. On move-out, load the baseline, run the diff, and render
     every finding as NEW / WORSENED / UNCHANGED / PRE-EXISTING against the baseline.

  3. AREA MEASUREMENT, HONESTLY. Replace fraction-of-frame arithmetic. Until a depth model
     lands (Prompt 2), require a scale reference in frame — an A4 sheet or a standard
     switch plate (75 x 125 mm Indian modular) — detect it, solve the homography to the
     wall plane, and report area with an explicit +/- error band. If no reference is
     detected, report pixel coverage and defect bounding-box dimensions only, and print
     "area not measurable" rather than a number. Never emit a sq-ft figure the app cannot
     defend. Calibrate fx, fy from CameraCharacteristics on the actual demo handset and
     commit the intrinsics; delete the hardcoded 500.0 px focal length.

  4. IR CLOSED LOOP. The IR feature must produce evidence of an appliance state CHANGE, not
     of a transmission. Sequence: capture 2 s of baseline room audio -> transmit -> capture
     4 s -> compare band energy for a compressor/fan onset -> simultaneously watch a user-
     framed indicator LED via CameraX for a luminance step. Report RESPONDED / NO RESPONSE /
     INCONCLUSIVE with the evidence attached. Ship a real bundled offline IR codeset for the
     AC brands common in Chennai rentals and cite its source and licence; if you cannot get
     real codes, implement a learn-from-remote capture flow instead and drop the brand list.

  5. WEAR-AND-TEAR CLASSIFIER (no ML). Take tenancy duration in months, surface type, and
     defect class; apply a documented depreciation table; output CHARGEABLE / PARTIALLY
     CHARGEABLE / NORMAL WEAR with the rule that fired printed next to it. Cite the rate and
     depreciation sources in the report with dates. This is the product, not a nicety.

  6. PRIVACY CLAIM MADE TRUE. allowBackup=false. Exclude every evidence path from backup and
     data-extraction rules, not just the database. Justify or drop ACCESS_FINE_LOCATION. Add
     an instrumented test that asserts no INTERNET permission in the merged manifest and that
     no dependency contributes one.

  7. SHIPPING HYGIENE. Remove the com.iqoo.multimodal bench package from release (debug
     variant only). Delete whichever model asset is not loaded. Add a release signing config.
     Enable minify with proguard rules for the inference library. Remove every placeholder
     string from user-visible UI.
```

---

## PROMPT 2 — Local training pipeline (RTX 5070 Ti)

```
Build a reproducible local training stack for SmartLease Edge on an RTX 5070 Ti laptop
(Blackwell, sm_120, ~12 GB usable VRAM). Windows host; use WSL2 if it simplifies ExecuTorch.

ENVIRONMENT — verify before writing training code
  Blackwell needs CUDA 12.8+ PyTorch wheels. Print torch.__version__, torch.version.cuda,
  torch.cuda.get_device_capability(), and run a real matmul on GPU. If capability is not
  supported by the installed wheel, fix the wheel index before proceeding. Pin everything in
  a lockfile. Everything below must run offline after a single setup step.

THESIS
  The bottleneck is data and measurement, not architecture. The GPU's highest-value job is
  auto-labelling and distillation, not longer YOLO runs. Build in this order.

  A. AUTO-LABEL FACTORY  (ml/pipeline/autolabel/)
     Grounding DINO (IDEA-Research/grounding-dino-base) for text-prompted boxes + SAM 2.1
     (facebook/sam2.1-hiera-large) for masks. Input: my own phone photos of Chennai rental
     interiors plus any licence-clean web images. Output: YOLO-seg and COCO-seg labels with a
     per-instance confidence, plus a human review queue sorted by uncertainty.
     Expand the class set from 4 to the classes the problem statement actually needs:
       surface: crack, peeling_paint, spalling, damp_seepage_stain, mould, tile_chip,
                hole_nail_mark, scuff_stain, rust, glass_crack
       fixtures (presence/absence/damage): ceiling_fan, tubelight, switchboard, socket,
                tap, geyser, door, window_grill, curtain_rod, sink
     Hold out a REAL test set of >=150 hand-labelled photos taken on the demo handset, and a
     clean-wall negative set of >=100 photos for false-positive rate. Never auto-label the
     test set. Report FP-per-clean-image as a first-class metric.

  B. DEPTH — this is the single highest-value model  (ml/pipeline/depth/)
     Per-pixel metric depth makes the sq-ft number defensible and removes the oblique-wall
     error that fraction-of-frame and naive pinhole both ignore.
     Start from depth-anything/Depth-Anything-V2-Small. Distil to a 256x256 student that
     lowers cleanly to QNN. Recover metric scale with a known-size reference (A4 / modular
     switch plate) or ARCore depth where available, and fit a single scale+shift per session.
     Then compute area by summing per-pixel Z_i^2 / (fx*fy) over the mask instead of assuming
     a fronto-parallel plane. Validate against tape measure on >=40 real surfaces across
     three viewing angles and publish the error distribution, not a single number.

  C. SEGMENTATION STUDENT  (ml/pipeline/seg/)
     Train two candidates on the expanded dataset and let the export gate decide:
       (i) YOLO11n-seg — drop-in, keeps the existing decoder contract.
       (ii) SegFormer-B0 (nvidia/mit-b0) — quantizes and lowers more predictably.
     Export-aware from the start: static shapes, no dynamic control flow, NHWC-friendly ops,
     avoid interpolate modes the QNN backend rejects. Per-channel symmetric INT8. If PTQ costs
     more than 3 mask-mAP points, run QAT.

  D. ACOUSTIC  (ml/pipeline/audio/)
     Current training set is 8 clips. Collect >=600 taps: >=5 surface types x >=3 rooms x
     >=2 handsets x >=2 strike implements, with a written protocol so it is reproducible.
     Teacher: MIT/ast-finetuned-audioset-10-10-0.4593 fine-tuned. Student: log-mel CNN under
     250 KB, distilled. Add a calibrated abstain class — "inconclusive" beats a confident
     wrong call in front of a judge. Add appliance-state classes (AC compressor on, fan,
     geyser) so the IR closed loop in Prompt 1 has a real classifier behind it.
     Report per-room cross-validation, never random split — random split leaks room acoustics.

  E. ON-DEVICE NARRATIVE  (ml/pipeline/llm/)
     Replace the rule-based narrative. LoRA fine-tune a sub-500M model (SmolVLM2-256M or
     gemma-3-270m-it) on pairs of {structured findings JSON -> neutral bilingual report
     paragraph}, English and Tamil. Ship via MediaPipe LLM Inference or llama.cpp. Constrain
     generation so no number appears in prose that is not present in the findings JSON, and
     add a test that asserts this. Only then may the deck claim on-device generative AI.

  F. EXPORT GATE  (ml/pipeline/gate/)
     No model enters the app without passing, automatically:
       torch.export -> QNN INT8 -> yolov8n_seg_htp-style .pte -> push to device ->
       measure cold + warm latency, sustained 10-minute thermal latency, and peak memory ->
       measure mAP / mIoU delta FP32 vs INT8 on the held-out real test set.
     Emit a markdown report per candidate and refuse to promote on regression. Wire the
     resulting .pte into the Android app with the ExecuTorch QNN runtime — right now nothing
     in app/ loads a .pte, so the NPU claim is unbacked until this gate closes the loop.

  G. ACTIVE-LEARNING LOOP  (ml/pipeline/loop.py)
     One command: train -> infer over the unlabelled pool -> rank frames by predictive
     entropy and mask instability under augmentation -> send the top 200 to the SAM2-assisted
     review queue -> retrain. Checkpoint and resume. Log to local TensorBoard; no cloud.
     This is what "endless local training" should mean, not more epochs on the same 4 classes.

DELIVERABLES
  ml/pipeline/ with a README that runs end to end from a clean clone, a single
  `make all` or `python -m ml.pipeline.run --stage <a..g>` entry point, a lockfile, a
  MODEL_CARD.md per shipped model (data, licence, metrics, known failure modes, INT8 delta,
  measured on-device latency), and a DATASET.md with provenance and licence per source.
  Every number in the pitch deck must trace to a file this pipeline produced.
```

---

## Sequencing advice

If judging is inside 72 hours, do **Prompt 0** and **Prompt 1** only. Prompt 2's items B and F
are the ones that change the score; A, D and G are worth more after the event than during it.

A defensible small model with a measured error bar beats an undefended better one. Right now
the project's problem is not model quality — it is that the headline numbers have no mechanism
behind them.
