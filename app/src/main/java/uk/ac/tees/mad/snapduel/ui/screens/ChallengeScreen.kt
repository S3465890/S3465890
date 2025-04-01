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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import uk.ac.tees.mad.snapduel.ui.navigation.Screen
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    navController: NavController,
    viewModel: ChallengeViewModel = viewModel()
) {
    val context = LocalContext.current
    val dailyPrompt by viewModel.dailyPrompt.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val userSubmissions by viewModel.userSubmissions.collectAsState()

    // Image-related states
    var capturedBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var base64Image by rememberSaveable { mutableStateOf<String?>(null) }

    // Location states
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

    // Permission Launcher
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

    LaunchedEffect(Unit) {
        val todayDate = System.currentTimeMillis()

    }

    Scaffold(
        containerColor = Color(0xFF1E90FF),
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SnapDuel: Daily Challenge"
                    )
                }
            )
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = { navController.navigate(Screen.Voting.route) },
                containerColor = Color(0xFF4CAF50)

            ) {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = "Voting Screen",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Go to Voting Screen")
                }
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
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Submit a picture of this topic and users will vote.",
                        style = MaterialTheme.typography.labelLarge
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
                                text = rememberDailyPrompt(),
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

                                    // Display the captured image
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
                                                viewModel.submitPhoto(
                                                    encoded,
                                                    locationLatitude,
                                                    locationLongitude
                                                )
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
                            .height(250.dp)
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

@Composable
fun rememberDailyPrompt(): String {
    val prompt = remember {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Get today's date as a string
        val seed = date.hashCode().toLong() // Use the date as a seed for randomness
        val prompts = listOf(
            "Something Blue", "A Sunset View", "Mirror Reflection", "A Book Cover", "Street Art", "Cloud Formations"
        )
        prompts.shuffled(Random(seed)).first() // Generate a consistent prompt for the day
    }
    return prompt
}



