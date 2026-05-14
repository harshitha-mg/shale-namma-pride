package com.example.shale.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shale.data.School
import com.example.shale.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionScreen(
    viewModel: MainViewModel,
    isAdminMode: Boolean,
    language: String,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onSchoolSelected: (School) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allSchools by viewModel.schools.collectAsState()
    val schools = allSchools.filter {
        it.displayName(language).contains(searchQuery, ignoreCase = true) ||
            it.displayLocation(language).contains(searchQuery, ignoreCase = true) ||
            it.nameKn.contains(searchQuery, ignoreCase = true) ||
            it.locationKn.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            AssistChip(onClick = onToggleLanguage, label = { Text(if (language == "en") "ಕನ್ನಡ" else "English") })
        }
        
        Surface(
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("S", color = MaterialTheme.colorScheme.onPrimary, fontSize = 40.sp, fontWeight = FontWeight.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            if (language == "kn") {
                if (isAdminMode) "ನಿರ್ವಾಹಕ ಪೋರ್ಟಲ್" else "ನಿಮ್ಮ ಶಾಲೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ"
            } else {
                if (isAdminMode) "Admin Portal" else "Select Your School"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            if (language == "kn") {
                if (isAdminMode) "ಶಾಲೆಯ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್ ನಿರ್ವಹಿಸಲು ಆಯ್ಕೆಮಾಡಿ" else "ಶಾಲೆಯ ಅಪ್ಡೇಟ್‌ಗಳೊಂದಿಗೆ ಸಂಪರ್ಕದಲ್ಲಿರಿ"
            } else {
                if (isAdminMode) "Choose a school to manage its native dashboard" else "Stay connected with school updates"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (language == "kn") "ಶಾಲೆ ಹುಡುಕಿ..." else "Search school...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.large
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(schools) { school ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSchoolSelected(school) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                school.displayName(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                            Text(
                                school.displayLocation(language),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (schools.isEmpty()) {
                item {
                    Text(
                        if (language == "kn") {
                            if (searchQuery.isBlank()) "Firestore ನಲ್ಲಿ ಶಾಲೆಗಳು ಇನ್ನೂ ಇಲ್ಲ." else "ಹೊಂದುವ ಶಾಲೆ ಸಿಗಲಿಲ್ಲ."
                        } else {
                            if (searchQuery.isBlank()) "No schools found in Firestore yet." else "No matching schools."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}
