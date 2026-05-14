package com.example.shale.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shale.data.Feedback
import com.example.shale.data.GeminiDeveloperRestApi
import com.example.shale.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun FeedbackScreen(viewModel: MainViewModel, language: String) {
    var feedbackText by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(true) }
    var authorName by remember { mutableStateOf("") }
    val status by viewModel.feedbackStatus.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val translatedFeedback = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(feedback, language) {
        if (language == "en") {
            translatedFeedback.clear()
        } else {
            feedback.forEach { item ->
                if (!translatedFeedback.containsKey(item.id)) {
                    val translated = GeminiDeveloperRestApi.translateText(item.content, language)
                    translatedFeedback[item.id] = translated ?: item.content
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF0F766E))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (language == "kn") "ಸಲಹೆಗಳು ಮತ್ತು ಪ್ರತಿಕ್ರಿಯೆ" else "Feedback & Suggestions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    if (language == "kn") "SDMC ಮತ್ತು ಶಾಲಾ ತಂಡಕ್ಕೆ ನೇರವಾದ, ಪಾರದರ್ಶಕ ಧ್ವನಿ." else "A direct, transparent channel for parents and the school committee.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedVisibility(visible = !isAnonymous) {
                        OutlinedTextField(
                            value = authorName,
                            onValueChange = { authorName = it },
                            label = { Text(if (language == "kn") "ನಿಮ್ಮ ಹೆಸರು" else "Your Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text(if (language == "kn") "ನಿಮ್ಮ ಸಲಹೆ" else "Your Suggestion") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (language == "kn") "ಅನಾಮಧೇಯವಾಗಿ ಕಳುಹಿಸಿ" else "Send anonymously")
                    }

                    Button(
                        onClick = {
                            viewModel.sendFeedback(feedbackText, isAnonymous, authorName)
                            feedbackText = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == "kn") "ಸಲಹೆ ಸಲ್ಲಿಸಿ" else "Submit Feedback")
                    }

                    status?.let {
                        Text(it.localizedFeedbackStatus(language), color = Color(0xFF0F766E), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Text(
                if (language == "kn") "ಸಾರ್ವಜನಿಕ ಸಲಹಾ ಫಲಕ" else "Public Suggestion Board",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }

        if (feedback.isEmpty()) {
            item {
                Text(
                    if (language == "kn") "ಇನ್ನೂ ಸಲಹೆಗಳು ಇಲ್ಲ. ಮೊದಲಿಗರಾಗಿ ಹಂಚಿಕೊಳ್ಳಿ." else "No suggestions yet. Be the first to share.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(feedback) { item ->
                FeedbackCard(item = item, language = language, translatedContent = translatedFeedback[item.id] ?: item.content)
            }
        }
    }
}

@Composable
private fun FeedbackCard(item: Feedback, language: String, translatedContent: String) {
    OutlinedCard(modifier = Modifier.animateContentSize(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(translatedContent, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (item.isAnonymous) {
                    if (language == "kn") "ಅನಾಮಧೇಯ ಪೋಷಕರು" else "Anonymous parent"
                } else {
                    item.authorName ?: if (language == "kn") "ಪೋಷಕರು" else "Parent"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun String.localizedFeedbackStatus(language: String): String {
    if (language != "kn") return this
    return when (this) {
        "Please enter feedback before submitting." -> "ಸಲ್ಲಿಸುವ ಮೊದಲು ದಯವಿಟ್ಟು ಸಲಹೆಯನ್ನು ನಮೂದಿಸಿ."
        "Feedback submitted. Thank you." -> "ಸಲಹೆ ಯಶಸ್ವಿಯಾಗಿ ಸಲ್ಲಿಸಲಾಗಿದೆ. ಧನ್ಯವಾದಗಳು."
        "Could not submit feedback. Check Firebase setup." -> "ಸಲಹೆ ಸಲ್ಲಿಸಲಾಗಲಿಲ್ಲ. Firebase ವ್ಯವಸ್ಥೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
        else -> this
    }
}
