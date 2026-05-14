package com.example.shale.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shale.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MealScreen(viewModel: MainViewModel, language: String) {
    val meals by viewModel.meals.collectAsState()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayMeal = meals.find { it.date == today }
    val menu = if (language == "kn") {
        todayMeal?.menuKn?.ifBlank { todayMeal.menuEn }
    } else {
        todayMeal?.menuEn?.ifBlank { todayMeal.menuKn }
    }
    val displayDate = todayMeal?.date?.toMealDisplayDate(language) ?: Date().toLocalizedMealDate(language)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (language == "kn") "ಇಂದಿನ ಮಧ್ಯಾಹ್ನದ ಊಟ" else "Today's Mid-Day Meal") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Text(
                    if (language == "kn") "ಮಕ್ಕಳ ತಟ್ಟೆಯಲ್ಲಿ ಏನಿದೆ?" else "What is on the plate today?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    displayDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (todayMeal != null) {
                    Box {
                        AsyncImage(
                            model = todayMeal.imageUrl,
                            contentDescription = "Meal Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(Color(0xFFFFF7ED)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Badge(containerColor = Color(0xFF16A34A)) {
                                    Text(if (language == "kn") "ಪರಿಶೀಲಿಸಲಾಗಿದೆ" else "Verified")
                                }
                                Badge(containerColor = Color(0xFFF97316)) {
                                    Text(if (language == "kn") "ತಾಜಾ ಅಪ್ಡೇಟ್" else "Fresh Update")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                menu ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(if (language == "kn") "ಗುಣಮಟ್ಟ ಪರಿಶೀಲನೆ" else "Quality Check", fontWeight = FontWeight.Bold)
                                Text(if (language == "kn") "ಇದೀಗ ನವೀಕರಿಸಲಾಗಿದೆ" else "Updated just now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Divider()
                        Text(
                            if (language == "kn") "ಪೋಷಕರಿಗೆ ನಂಬಿಕೆ ನೀಡಲು ಊಟದ ಫೋಟೋ ಮತ್ತು ಮೆನು ನೇರವಾಗಿ ಶಾಲೆಯಿಂದ ಬರುತ್ತದೆ."
                            else "Meal photo and menu are posted by the school so parents can see a fresh, trustworthy update.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (language == "kn") "ಇಂದಿನ ಊಟವನ್ನು ಇನ್ನೂ ನವೀಕರಿಸಲಾಗಿಲ್ಲ" else "Meal has not been updated yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.toMealDisplayDate(language: String): String {
    return runCatching {
        val parsed = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        val locale = if (language == "kn") Locale("kn", "IN") else Locale.ENGLISH
        parsed.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    }.getOrElse { this }
}

private fun Date.toLocalizedMealDate(language: String): String {
    val locale = if (language == "kn") Locale("kn", "IN") else Locale.ENGLISH
    return SimpleDateFormat("EEEE, d MMMM", locale).format(this)
}
