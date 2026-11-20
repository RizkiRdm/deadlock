package com.example

import com.example.domain.model.LockStatus
import com.example.domain.model.LockedAppPolicy
import com.example.security.PolicyIntegrityManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeadlockPolicyUnitTest {


    private lateinit var integrityManager: PolicyIntegrityManager

    @Before
    fun setUp() {
        integrityManager = PolicyIntegrityManager()
    }

    @Test
    fun calculateExpiration_oneYear_isCorrect() {
        val now = 1700000000000L // arbitrary fixed time
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, 1)
        }
        val expectedExpiresAt = calendar.timeInMillis

        val policy = LockedAppPolicy(
            id = "test-id-1",
            packageName = "com.instagram.android",
            labelSnapshot = "Instagram",
            startedAt = now,
            expiresAt = expectedExpiresAt,
            durationYears = 1,
            status = LockStatus.ACTIVE
        )

        assertFalse(policy.isExpired(currentTimeMs = now))
        assertFalse(policy.isExpired(currentTimeMs = now + 1000L * 60 * 60 * 24 * 364))
        assertTrue(policy.isExpired(currentTimeMs = expectedExpiresAt))
        assertTrue(policy.isExpired(currentTimeMs = expectedExpiresAt + 1000L))
    }

    @Test
    fun calculateExpiration_twoYears_isCorrect() {
        val now = 1700000000000L
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, 2)
        }
        val expectedExpiresAt = calendar.timeInMillis

        val policy = LockedAppPolicy(
            id = "test-id-2",
            packageName = "com.android.chrome",
            labelSnapshot = "Chrome",
            startedAt = now,
            expiresAt = expectedExpiresAt,
            durationYears = 2,
            status = LockStatus.ACTIVE
        )

        val remainingDaysAtStart = policy.remainingDays(currentTimeMs = now)
        assertTrue(remainingDaysAtStart >= 729)
    }

    @Test
    fun packageIdentity_isCanonicalIdentifier() {
        val targetPackage = "com.zhiliaoapp.musically"
        val label = "TikTok Video Platform"

        val policy = LockedAppPolicy(
            id = "uuid-123",
            packageName = targetPackage,
            labelSnapshot = label,
            startedAt = 1000L,
            expiresAt = 10000L,
            durationYears = 1,
            status = LockStatus.ACTIVE
        )

        assertEquals(targetPackage, policy.packageName)
        assertNotEquals(label, policy.packageName)
    }

    @Test
    fun policyIntegritySignature_detectsTampering() {
        val policy = LockedAppPolicy(
            id = "policy-abc",
            packageName = "com.example.target",
            labelSnapshot = "Target App",
            startedAt = 100000L,
            expiresAt = 200000L,
            durationYears = 1,
            status = LockStatus.ACTIVE
        )

        val signature = integrityManager.signPolicy(policy)
        assertTrue(signature.isNotEmpty())

        val signedPolicy = policy.copy(integritySignature = signature)
        assertTrue(integrityManager.verifyPolicy(signedPolicy))

        // Attempting to tamper with expiration date locally
        val tamperedPolicy = signedPolicy.copy(expiresAt = 100001L)
        assertFalse(integrityManager.verifyPolicy(tamperedPolicy))

        // Attempting to tamper with package name
        val tamperedPkg = signedPolicy.copy(packageName = "com.other.app")
        assertFalse(integrityManager.verifyPolicy(tamperedPkg))
    }

    @Test
    fun progressFraction_isBoundedBetweenZeroAndOne() {
        val startedAt = 1000L
        val expiresAt = 3000L

        val policy = LockedAppPolicy(
            id = "test-id",
            packageName = "com.example.app",
            labelSnapshot = "App",
            startedAt = startedAt,
            expiresAt = expiresAt,
            durationYears = 1,
            status = LockStatus.ACTIVE
        )

        assertEquals(0f, policy.progressFraction(currentTimeMs = 500L), 0.001f)
        assertEquals(0.5f, policy.progressFraction(currentTimeMs = 2000L), 0.001f)
        assertEquals(1f, policy.progressFraction(currentTimeMs = 3000L), 0.001f)
        assertEquals(1f, policy.progressFraction(currentTimeMs = 4000L), 0.001f)
    }

    @Test
    fun policySerialization_roundTrip_preservesSignature() {
        val policy = LockedAppPolicy(
            id = "vault-policy-1",
            packageName = "com.twitter.android",
            labelSnapshot = "X / Twitter",
            startedAt = 1000000L,
            expiresAt = 2000000L,
            durationYears = 1,
            status = LockStatus.ACTIVE
        )

        val signature = integrityManager.signPolicy(policy)
        val signedPolicy = policy.copy(integritySignature = signature)

        val obj = org.json.JSONObject().apply {
            put("id", signedPolicy.id)
            put("packageName", signedPolicy.packageName)
            put("labelSnapshot", signedPolicy.labelSnapshot)
            put("startedAt", signedPolicy.startedAt)
            put("expiresAt", signedPolicy.expiresAt)
            put("durationYears", signedPolicy.durationYears)
            put("status", signedPolicy.status.name)
            put("integritySignature", signedPolicy.integritySignature)
            put("lastVerifiedAt", signedPolicy.lastVerifiedAt)
        }

        val decoded = LockedAppPolicy(
            id = obj.getString("id"),
            packageName = obj.getString("packageName"),
            labelSnapshot = obj.getString("labelSnapshot"),
            startedAt = obj.getLong("startedAt"),
            expiresAt = obj.getLong("expiresAt"),
            durationYears = obj.getInt("durationYears"),
            status = LockStatus.valueOf(obj.getString("status")),
            integritySignature = obj.getString("integritySignature"),
            lastVerifiedAt = obj.getLong("lastVerifiedAt")
        )

        assertEquals(signedPolicy, decoded)
        assertTrue(integrityManager.verifyPolicy(decoded))
    }
}

