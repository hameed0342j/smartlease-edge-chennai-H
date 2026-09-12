### 🛠️ Solution Built So Far

#### 1. On-Device Vision Pipeline (YOLOv8n-Seg)
* **Trained Model:** Fine-tuned YOLOv8n-Seg model (`YOLOV8/best.pt`, exported to `app/src/main/assets/yolov8n_seg.ptl`) identifying 4 classes: crack, peeling, spalling, stain_mould. It runs on the **CPU** via PyTorch Lite. (`vision_best.ptl` was a separate 5-class *detection* export, incompatible with the segmentation decoder; it has been deleted from assets.)
* **Qualcomm Hexagon NPU Export (not in the APK):** an INT8 ExecuTorch bundle (`yolov8n_seg_htp.pte`, 3.7 MB) with `libQnnHtp.so` / `libQnnSystem.so` exists in `ml/YOLOV8/qnn_android_bundle/`. It was lowered for **SM8650 (Snapdragon 8 Gen 3)**, is missing the `libQnnHtpV##Skel.so` its own build script requires, and is not loaded by the app. No latency has been measured on any device.
* **Pinhole Metric Engine:** Projects active mask pixels to physical surface area ($\text{sq ft}$) using camera focal length ($f_x, f_y$) and distance ($Z$):
  $$\text{Area}_{\text{sq ft}} = \left(\frac{\text{Mask Pixels} \times Z^2}{f_x \times f_y}\right) \times 10.7639$$
* **Severity & Repair Estimation:** Translates defect dimensions into severity tiers (*Minor*, *Moderate*, *Severe*) and automated repair cost estimates based on Chennai market plaster/paint rates.
* **Safety Fallback:** Deterministic color/contrast heuristic segmenter ([`HeuristicDefectSegmenter`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/vision/DefectSegmenterFactory.kt)) if native neural delegates are unavailable.

#### 2. Acoustic Diagnostic Engine (Wall & Tile Tap Test)
* **Capture & FFT:** Real-time audio capture (`AudioRecord`) that isolates 150 ms knuckle-tap transients.
* **Classification:** Differentiates hollow from solid using decay time and the share of energy above 2 kHz, plus 13 MFCC means/stds and spectral moments.
* **Model that actually ships:** a 36-feature **logistic regression** (`acoustic_tap_model.json`, 73 KB), evaluated in pure Kotlin on the **CPU** — `AcousticModelBundle.hollowProbability()`. The mel filterbank and DCT basis travel in the same JSON so Kotlin cannot drift from the Python trainer, and a unit test enforces parity against golden vectors.
  - Trained on **15 taps from 8 recordings**. Grouped leave-one-recording-out accuracy **80% (95% CI 55–93%)** against a 53% majority baseline — the interval's lower bound is chance — reported in the UI with its interval, never the training score.
  - Fallback: amplitude/decay heuristic ([`AcousticTapClassifier.kt`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/acoustic/AcousticTapClassifier.kt)), used when no trained bundle loads.
  - Not shipped: `acoustic_cnn.ptl` (deleted — only the removed bench activity loaded it, and its audio path returned a hardcoded string) and `acoustic_tap_clf.joblib` (a Random Forest that lost to logistic regression in evaluation and was never loaded by Android).

#### 3. Appliance Functional Verification (IR Blaster)
* **Consumer IR Controller:** Native Android `ConsumerIrManager` wrapper ([`IrController.kt`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/ir/IrController.kt)) transmitting 38 kHz carrier bursts.
* **Functional vs. Visual:** Sends power/test pulses to appliances (AC/geysers) and verifies operational status rather than just recording external appearance.

#### 4. Android Native Application ([`smartlease-edge-chennai-master/app`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app))
* **UI & Camera:** Built with Jetpack Compose and CameraX ([`CameraController.kt`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/camera/CameraController.kt)).
* **Pose baseline:** `SensorManager` rotation-vector tracking ([`ArAlignmentTracker.kt`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/app/src/main/java/com/smartlease/edge/camera/ArAlignmentTracker.kt)) reporting pitch/roll drift from a captured pose, tolerance $4^\circ$. No overlay is rendered, no photo is stored, and the pose is held in memory for the session only.
* **Offline OCR:** Google ML Kit text recognition for electricity meters and appliance serial number plates.
* **100% Offline PDF:** Generates PDF inspection documents on-device (`PdfDocument`) with a session ID, a timestamp, and a **SHA-256 of exactly its findings printed on every page**. That digest detects alteration; it is **not** a signature and there is no signature capture — nothing binds a person to the findings.
* **Local Storage:** SQLite Room database, unencrypted, with cloud backup disabled at the application level.
* **Single Entry Point:** `MainActivity.kt` — the production walkthrough flow. (A second `com.iqoo.multimodal` activity existed but was unreachable from the launcher and ran inference on a blank bitmap; it has been deleted.)

#### 5. Interactive Python ML & Testing Hub ([`ml/app.py`](file:///c:/Users/shj08/Downloads/project-1/IQOO%20Hackathon/smartlease-edge-chennai-master/ml/app.py))
* Standalone Gradio desktop app allowing developers to test and demonstrate vision defect segmentation, audio tap classification, and report synthesis in a web browser without the phone.

---

### 📱 iQOO 15 Hardware Specifications & Mapping

The application is engineered to take advantage of the flagship **iQOO 15** hardware features:

| iQOO 15 Hardware Feature | Technical Specification | Exact Role in the Application |
| :--- | :--- | :--- |
| **Snapdragon 8 Elite (Gen 5) Hexagon NPU** | 3nm, HTP, INT4/INT8/FP16, 37% faster AI inference | **Not used.** All three run on the CPU: YOLOv8n-Seg via PyTorch Lite, a 36-feature logistic regression in Kotlin, and rule-based report text. The ExecuTorch INT8 `.pte` is compiled and in the repo but not in the APK, and no on-device latency has been measured. |
| **Stereo mics** | 44.1 kHz PCM via `AudioRecord` | Captures the tap transient on a button press. **Not** the Sensing Hub — its always-on audio path has no public third-party API. |
| **Consumer IR Blaster** | Integrated IR transmitter, 38 kHz carrier | Sends a remote code to an AC, geyser or TV. `ConsumerIrManager` is transmit-only, so the report records that the command was **sent**, not that the appliance responded. This is the one component a commodity phone lacks. |
| **Triple 50 MP Camera System** | Sony IMX921 main sensor with CIPA 4.5 Optical Image Stabilization (OIS) + 50 MP Ultrawide + Periscope Telephoto | • **OIS:** Locks AR ghost overlay stably on the wall.<br>• **Ultrawide:** Captures full wall dimensions in tight rental rooms.<br>• **Telephoto:** Inspects high-ceiling hairline cracks from 3+ meters away. |
| **2K LTPO AMOLED Display** | 6,000-nit local peak brightness | Guarantees that AR contours and damage overlays remain clearly visible in bright, sunlit rooms during midday inspections. |
| **7,000 mAh Battery + 8K Vapor Chamber** | Large-capacity battery with dual-drive liquid vapor chamber cooling | Enables continuous, sustained 30–40 minute property walkthroughs across 15–20 rooms with zero NPU thermal throttling. |
| **Dual Stereo Speakers** | 120% louder audio output | **Not used.** No confirmation tone is implemented; the verdict is printed in the findings log. |
| **Vivo Office Kit** | Low-latency wireless screen mirroring, remote control, and fast clipboard sharing | Projects live screen walkthroughs directly onto laptop/projector displays during evaluation rounds. |
| **Zero Network Architecture** | No `android.permission.INTERNET`; `allowBackup="false"` with every backup domain excluded | Records are not eligible for cloud backup and the app has no network path. The only egress is a user-initiated share sheet for one report PDF. |