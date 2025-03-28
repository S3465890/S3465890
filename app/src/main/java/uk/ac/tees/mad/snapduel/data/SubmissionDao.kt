package uk.ac.tees.mad.snapduel.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submission: Submission)

    @Query("SELECT * FROM submissions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserSubmissions(userId: String): Flow<List<Submission>>
}