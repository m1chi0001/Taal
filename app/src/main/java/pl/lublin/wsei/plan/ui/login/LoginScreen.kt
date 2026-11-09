@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package pl.lublin.wsei.plan.ui.login

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape

/**
 * Material 3 Expressive Login Screen for WSEI Lublin Wirtualny Dziekanat (APR System Sp. z o.o.)
 * with hardware-backed AES-256-GCM Android Keystore credential encryption.
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
    var rememberCredentials by rememberSaveable { mutableStateOf(true) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Expressive Hero Brand Header
                Surface(
                    shape = ActiveHeroCardAsymmetricShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = M3XSquircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = WseiIcons.TaalVolcano,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "UNIWERSYTET WSEI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Taal • Wirtualny Dziekanat",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                            }
                        }

                        Text(
                            text = "Zaloguj się swoimi poświadczeniami z systemu Wirtualny Dziekanat (APR System). Dane logowania są chronione szyfrowaniem sprzętowym AES-256-GCM w Android Keystore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = WseiIcons.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Szyfrowanie AES-256-GCM • MS-API-KEY aktywny",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. Main Credentials Form Surface
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Logowanie studenta",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                validationError = null
                            },
                            label = { Text("Login, nr albumu lub e-mail") },
                            supportingText = {
                                Text("Wprowadź swój numer albumu lub login z WD")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = WseiIcons.Person,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            enabled = !isAuthenticating,
                            isError = validationError != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                validationError = null
                            },
                            label = { Text("Hasło do Wirtualnego Dziekanatu") },
                            supportingText = {
                                val err = validationError ?: errorMessage
                                if (err != null) {
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text("Hasło zgodne z kontem WD WSEI")
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = WseiIcons.Lock,
                                    contentDescription = null
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isAuthenticating,
                            isError = validationError != null || errorMessage != null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        onLoginSubmit(username, password, rememberCredentials)
                                    } else {
                                        validationError = "Wprowadź login (nr albumu) oraz hasło."
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Encrypted credential storage toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAuthenticating) {
                                    rememberCredentials = !rememberCredentials
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = rememberCredentials,
                                onCheckedChange = { rememberCredentials = it },
                                enabled = !isAuthenticating
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Zapamiętaj poświadczenia (Zaszyfrowane)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Klucz sprzętowy AES-256-GCM w Android Keystore",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (username.isBlank() || password.isBlank()) {
                                    validationError = "Wprowadź login (nr albumu) oraz hasło."
                                } else {
                                    onLoginSubmit(username, password, rememberCredentials)
                                }
                            },
                            enabled = !isAuthenticating,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isAuthenticating) {
                                LoadingIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Autoryzacja w systemie WD…",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = WseiIcons.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Zaloguj się do Wirtualnego Dziekanatu",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // 3. Distinct Built-in Debug Test Account Section
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = WseiIcons.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = "ODSEPAROWANY PROFIL TESTOWY (DEBUG)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Piotr Testowy • Album 99901 • Cyberbezpieczeństwo i AI (Semestr 5)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Text(
                            text = "Login: „debug@wsei.lublin.pl” (lub „99901”), hasło „debug123”. Profil posiada zupełnie inne przedmioty (Kryptografia, Głębokie sieci neuronowe, Pentesty, DevSecOps), grupę LAB-CSI-5A oraz własne dane testowe, w pełni odseparowane od Twojego prawdziwego konta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    username = "debug@wsei.lublin.pl"
                                    password = "debug123"
                                    validationError = null
                                },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Wypełnij pola",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            FilledTonalButton(
                                onClick = onQuickDebugLogin,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = WseiIcons.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wejdź (Profil Debug)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
