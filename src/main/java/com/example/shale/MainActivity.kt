package com.example.shale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shale.ui.screens.MealScreen
import com.example.shale.ui.screens.FacilityScreen
import com.example.shale.ui.screens.FeedbackScreen
import com.example.shale.ui.screens.StudentStarsScreen
import com.example.shale.ui.screens.AdminLoginScreen
import com.example.shale.ui.theme.ShaleNammaPrideTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shale.viewmodel.MainViewModel

import com.example.shale.ui.screens.SchoolSelectionScreen
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private val currentUserState = mutableStateOf<FirebaseUser?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        currentUserState.value = auth.currentUser

        setContent {
            ShaleNammaPrideTheme {
                MainScreen(
                    currentUser = currentUserState.value,
                    onAdminSignIn = ::signInWithGoogle,
                    onSignOut = ::signOut
                )
            }
        }
    }

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                handleGoogleCredential(result.credential)
            } catch (error: Exception) {
                currentUserState.value = auth.currentUser
            }
        }
    }

    private fun handleGoogleCredential(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) {
                    currentUserState.value = auth.currentUser
                }
        }
    }

    private fun signOut() {
        auth.signOut()
        currentUserState.value = null
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentUser: FirebaseUser?,
    onAdminSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val navController = rememberNavController()
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    var selectedRole by remember { mutableStateOf<AppRole?>(null) }
    var language by remember { mutableStateOf("en") }
    
    val items = listOf(
        NavigationItem(if (language == "kn") "ಊಟ" else "Meal", Icons.Default.Home, "meal"),
        NavigationItem(if (language == "kn") "ಸೌಲಭ್ಯ" else "Facilities", Icons.Default.Home, "facilities"),
        NavigationItem(if (language == "kn") "ಸ್ಟಾರ್ಸ್" else "Stars", Icons.Default.Star, "stars"),
        NavigationItem(if (language == "kn") "ಸಲಹೆ" else "Feedback", Icons.Default.Favorite, "feedback")
    )
    
    var selectedItem by remember { mutableStateOf(0) }

    if (selectedRole == null && selectedSchool == null) {
        LandingScreen(
            language = language,
            onToggleLanguage = { language = if (language == "en") "kn" else "en" },
            onRoleSelected = { selectedRole = it }
        )
    } else if (selectedRole == AppRole.Admin && currentUser == null) {
        AdminSignInGate(
            language = language,
            onBack = { selectedRole = null },
            onSignIn = onAdminSignIn
        )
    } else if (selectedRole == AppRole.Admin && selectedSchool == null) {
        AdminLoginScreen(
            viewModel = viewModel,
            language = language,
            onToggleLanguage = { language = if (language == "en") "kn" else "en" },
            onClose = { selectedRole = null },
            onSignOut = {
                onSignOut()
                viewModel.clearSchool()
                selectedRole = null
            },
            adminEmail = currentUser?.email,
            adminUid = currentUser?.uid
        )
    } else if (selectedSchool == null) {
        SchoolSelectionScreen(
            viewModel = viewModel,
            isAdminMode = selectedRole == AppRole.Admin,
            language = language,
            onToggleLanguage = { language = if (language == "en") "kn" else "en" },
            onBack = { selectedRole = null },
            onSchoolSelected = { viewModel.selectSchool(it) }
        )
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        LaunchedEffect(currentRoute) {
            val routeIndex = items.indexOfFirst { it.route == currentRoute }
            if (routeIndex >= 0) selectedItem = routeIndex
        }
        val sectionColor = listOf(
            Color(0xFFF97316),
            Color(0xFF4F46E5),
            Color(0xFFB45309),
            Color(0xFF0F766E)
        )[selectedItem]
        val animatedSectionColor by animateColorAsState(
            targetValue = sectionColor,
            animationSpec = tween(300),
            label = "section-color"
        )

        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                viewModel.clearSchool()
                                selectedItem = 0
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(onClick = { language = if (language == "en") "kn" else "en" }) {
                                Text(if (language == "en") "ಕನ್ನಡ" else "English", color = animatedSectionColor, fontWeight = FontWeight.Bold)
                            }
                            if (selectedRole == AppRole.Admin) {
                                IconButton(onClick = { navController.navigate("admin_login") }) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Admin")
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                selectedSchool?.displayName(language) ?: "School",
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                selectedSchool?.displayLocation(language) ?: "",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = animatedSectionColor
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = animatedSectionColor,
                    tonalElevation = 8.dp
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp) }, // Smaller labels for mobile
                            selected = currentRoute == item.route || (currentRoute == null && selectedItem == index),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = animatedSectionColor,
                                selectedTextColor = Color.White,
                                indicatorColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.72f),
                                unselectedTextColor = Color.White.copy(alpha = 0.72f)
                            ),
                            onClick = {
                                selectedItem = index
                                navController.navigate(item.route) {
                                    popUpTo("meal") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(navController = navController, startDestination = "meal") {
                    composable("meal") { MealScreen(viewModel, language) }
                    composable("facilities") { FacilityScreen(viewModel, language) }
                    composable("stars") { StudentStarsScreen(viewModel, language) }
                    composable("feedback") { FeedbackScreen(viewModel, language) }
                    composable("admin_login") {
                        if (selectedRole == AppRole.Admin) {
                            AdminLoginScreen(viewModel = viewModel, language = language, onToggleLanguage = {
                                language = if (language == "en") "kn" else "en"
                            }, onClose = {
                                navController.popBackStack()
                            }, onSignOut = {
                                onSignOut()
                                navController.popBackStack()
                                viewModel.clearSchool()
                                selectedRole = null
                            }, adminEmail = currentUser?.email, adminUid = currentUser?.uid)
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppRole { Admin, User }

data class NavigationItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AdminSignInGate(
    language: String,
    onBack: () -> Unit,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.widthIn(max = 420.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (language == "kn") "Google ಮೂಲಕ ನಿರ್ವಾಹಕ ಲಾಗಿನ್" else "Admin Google Login",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (language == "kn") "ಶಾಲೆಯ ಊಟ, ಸೌಲಭ್ಯಗಳು ಮತ್ತು ವಿದ್ಯಾರ್ಥಿ ಸಾಧನೆಗಳನ್ನು ನವೀಕರಿಸಲು Google ಮೂಲಕ ಸೈನ್ ಇನ್ ಮಾಡಿ."
                    else "Sign in with Google to update meals, facilities, student stars, and review feedback.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(if (language == "kn") "Google ಮೂಲಕ ಸೈನ್ ಇನ್" else "Sign in with Google")
                }
                TextButton(onClick = onBack) {
                    Text(if (language == "kn") "ಹಿಂದೆ" else "Back")
                }
            }
        }
    }
}

@Composable
fun LandingScreen(
    language: String,
    onToggleLanguage: () -> Unit,
    onRoleSelected: (AppRole) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AssistChip(onClick = onToggleLanguage, label = { Text(if (language == "en") "ಕನ್ನಡ" else "English") })
            }

            Surface(
                modifier = Modifier.size(96.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("S", color = MaterialTheme.colorScheme.onPrimary, fontSize = 44.sp, fontWeight = FontWeight.Black)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (language == "kn") "ಶಾಲೆ ನಮ್ಮ ಪ್ರೈಡ್" else "Shale Namma Pride", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(
                    if (language == "kn") "ಕರ್ನಾಟಕ ಶಾಲೆಗಳಿಗಾಗಿ ಆತ್ಮೀಯ ಅಪ್ಡೇಟ್‌ಗಳು" else "School updates for Karnataka communities",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RoleCard(
                title = if (language == "kn") "ನಿರ್ವಾಹಕ" else "Admin",
                subtitle = if (language == "kn") "ಶಾಲೆಯ ಮಾಹಿತಿ ಮತ್ತು ಅಪ್ಡೇಟ್‌ಗಳನ್ನು ನಿರ್ವಹಿಸಿ" else "Manage your school profile and updates",
                icon = Icons.Default.AccountCircle,
                emphasized = true,
                onClick = { onRoleSelected(AppRole.Admin) }
            )
            RoleCard(
                title = if (language == "kn") "ಪೋಷಕರು ಅಥವಾ ವಿದ್ಯಾರ್ಥಿ" else "Parent or Student",
                subtitle = if (language == "kn") "ಊಟ, ಸೌಲಭ್ಯಗಳು, ಸಾಧನೆಗಳು ಮತ್ತು ಸಲಹೆಗಳನ್ನು ನೋಡಿ" else "View meals, facilities, student stars, and feedback",
                icon = Icons.Default.Home,
                emphasized = false,
                onClick = { onRoleSelected(AppRole.User) }
            )
            Text(
                if (language == "kn") "ಕರ್ನಾಟಕ ಶಾಲೆಗಳಿಗಾಗಿ ಪ್ರೀತಿಯಿಂದ ನಿರ್ಮಿಸಲಾಗಿದೆ" else "Made with care for Karnataka Schools",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (emphasized) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = contentColor.copy(alpha = 0.75f), fontSize = 13.sp)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = contentColor)
        }
    }
}
