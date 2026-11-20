package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.LockStatus
import com.example.domain.model.LockedAppPolicy

@Entity(tableName = "locked_policies")
data class LockedPolicyEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val labelSnapshot: String,
    val startedAt: Long,
    val expiresAt: Long,
    val durationYears: Int,
    val status: String,
    val integritySignature: String,
    val lastVerifiedAt: Long
) {
    fun toDomain(): LockedAppPolicy {
        val parsedStatus = try {
            LockStatus.valueOf(status)
        } catch (e: Exception) {
            LockStatus.INVALID
        }
        return LockedAppPolicy(
            id = id,
            packageName = packageName,
            labelSnapshot = labelSnapshot,
            startedAt = startedAt,
            expiresAt = expiresAt,
            durationYears = durationYears,
            status = parsedStatus,
            integritySignature = integritySignature,
            lastVerifiedAt = lastVerifiedAt
        )
    }

    companion object {
        fun fromDomain(domain: LockedAppPolicy): LockedPolicyEntity {
            return LockedPolicyEntity(
                id = domain.id,
                packageName = domain.packageName,
                labelSnapshot = domain.labelSnapshot,
                startedAt = domain.startedAt,
                expiresAt = domain.expiresAt,
                durationYears = domain.durationYears,
                status = domain.status.name,
                integritySignature = domain.integritySignature,
                lastVerifiedAt = domain.lastVerifiedAt
            )
        }
    }
}
