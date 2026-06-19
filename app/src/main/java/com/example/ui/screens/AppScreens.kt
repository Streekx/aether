package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatEntity
import com.example.data.MessageEntity
import com.example.data.StoryEntity
import com.example.ui.theme.*
import com.example.viewmodel.ActiveStoryState
import com.example.viewmodel.CallState
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- UI Navigation Constants ---
enum class AppScreen {
    Splash,
    Onboarding,
    Login,
    SignUp,
    Otp,
    MainFrame, // Host for lower navigation bar tabs
    ChatDetail,
    CallingUnit,
    MediaGallery,
    SearchPanel,
    PrivacyPanel,
    BlockedUsers,
    NotificationsPanel,
    AddNewContact,
    AddStory
}

enum class HomeTab {
    Chats,
    Contacts,
    Search,
    Notifications,
    Settings,
    Profile
}

// --- Procedural Gradient Avatars (Ultra Premium Look) ---
@Composable
fun CyberAvatar(
    name: String,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val char = if (name.isNotEmpty()) name.first().uppercase() else "A"
    // Clean seed hash based colors
    val hash = kotlin.math.abs(name.hashCode())
    val startColor = when (hash % 5) {
        0 -> Color(0xFF8E24AA) // Royal Purple
        1 -> Color(0xFFAB47BC) // Glow Purple
        2 -> Color(0xFF00E676) // Status Green
        3 -> Color(0xFF29B6F6) // Delivery Blue
        else -> Color(0xFFFF9100) // Orange Flame
    }
    val endColor = when ((hash + 2) % 5) {
        0 -> Color(0xFF5A189A) // Deep Muted Purple
        1 -> Color(0xFF3F51B5) // Indigo
        2 -> Color(0xFF00B0FF) // Light Blue
        3 -> Color(0xFFD500F9) // Electric Pink
        else -> Color(0xFFE040FB) // Light Purple
    }

    val gradientBrush = remember(name) {
        Brush.linearGradient(colors = listOf(startColor, endColor))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(gradientBrush)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            color = WhiteSmoke,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Indicator dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(1.5.dp, DeepBlack, CircleShape)
                    .background(StatusGreen, CircleShape)
            )
        }
    }
}

// Helper: Formatter
fun formatTimestamp(time: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return fmt.format(Date(time))
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigatorHost(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.Splash) }
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(HomeTab.Chats) }

    val authState by viewModel.authState.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val storyState by viewModel.storyState.collectAsState()
    val isLocked by viewModel.isPasscodeLocked.collectAsState()

    // Map VM state change to screen
    LaunchedEffect(gameStateKey(authState)) {
        when (authState) {
            is com.example.viewmodel.AuthState.Splash -> currentScreen = AppScreen.Splash
            is com.example.viewmodel.AuthState.Onboarding -> currentScreen = AppScreen.Onboarding
            is com.example.viewmodel.AuthState.Login -> currentScreen = AppScreen.Login
            is com.example.viewmodel.AuthState.SignUp -> currentScreen = AppScreen.SignUp
            is com.example.viewmodel.AuthState.OtpVerification -> currentScreen = AppScreen.Otp
            is com.example.viewmodel.AuthState.Authenticated -> {
                if (currentScreen == AppScreen.Splash || currentScreen == AppScreen.Onboarding || currentScreen == AppScreen.Login || currentScreen == AppScreen.SignUp || currentScreen == AppScreen.Otp) {
                    currentScreen = AppScreen.MainFrame
                }
            }
        }
    }

    // Handle incoming/outgoing active call triggers
    LaunchedEffect(callState.isActive) {
        if (callState.isActive) {
            currentScreen = AppScreen.CallingUnit
        } else if (currentScreen == AppScreen.CallingUnit) {
            currentScreen = AppScreen.MainFrame
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.Splash -> SplashScreen()
                    AppScreen.Onboarding -> OnboardingScreen(onGetStarted = { viewModel.completeOnboarding() })
                    AppScreen.Login -> LoginScreen(onLogin = { viewModel.login() })
                    AppScreen.SignUp -> SignUpScreen(onSignUp = { viewModel.signUp() })
                    AppScreen.Otp -> OtpScreen(onVerify = { viewModel.verifyOtp() }, onBackPressed = { currentScreen = AppScreen.Login })
                    AppScreen.MainFrame -> MainFrameHost(
                        viewModel = viewModel,
                        activeTab = activeTab,
                        onTabSelected = { activeTab = it },
                        onChatSelected = { id ->
                            selectedChatId = id
                            currentScreen = AppScreen.ChatDetail
                        },
                        onOpenSearch = { currentScreen = AppScreen.SearchPanel },
                        onOpenPrivacy = { currentScreen = AppScreen.PrivacyPanel },
                        onOpenNotifications = { currentScreen = AppScreen.NotificationsPanel },
                        onOpenMediaGallery = { currentScreen = AppScreen.MediaGallery },
                        onAddNewContact = { currentScreen = AppScreen.AddNewContact },
                        onPostStoryClicked = { currentScreen = AppScreen.AddStory }
                    )
                    AppScreen.ChatDetail -> ChatDetailScreen(
                        viewModel = viewModel,
                        chatId = selectedChatId ?: "chat_aura",
                        onBackPressed = { currentScreen = AppScreen.MainFrame }
                    )
                    AppScreen.CallingUnit -> CallingScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame }
                    )
                    AppScreen.SearchPanel -> SearchScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame },
                        onChatSelected = { id ->
                            selectedChatId = id
                            currentScreen = AppScreen.ChatDetail
                        }
                    )
                    AppScreen.PrivacyPanel -> PrivacyScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame },
                        onOpenBlocked = { currentScreen = AppScreen.BlockedUsers }
                    )
                    AppScreen.BlockedUsers -> BlockedUsersScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.PrivacyPanel }
                    )
                    AppScreen.NotificationsPanel -> NotificationsScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame }
                    )
                    AppScreen.AddNewContact -> AddNewContactScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame }
                    )
                    AppScreen.AddStory -> AddStoryScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = AppScreen.MainFrame }
                    )
                    else -> SplashScreen()
                }
            }

            // Simulated Security Passcode Lock Overlay
            if (isLocked) {
                PasscodeOverlayScreen(onUnlock = { viewModel.setPasscodeLocked(false) })
            }

            // Real-time Stories Overlay Player
            if (storyState.isOpen) {
                StoryPlayerOverlay(
                    state = storyState,
                    onNext = { viewModel.nextStory() },
                    onPrev = { viewModel.previousStory() },
                    onClose = { viewModel.closeStoryView() }
                )
            }
        }
    }
}

// Key helper for gameState
private fun gameStateKey(state: com.example.viewmodel.AuthState): Int {
    return when (state) {
        com.example.viewmodel.AuthState.Splash -> 0
        com.example.viewmodel.AuthState.Onboarding -> 1
        com.example.viewmodel.AuthState.Login -> 2
        com.example.viewmodel.AuthState.SignUp -> 3
        com.example.viewmodel.AuthState.OtpVerification -> 4
        com.example.viewmodel.AuthState.Authenticated -> 5
    }
}


// ==========================================
// 1. SPLASH / STARTUP SCREEN
// ==========================================
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(MutedPurple.copy(alpha = 0.45f), ObsidianBlack),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Giant glowing pulsing cyber orb logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(LightRoyalPurple.copy(alpha = 0.45f * alpha), Color.Transparent),
                                radius = size.minDimension * 0.9f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Hub,
                    contentDescription = "Logo",
                    tint = WhiteSmoke,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AETHER",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 12.sp,
                    color = WhiteSmoke,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SECURE WAVEGRID PROTOCOL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 3.sp,
                    color = LightRoyalPurple.copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif
                )
            )
        }
    }
}


// ==========================================
// 2. ONBOARDING MATRIX SCREEN
// ==========================================
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        Triple(
            Icons.Rounded.Security,
            "Sovereign Protocol",
            "Simulated decentralized SQLite database structures deployed onto reactive Room matrices ensures 100% on-device private local architecture."
        ),
        Triple(
            Icons.Rounded.OfflineBolt,
            "Hyper-Speed Streaming",
            "Zero buffering data flows with instant typing callbacks, and simulated low-latency offline communication adapters."
        ),
        Triple(
            Icons.Rounded.AutoAwesome,
            "Aura Assistant Core",
            "Connect directly to Google's server-less Gemini AI models to translate, orchestrate, and chat within your matrix nodes."
        )
    )

    val current = steps[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(28.dp)
    ) {
        // Top skipping anchor
        TextButton(
            onClick = onGetStarted,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("onboarding_skip"),
            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
        ) {
            Text("SKIP NODE", letterSpacing = 1.sp)
        }

        // Inner glowing guide card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(DarkSurfaceVariant, CircleShape)
                    .border(1.dp, RoyalPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = current.first,
                    contentDescription = "Onboarding icon",
                    tint = LightRoyalPurple,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = current.second,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WhiteSmoke,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = current.third,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pulse progress nodes
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == step) 28.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == step) RoyalPurple else DarkSurfaceVariant)
                    )
                }
            }
        }

        // Action button at footer
        Button(
            onClick = {
                if (step < steps.lastIndex) {
                    step++
                } else {
                    onGetStarted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .align(Alignment.BottomCenter)
                .testTag("onboarding_next"),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (step < steps.lastIndex) "NEXT MATRIX" else "INITIALIZE CORE",
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = WhiteSmoke
            )
        }
    }
}


// ==========================================
// 3. LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var handle by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = "Lock key",
                tint = RoyalPurple,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DECRYPT INSTANCE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WhiteSmoke,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Text(
                text = "Access secure neural networks",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it; error = "" },
                label = { Text("Cyber Phone / Handle", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = LightRoyalPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = "" },
                label = { Text("Matrix Decryption Key", color = TextSecondary) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = LightRoyalPurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = SoftRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (handle.isEmpty() || password.isEmpty()) {
                        error = "Matrix strings cannot remain blank."
                    } else {
                        onLogin()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("login_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DECRYPT NODE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}


// ==========================================
// 4. SIGN-UP / CREDENTIAL SYSTEM
// ==========================================
@Composable
fun SignUpScreen(onSignUp: () -> Unit) {
    var dName by remember { mutableStateOf("") }
    var userBio by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBox,
                contentDescription = "Profile creation",
                tint = LightRoyalPurple,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SYNCHRONIZE IDENTITY",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WhiteSmoke,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = dName,
                onValueChange = { dName = it },
                label = { Text("Display Matrix Alias", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = userBio,
                onValueChange = { userBio = it },
                label = { Text("Matrix Bio description", color = TextSecondary) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (dName.isNotEmpty()) {
                        onSignUp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("signup_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ESTABLISH ANCHOR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}


// ==========================================
// 5. OTP / SECURITY VERIFICATION
// ==========================================
@Composable
fun OtpScreen(onVerify: () -> Unit, onBackPressed: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = "Security OTP",
                tint = GlowPurple,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NEURAL KEY CHALLENGE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WhiteSmoke,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Text(
                text = "Enter simulated 4-digit master validation pin (Use 2026)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it; error = "" },
                label = { Text("2FA Neural Code", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(180.dp)
                    .testTag("otp_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp)
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = SoftRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (pin == "2026" || pin == "1234") {
                        onVerify()
                    } else {
                        error = "Challenge mismatch. Enter valid pin."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("otp_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("VALIDATE SECURITY BLOCK", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBackPressed) {
                Text("RE-ESTABLISH HANDLE", color = LightRoyalPurple)
            }
        }
    }
}


// ==========================================
// 6. GENERAL MAIN HOST FRAME WITH BOTTOM NAVIGATION
// ==========================================
// 6. GENERAL MAIN HOST FRAME WITH FLOATING GLASS NAVIGATION DOCK
// ==========================================
@Composable
fun MainFrameHost(
    viewModel: MainViewModel,
    activeTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onChatSelected: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMediaGallery: () -> Unit,
    onAddNewContact: () -> Unit,
    onPostStoryClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Tab Content Screen Container with generous bottom spacing to clear the floating dock
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp)
        ) {
            when (activeTab) {
                HomeTab.Chats -> ChatsTab(
                    viewModel = viewModel,
                    onChatSelected = onChatSelected,
                    onSearchClicked = { onTabSelected(HomeTab.Search) },
                    onPostStoryClicked = onPostStoryClicked
                )
                HomeTab.Contacts -> ContactsTab(
                    viewModel = viewModel,
                    onChatSelected = onChatSelected,
                    onAddContactClicked = onAddNewContact
                )
                HomeTab.Search -> SearchScreen(
                    viewModel = viewModel,
                    onBackPressed = { onTabSelected(HomeTab.Chats) },
                    onChatSelected = onChatSelected
                )
                HomeTab.Notifications -> NotificationsScreen(
                    viewModel = viewModel,
                    onBackPressed = { onTabSelected(HomeTab.Chats) }
                )
                HomeTab.Settings -> SettingsTab(
                    viewModel = viewModel,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenNotifications = onOpenNotifications,
                    onOpenMedia = onOpenMediaGallery
                )
                HomeTab.Profile -> ProfileTab(
                    viewModel = viewModel,
                    onPostStoryClicked = onPostStoryClicked
                )
            }
        }

        // Luxurious Floating Rounded Glass Navigation Dock Centered at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Elegant list of tabs with their icon and label
            val items = listOf(
                Triple(HomeTab.Chats, Icons.Rounded.Forum, "Chats"),
                Triple(HomeTab.Contacts, Icons.Rounded.PersonSearch, "Contacts"),
                Triple(HomeTab.Search, Icons.Rounded.Search, "Search"),
                Triple(HomeTab.Notifications, Icons.Rounded.Notifications, "Alerts"),
                Triple(HomeTab.Settings, Icons.Rounded.Settings, "Settings"),
                Triple(HomeTab.Profile, Icons.Rounded.MatrixProfileIcon, "Profile")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // High-end glass shadow backglow (radial neon purple glow)
                Box(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .height(68.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GlowPurple.copy(alpha = 0.35f), Color.Transparent),
                                radius = 280f
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                )

                // Glass dock body
                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .height(68.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBlack.copy(alpha = 0.85f),
                                    DeepBlack.copy(alpha = 0.95f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    WhiteSmoke.copy(alpha = 0.22f),
                                    LightRoyalPurple.copy(alpha = 0.40f),
                                    GlowPurple.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val tabWidth = maxWidth / 6
                    val activeIndex = when (activeTab) {
                        HomeTab.Chats -> 0
                        HomeTab.Contacts -> 1
                        HomeTab.Search -> 2
                        HomeTab.Notifications -> 3
                        HomeTab.Settings -> 4
                        HomeTab.Profile -> 5
                    }

                    // Animated slide indicator backglow pill
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * activeIndex,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
                        label = "dock_slide"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MutedPurple.copy(alpha = 0.45f),
                                        GlowPurple.copy(alpha = 0.20f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 0.8.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        LightRoyalPurple.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .drawBehind {
                                // Neon active bottom glow streak line
                                val barHeight = 2.dp.toPx()
                                val barWidth = size.width * 0.45f
                                val startX = (size.width - barWidth) / 2
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.Transparent, LightRoyalPurple, Color.Transparent)
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(startX, size.height - barHeight - 4),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                )
                            }
                    )

                    // Render tabs
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { item ->
                            val isSelected = activeTab == item.first
                            val tint by animateColorAsState(
                                targetValue = if (isSelected) WhiteSmoke else TextSecondary,
                                animationSpec = tween(220)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onTabSelected(item.first) }
                                    )
                                    .testTag("nav_tab_${item.third.lowercase()}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = item.third,
                                    modifier = Modifier
                                        .size(if (isSelected) 24.dp else 22.dp)
                                        .scale(if (isSelected) 1.12f else 1.0f),
                                    tint = tint
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = item.third,
                                    fontSize = 8.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = tint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Map custom vectors for Profile icons safely
private val Icons.Rounded.MatrixProfileIcon: ImageVector
    get() = Icons.Rounded.AccountBox


// ==========================================
// 7. CHATS TAB VIEW WITH HORIZONTAL STORIES RING
// ==========================================
@Composable
fun ChatsTab(
    viewModel: MainViewModel,
    onChatSelected: (String) -> Unit,
    onSearchClicked: () -> Unit,
    onPostStoryClicked: () -> Unit
) {
    val chats by viewModel.allChats.collectAsState()
    val storiesList by viewModel.stories.collectAsState()
    val statusMap by viewModel.typingStatusMap.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Futuristic Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Aether Nexus",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = WhiteSmoke,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onSearchClicked) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search Matrix", tint = WhiteSmoke)
                }

                IconButton(onClick = onPostStoryClicked) {
                    Icon(imageVector = Icons.Rounded.AddAPhoto, contentDescription = "Share Status", tint = LightRoyalPurple)
                }
            }
        }

        Divider(color = DarkSurfaceVariant, thickness = 1.dp)

        // Stories glowing strip
        HorizontalStoriesStrip(
            stories = storiesList,
            onStoryClicked = { index ->
                viewModel.openStoryView(storiesList, index)
            },
            onAddStoryClicked = onPostStoryClicked
        )

        Divider(color = DarkSurfaceVariant, thickness = 1.dp)

        // Pinned Chats / Standard list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_list")
        ) {
            items(chats) { chat ->
                val typingText = statusMap[chat.id]
                val subText = typingText ?: chat.lastMessageText
                val displaySubColor = if (typingText != null) LightRoyalPurple else TextSecondary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatSelected(chat.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberAvatar(name = chat.name, size = 52.dp, isOnline = chat.id == "chat_aura" || chat.id == "chat_grim" || chat.id == "chat_sophia")

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.name,
                                color = WhiteSmoke,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (chat.lastMessageTime > 0) {
                                Text(
                                    text = formatTimestamp(chat.lastMessageTime),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subText,
                                color = displaySubColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (chat.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(RoyalPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = WhiteSmoke,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = DarkSurfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
fun HorizontalStoriesStrip(
    stories: List<StoryEntity>,
    onStoryClicked: (Int) -> Unit,
    onAddStoryClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Add custom story bubble
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onAddStoryClicked() }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(2.dp, Brush.sweepGradient(listOf(GlowPurple, MutedPurple)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Status", tint = WhiteSmoke)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Your Wave", color = TextSecondary, fontSize = 11.sp)
        }

        // List of stories
        stories.forEachIndexed { index, story ->
            val borderBrush = Brush.sweepGradient(
                if (story.isViewed) {
                    listOf(LightGray, DarkSurfaceVariant)
                } else {
                    listOf(LightRoyalPurple, RoyalPurple, GlowPurple)
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClicked(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(2.dp)
                        .border(2.dp, borderBrush, CircleShape)
                        .padding(2.dp)
                ) {
                    CyberAvatar(name = story.userName, size = 48.dp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.userName.split(" ").firstOrNull() ?: "",
                    color = WhiteSmoke,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// ==========================================
// 8. INDIVIDUAL CHAT AND WRITING SCREEN
// ==========================================
@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    chatId: String,
    onBackPressed: () -> Unit
) {
    val chats by viewModel.allChats.collectAsState()
    val chat = chats.find { it.id == chatId } ?: ChatEntity(chatId, "Neural Line", false)
    val messages by viewModel.getMessages(chatId).collectAsState(initial = emptyList())
    val typingMap by viewModel.typingStatusMap.collectAsState()
    val typingText = typingMap[chatId]

    var textInput by remember { mutableStateOf("") }
    var voiceRecordingSecs by remember { mutableStateOf(0) }
    var isVoiceRecording by remember { mutableStateOf(false) }

    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            voiceRecordingSecs = 0
            while (isVoiceRecording) {
                delay(1000)
                if (isVoiceRecording) voiceRecordingSecs++
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackPressed, modifier = Modifier.testTag("chat_back_btn")) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteSmoke)
                        }

                        CyberAvatar(name = chat.name, size = 40.dp, isOnline = true)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(chat.name, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = typingText ?: "active secure link",
                                color = if (typingText != null) LightRoyalPurple else TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { viewModel.startCall(chat.id, isVideo = false) }) {
                            Icon(imageVector = Icons.Rounded.Call, contentDescription = "Voice Call", tint = WhiteSmoke)
                        }
                        IconButton(onClick = { viewModel.startCall(chat.id, isVideo = true) }) {
                            Icon(imageVector = Icons.Rounded.Videocam, contentDescription = "Video Session", tint = LightRoyalPurple)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
        ) {
            // Message Scroller
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(messages) { message ->
                    val isMine = message.isMine
                    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                    val containerColor = if (isMine) RoyalPurple else DarkSurfaceVariant
                    val cornerRadius = if (isMine) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = alignment
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(cornerRadius)
                                .background(containerColor)
                                .padding(12.dp)
                        ) {
                            if (!isMine && chat.isGroup) {
                                Text(
                                    text = message.senderName,
                                    color = LightRoyalPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            if (message.isVoice) {
                                // Dynamic Voice visualizer waveform
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play voice link", tint = WhiteSmoke)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    VoiceWaveformMock(modifier = Modifier.width(120.dp).height(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${message.voiceDuration}s", color = TextSecondary, fontSize = 11.sp)
                                }
                            } else {
                                Text(text = message.text, color = WhiteSmoke, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTimestamp(message.timestamp),
                                    color = TextSecondary.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )

                                if (isMine) {
                                    Icon(
                                        imageVector = if (message.deliveryStatus == "read") Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = "delivery status",
                                        tint = if (message.deliveryStatus == "read") DeliveryBlue else TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keyboard/Input Deck (Fully transparent premium liquid-glass design floating above bottom)
            val isTyping = textInput.isNotEmpty()
            val glowAlpha by animateFloatAsState(
                targetValue = if (isTyping) 0.65f else 0.15f,
                animationSpec = tween(350),
                label = "typing_glow"
            )
            val borderGlowColor by animateColorAsState(
                targetValue = if (isTyping) LightRoyalPurple else WhiteSmoke.copy(alpha = 0.2f),
                animationSpec = tween(350),
                label = "border_glow"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 6.dp)
            ) {
                // High-fidelity liquid-glass frosted controller shell
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    DarkSurface.copy(alpha = 0.45f),
                                    DeepBlack.copy(alpha = 0.75f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    borderGlowColor,
                                    GlowPurple.copy(alpha = glowAlpha),
                                    borderGlowColor.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .drawBehind {
                            // Subtly paint a futuristic dynamic radial typing aura
                            if (isTyping) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(LightRoyalPurple.copy(alpha = 0.15f), Color.Transparent),
                                        radius = size.width / 2f
                                    ),
                                    alpha = glowAlpha
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (isVoiceRecording) {
                        // Speech recorder active deck (fused glass layout)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(12.dp)
                                        .background(SoftRed, CircleShape)
                                        .border(2.dp, WhiteSmoke.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "RECORDING DATA NODE...",
                                    color = SoftRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = "${voiceRecordingSecs}s",
                                color = WhiteSmoke,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )

                            TextButton(
                                onClick = { isVoiceRecording = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = SoftRed.copy(alpha = 0.85f))
                            ) {
                                Text("DELETE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen.copy(alpha = 0.15f))
                                    .border(1.dp, StatusGreen.copy(alpha = 0.35f), CircleShape)
                                    .clickable {
                                        isVoiceRecording = false
                                        viewModel.sendMessage(chatId, "[Secure voice message: $voiceRecordingSecs seconds]", isVoice = true, voiceDuration = voiceRecordingSecs)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Transmit voice",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Standard message field with liquid drop glass actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fluid Glass Attachment Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WhiteSmoke.copy(alpha = 0.08f))
                                    .border(1.dp, WhiteSmoke.copy(alpha = 0.15f), CircleShape)
                                    .clickable { /* Media selection simulated trigger */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach nodes",
                                    tint = WhiteSmoke,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Compose secure cyber stream...", color = TextSecondary.copy(alpha = 0.8f), fontSize = 13.sp) },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = WhiteSmoke,
                                    unfocusedTextColor = WhiteSmoke
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field")
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            if (textInput.isNotEmpty()) {
                                // Fluid Glass Transmit Button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(LightRoyalPurple.copy(alpha = 0.15f))
                                        .border(1.dp, LightRoyalPurple.copy(alpha = 0.45f), CircleShape)
                                        .clickable {
                                            viewModel.sendMessage(chatId, textInput)
                                            textInput = ""
                                        }
                                        .testTag("chat_send_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Transmit",
                                        tint = LightRoyalPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // Fluid Glass Record Node Button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(WhiteSmoke.copy(alpha = 0.08f))
                                        .border(1.dp, WhiteSmoke.copy(alpha = 0.15f), CircleShape)
                                        .clickable { isVoiceRecording = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Record",
                                        tint = LightRoyalPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sine Wave mock for voicenote preview
@Composable
fun VoiceWaveformMock(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val waveWidth = size.width
        val waveHeight = size.height
        val midY = waveHeight / 2f
        val points = 24
        val path = Path()
        
        for (i in 0..points) {
            val ratio = i.toFloat() / points
            val x = ratio * waveWidth
            // Simulated sine amplitude pattern
            val amp = if (i % 3 == 0) midY * 0.75f else if (i % 2 == 0) midY * 0.35f else midY * 0.1f
            val y = midY + amp * kotlin.math.sin(ratio * Math.PI.toFloat() * 6).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = LightRoyalPurple, style = Stroke(width = 3f))
    }
}


// ==========================================
// 9. CALLS OVERLAY SCREEN (AUDIO & VIDEO CALLS)
// ==========================================
@Composable
fun CallingScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "pulseCircles")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseCirclesVal"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        if (callState.isVideo && callState.isConnected && callState.isCameraOn) {
            // Simulated Active Camera Matrix grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Drawing futuristic background matrix nodes
                        val cols = 12
                        val rows = 20
                        val gapX = size.width / cols
                        val gapY = size.height / rows
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                drawCircle(
                                    color = RoyalPurple.copy(alpha = 0.08f),
                                    radius = 3f,
                                    center = androidx.compose.ui.geometry.Offset(c * gapX, r * gapY)
                                )
                            }
                        }
                    }
            ) {
                // Secondary smaller peer glow frame top right
                Box(
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.5.dp, LightRoyalPurple, RoundedCornerShape(12.dp))
                        .align(Alignment.TopEnd)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Matrix node (You)", color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // Simulated audio acoustic circle pulse wave background
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulse)
                        .background(MutedPurple.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, RoyalPurple.copy(alpha = 0.35f), CircleShape)
                )
            }
        }

        // Concentric header data
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CyberAvatar(name = callState.userName, size = 110.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(callState.userName, color = WhiteSmoke, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (callState.isConnected) {
                    val m = callState.durationSec / 60
                    val s = callState.durationSec % 60
                    "Secure stream linked - %02d:%02d".format(m, s)
                } else {
                    "Establishing neural line channel..."
                },
                color = if (callState.isConnected) StatusGreen else LightRoyalPurple,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Interactive control tray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (callState.isIncoming) {
                    IconButton(
                        onClick = { viewModel.answerCall() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(StatusGreen, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Accept Line", tint = WhiteSmoke)
                    }

                    IconButton(
                        onClick = { viewModel.hangUpCall() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(SoftRed, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Decline Line", tint = WhiteSmoke)
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.toggleMuteCall() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (callState.isMuted) Color.White else DarkSurfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute Line",
                            tint = if (callState.isMuted) DeepBlack else WhiteSmoke
                        )
                    }

                    IconButton(
                        onClick = { viewModel.hangUpCall() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(SoftRed, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Hang Up", tint = WhiteSmoke)
                    }

                    if (callState.isVideo) {
                        IconButton(
                            onClick = { viewModel.toggleCameraCall() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (callState.isCameraOn) DarkSurfaceVariant else Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Toggle Matrix View",
                                tint = if (callState.isCameraOn) WhiteSmoke else DeepBlack
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 10. CONTACTS REGISTER / NODE INDEX TAB
// ==========================================
@Composable
fun ContactsTab(
    viewModel: MainViewModel,
    onChatSelected: (String) -> Unit,
    onAddContactClicked: () -> Unit
) {
    val contactList by viewModel.contacts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Grid Directory",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = WhiteSmoke,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            )

            Button(
                onClick = onAddContactClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MutedPurple),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add node", tint = WhiteSmoke, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD NODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhiteSmoke)
            }
        }

        Divider(color = DarkSurfaceVariant)

        // Contacts scroller
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("contacts_tab_list")
        ) {
            items(contactList.filter { !it.isBlocked }) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatSelected("chat_" + contact.id.replace("user_", "")) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberAvatar(name = contact.displayName, size = 48.dp, isOnline = contact.isOnline)

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(contact.displayName, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(contact.username, color = LightRoyalPurple, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(contact.bio, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Divider(color = DarkSurfaceVariant.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}


// ==========================================
// 11. ADD NEW CONTACT SCREEN
// ==========================================
@Composable
fun AddNewContactScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    var cName by remember { mutableStateOf("") }
    var cPhone by remember { mutableStateOf("") }
    var cUsername by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Matrix Endpoint", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = cName,
                onValueChange = { cName = it },
                label = { Text("Display Endpoint Label", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("add_contact_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cPhone,
                onValueChange = { cPhone = it },
                label = { Text("Cyber Link Key (Phone Number)", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cUsername,
                onValueChange = { cUsername = it },
                label = { Text("Public Matrix Handle (@alias)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (cName.isNotEmpty() && cPhone.isNotEmpty()) {
                        viewModel.addContact(cName, cPhone, cUsername)
                        onBackPressed()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("add_contact_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("BIND NODE ENDPOINT", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// 12. ADD STORY / STATUS SCREEN
// ==========================================
@Composable
fun AddStoryScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    var descInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Matrix Wave", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, RoyalPurple, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.AddPhotoAlternate, contentDescription = "Mock capture", tint = LightRoyalPurple, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SIMULATED LENS ATTACHMENT", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = descInput,
                onValueChange = { descInput = it },
                label = { Text("Broadcast inscription text", color = TextSecondary) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = WhiteSmoke,
                    unfocusedTextColor = WhiteSmoke
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (descInput.isNotEmpty()) {
                        viewModel.postStory(descInput)
                        onBackPressed()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("BROADCAST ON GRID", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// 13. STORIES OVERLAY PLAYER
// ==========================================
@Composable
fun StoryPlayerOverlay(
    state: ActiveStoryState,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    val story = state.stories.getOrNull(state.currentIndex) ?: return
    var tickProgress by remember { mutableStateOf(0f) }

    // Automatic story progression countdown timer
    LaunchedEffect(state.currentIndex) {
        tickProgress = 0f
        val steps = 100
        for (i in 0..steps) {
            delay(40) // 4 seconds count per story (40ms * 100)
            tickProgress = i.toFloat() / steps
        }
        onNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Render story background art (futuristic matrix visual style based on content word length)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MutedPurple, DeepBlack)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Cyclone, contentDescription = "matrix icon", tint = LightRoyalPurple.copy(alpha = 0.35f), modifier = Modifier.size(92.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = story.text,
                    color = WhiteSmoke,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )
            }
        }

        // Progression progress nodes strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.stories.indices.forEach { index ->
                val progressVal = when {
                    index < state.currentIndex -> 1f
                    index > state.currentIndex -> 0f
                    else -> tickProgress
                }
                LinearProgressIndicator(
                    progress = { progressVal },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp),
                    color = LightRoyalPurple,
                    trackColor = DarkSurfaceVariant
                )
            }
        }

        // Header info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CyberAvatar(name = story.userName, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(story.userName, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("•", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(formatTimestamp(story.timestamp), color = TextSecondary, fontSize = 11.sp)
            }

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Broadcast", tint = WhiteSmoke)
            }
        }

        // Custom touch segments side anchors for paging
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable { onPrev() }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable { onNext() }
            )
        }
    }
}


// ==========================================
// 14. SETTINGS MATRIX PREFERENCES TAB
// ==========================================
@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    onOpenPrivacy: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMedia: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.currentUserProfile.collectAsState()
    val isLocked by viewModel.isPasscodeLocked.collectAsState()
    var isMuteState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Obsidian Glass backdrop card for quick account details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CyberAvatar(name = user.displayName, size = 68.dp)

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(user.displayName, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(user.phone, color = TextSecondary, fontSize = 13.sp)
                    Text("@" + user.username, color = LightRoyalPurple, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Divider(color = DarkSurfaceVariant)

        // Multiple Account Switchers section (As requested by user!)
        SectionHeader(title = "SECURE MATRIX ACCOUNTS (MULTI-PROFILE)")
        viewModel.accounts.forEachIndexed { index, act ->
            val isActive = user.userId == act.userId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.switchAccount(index) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CyberAvatar(name = act.displayName, size = 32.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(act.displayName, color = if (isActive) LightRoyalPurple else WhiteSmoke, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                }
                if (isActive) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Active account", tint = GlowPurple)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions grid system
        SectionHeader(title = "SETTINGS DECK")

        SettingsItemRow(
            icon = Icons.Outlined.Security,
            title = "Privacy & Sovereign Encryption",
            subtitle = "Blocked user matrices, passcode simulated lockers",
            onClick = onOpenPrivacy
        )

        SettingsItemRow(
            icon = Icons.Outlined.NotificationsActive,
            title = "Unified Signal Alerts",
            subtitle = "Manage secure ringtone volumes, notification toggles",
            onClick = onOpenNotifications
        )

        SettingsItemRow(
            icon = Icons.Outlined.PermMedia,
            title = "Acoustic Media Folder",
            subtitle = "Explore received wave files & photographic archives",
            onClick = onOpenMedia
        )

        // Passcode simulation switch toggler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setPasscodeLocked(!isLocked) }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = LightRoyalPurple)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Matrix Node Passcode Block", color = WhiteSmoke, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Restrict interface entry when inactive", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Switch(
                checked = isLocked,
                onCheckedChange = { viewModel.setPasscodeLocked(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LightRoyalPurple,
                    checkedTrackColor = MutedPurple
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = LightRoyalPurple,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = WhiteSmoke)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = WhiteSmoke, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}


// ==========================================
// 15. PROFILE DETAIL Matrix POSTS TAB
// ==========================================
@Composable
fun ProfileTab(
    viewModel: MainViewModel,
    onPostStoryClicked: () -> Unit
) {
    val user by viewModel.currentUserProfile.collectAsState()
    
    // Simulate interactive micro posts list (Telegram inspired)
    var posts by remember { mutableStateOf(listOf(
        "Deploying the Aether core network successfully, UI is crisp!",
        "Designing the next-generation sovereign communication layers using native Kotlin."
    )) }
    var mockNewPostText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CyberAvatar(name = user.displayName, size = 92.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(user.displayName, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("@" + user.username, color = LightRoyalPurple, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.bio,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Divider(color = DarkSurfaceVariant)

        // Create micro post component
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(0.5.dp, RoyalPurple, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("POST A GRID BROADCAST STRING", color = LightRoyalPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = mockNewPostText,
                onValueChange = { mockNewPostText = it },
                placeholder = { Text("What's on your cipher grid today?", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = WhiteSmoke
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    if (mockNewPostText.isNotEmpty()) {
                        posts = listOf(mockNewPostText) + posts
                        mockNewPostText = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
            ) {
                Text("BCAST", color = WhiteSmoke, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bcast posts flow strip list
        SectionHeader(title = "CYBER INSTANCE TRANSMISSIONS")
        posts.forEach { post ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text("DECRYPTED MATRIX", color = LightRoyalPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(post, color = WhiteSmoke, fontSize = 14.sp)
            }
        }
    }
}


// ==========================================
// 16. MOUNTED SECURITY ACCESS PASSCODE LOCKED SCREEN
// ==========================================
@Composable
fun PasscodeOverlayScreen(onUnlock: () -> Unit) {
    val passcodeTyped = remember { mutableStateListOf<Int>() }
    var displayError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.LockClock, contentDescription = "Active System Lock", tint = RoyalPurple, modifier = Modifier.size(68.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("SECURE ACCESS MATRIX LOCKED", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
            Text(
                if (displayError) "Invalid cipher combination! Please retry." else "Configure numeric signature (Code: 1234)",
                color = if (displayError) SoftRed else TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Typestate indicators
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (0..3).forEach { dotIdx ->
                    val isActive = dotIdx < passcodeTyped.size
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isActive) LightRoyalPurple else DarkSurfaceVariant)
                            .border(1.dp, if (isActive) GlowPurple else Color.Transparent, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Virtual keypad metrics Layout
            val keys = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -2) // -1 back, -2 abort
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.width(260.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(keys) { key ->
                    when (key) {
                        -1 -> {
                            IconButton(onClick = { if (passcodeTyped.isNotEmpty()) passcodeTyped.removeAt(passcodeTyped.lastIndex) }) {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = TextSecondary)
                            }
                        }
                        -2 -> {
                            // Reset key and clean
                            IconButton(onClick = { passcodeTyped.clear() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset lock state", tint = TextSecondary)
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        if (passcodeTyped.size < 4) {
                                            passcodeTyped.add(key)
                                        }

                                        if (passcodeTyped.size == 4) {
                                            val comb = passcodeTyped.joinToString("")
                                            if (comb == "1234") {
                                                onUnlock()
                                            } else {
                                                displayError = true
                                                passcodeTyped.clear()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    key.toString(),
                                    color = WhiteSmoke,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 17. PRIVACY AND sovereign SECURITY SCREEN
// ==========================================
@Composable
fun PrivacyScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit,
    onOpenBlocked: () -> Unit
) {
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy & Security Deck", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
        ) {
            SectionHeader(title = "CYBERNETIC BLOCKS")
            SettingsItemRow(
                icon = Icons.Rounded.Block,
                title = "Blocked Matrix Networks",
                subtitle = "Active blocked contacts & spam filtration locks",
                onClick = onOpenBlocked
            )

            SectionHeader(title = "SOVEREIGN ENCRYPTION STATS")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Rounded.LockPerson, contentDescription = null, tint = StatusGreen)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Secured State Status", color = WhiteSmoke, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Room persistence layer secure signature initialized successfully.", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}


// ==========================================
// 18. BLOCKED MEMBERS DECK SCREEN
// ==========================================
@Composable
fun BlockedUsersScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val contactList by viewModel.contacts.collectAsState()
    val blockedList = contactList.filter { it.isBlocked }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Blocked Cyber Nodes", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
        ) {
            if (blockedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Safe Grid: Zero blocked contacts recorded.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(blockedList) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberAvatar(name = user.displayName, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(user.displayName, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(user.username, color = LightRoyalPurple, fontSize = 11.sp)
                                }
                            }

                            TextButton(onClick = { viewModel.blockUser(user.id, false) }) {
                                Text("UNBLOCK MATRIX", color = SoftRed)
                            }
                        }
                        Divider(color = DarkSurfaceVariant)
                    }
                }
            }
        }
    }
}


// ==========================================
// 19. RECEPTIVE SEARCH PANEL SCREEN
// ==========================================
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit,
    onChatSelected: (String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val chats by viewModel.allChats.collectAsState()
    val matchingChats = chats.filter { it.name.lowercase().contains(query.lowercase()) || it.lastMessageText.lowercase().contains(query.lowercase()) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Query network matrices...", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = WhiteSmoke
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_search_input")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
        ) {
            if (matchingChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching node clusters found.", color = TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(matchingChats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChatSelected(chat.id)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CyberAvatar(name = chat.name, size = 48.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(chat.name, color = WhiteSmoke, fontWeight = FontWeight.Bold)
                                Text(chat.lastMessageText, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Divider(color = DarkSurfaceVariant)
                    }
                }
            }
        }
    }
}


// ==========================================
// 20. MEDIA GALLERY VIEW SCREEN
// ==========================================
@Composable
fun MediaGalleryScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val itemsList = listOf(
        "Voice Notes Folder", "Transmitted Blueprints", "Encrypted Video logs",
        "Cybernetic Gifs", "Node Key files", "Audio streams"
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acoustic Media Folder", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(itemsList) { folder ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(0.5.dp, RoyalPurple, RoundedCornerShape(12.dp))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Rounded.FolderSpecial, contentDescription = null, tint = LightRoyalPurple, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(folder, color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Simulated offline cache", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 21. UNIFIED ALERT NOTIFICATIONS DECK
// ==========================================
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    val isAlertsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    var alertVolume by remember { mutableStateOf(0.75f) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteSmoke)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unified Alert Preferences", color = WhiteSmoke, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Global Signal Pulse alerts", color = WhiteSmoke, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Configure foreground and background notifications", color = TextSecondary, fontSize = 11.sp)
                }

                Switch(
                    checked = isAlertsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LightRoyalPurple,
                        checkedTrackColor = MutedPurple
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text("Tonal Decibel Volume", color = LightRoyalPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = alertVolume,
                onValueChange = { alertVolume = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = LightRoyalPurple,
                    activeTrackColor = RoyalPurple,
                    inactiveTrackColor = DarkSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onBackPressed,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
            ) {
                Text("SAVE ALERTS CONFIGURATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}
