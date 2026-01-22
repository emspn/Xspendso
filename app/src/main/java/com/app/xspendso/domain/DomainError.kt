package com.app.xspendso.domain

sealed class DomainError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    sealed class SyncError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        object SmsReadPermissionDenied : SyncError("SMS read permission not granted")
        object DatabaseWriteError : SyncError("Failed to save transactions to database")
        object NetworkError : SyncError("Network connection failed")
        object FirestoreError : SyncError("Cloud sync failed")
        data class UnexpectedSyncError(val e: Throwable) : SyncError("An unexpected error occurred during sync", e)
    }

    sealed class ExportError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        object StoragePermissionDenied : ExportError("Storage permission not granted")
        object FileCreationError : ExportError("Failed to create export file")
        data class UnexpectedExportError(val e: Throwable) : ExportError("An unexpected error occurred during export", e)
    }

    sealed class AuthError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        object InvalidCredentials : AuthError("Invalid username or password")
        object NetworkError : AuthError("Network connection failed")
        data class UnexpectedAuthError(val e: Throwable) : AuthError("An unexpected authentication error occurred", e)
    }
}

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val error: DomainError) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
