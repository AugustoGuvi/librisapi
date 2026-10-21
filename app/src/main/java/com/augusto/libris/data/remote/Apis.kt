package com.augusto.libris.data.remote

import com.augusto.libris.data.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("printType") printType: String = "books",
        @Query("startIndex") startIndex: Int = 0,
        @Query("key") apiKey: String? = null
    ): GoogleBooksResponseDto
}

interface OpenLibraryApi {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i,language,isbn,publisher,number_of_pages_median",
        @Query("offset") offset: Int = 0
    ): OpenLibraryBooksResponseDto

    @GET("search/authors.json")
    suspend fun searchAuthors(
        @Query("q") name: String,
        @Query("limit") limit: Int = 8
    ): AuthorSearchResponseDto

    @GET("authors/{authorId}.json")
    suspend fun getAuthor(@Path("authorId") authorId: String): AuthorDetailDto

    @GET("authors/{authorId}/works.json")
    suspend fun getWorks(
        @Path("authorId") authorId: String,
        @Query("limit") limit: Int = 16
    ): AuthorWorksResponseDto
}

interface GutendexApi {
    @GET("books/")
    suspend fun searchBooks(@Query("search") query: String): GutendexResponseDto
}
