package com.augusto.libris.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.augusto.libris.ui.theme.Honey
import com.augusto.libris.ui.theme.Ink
import com.augusto.libris.ui.theme.Paper
import com.augusto.libris.ui.theme.Rust

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val estado by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = Ink, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null,
                    tint = Honey, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text("LIBRIS", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black, color = Color.White)
                Text("TU BIBLIOTECA DIGITAL", style = MaterialTheme.typography.labelSmall,
                    color = Honey)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(if (estado.modoRegistro) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("Ingresa para utilizar tu biblioteca digital.",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = estado.correo,
            onValueChange = viewModel::cambiarCorreo,
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(
            value = estado.contrasena,
            onValueChange = viewModel::cambiarContrasena,
            label = { Text("Contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (estado.mostrarContrasena) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = viewModel::alternarContrasena) {
                    Text(if (estado.mostrarContrasena) "Ocultar" else "Mostrar")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        estado.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
        }
        estado.mensaje?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Rust, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::enviar,
            enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Rust),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(if (estado.modoRegistro) "Crear cuenta" else "Entrar",
                    fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = viewModel::cambiarModo, enabled = !estado.cargando) {
            Text(if (estado.modoRegistro) "Ya tengo una cuenta" else "Crear una cuenta")
        }
        if (!estado.modoRegistro) {
            TextButton(onClick = viewModel::recuperarContrasena,
                enabled = !estado.cargando) {
                Text("Olvidé mi contraseña")
            }
        }
    }
}
