
package com.augusto.libris.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class CloudFavorite(
    val bookId: String,
    val title: String,
    val authorLabel: String,
    val coverUrl: String?,
    val bookJson: String,
    val addedAt: Long,
    val note: String?
)

data class CloudFavoritesResponse(
    val data: List<CloudFavorite>
)

data class CloudActionResult(
    val bookId: String,
    val title: String? = null,
    val note: String? = null
)

data class CloudActionResponse(
    val data: List<CloudActionResult>
)

data class DeleteFavoriteRequest(
    val bookId: String
)

data class UpdateNoteRequest(
    val bookId: String,
    val note: String?
)

interface LibrisCloudApi {

    // Obtener favoritos
    @POST("v2/functions/listar-favoritos")
    suspend fun listarFavoritos(
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String> = emptyMap()
    ): CloudFavoritesResponse

    // Guardar favorito
    @POST("v2/functions/guardar-favorito")
    suspend fun guardarFavorito(
        @Header("Authorization") authorization: String,
        @Body favorito: CloudFavorite
    ): CloudActionResponse

    // Eliminar favorito
    @POST("v2/functions/eliminar-favorito")
    suspend fun eliminarFavorito(
        @Header("Authorization") authorization: String,
        @Body request: DeleteFavoriteRequest
    ): CloudActionResponse

    // Actualizar nota
    @POST("v2/functions/actualizar-nota")
    suspend fun actualizarNota(
        @Header("Authorization") authorization: String,
        @Body request: UpdateNoteRequest
    ): CloudActionResponse
}
