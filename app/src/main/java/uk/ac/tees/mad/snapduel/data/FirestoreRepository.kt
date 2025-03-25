package uk.ac.tees.mad.snapduel.data

import android.graphics.Bitmap
import com.google.firebase.firestore.FirebaseFirestore
import uk.ac.tees.mad.snapduel.utils.bitmapToBase64

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    fun uploadPhoto(bitmap: Bitmap, prompt: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val base64Image = bitmapToBase64(bitmap)
        val photoData = hashMapOf(
            "prompt" to prompt,
            "imageBase64" to base64Image,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("submissions")
            .add(photoData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }
}
