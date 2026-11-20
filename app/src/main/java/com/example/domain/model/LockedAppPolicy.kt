package com.example.domain.model

data class LockedAppPolicy(
    val id: String,
    val packageName: String,
    val labelSnapshot: String,
    val startedAt: Long,
    val expiresAt: Long,
    val durationYears: Int,
    val status: LockStatus,
    val integritySignature: String = "",
    val lastVerifiedAt: Long = startedAt
) {
    fun isExpired(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return currentTimeMs >= expiresAt
    }

    fun remainingDays(currentTimeMs: Long = System.currentTimeMillis()): Long {
        val diff = expiresAt - currentTimeMs
        return if (diff > 0) (diff / (1000L * 60 * 60 * 24)) else 0L
    }

    fun progressFraction(currentTimeMs: Long = System.currentTimeMillis()): Float {
        val total = (expiresAt - startedAt).coerceAtLeast(1L)
        val elapsed = (currentTimeMs - startedAt).coerceIn(0L, total)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}


