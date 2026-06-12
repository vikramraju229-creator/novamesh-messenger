@file:OptIn(ExperimentalMaterial3Api::class)

package com.novamesh.ui.screens.auth

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.PhoneAuthProvider
import com.novamesh.ui.screens.auth.AuthState

/** WhatsApp-style OTP constants. */
object WhatsAppCommands {
    /** Regex for validating phone numbers: 7-15 digits. */
    const val PHONE_REGEX = "^\\+?[1-9]\\d{6,14}$"

    /** OTP code length. */
    const val OTP_LENGTH = 6

    /** OTP resend timer in seconds. */
    const val RESEND_TIMER = 60
}

/**
 * Country code data.
 */
private data class CountryCode(
    val name: String,
    val flag: String,
    val code: String,
    val dialCode: String,
)

private val countryCodes = listOf(
    CountryCode("Afghanistan", "\uD83C\uDDE6\uD83C\uDDEB", "AF", "+93"),
    CountryCode("Albania", "\uD83C\uDDE6\uD83C\uDDF1", "AL", "+355"),
    CountryCode("Algeria", "\uD83C\uDDE9\uD83C\uDDFF", "DZ", "+213"),
    CountryCode("Argentina", "\uD83C\uDDE6\uD83C\uDDF7", "AR", "+54"),
    CountryCode("Australia", "\uD83C\uDDE6\uD83C\uDDFA", "AU", "+61"),
    CountryCode("Austria", "\uD83C\uDDE6\uD83C\uDDF9", "AT", "+43"),
    CountryCode("Bangladesh", "\uD83C\uDDE7\uD83C\uDDE9", "BD", "+880"),
    CountryCode("Belgium", "\uD83C\uDDE7\uD83C\uDDEA", "BE", "+32"),
    CountryCode("Brazil", "\uD83C\uDDE7\uD83C\uDDF7", "BR", "+55"),
    CountryCode("Canada", "\uD83C\uDDE8\uD83C\uDDE6", "CA", "+1"),
    CountryCode("China", "\uD83C\uDDE8\uD83C\uDDF3", "CN", "+86"),
    CountryCode("Colombia", "\uD83C\uDDE8\uD83C\uDDF4", "CO", "+57"),
    CountryCode("Denmark", "\uD83C\uDDE9\uD83C\uDDF0", "DK", "+45"),
    CountryCode("Egypt", "\uD83C\uDDEA\uD83C\uDDEC", "EG", "+20"),
    CountryCode("Finland", "\uD83C\uDDEB\uD83C\uDDEE", "FI", "+358"),
    CountryCode("France", "\uD83C\uDDEB\uD83C\uDDF7", "FR", "+33"),
    CountryCode("Germany", "\uD83C\uDDE9\uD83C\uDDEA", "DE", "+49"),
    CountryCode("Greece", "\uD83C\uDDEC\uD83C\uDDF7", "GR", "+30"),
    CountryCode("Hong Kong", "\uD83C\uDDED\uD83C\uDDF0", "HK", "+852"),
    CountryCode("India", "\uD83C\uDDEE\uD83C\uDDF3", "IN", "+91"),
    CountryCode("Indonesia", "\uD83C\uDDEE\uD83C\uDDE9", "ID", "+62"),
    CountryCode("Ireland", "\uD83C\uDDEE\uD83C\uDDEA", "IE", "+353"),
    CountryCode("Israel", "\uD83C\uDDEE\uD83C\uDDF1", "IL", "+972"),
    CountryCode("Italy", "\uD83C\uDDEE\uD83C\uDDF9", "IT", "+39"),
    CountryCode("Japan", "\uD83C\uDDEF\uD83C\uDDF5", "JP", "+81"),
    CountryCode("Kenya", "\uD83C\uDDF0\uD83C\uDDEA", "KE", "+254"),
    CountryCode("Malaysia", "\uD83C\uDDF2\uD83C\uDDFE", "MY", "+60"),
    CountryCode("Mexico", "\uD83C\uDDF2\uD83C\uDDFD", "MX", "+52"),
    CountryCode("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", "NL", "+31"),
    CountryCode("New Zealand", "\uD83C\uDDF3\uD83C\uDDFF", "NZ", "+64"),
    CountryCode("Nigeria", "\uD83C\uDDF3\uD83C\uDDEC", "NG", "+234"),
    CountryCode("Norway", "\uD83C\uDDF3\uD83C\uDDF4", "NO", "+47"),
    CountryCode("Pakistan", "\uD83C\uDDF5\uD83C\uDDF0", "PK", "+92"),
    CountryCode("Philippines", "\uD83C\uDDF5\uD83C\uDDED", "PH", "+63"),
    CountryCode("Poland", "\uD83C\uDDF5\uD83C\uDDF1", "PL", "+48"),
    CountryCode("Portugal", "\uD83C\uDDF5\uD83C\uDDF9", "PT", "+351"),
    CountryCode("Russia", "\uD83C\uDDF7\uD83C\uDDFA", "RU", "+7"),
    CountryCode("Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6", "SA", "+966"),
    CountryCode("Singapore", "\uD83C\uDDF8\uD83C\uDDEC", "SG", "+65"),
    CountryCode("South Africa", "\uD83C\uDDFF\uD83C\uDDE6", "ZA", "+27"),
    CountryCode("South Korea", "\uD83C\uDDF0\uD83C\uDDF7", "KR", "+82"),
    CountryCode("Spain", "\uD83C\uDDEA\uD83C\uDDF8", "ES", "+34"),
    CountryCode("Sweden", "\uD83C\uDDF8\uD83C\uDDEA", "SE", "+46"),
    CountryCode("Switzerland", "\uD83C\uDDE8\uD83C\uDDED", "CH", "+41"),
    CountryCode("Taiwan", "\uD83C\uDDF9\uD83C\uDDFC", "TW", "+886"),
    CountryCode("Thailand", "\uD83C\uDDF9\uD83C\uDDED", "TH", "+66"),
    CountryCode("Turkey", "\uD83C\uDDF9\uD83C\uDDF7", "TR", "+90"),
    CountryCode("UAE", "\uD83C\uDDE6\uD83C\uDDEA", "AE", "+971"),
    CountryCode("UK", "\uD83C\uDDEC\uD83C\uDDE7", "GB", "+44"),
    CountryCode("USA", "\uD83C\uDDFA\uD83C\uDDF8", "US", "+1"),
    CountryCode("Vietnam", "\uD83C\uDDFB\uD83C\uDDF3", "VN", "+84"),
)

/**
 * Main auth screen that routes between login method selection,
 * phone OTP, email auth, and profile creation.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var currentStep by remember { mutableStateOf(AuthStep.SELECT_METHOD) }
    var verificationId by remember { mutableStateOf("") }
    var resendToken: PhoneAuthProvider.ForceResendingToken? by remember { mutableStateOf(null) }
    var currentPhoneNumber by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        when (state) {
            is AuthState.OTPSent -> {
                verificationId = (state as AuthState.OTPSent).verificationId
                resendToken = (state as AuthState.OTPSent).forceResendingToken
                currentStep = AuthStep.VERIFY_OTP
            }
            is AuthState.ProfileExists -> onAuthSuccess()
            is AuthState.NeedsProfile -> currentStep = AuthStep.CREATE_PROFILE
            is AuthState.Success -> onAuthSuccess()
            else -> {}
        }
    }

    val activity = LocalContext.current as Activity

    when (currentStep) {
        AuthStep.SELECT_METHOD -> SelectMethodScreen(
            onPhoneSelected = { currentStep = AuthStep.ENTER_PHONE },
            onEmailSelected = { currentStep = AuthStep.EMAIL_LOGIN },
        )
        AuthStep.ENTER_PHONE -> EnterPhoneScreen(
            isLoading = state is AuthState.Loading,
            error = (state as? AuthState.Error)?.message,
            onSendOTP = { phone ->
                currentPhoneNumber = phone
                viewModel.sendOTP(phone, activity)
            },
            onBack = { currentStep = AuthStep.SELECT_METHOD },
            onErrorDismiss = { viewModel.resetState() },
        )
        AuthStep.VERIFY_OTP -> VerifyOTPScreen(
            verificationId = verificationId,
            phoneNumber = currentPhoneNumber,
            isLoading = state is AuthState.Loading,
            error = (state as? AuthState.Error)?.message,
            onVerify = { otp -> viewModel.verifyOTP(verificationId, otp) },
            onResend = {
                resendToken?.let { viewModel.resendOTP(it, activity) }
            },
            onBack = {
                currentStep = AuthStep.ENTER_PHONE
                viewModel.resetState()
            },
            onErrorDismiss = { viewModel.resetState() },
        )
        AuthStep.EMAIL_LOGIN -> EmailLoginScreen(
            isLoading = state is AuthState.Loading,
            error = (state as? AuthState.Error)?.message,
            onSignIn = { email, password -> viewModel.signInWithEmail(email, password) },
            onSignUp = { email, password -> viewModel.signUpWithEmail(email, password) },
            onResetPassword = { email -> viewModel.resetPassword(email) },
            onBack = { currentStep = AuthStep.SELECT_METHOD; viewModel.resetState() },
            onErrorDismiss = { viewModel.resetState() },
        )
        AuthStep.CREATE_PROFILE -> CreateProfileScreen(
            isLoading = state is AuthState.Loading,
            error = (state as? AuthState.Error)?.message,
            onCreateProfile = { name, username, photoUri ->
                viewModel.createProfile(name, username, photoUri)
            },
            onErrorDismiss = { viewModel.resetState() },
        )
    }
}

private enum class AuthStep { SELECT_METHOD, ENTER_PHONE, VERIFY_OTP, EMAIL_LOGIN, CREATE_PROFILE }

// =============================================================================
// SCREEN 1: EnterPhoneScreen
// =============================================================================

@Composable
private fun EnterPhoneScreen(
    isLoading: Boolean,
    error: String?,
    onSendOTP: (String) -> Unit,
    onBack: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var selectedCountry by remember { mutableStateOf(countryCodes.find { it.code == "US" } ?: countryCodes[0]) }
    var phoneNumber by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter Your Phone Number",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NovaMesh will send an OTP to verify your number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Country code picker
            Text(
                text = "Country",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = "${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.dialCode})",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showCountryPicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            DropdownMenu(
                expanded = showCountryPicker,
                onDismissRequest = { showCountryPicker = false },
                modifier = Modifier.height(300.dp),
            ) {
                countryCodes.forEach { country ->
                    DropdownMenuItem(
                        text = {
                            Text("${country.flag} ${country.name} (${country.dialCode})")
                        },
                        onClick = {
                            selectedCountry = country
                            showCountryPicker = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone number input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 15) phoneNumber = it },
                label = { Text("Phone number") },
                placeholder = { Text("5551234567") },
                leadingIcon = { Text(selectedCountry.dialCode, style = MaterialTheme.typography.bodyLarge) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            error?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Send OTP button
            Button(
                onClick = { onSendOTP("${selectedCountry.dialCode}$phoneNumber") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = phoneNumber.length >= 7 && !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Send OTP", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 1b: SelectMethodScreen
// =============================================================================

/**
 * Login method selection — Phone (WhatsApp-style OTP) or Email/Password.
 */
@Composable
private fun SelectMethodScreen(
    onPhoneSelected: () -> Unit,
    onEmailSelected: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ─── App logo / branding ───
            Text(
                text = "NovaMesh",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Private · Encrypted · Yours",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ─── Phone login button (WhatsApp-style) ───
            Button(
                onClick = onPhoneSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Phone",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Divider ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
                Text(
                    text = "  or  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Email login button ───
            OutlinedButton(
                onClick = onEmailSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Email",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// =============================================================================
// SCREEN 1c: EmailLoginScreen
// =============================================================================

/**
 * Email/password login and sign-up screen.
 *
 * Features:
 * - Toggle between Sign In and Sign Up mode
 * - Password visibility toggle
 * - Forgot password link
 * - Keyboard actions (Next → Done on last field)
 */
@Composable
private fun EmailLoginScreen(
    isLoading: Boolean,
    error: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
    onResetPassword: (email: String) -> Unit,
    onBack: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmailSent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ─── Icon ───
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Title ───
            Text(
                text = if (isSignUpMode) "Create Account" else "Sign In with Email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSignUpMode) {
                    "Set up an email account to get started"
                } else {
                    "Enter your email and password to continue"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ─── Email field ───
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    resetEmailSent = false
                },
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Password field (hidden in forgot-password mode) ───
            if (!showForgotPassword) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text(if (isSignUpMode) "At least 6 characters" else "Your password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                if (isSignUpMode) onSignUp(email.trim(), password)
                                else onSignIn(email.trim(), password)
                            }
                        },
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ─── Forgot password link (sign-in mode only) ───
                if (!isSignUpMode) {
                    TextButton(
                        onClick = { showForgotPassword = true },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            "Forgot password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // ─── Forgot password mode ───
            if (showForgotPassword) {
                Text(
                    text = "Enter your email and we'll send you a reset link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (resetEmailSent) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "✅ Reset email sent! Check your inbox.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = {
                        showForgotPassword = false
                        resetEmailSent = false
                    }) {
                        Text("Back to Sign In")
                    }
                } else {
                    Button(
                        onClick = {
                            if (email.isNotBlank()) {
                                onResetPassword(email.trim())
                                resetEmailSent = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = email.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Send Reset Email")
                    }
                }
            }

            // ─── Error display ───
            error?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Primary action button ───
            if (!showForgotPassword) {
                Button(
                    onClick = {
                        if (isSignUpMode) onSignUp(email.trim(), password)
                        else onSignIn(email.trim(), password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (isSignUpMode) "Create Account" else "Sign In",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Toggle sign-in / sign-up ───
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {
                        isSignUpMode = !isSignUpMode
                        showForgotPassword = false
                        onErrorDismiss()
                    }) {
                        Text(
                            if (isSignUpMode) "Sign In" else "Sign Up",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 2: VerifyOTPScreen
// =============================================================================

@Composable
private fun VerifyOTPScreen(
    verificationId: String,
    phoneNumber: String,
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    val otpDigits = remember { mutableStateOf(List(WhatsAppCommands.OTP_LENGTH) { "" }) }
    val otpText = remember { otpDigits.value.joinToString("") }
    var countdown by remember { mutableIntStateOf(WhatsAppCommands.RESEND_TIMER) }
    var canResend by remember { mutableStateOf(false) }
    val focusRequesters = remember {
        List(WhatsAppCommands.OTP_LENGTH) { FocusRequester() }
    }

    // Timer countdown
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        } else {
            canResend = true
        }
    }

    // Auto-verify when all 6 digits are filled
    LaunchedEffect(otpDigits.value) {
        val fullOtp = otpDigits.value.joinToString("")
        if (fullOtp.length == WhatsAppCommands.OTP_LENGTH) {
            onVerify(fullOtp)
        }
    }

    // Format phone number for display (e.g., "+91 XXXXX X1234")
    val displayPhone = remember(phoneNumber) {
        val cleaned = phoneNumber.filter { it.isDigit() }
        when {
            cleaned.length <= 6 -> phoneNumber
            else -> {
                val last4 = cleaned.takeLast(4)
                val prefix = cleaned.dropLast(4)
                val masked = prefix.mapIndexed { index, c ->
                    if (index >= prefix.length - 4) c else 'X'
                }.joinToString("")
                phoneNumber.takeWhile { !it.isDigit() } + masked + " " + last4
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the 6-digit code sent to $displayPhone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ─── 6 individual OTP digit boxes ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (i in 0 until WhatsAppCommands.OTP_LENGTH) {
                    OtpDigitBox(
                        digit = otpDigits.value[i],
                        isFocused = false,
                        onValueChange = { newDigit ->
                            val newList = otpDigits.value.toMutableList()
                            if (newDigit.isEmpty()) {
                                newList[i] = ""
                            } else {
                                val digit = newDigit.takeLast(1)
                                if (digit.all { it.isDigit() }) {
                                    newList[i] = digit
                                    // Auto-advance to next box
                                    if (i < WhatsAppCommands.OTP_LENGTH - 1) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                }
                            }
                            otpDigits.value = newList
                        },
                        onBackspace = {
                            if (otpDigits.value[i].isEmpty() && i > 0) {
                                focusRequesters[i - 1].requestFocus()
                            }
                        },
                        focusRequester = focusRequesters[i],
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            error?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Resend
            if (canResend) {
                TextButton(onClick = {
                    countdown = WhatsAppCommands.RESEND_TIMER
                    canResend = false
                    onResend()
                }) {
                    Text("Resend OTP")
                }
            } else {
                Text(
                    text = "Resend code in ${countdown}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A single OTP digit input box styled like WhatsApp.
 */
@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    focusRequester: FocusRequester,
) {
    val borderColor = when {
        digit.isNotEmpty() -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = if (digit.isNotEmpty()) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = digit,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 1)) {
                    onValueChange(newValue)
                }
                // Handle backspace: if new value is empty and we had a digit, go back
                if (newValue.isEmpty() && digit.isNotEmpty()) {
                    // Already handled by clearing
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    innerTextField()
                }
            },
        )
    }
}

// =============================================================================
// SCREEN 3: CreateProfileScreen
// =============================================================================

@Composable
private fun CreateProfileScreen(
    isLoading: Boolean,
    error: String?,
    onCreateProfile: (name: String, username: String, photoUri: String?) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var usernameAutoGenerated by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        photoUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        // Convert bitmap to URI for upload
        bitmap?.let {
            val path = android.provider.MediaStore.Images.Media.insertImage(
                context.contentResolver, it, "profile_photo", null
            )
            path?.let { photoUri = Uri.parse(it) }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Create Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set up your profile to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile photo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            galleryLauncher.launch("image/*")
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Add Photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = {
                if (hasCameraPermission) {
                    cameraLauncher.launch(null)
                } else {
                    galleryLauncher.launch("image/*")
                }
            }) {
                Text("Choose from Gallery or Camera")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Full name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (usernameAutoGenerated) {
                        username = it.lowercase()
                            .replace(" ", "_")
                            .filter { c -> c.isLetterOrDigit() || c == '_' }
                            .take(20)
                    }
                },
                label = { Text("Full Name") },
                placeholder = { Text("John Doe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameAutoGenerated = false
                },
                label = { Text("Username") },
                placeholder = { Text("johndoe") },
                leadingIcon = { Text("@", style = MaterialTheme.typography.bodyLarge) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error
            error?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Get Started button
            Button(
                onClick = {
                    onCreateProfile(
                        fullName.trim(),
                        username.trim(),
                        photoUri?.toString(),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = fullName.isNotBlank() && username.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Get Started", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
