@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package pl.lublin.wsei.plan.ui.login

import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/**
 * Logowanie do Wirtualnego Dziekanatu WSEI.
 * Konto testowe jest widoczne tylko w buildach debug.
 */
@Composable
fun LoginScreen(
    isAuthenticating: Boolean,
    errorMessage: String?,
    savedLoginHint: String? = null,
    onLoginSubmit: (username: String, password: String, rememberCredentials: Boolean) -> Unit,
    onQuickDebugLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by rememberSaveable(savedLoginHint) { mutableStateOf(savedLoginHint.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberCredentials by rememberSaveable { mutableStateOf(true) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val shownError = validationError ?: errorMessage

    fun submit() {
        focusManager.clearFocus()
        if (username.isBlank() || password.isBlank()) {
            validationError = "Wpisz numer albumu i hasło."
        } else {
            onLoginSubmit(username.trim(), password, rememberCredentials)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WseiSpacing.xl, vertical = WseiSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            // Nagłówek: logo w kształcie cookie i ekspresyjny tytuł
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(M3XCookieShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WseiIcons.TaalVolcano,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)) {
                Text(
                    text = "Zaloguj się",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Użyj danych z Wirtualnego Dziekanatu WSEI.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = WseiHeroShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(WseiSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            validationError = null
                        },
                        label = { Text("Numer albumu lub e-mail") },
                        leadingIcon = { Icon(imageVector = WseiIcons.Person, contentDescription = null) },
                        singleLine = true,
                        enabled = !isAuthenticating,
                        isError = validationError != null && username.isBlank(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Hasło") },
                        leadingIcon = { Icon(imageVector = WseiIcons.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                                    contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isAuthenticating,
                        isError = shownError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = shownError != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = shownError.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = WseiSpacing.l, vertical = WseiSpacing.m)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !isAuthenticating, role = Role.Checkbox) {
                                rememberCredentials = !rememberCredentials
                            }
                            .padding(vertical = WseiSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
                    ) {
                        Checkbox(
                            checked = rememberCredentials,
                            onCheckedChange = null,
                            enabled = !isAuthenticating,
                            modifier = Modifier.padding(start = WseiSpacing.xs, end = WseiSpacing.s)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zapamiętaj mnie",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Dane są szyfrowane w Android Keystore.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { submit() },
                enabled = !isAuthenticating,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = WseiSpacing.xxl),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                AnimatedContent(
                    targetState = isAuthenticating,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "loginButtonContent"
                ) { authenticating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (authenticating) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(WseiSpacing.m))
                            Text(text = "Logowanie…", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text(text = "Zaloguj się", style = MaterialTheme.typography.titleMedium)
                            Icon(
                                imageVector = WseiIcons.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = WseiSpacing.s)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isDebuggable) {
                TextButton(
                    onClick = onQuickDebugLogin,
                    enabled = !isAuthenticating,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Konto testowe (debug)")
                }
            }
        }
    }
}
