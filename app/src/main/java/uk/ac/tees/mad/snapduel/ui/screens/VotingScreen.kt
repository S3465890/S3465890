package uk.ac.tees.mad.snapduel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.snapduel.data.Submission
import uk.ac.tees.mad.snapduel.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val submissions = remember { mutableStateListOf<Submission>() }
    var isLoading by remember { mutableStateOf(true) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {

        listenerRegistration = firestore.collection("submissions")
            .orderBy("votes", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val updatedList = it.documents.mapNotNull { item ->

                        Submission(
                            id = item.id,
                            image = item["image"] as String,
                            latitude = item["latitude"] as Double,
                            longitude = item["longitude"] as Double,
                            timestamp = item["timestamp"] as Long,
                            userId = item["userId"] as String,
                            votes = (item["votes"] as Long).toInt(),
                        )
                    }
                    submissions.clear()
                    submissions.addAll(updatedList)
                    isLoading = false
                }
            }

        onDispose { listenerRegistration?.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Voting Screen", fontSize = 20.sp) }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "back"
                    )
                }
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (submissions.isEmpty()) {
                Text("No submissions yet!", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(submissions) { submission ->
                        SubmissionItem(submission, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionItem(submission: Submission, navController: NavController) {
    val bitmap = decodeBase64ToBitmap(submission.image)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "submission",
                    submission
                )
                navController.navigate(Screen.PhotoDetails.route)
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Submitted on: " + formatTimestamp(submission.timestamp),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Submitted Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Votes: ${submission.votes}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Submitted by: ${submission.userId}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
