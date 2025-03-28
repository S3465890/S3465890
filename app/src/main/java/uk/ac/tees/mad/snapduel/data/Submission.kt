package uk.ac.tees.mad.snapduel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "submissions")
data class Submission(
    @PrimaryKey val id: String,
    val image: String,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val userId: String
)