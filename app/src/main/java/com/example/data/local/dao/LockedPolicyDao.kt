package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LockedPolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedPolicyDao {
    @Query("SELECT * FROM locked_policies ORDER BY startedAt DESC")
    fun getAllPolicies(): Flow<List<LockedPolicyEntity>>

    @Query("SELECT * FROM locked_policies ORDER BY startedAt DESC")
    suspend fun getAllPoliciesList(): List<LockedPolicyEntity>

    @Query("SELECT * FROM locked_policies WHERE packageName = :packageName LIMIT 1")
    suspend fun getPolicyByPackageName(packageName: String): LockedPolicyEntity?

    @Query("SELECT * FROM locked_policies WHERE id = :id LIMIT 1")
    suspend fun getPolicyById(id: String): LockedPolicyEntity?

    @Query("SELECT * FROM locked_policies WHERE status = 'ACTIVE'")
    suspend fun getActivePoliciesList(): List<LockedPolicyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: LockedPolicyEntity)

    @Update
    suspend fun updatePolicy(policy: LockedPolicyEntity)
}
