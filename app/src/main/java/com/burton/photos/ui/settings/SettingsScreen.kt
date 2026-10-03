package com.burton.photos.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.photos.BuildConfig
import com.burton.photos.ui.theme.BurtonIvory
import com.burton.photos.ui.theme.BurtonMute

@Composable
fun SettingsScreen(
    onConnect: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val local by viewModel.local.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Settings", color = BurtonIvory)
        Spacer(Modifier.height(16.dp))
        if (local) {
            Text("On this phone", color = BurtonIvory)
            Text(
                "You're browsing the camera roll. Connect a Burton Photos server whenever you want a shared library.",
                color = BurtonMute,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                Text("Connect to a server")
            }
        } else {
            Text(user?.name ?: "—", color = BurtonIvory)
            Text(user?.email ?: "", color = BurtonMute)
            Text(
                "Mode: ${mode.ifBlank { "accounts" }}",
                color = BurtonMute,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(viewModel.origin(), color = BurtonMute, modifier = Modifier.padding(top = 4.dp))
            health?.let { status ->
                Spacer(Modifier.height(16.dp))
                Text(if (status.ok) "API healthy" else "API degraded", color = BurtonIvory)
                status.checks.forEach { (name, ok) ->
                    Text("$name: ${if (ok) "ok" else "fail"}", color = BurtonMute)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                Text("Disconnect server")
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Version ${BuildConfig.VERSION_NAME}", color = BurtonMute)
    }
}
