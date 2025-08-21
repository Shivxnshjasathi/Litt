package com.example.lilt

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.lilt.ui.theme.AppTypography
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// A reusable background composable to maintain design consistency
@Composable
fun AuthBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.landingscreenbg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            // ContentScale.Crop ensures the image fills the screen, cropping as needed.
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
        )
        content()
    }
}

// Enum to manage the current state of the Auth screen
enum class AuthMode {
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // Observe state from the ViewModel
    val authResult by authViewModel.authResult.observeAsState()
    val error by authViewModel.error.observeAsState()
    val successMessage by authViewModel.successMessage.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)

    // State for UI elements
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }

    // State for Snackbar feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle successful authentication
    LaunchedEffect(authResult) {
        if (authResult?.user != null) {
            navController.navigate("landingScreen") {
                popUpTo("auth") { inclusive = true } // Navigate and clear back stack
            }
        }
    }

    // Handle and display errors in a Snackbar
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            authViewModel.clearMessages() // Clear the error after showing it
        }
    }

    // Handle and display success messages in a Snackbar
    LaunchedEffect(successMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            authViewModel.clearMessages() // Clear the message after showing it
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        // By removing the padding from the AuthBackground, it will draw behind the status bar.
        AuthBackground {
            // The main content Column still uses the padding to avoid overlapping with system elements.
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding here instead of on the background
                    .padding(horizontal = 32.dp), // Keep your horizontal padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // UI IMPROVEMENT: Replaced Icon with a circular Image
//                Image(
//                    painter = painterResource(id = R.drawable.photo),
//                    contentDescription = "App Logo",
//                    modifier = Modifier
//                        .size(100.dp)
//                        .clip(CircleShape),
//                    contentScale = ContentScale.Crop
//                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (authMode == AuthMode.LOGIN) "Welcome Back" else "Create Account",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary,
                    style = AppTypography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Login to continue" else "Sign up to get started",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    style = AppTypography.bodyLarge
                )
                Spacer(modifier = Modifier.height(48.dp))

                // Email Input Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50), // UI IMPROVEMENT: Rounded corners
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Filled.Email, contentDescription = "Email Icon") },
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = MaterialTheme.colorScheme.secondary,
                        unfocusedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        // UI IMPROVEMENT: Made background semi-transparent and removed border
                        focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password Input Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50), // UI IMPROVEMENT: Rounded corners
                    singleLine = true,
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = "Password Icon") },
                    trailingIcon = {
                        val image = if (passwordVisibility)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                            Icon(imageVector = image, contentDescription = "Toggle password visibility")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        // UI IMPROVEMENT: Made text and icons clearly visible
                        focusedTextColor = MaterialTheme.colorScheme.secondary,
                        unfocusedTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Loading indicator or Auth Buttons
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Crossfade(targetState = authMode, label = "auth_button_crossfade") { mode ->
                        when (mode) {
                            AuthMode.LOGIN -> Button(
                                onClick = { authViewModel.login(email, password) },
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Login")
                            }
                            AuthMode.SIGNUP -> Button(
                                onClick = { authViewModel.signUp(email, password) },
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Sign Up")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Toggle between Login/Sign Up and Forgot Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                    }) {
                        Text(
                            text = if (authMode == AuthMode.LOGIN) "Create an account" else "Have an account? Login",
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    if (authMode == AuthMode.LOGIN) {
                        TextButton(onClick = { authViewModel.forgotPassword(email) }) {
                            Text("Forgot Password?")
                        }
                    }
                }
            }
        }
    }
}

// ViewModel to handle all Firebase Authentication logic
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _authResult = MutableLiveData<com.google.firebase.auth.AuthResult?>()
    val authResult: LiveData<com.google.firebase.auth.AuthResult?> = _authResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty."
            return
        }
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = task.result
                } else {
                    _error.value = task.exception?.message
                }
                _isLoading.value = false
            }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password cannot be empty."
            return
        }
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = task.result
                } else {
                    _error.value = task.exception?.message
                }
                _isLoading.value = false
            }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _error.value = "Please enter your email to reset the password."
            return
        }
        _isLoading.value = true
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _successMessage.value = "Password reset email sent."
                } else {
                    _error.value = task.exception?.message
                }
                _isLoading.value = false
            }
    }

    // Clears message states after they have been displayed
    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}
