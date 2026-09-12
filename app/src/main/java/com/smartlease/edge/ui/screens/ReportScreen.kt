package com.smartlease.edge.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.smartlease.edge.report.FindingsDigest
import com.smartlease.edge.report.InspectionReport
import com.smartlease.edge.report.QrCode
import com.smartlease.edge.report.ReportGenerator

@Composable
fun ReportScreen(report: InspectionReport?, onBack: () -> Unit) {
    val context = LocalContext.current
    var shareError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("< Back") }
        Text("Inspection Report", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        if (report == null) {
            Spacer(Modifier.height(16.dp))
            Text("No report generated yet for this session.")
            return@Column
        }

        Spacer(Modifier.height(4.dp))
        Text(report.propertyLabel)
        Text("Session ${report.sessionId}", fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Text(report.overallVerdict, Modifier.padding(12.dp))
        }

        // Shown on screen as well as in the PDF footer so both parties can read the same
        // digest off the same phone and check it against the document afterwards.
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "SHA-256 of ${report.findingCount} finding(s)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))

                // Scannable by any phone's stock camera app: no install, no network. The
                // hex below it is the same string, so it can be checked either way.
                val qr = remember(report.findingsSha256) {
                    runCatching { QrCode.bitmap(report.findingsSha256, 512).asImageBitmap() }
                        .getOrNull()
                }
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "QR code of the findings digest",
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Scan to read this digest on another phone.", fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                }

                Text(FindingsDigest.grouped(report.findingsSha256), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(report.findingsSha256, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Change one character of one finding and this digest changes. It is not a " +
                            "signature — it does not identify who recorded them.",
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { shareError = shareReport(context, report) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Share report (PDF)") }

        shareError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, fontSize = 11.sp)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(report.sections) { section ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(section.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(section.body, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Hand the rendered PDF to whatever the user picks. Before this existed the report was
 * written to app-private storage with no provider and no intent, so the document the whole
 * product is about could not reach either party.
 *
 * The digest goes in the message body as well as inside the PDF, so a recipient can compare
 * the two without opening anything.
 *
 * @return null on success, or a message to show the user.
 */
private fun shareReport(
    context: android.content.Context,
    report: InspectionReport
): String? {
    val file = ReportGenerator.reportFile(context, report.sessionId)
    if (!file.exists()) {
        return "Report PDF not found — generate the report first."
    }
    return try {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".reports", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "SmartLease Edge report — session ${report.sessionId.take(8)}"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "SHA-256 of ${report.findingCount} finding(s): ${report.findingsSha256}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share report").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        null
    } catch (e: Exception) {
        "Could not share: ${e.message ?: e.javaClass.simpleName}"
    }
}
