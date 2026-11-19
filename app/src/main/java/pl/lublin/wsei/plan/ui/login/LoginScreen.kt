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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
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
 *
 * Układ: na telefonie w pionie i na tabletach jedna wyśrodkowana kolumna (maks.
 * [WseiSpacing.formMaxWidth]); na niskich, szerokich oknach (telefon w poziomie) nagłówek stoi
 * obok formularza. Całość omija paski systemowe, wycięcie aparatu i klawiaturę.
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
    val scrollState = rememberScrollState()

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

    // Nagłówek: logo w kształcie cookie i ekspresyjny tytuł
    val header: @Composable ColumnScope.() -> Unit = {
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
    }

    val form: @Composable ColumnScope.() -> Unit = {
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
                    // Podpowiedź dla menedżerów haseł (autouzupełnianie loginu).
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password }
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

    // safeDrawing = paski systemowe + wycięcie aparatu + klawiatura: pola nie chowają się pod
    // klawiaturą, a w poziomie treść omija wycięcie aparatu.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Telefon w poziomie: nagłówek obok formularza zamiast nad nim.
        val sideBySide = maxWidth >= 600.dp && maxHeight < 480.dp
        if (sideBySide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = WseiSpacing.xxl),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxl, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .widthIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.l),
                    content = header
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = WseiSpacing.formMaxWidth)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(vertical = WseiSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.l),
                    content = form
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = WseiSpacing.formMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = WseiSpacing.xl, vertical = WseiSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
            ) {
                header()
                form()
            }
        }
    }
}
