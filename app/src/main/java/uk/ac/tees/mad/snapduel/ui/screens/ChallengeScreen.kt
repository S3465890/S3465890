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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.io.ByteArrayOutputStream

@Composable
fun ChallengeScreen(navController: NavController) {
    val context = LocalContext.current
    val dailyPrompt by remember { mutableStateOf("Something Blue") }

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
        contentColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Daily Challenge",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

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

                                // Display Captured Image
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

                                // Location Tagging Toggle
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
                                            // Prepare submission data
                                            val submissionData = mapOf(
                                                "image" to encoded,
                                                "latitude" to locationLatitude,
                                                "longitude" to locationLongitude
                                            )

                                            // actual submission logic
                                            navController.navigate("voting_route")
                                        }
                                    },
                                    enabled = base64Image != null && locationLatitude != null && locationLongitude != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "Submit",
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
