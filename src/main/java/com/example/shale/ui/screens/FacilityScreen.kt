package com.example.shale.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shale.data.Facility
import com.example.shale.viewmodel.MainViewModel

@Composable
fun FacilityScreen(viewModel: MainViewModel, language: String) {
    val facilities by viewModel.facilities.collectAsState()
    var selectedFacility by remember { mutableStateOf<Facility?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (language == "kn") "ಸೌಲಭ್ಯ ಪ್ರವಾಸ" else "Facility Tour",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (language == "kn") "ನಿಮ್ಮ ಶಾಲೆಯ ಹೆಮ್ಮೆ ತುಂಬಿದ ಸ್ಥಳಗಳನ್ನು ನೋಡಿ" else "Swipe through the spaces that make your school proud.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            if (facilities.isEmpty()) {
                EmptyPanel(if (language == "kn") "ಸೌಲಭ್ಯಗಳು ಇನ್ನೂ ಸೇರಿಸಲಾಗಿಲ್ಲ" else "No facilities added yet")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(facilities) { facility ->
                        FacilityCard(facility = facility, language = language, onClick = { selectedFacility = facility })
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF4F46E5))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (language == "kn") "ಲ್ಯಾಬ್, ಗ್ರಂಥಾಲಯ, ಕ್ರೀಡೆ ಮತ್ತು ತರಗತಿಗಳ ಕಥೆಗಳನ್ನು ಇಲ್ಲಿ ಹಂಚಿಕೊಳ್ಳಿ."
                        else "Use this space to tell the story of labs, libraries, sports rooms, and classrooms.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    selectedFacility?.let { facility ->
        AlertDialog(
            onDismissRequest = { selectedFacility = null },
            confirmButton = {
                TextButton(onClick = { selectedFacility = null }) {
                    Text(if (language == "kn") "ಮುಚ್ಚಿ" else "Close")
                }
            },
            title = { Text(facility.displayName(language), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = facility.imageUrl,
                        contentDescription = facility.displayName(language),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFE0E7FF)),
                        contentScale = ContentScale.Crop
                    )
                    Text(facility.displayDescription(language).ifBlank {
                        if (language == "kn") "ಈ ಸೌಲಭ್ಯವು ವಿದ್ಯಾರ್ಥಿಗಳ ಕಲಿಕೆಯನ್ನು ಬೆಂಬಲಿಸುತ್ತದೆ." else "This facility supports everyday student learning."
                    })
                }
            }
        )
    }
}

@Composable
private fun FacilityCard(facility: Facility, language: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column {
            Box {
                AsyncImage(
                    model = facility.imageUrl,
                    contentDescription = facility.displayName(language),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(Color(0xFFE0E7FF)),
                    contentScale = ContentScale.Crop
                )
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    containerColor = Color(0xFF4F46E5)
                ) {
                    Text(if (language == "kn") "ಅನ್ವೇಷಿಸಿ" else "Discover")
                }
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(facility.displayName(language), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(
                    facility.displayDescription(language).ifBlank {
                        if (language == "kn") "ಇನ್ನಷ್ಟು ತಿಳಿಯಲು ಟ್ಯಾಪ್ ಮಾಡಿ" else "Tap to learn more"
                    },
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
