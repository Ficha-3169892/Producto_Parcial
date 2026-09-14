package com.example.ctma.auth

interface TokenProvider {
    suspend fun obtenerToken(): String?
}

class SessionTokenProvider : TokenProvider {
    private var tokenSesion: String? = null

    fun guardarToken(token: String) {
        tokenSesion = token
    }

    override suspend fun obtenerToken(): String? {
        return tokenSesion
    }
}
