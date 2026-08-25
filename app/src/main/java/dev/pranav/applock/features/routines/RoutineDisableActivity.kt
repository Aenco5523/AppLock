package dev.pranav.applock.features.routines

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pranav.applock.R
import dev.pranav.applock.core.utils.SecurityUtils
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.data.repository.PreferencesRepository
import dev.pranav.applock.features.lockscreen.ui.PatternLockScreen
import dev.pranav.applock.services.AppLockManager
import dev.pranav.applock.ui.theme.AppLockTheme

/**
 * Entry point used by Android App Shortcuts / Samsung Modes and Routines to disable protection.
 *
 * This activity is exported so the system can launch the static shortcut. Because any external
 * application could explicitly invoke an exported activity, every disable request requires the
 * user's configured AppLock credential before the protection state can be changed or maintained.
 */
class RoutineDisableActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = applicationContext.appLockRepository()

        setContent {
            AppLockTheme {
                when (repository.getLockType()) {
                    PreferencesRepository.LOCK_TYPE_PATTERN -> {
                        RoutineDisablePatternScreen(
                            onCancel = ::finishWithoutAnimation,
                            onPatternAttempt = repository::validatePattern,
                            onVerified = ::disableProtection
                        )
                    }

                    PreferencesRepository.LOCK_TYPE_PASSWORD -> {
                        RoutineDisableCredentialScreen(
                            numeric = false,
                            onCancel = ::finishWithoutAnimation,
                            validate = repository::validatePassword,
                            onVerified = ::disableProtection
                        )
                    }

                    else -> {
                        RoutineDisableCredentialScreen(
                            numeric = true,
                            onCancel = ::finishWithoutAnimation,
                            validate = repository::validatePassword,
                            onVerified = ::disableProtection
                        )
                    }
                }
            }
        }
    }

    private fun disableProtection() {
        applicationContext.appLockRepository().setProtectEnabled(false)

        AppLockManager.clearTemporarilyUnlockedApp()
        AppLockManager.appUnlockTimes.clear()
        AppLockManager.isLockScreenShown.set(false)

        Toast.makeText(this, R.string.routine_disable_success, Toast.LENGTH_SHORT).show()
        finishWithoutAnimation()
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }
}

@Composable
private fun RoutineDisableCredentialScreen(
    numeric: Boolean,
    onCancel: () -> Unit,
    validate: (String) -> Boolean,
    onVerified: () -> Unit
) {
    var credential by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.routine_disable_auth_prompt),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = credential,
                onValueChange = { input ->
                    credential = if (numeric) {
                        input.filter(Char::isDigit).take(64)
                    } else {
                        SecurityUtils.sanitizePassword(input)
                    }
                    showError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        stringResource(
                            if (numeric) R.string.routine_disable_pin_label
                            else R.string.password_label
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password
                ),
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                isError = showError,
                singleLine = true
            )

            if (showError) {
                Text(
                    text = stringResource(
                        if (numeric) R.string.incorrect_pin_try_again
                        else R.string.incorrect_password_try_again
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel_button))
                }

                Button(
                    onClick = {
                        if (validate(credential)) {
                            onVerified()
                        } else {
                            showError = true
                            credential = ""
                        }
                    },
                    enabled = credential.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.verify_button))
                }
            }
        }
    }
}

@Composable
private fun RoutineDisablePatternScreen(
    onCancel: () -> Unit,
    onPatternAttempt: (String) -> Boolean,
    onVerified: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.routine_disable_auth_prompt),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            PatternLockScreen(
                modifier = Modifier.weight(1f),
                fromMainActivity = false,
                lockedAppName = null,
                triggeringPackageName = null,
                onPatternAttempt = { pattern ->
                    val valid = onPatternAttempt(pattern)
                    if (valid) onVerified()
                    valid
                }
            )

            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    }
}
