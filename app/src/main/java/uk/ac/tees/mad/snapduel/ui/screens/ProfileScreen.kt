package uk.ac.tees.mad.snapduel.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import uk.ac.tees.mad.snapduel.data.Submission
import uk.ac.tees.mad.snapduel.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val submissions = remember { mutableStateListOf<Submission>() }
    var totalVotes by remember { mutableStateOf(0) }
    val db = FirebaseFirestore.getInstance()

    val userId = Firebase.auth.currentUser?.uid

    // Fetch user submissions and calculate total votes
    DisposableEffect(userId) {
        val listenerRegistration = db.collection("submissions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val userSubmissions = it.documents.mapNotNull { item ->
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
                    submissions.addAll(userSubmissions)

                    // Calculate total votes
                    totalVotes = userSubmissions.sumOf { it.votes }
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        fontSize = 24.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // User Score Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Total Votes", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = totalVotes.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User Submissions List
            LazyVerticalGrid(
                columns = GridCells.Fixed(2)
            ) {
                items(submissions) { submission ->
                    SubmissionItem(submission, onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "submission",
                            submission
                        )
                        navController.navigate(Screen.PhotoDetails.route)
                    })
                }
            }
        }
    }
}

@Composable
fun SubmissionItem(submission: Submission, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Image(
            bitmap = decodeBase64ToBitmap(submission.image).asImageBitmap(),
            contentDescription = "User Submission",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "Votes: ${submission.votes}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
