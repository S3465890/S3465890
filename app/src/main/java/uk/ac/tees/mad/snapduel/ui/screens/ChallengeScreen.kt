package uk.ac.tees.mad.snapduel.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import uk.ac.tees.mad.snapduel.data.AppDatabase
import uk.ac.tees.mad.snapduel.data.Submission
import uk.ac.tees.mad.snapduel.ui.navigation.Screen
import java.io.ByteArrayOutputStream
import java.util.UUID

@Composable
fun ChallengeScreen(navController: NavController) {
    val context = LocalContext.current
    val dailyPrompt by remember { mutableStateOf("Something Blue") }
    var isSubmitting by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val submissionDao = AppDatabase.getInstance(context).submissionDao()

    val userId = auth.currentUser?.uid ?: ""
    val userSubmissions by submissionDao.getUserSubmissions(userId)
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Image-related states
    var capturedBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var base64Image by rememberSaveable { mutableStateOf<String?>(null) }

    // Location-related states
    var locationLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var isLocationEnabled by rememberSaveable { mutableStateOf(false) }

    // Permission states
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(checkCameraPermission(context))
    }
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(checkLocationPermission(context))
    }

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Camera Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                capturedBitmap = it
                base64Image = encodeImageToBase64(it)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = getBitmapFromUri(context, it)
            capturedBitmap = bitmap
            base64Image = bitmap?.let { encodeImageToBase64(it) }
        }
    }

    // Location Fetching Function
    val fetchLocation: () -> Unit = {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location: Location? ->
                    location?.let {
                        locationLatitude = it.latitude
                        locationLongitude = it.longitude
                        isLocationEnabled = true
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E90FF),
        contentColor = Color.White,
        floatingActionButton = {
            Button(
                onClick = { navController.navigate(Screen.Voting.route) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.HowToVote,
                    contentDescription = "Voting Screen",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(8.dp))
                Text("Go to Voting Screen")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Daily Challenge",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.elevatedCardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dailyPrompt,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CameraButton(
                                    icon = Icons.Filled.AddAPhoto,
                                    text = "Take Photo",
                                    onClick = {
                                        if (hasCameraPermission) {

                                            val intent =
                                                android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                            cameraLauncher.launch(intent)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                CameraButton(
                                    icon = Icons.Filled.Image,
                                    text = "Gallery",
                                    onClick = {
                                        galleryLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Image and Location Section
                            AnimatedVisibility(
                                visible = capturedBitmap != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Display the aptured Image
                                    capturedBitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Captured Image",
                                            modifier = Modifier
                                                .size(250.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Location Tagging Toggling
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isLocationEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    fetchLocation()
                                                } else {
                                                    // Reset location when unchecked
                                                    locationLatitude = null
                                                    locationLongitude = null
                                                    isLocationEnabled = false
                                                }
                                            }
                                        )
                                        Text("Tag Location")
                                    }

                                    // Location Display
                                    if (isLocationEnabled && locationLatitude != null && locationLongitude != null) {
                                        Text(
                                            text = "Latitude: $locationLatitude\nLongitude: $locationLongitude",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }


                                    Button(
                                        onClick = {
                                            base64Image?.let { encoded ->
                                                val submission = Submission(
                                                    UUID.randomUUID().toString(),
                                                    encoded,
                                                    null,
                                                    null,
                                                    System.currentTimeMillis(),
                                                    userId
                                                )
                                                scope.launch {
                                                    submissionDao.insert(submission)
                                                    firestore.collection("submissions")
                                                        .add(submission)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(
                                                                context,
                                                                "Submission Successful!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            capturedBitmap = null
                                                            base64Image = null
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(
                                                                context,
                                                                "Submission Failed!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    isSubmitting = false
                                                }
                                            }
                                        },
                                        enabled = (base64Image != null && locationLatitude != null && locationLongitude != null && !isSubmitting),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = if (isSubmitting) "Loading..." else "Submit",
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Submit Photo")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//                    Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Previous submissions",
                        style = MaterialTheme.typography.titleLarge
                    )
//                    }
                }
                items(userSubmissions) { submission ->
                    Image(
                        bitmap = decodeBase64ToBitmap(submission.image).asImageBitmap(),
                        contentDescription = "Previous Submission",
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop

                    )
                }

            }
        }
    }
}

@Composable
fun CameraButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

// Utility Functions
fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun encodeImageToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

fun decodeBase64ToBitmap(encodedString: String): Bitmap {
    val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}
