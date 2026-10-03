package com.burton.photos.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.ui.theme.BurtonDanger
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connect a server", color = BurtonIvory)
        Spacer(Modifier.height(8.dp))
        Text(
            "Optional. Your camera roll already works. Add a Burton Photos origin when you have one.",
            color = BurtonMute,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = ui.baseUrl,
            onValueChange = viewModel::setBaseUrl,
            label = { Text("Server URL") },
            placeholder = { Text("https://photos.example") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::probe,
            enabled = !ui.probing && ui.baseUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.probing) "Checking…" else "Use this server")
        }
        if (ui.config != null && !ui.config!!.autoLogin) {
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = ui.email,
                onValueChange = viewModel::setEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.password,
                onValueChange = viewModel::setPassword,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::login,
                enabled = !ui.submitting && ui.email.isNotBlank() && ui.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.submitting) "Signing in…" else "Sign in")
            }
            if (ui.config?.register == true) {
                TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                    Text("Create an account")
                }
            }
        }
        ui.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = BurtonDanger)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Not now")
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create account", color = BurtonIvory)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = ui.baseUrl,
            onValueChange = viewModel::setBaseUrl,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.name,
            onValueChange = viewModel::setName,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.email,
            onValueChange = viewModel::setEmail,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.password,
            onValueChange = viewModel::setPassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::register,
            enabled = !ui.submitting && ui.email.isNotBlank() && ui.password.length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.submitting) "Creating…" else "Create account")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to sign in")
        }
        ui.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = BurtonDanger)
        }
    }
}
