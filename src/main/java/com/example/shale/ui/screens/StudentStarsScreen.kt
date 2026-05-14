package com.example.shale.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shale.data.StudentStar
import com.example.shale.viewmodel.MainViewModel

@Composable
fun StudentStarsScreen(viewModel: MainViewModel, language: String) {
    val stars by viewModel.studentStars.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (language == "kn") "ಎಕ್ಸಲೆನ್ಸ್ ರೆಕಾರ್ಡ್" else "The Excellence Record",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                    }
                    Text(
                        if (language == "kn") "ಸಾಧನೆ ಮಾಡಿದ ವಿದ್ಯಾರ್ಥಿಗಳ ಶಾಲಾ ಪತ್ರಿಕೆ" else "A school newspaper page for students who shine.",
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }

        if (stars.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (language == "kn") "ವಿದ್ಯಾರ್ಥಿ ಸಾಧನೆಗಳು ಇನ್ನೂ ಪ್ರಕಟವಾಗಿಲ್ಲ" else "No student stars published yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(stars) { index, star ->
                NewspaperStarCard(star = star, language = language, index = index)
            }
        }
    }
}

@Composable
private fun NewspaperStarCard(star: StudentStar, language: String, index: Int) {
    val accent = if (index % 2 == 0) Color(0xFFB45309) else Color(0xFF4F46E5)

    Card(
        modifier = Modifier.animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
    ) {
        Column {
            Box {
                AsyncImage(
                    model = star.imageUrl,
                    contentDescription = star.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (index == 0) 300.dp else 220.dp)
                        .background(Color(0xFFFDE68A)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(star.displayName(language).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(star.displayTitle(language), color = Color.White.copy(alpha = 0.82f), fontFamily = FontFamily.Serif)
                }
            }
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.weight(1f), color = accent)
                    Text(
                        if (language == "kn") " ಸಾಧನೆ " else " Achievement ",
                        color = accent,
                        fontWeight = FontWeight.Black
                    )
                    Divider(modifier = Modifier.weight(1f), color = accent)
                }
                Text(
                    star.displayAchievement(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                if (star.quote.isNotBlank()) {
                    Text("\"${star.quote}\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
