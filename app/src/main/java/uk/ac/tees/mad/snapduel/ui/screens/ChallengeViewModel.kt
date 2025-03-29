package uk.ac.tees.mad.snapduel.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.ac.tees.mad.snapduel.data.AppDatabase
import uk.ac.tees.mad.snapduel.data.Submission
import java.util.UUID

class ChallengeViewModel(private val application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val submissionDao = AppDatabase.getInstance(application).submissionDao()

    private val _dailyPrompt = MutableStateFlow("Something Blue")
    val dailyPrompt = _dailyPrompt.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val userId = auth.currentUser?.uid ?: ""

    val userSubmissions = submissionDao.getUserSubmissions(userId).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun submitPhoto(base64Image: String?, locationLatitude: Double?, locationLongitude: Double?) {
        if (base64Image == null || locationLatitude == null || locationLongitude == null) return

        _isSubmitting.value = true
        val submission = Submission(
            id = UUID.randomUUID().toString(),
            image = base64Image,
            latitude = locationLatitude,
            longitude = locationLongitude,
            timestamp = System.currentTimeMillis(),
            userId = userId
        )

        viewModelScope.launch {
            submissionDao.insert(submission)
            firestore.collection("submissions").add(submission)
                .addOnSuccessListener {
                    _isSubmitting.value = false

                    Toast.makeText(
                        application.applicationContext,
                        "Submission successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    _isSubmitting.value = false
                    Toast.makeText(
                        application.applicationContext,
                        "Submission failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
