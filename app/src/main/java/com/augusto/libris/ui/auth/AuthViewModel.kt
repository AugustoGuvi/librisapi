package com.augusto.libris.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augusto.libris.data.repository.AuthRepository
import com.augusto.libris.data.repository.ResultadoAuth
import com.augusto.libris.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val correo: String = "",
    val contrasena: String = "",
    val modoRegistro: Boolean = false,
    val mostrarContrasena: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class AuthViewModel : ViewModel() {
    private val repositorio = AuthRepository()
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    // Valor inicial sincrónico: no se muestra el login fugazmente al reabrir la app.
    val usuario: StateFlow<Usuario?> = repositorio.observarSesion()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repositorio.usuarioActual())

    fun cambiarCorreo(valor: String) = _uiState.update {
        it.copy(correo = valor, error = null, mensaje = null)
    }

    fun cambiarContrasena(valor: String) = _uiState.update {
        it.copy(contrasena = valor, error = null, mensaje = null)
    }

    fun cambiarModo() = _uiState.update {
        it.copy(modoRegistro = !it.modoRegistro, error = null, mensaje = null, contrasena = "")
    }

    fun alternarContrasena() = _uiState.update {
        it.copy(mostrarContrasena = !it.mostrarContrasena)
    }

    fun enviar() {
        val estado = _uiState.value
        if (estado.cargando) return
        val correo = estado.correo.trim()
        validar(correo, estado.contrasena)?.let { error ->
            _uiState.update { it.copy(error = error, mensaje = null) }
            return
        }
        _uiState.update { it.copy(cargando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            val resultado = if (estado.modoRegistro) {
                repositorio.registrar(correo, estado.contrasena)
            } else {
                repositorio.iniciarSesion(correo, estado.contrasena)
            }
            _uiState.update {
                when (resultado) {
                    ResultadoAuth.Exito -> it.copy(cargando = false, contrasena = "", error = null)
                    is ResultadoAuth.Error -> it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun recuperarContrasena() {
        if (_uiState.value.cargando) return
        val correo = _uiState.value.correo.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            _uiState.update { it.copy(error = "Escribe un correo válido para recuperar la contraseña.") }
            return
        }
        _uiState.update { it.copy(cargando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = repositorio.recuperarContrasena(correo)) {
                ResultadoAuth.Exito -> _uiState.update {
                    it.copy(cargando = false, mensaje = "Si ese correo tiene una cuenta, recibirás instrucciones de recuperación.")
                }
                is ResultadoAuth.Error -> _uiState.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarSesion() {
        repositorio.cerrarSesion()
        _uiState.value = AuthUiState()
    }

    private fun validar(correo: String, contrasena: String): String? = when {
        !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Escribe un correo válido."
        contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        else -> null
    }
}
