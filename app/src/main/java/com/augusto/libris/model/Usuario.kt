package com.augusto.libris.model

/** Modelo de dominio: la interfaz no depende de FirebaseUser. */
data class Usuario(
    val uid: String,
    val email: String
)
