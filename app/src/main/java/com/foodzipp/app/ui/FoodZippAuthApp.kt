package com.foodzipp.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foodzipp.app.R
import com.foodzipp.app.auth.LoginOption
import com.foodzipp.app.auth.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodZippAuthApp(
    onBack: () -> Unit,
    onCancel: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage
        if (!message.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FoodZipp Secure Login",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.onCancelAuthentication()
                        onCancel()
                    }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QrSection(
                qrUrl = state.qrUrl,
                onCopyUrl = { copyToClipboard(context, "FoodZipp Login", state.qrUrl) },
                dynamicEnabled = state.dynamicQr,
                onDynamicToggle = { viewModel.toggleDynamicQr() },
                qrBitmapAvailable = state.qrBitmap != null,
                state = state
            )

            LoginOptionSelector(
                selectedOption = state.selectedOption,
                onOptionSelected = viewModel::onSelectOption
            )

            LoginInputSection(
                state = state,
                onInputChanged = viewModel::onInputValueChange,
                onOtpChanged = viewModel::onOtpValueChange,
                onResendOtp = { viewModel.authenticate() }
            )

            TermsSection(
                termsAccepted = state.termsAccepted,
                onTermsChanged = viewModel::onTermsAcceptedChange
            )

            ActionButtons(
                state = state,
                onAuthenticate = viewModel::authenticate,
                onCancel = {
                    viewModel.onCancelAuthentication()
                    onCancel()
                }
            )

            ConfirmationMessage(state.loginComplete)

            AnalyticsSection(events = state.analyticsEvents)
        }
    }
}

@Composable
private fun QrSection(
    qrUrl: String,
    onCopyUrl: () -> Unit,
    dynamicEnabled: Boolean,
    onDynamicToggle: () -> Unit,
    qrBitmapAvailable: Boolean,
    state: com.foodzipp.app.auth.LoginUiState
) {
    val qrBitmap = state.qrBitmap
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan & Login",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (qrBitmapAvailable && qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "FoodZipp QR code",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Generating QR...")
                }
            }

            Text(
                text = qrUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = dynamicEnabled,
                        onCheckedChange = { onDynamicToggle() },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text("Dynamic tracking")
                        Text(
                            "Generate unique sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                OutlinedButton(onClick = onCopyUrl) {
                    Text("Copy URL")
                }
            }
        }
    }
}

@Composable
private fun LoginOptionSelector(
    selectedOption: LoginOption,
    onOptionSelected: (LoginOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Choose a login method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoginOption.values().forEach { option ->
                AssistChip(
                    onClick = { onOptionSelected(option) },
                    label = { Text(option.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        labelColor = if (selectedOption == option) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        borderColor = if (selectedOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoginInputSection(
    state: com.foodzipp.app.auth.LoginUiState,
    onInputChanged: (String) -> Unit,
    onOtpChanged: (String) -> Unit,
    onResendOtp: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val keyboardType = when (state.selectedOption) {
            LoginOption.MOBILE -> KeyboardType.Phone
            LoginOption.EMAIL -> KeyboardType.Email
            else -> KeyboardType.Text
        }
        OutlinedTextField(
            value = state.inputValue,
            onValueChange = onInputChanged,
            label = { Text(text = state.selectedOption.displayName) },
            placeholder = { Text(text = state.selectedOption.placeholder) },
            isError = state.inputError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors()
        )
        if (state.inputError != null) {
            Text(
                text = state.inputError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (state.selectedOption == LoginOption.MOBILE || state.selectedOption == LoginOption.EMAIL) {
            if (state.otpSent) {
                Divider()
                Text("Enter the OTP sent to you", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.otpValue,
                    onValueChange = onOtpChanged,
                    label = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onResendOtp) {
                    Text("Resend code")
                }
            }
        }
    }
}

@Composable
private fun TermsSection(
    termsAccepted: Boolean,
    onTermsChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = termsAccepted,
            onCheckedChange = onTermsChanged,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text("I accept the terms of service and privacy policy")
            Text(
                "Read our policies",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ActionButtons(
    state: com.foodzipp.app.auth.LoginUiState,
    onAuthenticate: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        Button(
            onClick = onAuthenticate,
            enabled = state.canSubmit,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                when (state.selectedOption) {
                    LoginOption.MOBILE, LoginOption.EMAIL -> if (state.otpSent) "Verify" else "Send OTP"
                    LoginOption.INSTAGRAM -> "Login with Instagram"
                    LoginOption.FACEBOOK -> "Login with Facebook"
                }
            )
        }
    }
}

@Composable
private fun ConfirmationMessage(loginComplete: Boolean) {
    AnimatedVisibility(visible = loginComplete) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Welcome, you're logged in!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("You can now continue to your dashboard.")
            }
        }
    }
}

@Composable
private fun AnalyticsSection(events: List<Pair<String, Map<String, String>>>) {
    if (events.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent analytics events", fontWeight = FontWeight.SemiBold)
            events.takeLast(5).reversed().forEach { (event, attributes) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp)
                ) {
                    Text(event, fontWeight = FontWeight.SemiBold)
                    attributes.forEach { (key, value) ->
                        Text("$key: $value", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, value)
    clipboard.setPrimaryClip(clip)
}
