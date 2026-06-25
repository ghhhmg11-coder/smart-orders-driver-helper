package com.smartorders.driverhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.ui.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppPreferences.isLoggedIn(this)) {
            goToMain()
            return
        }

        setContent {
            SmartOrdersTheme {
                LoginScreen(
                    onLogin = { username, password ->
                        if (username == "admin" && password == "1234") {
                            AppPreferences.setLoggedIn(this, true)
                            goToMain()
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Boolean) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Color(0xFF1A0533))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // App logo area
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Purple80, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🚗", fontSize = 40.sp)
            }

            Text(
                text = "Smart Orders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Driver Helper",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; error = "" },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple60,
                    focusedLabelColor = Purple60,
                    focusedLeadingIconColor = Purple60,
                    unfocusedBorderColor = SurfaceVariant,
                    unfocusedLabelColor = OnSurfaceVariant,
                    cursorColor = Purple60,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = "" },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple60,
                    focusedLabelColor = Purple60,
                    focusedLeadingIconColor = Purple60,
                    unfocusedBorderColor = SurfaceVariant,
                    unfocusedLabelColor = OnSurfaceVariant,
                    cursorColor = Purple60,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = RedAccent,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val success = onLogin(username.trim(), password.trim())
                    if (!success) error = "❌ Wrong username or password"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Default: admin / 1234",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
