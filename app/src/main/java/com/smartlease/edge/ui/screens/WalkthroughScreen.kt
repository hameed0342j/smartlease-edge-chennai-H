package com.smartlease.edge.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.smartlease.edge.acoustic.AcousticTapClassifier
import com.smartlease.edge.acoustic.TrainedTapClassifier
import com.smartlease.edge.camera.ArAlignmentTracker
import com.smartlease.edge.camera.CameraController
import com.smartlease.edge.data.AppDatabase
import com.smartlease.edge.data.FindingType
import com.smartlease.edge.data.InspectionEntity
import com.smartlease.edge.data.Severity
import com.smartlease.edge.ir.CommonAcIrProfiles
import com.smartlease.edge.ir.IrController
import com.smartlease.edge.ocr.OcrEngine
import com.smartlease.edge.report.InspectionReport
import com.smartlease.edge.report.ReportGenerator
import com.smartlease.edge.safety.SafetyGate
import com.smartlease.edge.vision.DefectSegmenter
import com.smartlease.edge.vision.DefectSegmenterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * The main demo screen. Walks through every subsystem in one flow: pose baseline, capture
 * plus OCR, capture plus vision segmenter, acoustic tap test, IR transmit, safety-gated
 * findings feed, then generate a timestamped local report with a SHA-256 of its findings.
 *
 * Nothing heavy runs on the main thread: the two models load in a LaunchedEffect, and every
 * inference, the tap recording and the PDF render are dispatched to Dispatchers.Default.
 */
@Composable
fun WalkthroughScreen(onReportGenerated: (InspectionReport) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Full UUID, not the first 8 hex characters. The session ID goes into the digest and
    // onto the report; 32 bits of client-generated identifier is not an identifier.
    val sessionId = remember { UUID.randomUUID().toString() }
    val cameraController = remember { CameraController(context, lifecycleOwner) }
    val arTracker = remember { ArAlignmentTracker(context) }
    val irController = remember { IrController(context) }
    val db = remember { AppDatabase.get(context) }

    // Loaded off the main thread by the LaunchedEffect below, not in remember { }.
    // DefectSegmenterFactory.create copies a 13.7 MB asset on first run and then parses a
    // TorchScript module; TrainedTapClassifier.create parses a 73 KB JSON carrying a
    // 10,280-float mel filterbank. Both used to happen during composition, which froze the
    // screen for seconds the first time anyone opened it.
    var visionSegmenter by remember { mutableStateOf<DefectSegmenter?>(null) }
    var trainedTapModel by remember { mutableStateOf<TrainedTapClassifier?>(null) }
    var modelsLoading by remember { mutableStateOf(true) }

    var alignmentState by remember { mutableStateOf<ArAlignmentTracker.AlignmentState?>(null) }
    var findingsLog by remember { mutableStateOf(listOf<String>()) }
    var lastSafetyVerdict by remember { mutableStateOf<SafetyGate.Verdict?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val segmenter = withContext(Dispatchers.Default) { DefectSegmenterFactory.create(context) }
        // null when no trained model ships -- classify() then keeps the heuristic
        val tap = withContext(Dispatchers.Default) { TrainedTapClassifier.create(context) }
        visionSegmenter = segmenter
        trainedTapModel = tap
        modelsLoading = false
    }

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(Unit) {
        arTracker.start { alignmentState = it }
        onDispose { arTracker.stop() }
    }

    // Suspends until the row is committed. It used to fire-and-forget into scope.launch
    // while "Generate Report" read the same table, so the last finding of a session could
    // be missing from the PDF -- and from the digest computed over it.
    suspend fun logFinding(type: FindingType, label: String) {
        val verdict = SafetyGate.evaluate(label)
        lastSafetyVerdict = verdict
        val severity = verdict.escalatedSeverity ?: Severity.INFO
        val suffix = if (verdict.reason != null) " -> " + verdict.reason else ""
        findingsLog = findingsLog + ("[" + type.name + "] " + label + suffix)

        try {
            db.inspectionDao().insert(
                InspectionEntity(
                    sessionId = sessionId,
                    timestampEpochMillis = System.currentTimeMillis(),
                    findingType = type,
                    label = label,
                    detailJson = "{}",
                    severity = severity
                )
            )
        } catch (e: Exception) {
            // The finding is already on screen; losing the row must not kill the session.
            findingsLog = findingsLog + "  (not saved: " + (e.message ?: e.javaClass.simpleName) + ")"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Walkthrough - session ${sessionId.take(8)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))

        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                modifier = Modifier.fillMaxWidth().height(240.dp),
                update = { previewView ->
                    scope.launch {
                        try {
                            cameraController.bindTo(previewView)
                        } catch (e: Exception) {
                            // Another app holding the camera would otherwise crash the
                            // walkthrough the moment this screen composes.
                            findingsLog = findingsLog + "Camera unavailable: " + (e.message ?: e.javaClass.simpleName)
                        }
                    }
                }
            )
        } else {
            Card(Modifier.fillMaxWidth().height(240.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Camera permission not granted")
                }
            }
        }

        if (modelsLoading) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(14.dp).width(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Loading models...", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        alignmentState?.let { s ->
            val statusText = if (s.deltaFromBaselineDeg == null)
                "Pitch " + s.pitchDeg.toInt() + ", Roll " + s.rollDeg.toInt() + " - no baseline captured yet"
            else
                "Delta from baseline: " + "%.1f".format(s.deltaFromBaselineDeg) + " deg - " + (if (s.isAligned) "ALIGNED" else "adjust angle")
            Text(statusText, fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { arTracker.captureBaseline() }) { Text("Set Baseline") }

            // Gated on the permission, not just on `busy`: without CAMERA the PreviewView
            // below is never composed, so bindTo() never runs and captureBitmap() throws
            // IllegalStateException into a coroutine with nothing to catch it.
            Button(enabled = !busy && !modelsLoading && hasCameraPermission, onClick = {
                scope.launch {
                    busy = true
                    try {
                        val segmenter = visionSegmenter ?: return@launch
                        val bitmap = cameraController.captureBitmap()
                        val text = try {
                            OcrEngine.readText(bitmap)
                        } catch (e: Exception) {
                            // OcrEngine resumes with the exception on ML Kit failure. A failed
                            // text read must not take the capture -- or the walkthrough -- down.
                            findingsLog = findingsLog + "OCR unavailable: " + (e.message ?: "unknown error")
                            ""
                        }
                        if (text.isNotBlank()) {
                            logFinding(FindingType.OCR_TEXT_READ, text.take(120))
                        }
                        // 640x640 forward pass plus an 8400-anchor decode. On the main thread
                        // this was hundreds of milliseconds of frozen UI per capture.
                        val defects = withContext(Dispatchers.Default) {
                            segmenter.segmentDefects(bitmap, frameWidthInches = 48f, frameHeightInches = 36f)
                        }
                        // Label reflects which segmenter actually ran: a heuristic result must
                        // never read like a model detection in the tenant-facing report.
                        val mode = if (segmenter.isTrainedModel) "YOLOv8n-Seg" else "heuristic"
                        defects.forEach { d ->
                            val areaStr = "%.2f".format(d.areaSqFtEstimate)
                            val confStr = "%.0f".format(d.confidence * 100f)
                            logFinding(
                                FindingType.VISUAL_DEFECT,
                                d.label + ", ~" + areaStr + " sq ft (" + mode + ", " + confStr + "% confidence)"
                            )
                        }
                        if (defects.isEmpty() && text.isBlank()) {
                            findingsLog = findingsLog + "Capture: nothing flagged"
                        }
                    } catch (e: Exception) {
                        findingsLog = findingsLog + "Capture failed: " + (e.message ?: e.javaClass.simpleName)
                    } finally {
                        busy = false
                    }
                }
            }) {
                Text(
                    when {
                        !hasCameraPermission -> "Camera permission needed"
                        modelsLoading -> "Loading..."
                        else -> "Capture + Analyze"
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy && !modelsLoading && hasMicPermission, onClick = {
                scope.launch {
                    busy = true
                    try {
                        // 1.2 s of blocking AudioRecord reads plus a 4096-point FFT. This ran
                        // on the main thread and froze the UI for the whole recording.
                        val result = withContext(Dispatchers.Default) {
                            AcousticTapClassifier.recordAndClassifyOneTap(trained = trainedTapModel)
                        }
                        logFinding(
                            FindingType.ACOUSTIC_TAP,
                            result.verdict.toString() + " (" + result.confidenceNote + ")"
                        )
                    } catch (e: Exception) {
                        // AudioRecord construction throws if the mic is held by another app.
                        findingsLog = findingsLog + "Tap test failed: " + (e.message ?: e.javaClass.simpleName)
                    } finally {
                        busy = false
                    }
                }
            }) {
                Text(
                    when {
                        !hasMicPermission -> "Mic permission needed"
                        modelsLoading -> "Loading..."
                        else -> "Tap Test (1.2s)"
                    }
                )
            }

            Button(enabled = !busy, onClick = {
                scope.launch {
                    val profile = CommonAcIrProfiles.profiles.first()
                    // NEC-family header timings. The demo unit's real burst has to be captured
                    // on-site with an external receiver -- ConsumerIrManager cannot receive IR
                    // (see IrController) -- so the label below never claims more than was done:
                    // the emitter fired, and nothing confirmed the appliance responded.
                    val necHeaderBurst = intArrayOf(9000, 4500, 560, 560, 560, 1690)
                    val result = irController.transmit(profile.typicalCarrierHz, necHeaderBurst)
                    val label = when (result) {
                        is IrController.TransmitResult.Success ->
                            "IR command transmitted — " + profile.brand + ", " +
                                    (profile.typicalCarrierHz / 1000) + " kHz (pattern not verified against this unit)"
                        is IrController.TransmitResult.Failure -> "IR transmit failed: " + result.reason
                    }
                    logFinding(FindingType.IR_APPLIANCE_CHECK, label)
                }
            }) { Text(if (irController.hasIrBlaster) "Trigger AC (IR)" else "No IR blaster detected") }
        }

        lastSafetyVerdict?.let { v ->
            if (!v.allowAsIs) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFDECEC))) {
                    Text("WARNING: " + v.reason, Modifier.padding(10.dp), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Findings log", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            findingsLog.forEach { line -> Text("- " + line, fontSize = 11.sp) }
        }

        Button(
            enabled = findingsLog.isNotEmpty() && !busy,
            onClick = {
                scope.launch {
                    busy = true
                    try {
                        val findings = db.inspectionDao().findingsForSessionOnce(sessionId)
                        // Digest, layout and file write, all off the UI thread. Built once
                        // here and handed upwards -- MainActivity used to re-query and
                        // rebuild it, producing a second report object for the same session.
                        val report = withContext(Dispatchers.Default) {
                            val r = ReportGenerator.buildReport(sessionId, "Demo Property, Chennai", findings)
                            ReportGenerator.renderToPdf(context, r)
                            r
                        }
                        onReportGenerated(report)
                    } catch (e: Exception) {
                        findingsLog = findingsLog + "Report generation failed: " + (e.message ?: e.javaClass.simpleName)
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Generate Report") }
    }
}
