package com.viora.app.presentation.linkmessage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.viora.app.presentation.components.PrimaryActionButton
import com.viora.app.presentation.components.SecondaryActionButton
import com.viora.app.presentation.components.VioraDarkBackground
import com.viora.app.presentation.components.VioraPrimaryCyan
import com.viora.app.presentation.components.VioraSpacing
import com.viora.app.presentation.scanner.ScannerViewModel

/**
 * The existing "text/link input path" (Phase-N UI bug fix): reuses
 * [ScannerViewModel.onSharedText] — the exact same VioraContext → GuardianAI →
 * Result pipeline the Android Share Sheet already drives — through an in-app
 * entry point, since Home's "Payment Link / Message" button previously had no
 * real destination and was incorrectly wired to the QR scanner instead.
 *
 * No new analysis logic lives here: this screen only collects text and hands
 * it to the existing ScannerViewModel method unchanged.
 */
@Composable
fun LinkMessageScreen(
    onBackClick: () -> Unit,
    onChecked: () -> Unit,
    viewModel: ScannerViewModel
) {
    var text by remember { mutableStateOf("") }
    var showEmptyError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VioraDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VioraSpacing.xl)
        ) {
            Spacer(modifier = Modifier.height(VioraSpacing.sm))

            Text(
                text = "LINK / MESSAGE",
                color = VioraPrimaryCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(VioraSpacing.xs))
            Text(
                text = "Paste a payment link or a suspicious message to check it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showEmptyError = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Paste text or link here…") },
                isError = showEmptyError
            )

            if (showEmptyError) {
                Spacer(modifier = Modifier.height(VioraSpacing.xs))
                Text(
                    text = "Enter some text or a link first.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(VioraSpacing.lg))

            PrimaryActionButton(
                text = "Check",
                onClick = {
                    val accepted = viewModel.onSharedText(text, sourceApplication = null)
                    if (accepted) onChecked() else showEmptyError = true
                }
            )

            Spacer(modifier = Modifier.height(VioraSpacing.md))

            SecondaryActionButton(text = "Back", onClick = onBackClick)
        }
    }
}
