package uk.ac.tees.mad.snapduel.ui.screens


import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import uk.ac.tees.mad.snapduel.data.Submission
import uk.ac.tees.mad.snapduel.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val submissions = remember { mutableStateListOf<Submission>() }
    var totalVotes by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val userId = Firebase.auth.currentUser?.uid


    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }


    // Fetching user submissions and calculate total votes
    DisposableEffect(userId) {

        val userListener =
            db.collection("users").document(userId ?: "").addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    username = it.getString("username") ?: "User"
                    bio = it.getString("bio") ?: "No bio available"
                }
            }
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
            userListener.remove()
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
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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


            // User Score
            Card(
                modifier = Modifier,
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(modifier = Modifier.weight(1f)) {
                        // User Info
                        Text(text = username, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = bio,
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Total Votes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = totalVotes.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

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
    if (showSettings) {
        AccountSettingsDialog(
            userId = userId ?: "",
            username = username,
            bio = bio,
            navController = navController,
            onClose = { showSettings = false }
        )
    }
}

@Composable
fun AccountSettingsDialog(
    userId: String,
    username: String,
    bio: String,
    onClose: () -> Unit,
    navController: NavController,
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    var newUsername by remember { mutableStateOf(username) }
    var newBio by remember { mutableStateOf(bio) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Account Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                db.collection("users").document(userId).set(
                    mapOf("username" to newUsername, "bio" to newBio)
                ).addOnSuccessListener {

                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    onClose()
                }.addOnFailureListener {
                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                    it.printStackTrace()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Column {
                Button(
                    colors = ButtonDefaults.buttonColors(Color.Red),
                    onClick = {
                        auth.signOut()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0)
                        }
                    }
                ) {
                    Text("Logout")
                }

            }
        }
    )
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
