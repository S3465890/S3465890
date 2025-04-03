package uk.ac.tees.mad.snapduel.ui.screens

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.firestore.FirebaseFirestore
import uk.ac.tees.mad.snapduel.data.Submission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailsScreen(
    navController: NavController,
    submission: Submission
) {
    var votes by remember { mutableStateOf(submission.votes) }
    val context = LocalContext.current


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display Image
            Image(
                bitmap = decodeBase64ToBitmap(submission.image).asImageBitmap(),
                contentDescription = "Submitted Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submission Details
            Text(
                text = "Submitted on: ${
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(Date(submission.timestamp))
                }",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "User ID: ${submission.userId}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (submission.latitude != null && submission.longitude != null) {
                val location = getLocationFromLatlng(
                    context,
                    submission.latitude,
                    submission.longitude
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (location.isEmpty()) {
                    LinearProgressIndicator()
                } else {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            // Submission Details
            Text(
                text = "Votes: $votes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Voting Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(onClick = {
                    updateVote(
                        submission.id,
                        true
                    ) { success ->
                        if (success) votes += 1
                    }
                }, colors = ButtonDefaults.buttonColors(Color.Green)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Upvote")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upvote")
                }

                Button(onClick = {
                    updateVote(submission.id, false) { success ->
                        if (success) votes -= 1
                    }
                }, colors = ButtonDefaults.buttonColors(Color.Red)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Downvote")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Downvote")
                }
            }
        }
    }
}

fun getLocationFromLatlng(context: Context, latitude: Double, longitude: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            "${address.locality}, ${address.countryName}"
        } else {
            "Unknown Location"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Location not available"
    }
}

fun updateVote(submissionId: String, isUpvote: Boolean, onComplete: (Boolean) -> Unit) {

    val db = FirebaseFirestore.getInstance()
    val submissionsRef = db.collection("submissions")

    val docRef = submissionsRef.document(submissionId)

    db.runTransaction { transaction ->
        println("r called")
        println(submissionId)
        val snapshot = transaction.get(docRef)
        val currentVotes = snapshot.getLong("votes") ?: 0
        val newVotes = if (isUpvote) currentVotes + 1 else currentVotes - 1
        transaction.update(docRef, "votes", newVotes)
    }.addOnSuccessListener {
        println("c called")
        onComplete(true)
    }.addOnFailureListener {
        it.printStackTrace()
        onComplete(false)
    }
}