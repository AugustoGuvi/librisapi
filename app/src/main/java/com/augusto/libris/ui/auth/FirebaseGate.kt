package com.augusto.libris.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augusto.libris.ui.theme.Honey
import com.augusto.libris.ui.theme.Ink

/** Protege toda Libris sin alterar su navegación interna ni sus tres APIs. */
@Composable
fun FirebaseGate(content: @Composable () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val usuario by authViewModel.usuario.collectAsState()

    if (usuario == null) {
        LoginScreen(viewModel = authViewModel)
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usuario?.email.orEmpty(),
                    color = Honey,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = authViewModel::cerrarSesion) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Salir", color = Color.White)
                }
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
