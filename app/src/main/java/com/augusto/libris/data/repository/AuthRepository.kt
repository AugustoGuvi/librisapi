package com.augusto.libris.data.repository

import com.augusto.libris.model.Usuario
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

sealed interface ResultadoAuth {
    data object Exito : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

/** Única capa de Libris que conoce FirebaseAuth. */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun usuarioActual(): Usuario? = auth.currentUser?.let { firebaseUser ->
        Usuario(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty())
    }

    fun observarSesion(): Flow<Usuario?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { instancia ->
            val usuario = instancia.currentUser?.let {
                Usuario(uid = it.uid, email = it.email.orEmpty())
            }
            trySend(usuario)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAuth =
        ejecutar { auth.signInWithEmailAndPassword(correo, contrasena).await() }

    suspend fun registrar(correo: String, contrasena: String): ResultadoAuth =
        ejecutar { auth.createUserWithEmailAndPassword(correo, contrasena).await() }

    suspend fun recuperarContrasena(correo: String): ResultadoAuth =
        ejecutar { auth.sendPasswordResetEmail(correo).await() }

    fun cerrarSesion() = auth.signOut()

    private suspend fun ejecutar(operacion: suspend () -> Unit): ResultadoAuth = try {
        operacion()
        ResultadoAuth.Exito
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (error: Exception) {
        ResultadoAuth.Error(traducirError(error))
    }

    private fun traducirError(error: Exception): String = when (error) {
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usa al menos 6 caracteres."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseNetworkException -> "No hay conexión a internet. Comprueba tu red."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Vuelve a probar más tarde."
        else -> "No se pudo completar la operación. Inténtalo nuevamente."
    }
}
