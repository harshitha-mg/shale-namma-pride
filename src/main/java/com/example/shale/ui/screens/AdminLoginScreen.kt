package com.example.shale.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shale.data.AiAnalysisEngine
import com.example.shale.data.Facility
import com.example.shale.data.FeedbackSentimentAnalysis
import com.example.shale.data.GeminiAnalysisApi
import com.example.shale.data.GeminiDeveloperRestApi
import com.example.shale.data.MealNutritionAnalysis
import com.example.shale.data.School
import com.example.shale.data.StudentStar
import com.example.shale.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.Locale

private enum class AdminSectionKey { AddSchool, SchoolSettings, Meal, Facilities, Students, Feedback }

@Composable
fun AdminLoginScreen(
    viewModel: MainViewModel,
    language: String,
    onToggleLanguage: () -> Unit,
    onClose: () -> Unit,
    onSignOut: () -> Unit,
    adminEmail: String?,
    adminUid: String?
) {
    val adminStatus by viewModel.adminStatus.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val schools by viewModel.schools.collectAsState()
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val activeSchool = selectedSchool
    val meals by viewModel.meals.collectAsState()
    val facilities by viewModel.facilities.collectAsState()
    val studentStars by viewModel.studentStars.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayMeal = meals.find { it.date == today }

    var selectedSection by remember { mutableStateOf(AdminSectionKey.AddSchool) }
    LaunchedEffect(selectedSchool?.id) {
        selectedSection = if (selectedSchool == null) AdminSectionKey.AddSchool else AdminSectionKey.SchoolSettings
    }
    val adminBackground by animateColorAsState(
        targetValue = selectedSection.color().copy(alpha = 0.04f),
        animationSpec = tween(300),
        label = "admin-background"
    )

    var newSchoolNameEn by remember { mutableStateOf("") }
    var newSchoolNameKn by remember { mutableStateOf("") }
    var newSchoolLocationEn by remember { mutableStateOf("") }
    var newSchoolLocationKn by remember { mutableStateOf("") }
    var newSchoolDiceCode by remember { mutableStateOf("") }

    var mealMenuEn by remember(today) { mutableStateOf("") }
    var mealMenuKn by remember(today) { mutableStateOf("") }
    var mealImage by remember(today) { mutableStateOf("") }
    var selectedMealImageUri by remember { mutableStateOf<Uri?>(null) }
    val mealImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedMealImageUri = it
    }

    var facilityNameEn by remember { mutableStateOf("") }
    var facilityNameKn by remember { mutableStateOf("") }
    var facilityDescEn by remember { mutableStateOf("") }
    var facilityDescKn by remember { mutableStateOf("") }
    var facilityImage by remember { mutableStateOf("") }
    var selectedFacilityImageUri by remember { mutableStateOf<Uri?>(null) }
    val facilityImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedFacilityImageUri = it
    }

    var studentName by remember { mutableStateOf("") }
    var studentNameKn by remember { mutableStateOf("") }
    var studentTitleEn by remember { mutableStateOf("") }
    var studentTitleKn by remember { mutableStateOf("") }
    var achievementEn by remember { mutableStateOf("") }
    var achievementKn by remember { mutableStateOf("") }
    var studentQuote by remember { mutableStateOf("") }
    var studentImage by remember { mutableStateOf("") }
    var selectedStudentImageUri by remember { mutableStateOf<Uri?>(null) }
    var studentDate by remember { mutableStateOf(today) }
    val studentImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedStudentImageUri = it
    }

    var editSchoolNameEn by remember(selectedSchool?.id) { mutableStateOf(selectedSchool?.nameEn ?: "") }
    var editSchoolNameKn by remember(selectedSchool?.id) { mutableStateOf(selectedSchool?.nameKn ?: "") }
    var editSchoolLocationEn by remember(selectedSchool?.id) { mutableStateOf(selectedSchool?.locationEn ?: "") }
    var editSchoolLocationKn by remember(selectedSchool?.id) { mutableStateOf(selectedSchool?.locationKn ?: "") }
    var editSchoolDiceCode by remember(selectedSchool?.id) { mutableStateOf(selectedSchool?.diceCode ?: "") }

    var editingFacility by remember { mutableStateOf<Facility?>(null) }
    var editingStudent by remember { mutableStateOf<StudentStar?>(null) }
    var mealAnalysis by remember(todayMeal?.id) { mutableStateOf<MealNutritionAnalysis?>(null) }
    var feedbackAnalysis by remember(feedback.size) { mutableStateOf<FeedbackSentimentAnalysis?>(null) }
    var generatingMealAnalysis by remember { mutableStateOf(false) }
    var generatingFeedbackAnalysis by remember { mutableStateOf(false) }
    var translatingField by remember { mutableStateOf<String?>(null) }
    var pendingPdf by remember { mutableStateOf<ByteArray?>(null) }
    val translatedFeedback = remember { mutableStateMapOf<String, String>() }
    val reportSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val pdf = pendingPdf
        if (uri != null && pdf != null) {
            context.writePdf(uri, pdf)
        }
    }

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

    fun requestKannadaTranslation(fieldId: String, sourceText: String, onTranslated: (String) -> Unit) {
        if (sourceText.isBlank() || translatingField != null) return
        translatingField = fieldId
        coroutineScope.launch {
            val translated = GeminiDeveloperRestApi.translateText(sourceText, "kn") ?: sourceText
            onTranslated(translated)
            translatingField = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(adminBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdminHeader(
                language = language,
                adminEmail = adminEmail,
                selectedSchool = activeSchool,
                onToggleLanguage = onToggleLanguage,
                onClose = onClose
            )
        }

        adminStatus?.let { status ->
            item { AssistChip(onClick = {}, label = { Text(status.localizedAdminStatus(language)) }) }
        }

        item {
            AdminNavigation(
                language = language,
                selectedSchool = activeSchool,
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
        }

        if (activeSchool == null) {
            item {
                AddSchoolSection(
                    language = language,
                    nameEn = newSchoolNameEn,
                    nameKn = newSchoolNameKn,
                    locationEn = newSchoolLocationEn,
                    locationKn = newSchoolLocationKn,
                    diceCode = newSchoolDiceCode,
                    onNameEn = { newSchoolNameEn = it },
                    onNameKn = { newSchoolNameKn = it },
                    onLocationEn = { newSchoolLocationEn = it },
                    onLocationKn = { newSchoolLocationKn = it },
                    onDiceCode = { newSchoolDiceCode = it },
                    onTranslateName = {
                        requestKannadaTranslation("newSchoolName", newSchoolNameEn) { newSchoolNameKn = it }
                    },
                    onTranslateLocation = {
                        requestKannadaTranslation("newSchoolLocation", newSchoolLocationEn) { newSchoolLocationKn = it }
                    },
                    translatingName = translatingField == "newSchoolName",
                    translatingLocation = translatingField == "newSchoolLocation",
                    onAdd = {
                        adminUid?.let { uid ->
                            viewModel.createSchool(
                                adminUid = uid,
                                adminEmail = adminEmail,
                                nameEn = newSchoolNameEn,
                                nameKn = newSchoolNameKn,
                                locationEn = newSchoolLocationEn,
                                locationKn = newSchoolLocationKn,
                                diceCode = newSchoolDiceCode
                            )
                            newSchoolNameEn = ""
                            newSchoolNameKn = ""
                            newSchoolLocationEn = ""
                            newSchoolLocationKn = ""
                            newSchoolDiceCode = ""
                        }
                    }
                )
            }
            item {
                SchoolListSection(
                    language = language,
                    schools = schools,
                    selectedSchool = activeSchool,
                    onSelect = { viewModel.selectSchool(it) },
                    onDelete = {
                        adminUid?.let { uid -> viewModel.deleteSchool(uid, it.id) }
                    }
                )
            }
        } else {
            item {
                AnimatedContent(targetState = selectedSection, label = "admin-section") { section ->
                when (section) {
                    AdminSectionKey.SchoolSettings -> AdminSection(title = text(language, "School Settings", "ಶಾಲೆ ಸೆಟ್ಟಿಂಗ್ಸ್"), accent = section.color()) {
                        AppTextField(editSchoolNameEn, { editSchoolNameEn = it }, "School name (English)")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "editSchoolName",
                            enabled = editSchoolNameEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("editSchoolName", editSchoolNameEn) { editSchoolNameKn = it } }
                        )
                        AppTextField(editSchoolNameKn, { editSchoolNameKn = it }, "ಶಾಲೆಯ ಹೆಸರು (ಕನ್ನಡ)")
                        AppTextField(editSchoolLocationEn, { editSchoolLocationEn = it }, "Location (English)")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "editSchoolLocation",
                            enabled = editSchoolLocationEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("editSchoolLocation", editSchoolLocationEn) { editSchoolLocationKn = it } }
                        )
                        AppTextField(editSchoolLocationKn, { editSchoolLocationKn = it }, "ಸ್ಥಳ (ಕನ್ನಡ)")
                        AppTextField(editSchoolDiceCode, { editSchoolDiceCode = it }, text(language, "School dice code", "ಶಾಲೆಯ ಡೈಸ್ ಕೋಡ್"))
                        Button(
                            onClick = {
                                val uid = adminUid ?: return@Button
                                viewModel.updateSchoolDetails(
                                    adminUid = uid,
                                    schoolId = activeSchool.id,
                                    nameEn = editSchoolNameEn,
                                    nameKn = editSchoolNameKn,
                                    locationEn = editSchoolLocationEn,
                                    locationKn = editSchoolLocationKn,
                                    diceCode = editSchoolDiceCode
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text(language, "Save School Details", "ಶಾಲೆಯ ವಿವರಗಳನ್ನು ಉಳಿಸಿ"))
                        }
                    }

                    AdminSectionKey.Meal -> AdminSection(title = text(language, "Daily Meal Update", "ದೈನಂದಿನ ಊಟದ ನವೀಕರಣ"), accent = section.color()) {
                        if (todayMeal != null) {
                            Text(
                                text(language, "Today's meal has already been updated.", "ಇಂದಿನ ಊಟವನ್ನು ಈಗಾಗಲೇ ನವೀಕರಿಸಲಾಗಿದೆ."),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (language == "kn") todayMeal.menuKn.ifBlank { todayMeal.menuEn } else todayMeal.menuEn.ifBlank { todayMeal.menuKn },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    generatingMealAnalysis = true
                                    coroutineScope.launch {
                                        mealAnalysis = GeminiAnalysisApi.analyzeMeal(todayMeal, language)
                                            ?: GeminiDeveloperRestApi.analyzeMeal(todayMeal, language)
                                            ?: AiAnalysisEngine.analyzeMeal(todayMeal)
                                        generatingMealAnalysis = false
                                    }
                                },
                                enabled = !generatingMealAnalysis,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (generatingMealAnalysis) text(language, "Generating analysis...", "ವಿಶ್ಲೇಷಣೆ ರಚಿಸಲಾಗುತ್ತಿದೆ...")
                                    else text(language, "Generate Meal Nutrition Analysis", "ಊಟದ ಪೋಷಣಾ ವಿಶ್ಲೇಷಣೆ ರಚಿಸಿ")
                                )
                            }
                            mealAnalysis?.let { analysis ->
                                MealAnalysisCard(
                                    analysis = analysis,
                                    language = language,
                                    onDownload = {
                                        pendingPdf = analysis.toPdf(language)
                                        reportSaver.launch("meal_nutrition_report_$today.pdf")
                                    }
                                )
                            }
                        } else {
                            Text(text(language, "Meal has not been updated yet for $today.", "$today ಗಾಗಿ ಊಟವನ್ನು ಇನ್ನೂ ನವೀಕರಿಸಲಾಗಿಲ್ಲ."))
                            AppTextField(mealMenuEn, { mealMenuEn = it }, "Menu in English")
                            TranslateAction(
                                language = language,
                                translating = translatingField == "mealMenu",
                                enabled = mealMenuEn.isNotBlank(),
                                onClick = { requestKannadaTranslation("mealMenu", mealMenuEn) { mealMenuKn = it } }
                            )
                            AppTextField(mealMenuKn, { mealMenuKn = it }, "ಕನ್ನಡ ಮೆನು")
                            ImagePickerButton(language, selectedMealImageUri != null) { mealImagePicker.launch("image/*") }
                            AppTextField(mealImage, { mealImage = it }, text(language, "Image URL fallback", "ಚಿತ್ರ URL"))
                            Button(
                                onClick = {
                                    viewModel.postMeal(today, mealMenuEn, mealMenuKn, mealImage, selectedMealImageUri)
                                    mealMenuEn = ""
                                    mealMenuKn = ""
                                    mealImage = ""
                                    selectedMealImageUri = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text(language, "Publish Today's Meal", "ಇಂದಿನ ಊಟ ಪ್ರಕಟಿಸಿ"))
                            }
                        }
                    }

                    AdminSectionKey.Facilities -> AdminSection(title = text(language, "Facilities", "ಸೌಲಭ್ಯಗಳು"), accent = section.color()) {
                        AppTextField(facilityNameEn, { facilityNameEn = it }, "Facility name")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "facilityName",
                            enabled = facilityNameEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("facilityName", facilityNameEn) { facilityNameKn = it } }
                        )
                        AppTextField(facilityNameKn, { facilityNameKn = it }, "ಸೌಲಭ್ಯದ ಹೆಸರು")
                        AppTextField(facilityDescEn, { facilityDescEn = it }, "Description")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "facilityDescription",
                            enabled = facilityDescEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("facilityDescription", facilityDescEn) { facilityDescKn = it } }
                        )
                        AppTextField(facilityDescKn, { facilityDescKn = it }, "ವಿವರಣೆ")
                        ImagePickerButton(language, selectedFacilityImageUri != null) { facilityImagePicker.launch("image/*") }
                        AppTextField(facilityImage, { facilityImage = it }, "Image URL")
                        Button(
                            onClick = {
                                viewModel.addFacility(facilityNameEn, facilityNameKn, facilityDescEn, facilityDescKn, facilityImage, selectedFacilityImageUri)
                                facilityNameEn = ""
                                facilityNameKn = ""
                                facilityDescEn = ""
                                facilityDescKn = ""
                                facilityImage = ""
                                selectedFacilityImageUri = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text(language, "Add Facility", "ಸೌಲಭ್ಯ ಸೇರಿಸಿ"))
                        }
                        Divider()
                        AdminItemListTitle(text(language, "Already Added Facilities", "ಈಗಾಗಲೇ ಸೇರಿಸಿದ ಸೌಲಭ್ಯಗಳು"))
                        if (facilities.isEmpty()) {
                            Text(text(language, "No facilities added yet.", "ಸೌಲಭ್ಯಗಳು ಇನ್ನೂ ಸೇರಿಸಲಾಗಿಲ್ಲ."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            facilities.forEach { facility ->
                                FacilityAdminRow(
                                    facility = facility,
                                    language = language,
                                    onEdit = { editingFacility = facility },
                                    onDelete = { viewModel.deleteFacility(facility.id) }
                                )
                            }
                        }
                    }

                    AdminSectionKey.Students -> AdminSection(title = text(language, "Student Stars", "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ಸ್"), accent = section.color()) {
                        AppTextField(studentName, { studentName = it }, text(language, "Student name (English)", "ವಿದ್ಯಾರ್ಥಿ ಹೆಸರು (ಇಂಗ್ಲಿಷ್)"))
                        TranslateAction(
                            language = language,
                            translating = translatingField == "studentName",
                            enabled = studentName.isNotBlank(),
                            onClick = { requestKannadaTranslation("studentName", studentName) { studentNameKn = it } }
                        )
                        AppTextField(studentNameKn, { studentNameKn = it }, text(language, "Student name (Kannada)", "ವಿದ್ಯಾರ್ಥಿ ಹೆಸರು (ಕನ್ನಡ)"))
                        AppTextField(studentTitleEn, { studentTitleEn = it }, "Title in English")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "studentTitle",
                            enabled = studentTitleEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("studentTitle", studentTitleEn) { studentTitleKn = it } }
                        )
                        AppTextField(studentTitleKn, { studentTitleKn = it }, "ಕನ್ನಡ ಶೀರ್ಷಿಕೆ")
                        AppTextField(achievementEn, { achievementEn = it }, "Achievement in English")
                        TranslateAction(
                            language = language,
                            translating = translatingField == "studentAchievement",
                            enabled = achievementEn.isNotBlank(),
                            onClick = { requestKannadaTranslation("studentAchievement", achievementEn) { achievementKn = it } }
                        )
                        AppTextField(achievementKn, { achievementKn = it }, "ಕನ್ನಡ ಸಾಧನೆ")
                        AppTextField(studentQuote, { studentQuote = it }, text(language, "Quote or description", "ಉಲ್ಲೇಖ ಅಥವಾ ವಿವರಣೆ"))
                        ImagePickerButton(language, selectedStudentImageUri != null) { studentImagePicker.launch("image/*") }
                        AppTextField(studentImage, { studentImage = it }, "Image URL")
                        AppTextField(studentDate, { studentDate = it }, "YYYY-MM-DD")
                        Button(
                            onClick = {
                                viewModel.addStudentStar(studentName, studentNameKn, studentTitleEn, studentTitleKn, achievementEn, achievementKn, studentQuote, studentImage, selectedStudentImageUri, studentDate)
                                studentName = ""
                                studentNameKn = ""
                                studentTitleEn = ""
                                studentTitleKn = ""
                                achievementEn = ""
                                achievementKn = ""
                                studentQuote = ""
                                studentImage = ""
                                selectedStudentImageUri = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text(language, "Publish Star", "ಸ್ಟಾರ್ ಪ್ರಕಟಿಸಿ"))
                        }
                        Divider()
                        AdminItemListTitle(text(language, "Already Published Stars", "ಈಗಾಗಲೇ ಪ್ರಕಟಿಸಿದ ಸ್ಟಾರ್ಸ್"))
                        if (studentStars.isEmpty()) {
                            Text(text(language, "No student stars published yet.", "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ಸ್ ಇನ್ನೂ ಪ್ರಕಟವಾಗಿಲ್ಲ."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            studentStars.forEach { star ->
                                StudentAdminRow(
                                    star = star,
                                    language = language,
                                    onEdit = { editingStudent = star },
                                    onDelete = { viewModel.deleteStudentStar(star.id) }
                                )
                            }
                        }
                    }

                    AdminSectionKey.Feedback -> AdminSection(title = text(language, "Incoming Feedback", "ಬಂದಿರುವ ಸಲಹೆಗಳು"), accent = section.color()) {
                        Button(
                            onClick = {
                                    generatingFeedbackAnalysis = true
                                    coroutineScope.launch {
                                    feedbackAnalysis = GeminiAnalysisApi.analyzeFeedback(feedback, language)
                                        ?: GeminiDeveloperRestApi.analyzeFeedback(feedback, language)
                                        ?: AiAnalysisEngine.analyzeFeedback(feedback)
                                    generatingFeedbackAnalysis = false
                                }
                            },
                            enabled = feedback.isNotEmpty() && !generatingFeedbackAnalysis,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (generatingFeedbackAnalysis) text(language, "Generating analysis...", "ವಿಶ್ಲೇಷಣೆ ರಚಿಸಲಾಗುತ್ತಿದೆ...")
                                else text(language, "Generate AI Feedback Sentiment Analysis", "AI ಪ್ರತಿಕ್ರಿಯೆ ಭಾವನೆ ವಿಶ್ಲೇಷಣೆ ರಚಿಸಿ")
                            )
                        }
                        feedbackAnalysis?.let { analysis ->
                            FeedbackAnalysisCard(
                                analysis = analysis,
                                language = language,
                                onDownload = {
                                    pendingPdf = analysis.toPdf(language)
                                    reportSaver.launch("feedback_sentiment_report_$today.pdf")
                                }
                            )
                        }
                        Divider()
                        if (feedback.isEmpty()) {
                            Text(text(language, "No feedback yet.", "ಸಲಹೆಗಳು ಇನ್ನೂ ಬಂದಿಲ್ಲ."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            feedback.forEach { item ->
                                OutlinedCard(shape = RoundedCornerShape(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(translatedFeedback[item.id] ?: item.content)
                                            Text(
                                                if (item.isAnonymous) text(language, "Anonymous", "ಅನಾಮಧೇಯ") else item.authorName ?: text(language, "Parent", "ಪೋಷಕರು"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteFeedback(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = text(language, "Delete feedback", "ಸಲಹೆ ಅಳಿಸಿ"))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AdminSectionKey.AddSchool -> Unit
                }
                }
            }
        }

        item {
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(text(language, "Sign Out", "ಸೈನ್ ಔಟ್"))
            }
        }
    }

    editingFacility?.let { facility ->
        EditFacilityDialog(
            facility = facility,
            language = language,
            onDismiss = { editingFacility = null },
            onSave = { nameEn, nameKn, descEn, descKn, imageUrl, imageUri ->
                viewModel.updateFacility(facility.id, nameEn, nameKn, descEn, descKn, imageUrl, imageUri)
                editingFacility = null
            }
        )
    }

    editingStudent?.let { star ->
        EditStudentDialog(
            star = star,
            language = language,
            onDismiss = { editingStudent = null },
            onSave = { name, nameKn, titleEn, titleKn, achEn, achKn, quote, imageUrl, date, imageUri ->
                viewModel.updateStudentStar(star.id, name, nameKn, titleEn, titleKn, achEn, achKn, quote, imageUrl, date, imageUri)
                editingStudent = null
            }
        )
    }
}

@Composable
private fun AdminHeader(language: String, adminEmail: String?, selectedSchool: School?, onToggleLanguage: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text(language, "Admin Portal", "ನಿರ್ವಾಹಕ ಪೋರ್ಟಲ್"),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            TextButton(onClick = onToggleLanguage) {
                Text(if (language == "en") "ಕನ್ನಡ" else "English", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = text(language, "Close", "ಮುಚ್ಚಿ"))
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    selectedSchool?.displayName(language) ?: adminEmail ?: text(language, "Google admin", "Google ನಿರ್ವಾಹಕ"),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminNavigation(
    language: String,
    selectedSchool: School?,
    selectedSection: AdminSectionKey,
    onSectionSelected: (AdminSectionKey) -> Unit
) {
    val sections = if (selectedSchool == null) {
        listOf(AdminSectionKey.AddSchool)
    } else {
        listOf(AdminSectionKey.SchoolSettings, AdminSectionKey.Meal, AdminSectionKey.Facilities, AdminSectionKey.Students, AdminSectionKey.Feedback)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sections) { section ->
            FilterChip(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                label = { Text(section.label(language)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = section.color(),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun AddSchoolSection(
    language: String,
    nameEn: String,
    nameKn: String,
    locationEn: String,
    locationKn: String,
    diceCode: String,
    onNameEn: (String) -> Unit,
    onNameKn: (String) -> Unit,
    onLocationEn: (String) -> Unit,
    onLocationKn: (String) -> Unit,
    onDiceCode: (String) -> Unit,
    onTranslateName: () -> Unit,
    onTranslateLocation: () -> Unit,
    translatingName: Boolean,
    translatingLocation: Boolean,
    onAdd: () -> Unit
) {
    AdminSection(title = text(language, "Add New School", "ಹೊಸ ಶಾಲೆ ಸೇರಿಸಿ"), accent = AdminSectionKey.AddSchool.color()) {
        AppTextField(nameEn, onNameEn, "School name (English)")
        TranslateAction(language, translatingName, nameEn.isNotBlank(), onTranslateName)
        AppTextField(nameKn, onNameKn, "ಶಾಲೆಯ ಹೆಸರು (ಕನ್ನಡ)")
        AppTextField(locationEn, onLocationEn, "Location (English)")
        TranslateAction(language, translatingLocation, locationEn.isNotBlank(), onTranslateLocation)
        AppTextField(locationKn, onLocationKn, "ಸ್ಥಳ (ಕನ್ನಡ)")
        AppTextField(diceCode, onDiceCode, text(language, "School dice code", "ಶಾಲೆಯ ಡೈಸ್ ಕೋಡ್"))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text(text(language, "Add School", "ಶಾಲೆ ಸೇರಿಸಿ"))
        }
    }
}

@Composable
private fun SchoolListSection(
    language: String,
    schools: List<School>,
    selectedSchool: School?,
    onSelect: (School) -> Unit,
    onDelete: (School) -> Unit
) {
    AdminSection(title = text(language, "Schools", "ಶಾಲೆಗಳು"), accent = AdminSectionKey.AddSchool.color()) {
        if (schools.isEmpty()) {
            Text(text(language, "No schools yet.", "ಶಾಲೆಗಳು ಇನ್ನೂ ಇಲ್ಲ."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            schools.forEach { school ->
                SchoolRow(
                    school = school,
                    language = language,
                    isSelected = selectedSchool?.id == school.id,
                    onSelect = { onSelect(school) },
                    onDelete = { onDelete(school) }
                )
            }
        }
    }
}

@Composable
private fun SchoolRow(school: School, language: String, isSelected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    school.displayName(language),
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Text(
                    (school.displayLocation(language).ifBlank { "" } + "  " + school.diceCode).trim(),
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelect) { Text(text(language, "Select", "ಆಯ್ಕೆ")) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = text(language, "Delete", "ಅಳಿಸಿ"))
                }
            }
        }
    }
}

@Composable
private fun FacilityAdminRow(facility: Facility, language: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(facility.displayName(language), fontWeight = FontWeight.Bold)
                Text(facility.displayDescription(language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = text(language, "Edit", "ತಿದ್ದು")) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = text(language, "Delete", "ಅಳಿಸಿ")) }
        }
    }
}

@Composable
private fun StudentAdminRow(star: StudentStar, language: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(star.displayName(language), fontWeight = FontWeight.Bold)
                Text(star.displayAchievement(language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = text(language, "Edit", "ತಿದ್ದು")) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = text(language, "Delete", "ಅಳಿಸಿ")) }
        }
    }
}

@Composable
private fun MealAnalysisCard(analysis: MealNutritionAnalysis, language: String, onDownload: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF7ED))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text(language, "AI Meal Nutrition Report", "AI ಊಟದ ಪೋಷಣಾ ವರದಿ"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFEA580C))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile(text(language, "Calories", "ಕ್ಯಾಲರಿ"), "${analysis.calories}", Color(0xFFEA580C), Modifier.weight(1f))
                MetricTile(text(language, "Protein", "ಪ್ರೋಟೀನ್"), "${analysis.protein}g", Color(0xFF16A34A), Modifier.weight(1f))
                MetricTile(text(language, "Iron", "ಕಬ್ಬಿಣ"), "${analysis.iron}mg", Color(0xFF7C3AED), Modifier.weight(1f))
            }
            Text(text(language, "Nutrition graph", "ಪೋಷಣಾ ಗ್ರಾಫ್"), fontWeight = FontWeight.Bold)
            CategoryBar(text(language, "Calories", "ಕ್ಯಾಲರಿ"), analysis.calories, 850, Color(0xFFEA580C), "${analysis.calories} kcal")
            CategoryBar(text(language, "Protein", "ಪ್ರೋಟೀನ್"), analysis.protein, 35, Color(0xFF16A34A), "${analysis.protein}g")
            CategoryBar(text(language, "Iron", "ಕಬ್ಬಿಣ"), (analysis.iron * 10).toInt(), 80, Color(0xFF7C3AED), "${analysis.iron}mg")
            MetricBar(
                label = text(language, "Balanced diet score", "ಸಮತೋಲನ ಆಹಾರ ಸ್ಕೋರ್"),
                value = analysis.balancedScore,
                color = Color(0xFF0F766E)
            )
            Text(
                text(language, "Detected nutrition groups", "ಗುರುತಿಸಿದ ಪೋಷಕ ಗುಂಪುಗಳು"),
                fontWeight = FontWeight.Bold
            )
            Text(analysis.detectedGroups.joinToString(", ") { it.localizedAnalysisLabel(language) }.ifBlank { text(language, "Not enough details in menu", "ಮೆನುವಿನಲ್ಲಿ ಸಾಕಷ್ಟು ವಿವರಗಳಿಲ್ಲ") })
            Text(text(language, "Suggestions", "ಸಲಹೆಗಳು"), fontWeight = FontWeight.Bold)
            (if (language == "kn") analysis.suggestionsKn else analysis.suggestionsEn).forEach {
                Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text(text(language, "Download Report", "ವರದಿ ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ"))
            }
        }
    }
}

@Composable
private fun FeedbackAnalysisCard(analysis: FeedbackSentimentAnalysis, language: String, onDownload: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF0FDFA))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text(language, "AI Feedback Sentiment Report", "AI ಪ್ರತಿಕ್ರಿಯೆ ಭಾವನೆ ವರದಿ"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF0F766E))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile(text(language, "Positive", "ಧನಾತ್ಮಕ"), "${analysis.positiveCount}", Color(0xFF16A34A), Modifier.weight(1f))
                MetricTile(text(language, "Neutral", "ತಟಸ್ಥ"), "${analysis.neutralCount}", Color(0xFF64748B), Modifier.weight(1f))
                MetricTile(text(language, "Negative", "ಋಣಾತ್ಮಕ"), "${analysis.negativeCount}", Color(0xFFDC2626), Modifier.weight(1f))
            }
            Text(text(language, "Sentiment graph", "ಭಾವನೆ ಗ್ರಾಫ್"), fontWeight = FontWeight.Bold)
            val totalFeedback = analysis.items.size.coerceAtLeast(1)
            CategoryBar(text(language, "Positive", "ಧನಾತ್ಮಕ"), analysis.positiveCount, totalFeedback, Color(0xFF16A34A))
            CategoryBar(text(language, "Neutral", "ತಟಸ್ಥ"), analysis.neutralCount, totalFeedback, Color(0xFF64748B))
            CategoryBar(text(language, "Negative", "ಋಣಾತ್ಮಕ"), analysis.negativeCount, totalFeedback, Color(0xFFDC2626))
            MetricBar(text(language, "Urgent priority items", "ತುರ್ತು ಆದ್ಯತೆಯ ವಿಷಯಗಳು"), analysis.urgentCount * 20, Color(0xFFDC2626))
            Text(text(language, "Frequently mentioned problems", "ಹೆಚ್ಚಾಗಿ ಉಲ್ಲೇಖಿಸಿದ ಸಮಸ್ಯೆಗಳು"), fontWeight = FontWeight.Bold)
            if (analysis.frequentProblems.isEmpty()) {
                Text(text(language, "No repeated complaint pattern found.", "ಪುನರಾವರ್ತಿತ ದೂರು ಮಾದರಿ ಕಂಡುಬಂದಿಲ್ಲ."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                analysis.frequentProblems.forEach { Text("• ${it.localizedProblem(language)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(text(language, "Category graph", "ವರ್ಗ ಗ್ರಾಫ್"), fontWeight = FontWeight.Bold)
            val maxCount = analysis.categoryCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            analysis.categoryCounts.forEach { (category, count) ->
                CategoryBar(category.localizedAnalysisLabel(language), count, maxCount, Color(0xFF0F766E))
            }
            Text(text(language, "Detailed AI classification", "ವಿವರವಾದ AI ವರ್ಗೀಕರಣ"), fontWeight = FontWeight.Bold)
            analysis.items.take(5).forEach { item ->
                OutlinedCard(shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.sentiment.localizedAnalysisLabel(language)} • ${item.category.localizedAnalysisLabel(language)} • ${item.priority.localizedAnalysisLabel(language)}", fontWeight = FontWeight.Bold)
                        Text(item.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text(text(language, "Download Report", "ವರದಿ ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ"))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricBar(label: String, value: Int, color: Color) {
    val clamped = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("$clamped%", color = color, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { clamped / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = color.copy(alpha = 0.16f)
        )
    }
}

@Composable
private fun CategoryBar(category: String, count: Int, maxCount: Int, color: Color, displayValue: String = count.toString()) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category)
            Text(displayValue, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { count.toFloat() / maxCount.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.14f)
        )
    }
}

@Composable
private fun EditFacilityDialog(
    facility: Facility,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Uri?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var nameEn by remember(facility.id) { mutableStateOf(facility.nameEn) }
    var nameKn by remember(facility.id) { mutableStateOf(facility.nameKn) }
    var descEn by remember(facility.id) { mutableStateOf(facility.descriptionEn) }
    var descKn by remember(facility.id) { mutableStateOf(facility.descriptionKn) }
    var imageUrl by remember(facility.id) { mutableStateOf(facility.imageUrl) }
    var selectedImageUri by remember(facility.id) { mutableStateOf<Uri?>(null) }
    var translatingField by remember(facility.id) { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    fun requestKannadaTranslation(fieldId: String, sourceText: String, onTranslated: (String) -> Unit) {
        if (sourceText.isBlank() || translatingField != null) return
        translatingField = fieldId
        coroutineScope.launch {
            val translated = GeminiDeveloperRestApi.translateText(sourceText, "kn") ?: sourceText
            onTranslated(translated)
            translatingField = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text(language, "Edit Facility", "ಸೌಲಭ್ಯ ತಿದ್ದು")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(nameEn, { nameEn = it }, "Facility name")
                TranslateAction(
                    language = language,
                    translating = translatingField == "facilityEditName",
                    enabled = nameEn.isNotBlank(),
                    onClick = { requestKannadaTranslation("facilityEditName", nameEn) { nameKn = it } }
                )
                AppTextField(nameKn, { nameKn = it }, "ಸೌಲಭ್ಯದ ಹೆಸರು")
                AppTextField(descEn, { descEn = it }, "Description")
                TranslateAction(
                    language = language,
                    translating = translatingField == "facilityEditDescription",
                    enabled = descEn.isNotBlank(),
                    onClick = { requestKannadaTranslation("facilityEditDescription", descEn) { descKn = it } }
                )
                AppTextField(descKn, { descKn = it }, "ವಿವರಣೆ")
                ImagePickerButton(language, selectedImageUri != null) { imagePicker.launch("image/*") }
                AppTextField(imageUrl, { imageUrl = it }, "Image URL")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(nameEn, nameKn, descEn, descKn, imageUrl, selectedImageUri) }) { Text(text(language, "Save", "ಉಳಿಸಿ")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text(language, "Cancel", "ರದ್ದು")) } }
    )
}

@Composable
private fun EditStudentDialog(
    star: StudentStar,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String, Uri?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember(star.id) { mutableStateOf(star.name) }
    var nameKn by remember(star.id) { mutableStateOf(star.nameKn) }
    var titleEn by remember(star.id) { mutableStateOf(star.titleEn) }
    var titleKn by remember(star.id) { mutableStateOf(star.titleKn) }
    var achievementEn by remember(star.id) { mutableStateOf(star.achievementEn) }
    var achievementKn by remember(star.id) { mutableStateOf(star.achievementKn) }
    var quote by remember(star.id) { mutableStateOf(star.quote) }
    var imageUrl by remember(star.id) { mutableStateOf(star.imageUrl) }
    var date by remember(star.id) { mutableStateOf(star.date) }
    var selectedImageUri by remember(star.id) { mutableStateOf<Uri?>(null) }
    var translatingField by remember(star.id) { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    fun requestKannadaTranslation(fieldId: String, sourceText: String, onTranslated: (String) -> Unit) {
        if (sourceText.isBlank() || translatingField != null) return
        translatingField = fieldId
        coroutineScope.launch {
            val translated = GeminiDeveloperRestApi.translateText(sourceText, "kn") ?: sourceText
            onTranslated(translated)
            translatingField = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text(language, "Edit Student Star", "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ತಿದ್ದು")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(name, { name = it }, text(language, "Student name (English)", "ವಿದ್ಯಾರ್ಥಿ ಹೆಸರು (ಇಂಗ್ಲಿಷ್)"))
                TranslateAction(
                    language = language,
                    translating = translatingField == "studentEditName",
                    enabled = name.isNotBlank(),
                    onClick = { requestKannadaTranslation("studentEditName", name) { nameKn = it } }
                )
                AppTextField(nameKn, { nameKn = it }, text(language, "Student name (Kannada)", "ವಿದ್ಯಾರ್ಥಿ ಹೆಸರು (ಕನ್ನಡ)"))
                AppTextField(titleEn, { titleEn = it }, "Title in English")
                TranslateAction(
                    language = language,
                    translating = translatingField == "studentEditTitle",
                    enabled = titleEn.isNotBlank(),
                    onClick = { requestKannadaTranslation("studentEditTitle", titleEn) { titleKn = it } }
                )
                AppTextField(titleKn, { titleKn = it }, "ಕನ್ನಡ ಶೀರ್ಷಿಕೆ")
                AppTextField(achievementEn, { achievementEn = it }, "Achievement in English")
                TranslateAction(
                    language = language,
                    translating = translatingField == "studentEditAchievement",
                    enabled = achievementEn.isNotBlank(),
                    onClick = { requestKannadaTranslation("studentEditAchievement", achievementEn) { achievementKn = it } }
                )
                AppTextField(achievementKn, { achievementKn = it }, "ಕನ್ನಡ ಸಾಧನೆ")
                AppTextField(quote, { quote = it }, text(language, "Quote or description", "ಉಲ್ಲೇಖ ಅಥವಾ ವಿವರಣೆ"))
                ImagePickerButton(language, selectedImageUri != null) { imagePicker.launch("image/*") }
                AppTextField(imageUrl, { imageUrl = it }, "Image URL")
                AppTextField(date, { date = it }, "YYYY-MM-DD")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, nameKn, titleEn, titleKn, achievementEn, achievementKn, quote, imageUrl, date, selectedImageUri) }) { Text(text(language, "Save", "ಉಳಿಸಿ")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text(language, "Cancel", "ರದ್ದು")) } }
    )
}

@Composable
private fun ImagePickerButton(language: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Text(
            if (selected) text(language, "Image selected from device", "ಚಿತ್ರ ಆಯ್ಕೆಮಾಡಲಾಗಿದೆ")
            else text(language, "Choose Image From Device", "ಸಾಧನದಿಂದ ಚಿತ್ರ ಆಯ್ಕೆಮಾಡಿ")
        )
    }
    Text(
        text(language, "Or use an image URL below", "ಅಥವಾ ಕೆಳಗೆ ಚಿತ್ರ URL ಬಳಸಿ"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TranslateAction(
    language: String,
    translating: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled && !translating,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (translating) {
                text(language, "Translating to Kannada...", "ಕನ್ನಡಕ್ಕೆ ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ...")
            } else {
                text(language, "Translate to Kannada", "ಕನ್ನಡಕ್ಕೆ ಅನುವಾದಿಸಿ")
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AdminItemListTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
}

@Composable
private fun AdminSection(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val container by animateColorAsState(
        targetValue = accent.copy(alpha = 0.08f),
        animationSpec = tween(300),
        label = "admin-card-color"
    )
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .background(accent, RoundedCornerShape(50))
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = accent)
            content()
        }
    }
}

@Composable
private fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private fun AdminSectionKey.label(language: String): String = when (this) {
    AdminSectionKey.AddSchool -> text(language, "Add School", "ಶಾಲೆ ಸೇರಿಸಿ")
    AdminSectionKey.SchoolSettings -> text(language, "School", "ಶಾಲೆ")
    AdminSectionKey.Meal -> text(language, "Meal", "ಊಟ")
    AdminSectionKey.Facilities -> text(language, "Facilities", "ಸೌಲಭ್ಯಗಳು")
    AdminSectionKey.Students -> text(language, "Stars", "ಸ್ಟಾರ್ಸ್")
    AdminSectionKey.Feedback -> text(language, "Feedback", "ಸಲಹೆಗಳು")
}

private fun AdminSectionKey.color(): Color = when (this) {
    AdminSectionKey.AddSchool -> Color(0xFF2563EB)
    AdminSectionKey.SchoolSettings -> Color(0xFF7C3AED)
    AdminSectionKey.Meal -> Color(0xFFEA580C)
    AdminSectionKey.Facilities -> Color(0xFF4F46E5)
    AdminSectionKey.Students -> Color(0xFFB45309)
    AdminSectionKey.Feedback -> Color(0xFF0F766E)
}

private fun MealNutritionAnalysis.toPdf(language: String): ByteArray {
    val writer = PdfReportWriter(text(language, "Meal Nutrition Analysis Report", "ಊಟದ ಪೋಷಣಾ ವಿಶ್ಲೇಷಣೆ ವರದಿ"), Color(0xFFEA580C))
    writer.text("${text(language, "Menu", "ಮೆನು")}: $menu", bold = true)
    writer.space(10f)
    writer.metricRow(
        listOf(
            text(language, "Calories", "ಕ್ಯಾಲರಿ") to calories.toString(),
            text(language, "Protein", "ಪ್ರೋಟೀನ್") to "${protein}g",
            text(language, "Iron", "ಕಬ್ಬಿಣ") to "${iron}mg"
        )
    )
    writer.section(text(language, "Nutrition graph", "ಪೋಷಣಾ ಗ್ರಾಫ್"))
    writer.bar(text(language, "Calories", "ಕ್ಯಾಲರಿ"), calories, 850, Color(0xFFEA580C), "${calories} kcal")
    writer.bar(text(language, "Protein", "ಪ್ರೋಟೀನ್"), protein, 35, Color(0xFF16A34A), "${protein}g")
    writer.bar(text(language, "Iron", "ಕಬ್ಬಿಣ"), (iron * 10).toInt(), 80, Color(0xFF7C3AED), "${iron}mg")
    writer.bar(text(language, "Balanced diet score", "ಸಮತೋಲನ ಆಹಾರ ಸ್ಕೋರ್"), balancedScore, 100, Color(0xFF0F766E))
    writer.section(text(language, "Detected nutrition groups", "ಗುರುತಿಸಿದ ಪೋಷಕ ಗುಂಪುಗಳು"))
    writer.text(detectedGroups.joinToString(", ") { it.localizedAnalysisLabel(language) }.ifBlank { text(language, "Not enough details in menu", "ಮೆನುವಿನಲ್ಲಿ ಸಾಕಷ್ಟು ವಿವರಗಳಿಲ್ಲ") })
    writer.section(text(language, "Suggestions", "ಸಲಹೆಗಳು"))
    (if (language == "kn") suggestionsKn else suggestionsEn).forEach { writer.bullet(it) }
    return writer.finish()
}

private fun FeedbackSentimentAnalysis.toPdf(language: String): ByteArray {
    val writer = PdfReportWriter(text(language, "Feedback Sentiment Analysis Report", "ಪ್ರತಿಕ್ರಿಯೆ ಭಾವನೆ ವಿಶ್ಲೇಷಣೆ ವರದಿ"), Color(0xFF0F766E))
    writer.metricRow(
        listOf(
            text(language, "Positive", "ಧನಾತ್ಮಕ") to positiveCount.toString(),
            text(language, "Neutral", "ತಟಸ್ಥ") to neutralCount.toString(),
            text(language, "Negative", "ಋಣಾತ್ಮಕ") to negativeCount.toString()
        )
    )
    val total = items.size.coerceAtLeast(1)
    writer.section(text(language, "Sentiment graph", "ಭಾವನೆ ಗ್ರಾಫ್"))
    writer.bar(text(language, "Positive", "ಧನಾತ್ಮಕ"), positiveCount, total, Color(0xFF16A34A))
    writer.bar(text(language, "Neutral", "ತಟಸ್ಥ"), neutralCount, total, Color(0xFF64748B))
    writer.bar(text(language, "Negative", "ಋಣಾತ್ಮಕ"), negativeCount, total, Color(0xFFDC2626))
    writer.bar(text(language, "Urgent priority items", "ತುರ್ತು ಆದ್ಯತೆಯ ವಿಷಯಗಳು"), urgentCount, total, Color(0xFFDC2626))
    writer.section(text(language, "Category graph", "ವರ್ಗ ಗ್ರಾಫ್"))
    val maxCount = categoryCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    categoryCounts.forEach { (category, count) ->
        writer.bar(category.localizedAnalysisLabel(language), count, maxCount, Color(0xFF0F766E))
    }
    writer.section(text(language, "Frequently mentioned problems", "ಹೆಚ್ಚಾಗಿ ಉಲ್ಲೇಖಿಸಿದ ಸಮಸ್ಯೆಗಳು"))
    if (frequentProblems.isEmpty()) writer.bullet(text(language, "No repeated complaint pattern found.", "ಪುನರಾವರ್ತಿತ ದೂರು ಮಾದರಿ ಕಂಡುಬಂದಿಲ್ಲ."))
    frequentProblems.forEach { writer.bullet(it.localizedProblem(language)) }
    writer.section(text(language, "Detailed classification", "ವಿವರವಾದ ವರ್ಗೀಕರಣ"))
    items.forEachIndexed { index, item ->
        writer.text("${index + 1}. ${item.sentiment.localizedAnalysisLabel(language)} | ${item.category.localizedAnalysisLabel(language)} | ${item.priority.localizedAnalysisLabel(language)}", bold = true)
        writer.text(item.summary)
        writer.space(6f)
    }
    return writer.finish()
}

private class PdfReportWriter(title: String, private val accent: Color) {
    private val document = PdfDocument()
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 42f
    private var pageNumber = 0
    private lateinit var canvas: android.graphics.Canvas
    private lateinit var page: PdfDocument.Page
    private var y = margin
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent.toArgb()
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(31, 41, 55)
        textSize = 11.5f
    }
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(17, 24, 39)
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    init {
        newPage()
        canvas.drawText(title, margin, y, titlePaint)
        y += 22f
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 5f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent.toArgb() })
        y += 24f
    }

    fun text(value: String, bold: Boolean = false) {
        wrap(value, if (bold) boldPaint else bodyPaint).forEach {
            ensureSpace(18f)
            canvas.drawText(it, margin, y, if (bold) boldPaint else bodyPaint)
            y += 16f
        }
    }

    fun section(value: String) {
        space(10f)
        text(value, bold = true)
    }

    fun bullet(value: String) = text("- $value")

    fun space(amount: Float) {
        ensureSpace(amount)
        y += amount
    }

    fun metricRow(metrics: List<Pair<String, String>>) {
        ensureSpace(74f)
        val gap = 10f
        val width = (pageWidth - margin * 2 - gap * (metrics.size - 1)) / metrics.size
        metrics.forEachIndexed { index, metric ->
            val left = margin + index * (width + gap)
            val rect = RectF(left, y, left + width, y + 62f)
            canvas.drawRoundRect(rect, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent.copy(alpha = 0.10f).toArgb() })
            titlePaint.textSize = 18f
            canvas.drawText(metric.second, left + 12f, y + 26f, titlePaint)
            titlePaint.textSize = 22f
            canvas.drawText(metric.first, left + 12f, y + 48f, bodyPaint)
        }
        y += 76f
    }

    fun bar(label: String, value: Int, max: Int, color: Color, displayValue: String = value.toString()) {
        ensureSpace(38f)
        val ratio = if (max <= 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        canvas.drawText("$label  $displayValue", margin, y, boldPaint)
        y += 9f
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 12f), 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.copy(alpha = 0.14f).toArgb() })
        canvas.drawRoundRect(RectF(margin, y, margin + (pageWidth - margin * 2) * ratio, y + 12f), 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
        y += 28f
    }

    fun finish(): ByteArray {
        document.finishPage(page)
        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    private fun newPage() {
        if (::page.isInitialized) document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
    }

    private fun ensureSpace(required: Float) {
        if (y + required > pageHeight - margin) newPage()
    }

    private fun wrap(value: String, paint: Paint): List<String> {
        val words = value.split(" ")
        val lines = mutableListOf<String>()
        var line = ""
        words.forEach { word ->
            val test = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(test) > pageWidth - margin * 2 && line.isNotBlank()) {
                lines += line
                line = word
            } else {
                line = test
            }
        }
        if (line.isNotBlank()) lines += line
        return lines.ifEmpty { listOf(value) }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

private fun String.localizedProblem(language: String): String {
    if (language != "kn") return this
    val parts = split(":")
    if (parts.size != 2) return localizedAnalysisLabel(language)
    return "${parts[0].localizedAnalysisLabel(language)}:${parts[1]}"
}

private fun String.localizedAnalysisLabel(language: String): String {
    if (language != "kn") return this
    return when (this) {
        "Positive" -> "ಧನಾತ್ಮಕ"
        "Neutral" -> "ತಟಸ್ಥ"
        "Negative" -> "ಋಣಾತ್ಮಕ"
        "High" -> "ಹೆಚ್ಚು"
        "Medium" -> "ಮಧ್ಯಮ"
        "Low" -> "ಕಡಿಮೆ"
        "Meal" -> "ಊಟ"
        "Facilities" -> "ಸೌಲಭ್ಯಗಳು"
        "Academics" -> "ಶೈಕ್ಷಣಿಕ"
        "Transport" -> "ಸಾರಿಗೆ"
        "Safety" -> "ಸುರಕ್ಷತೆ"
        "General" -> "ಸಾಮಾನ್ಯ"
        "Carbohydrate" -> "ಕಾರ್ಬೋಹೈಡ್ರೇಟ್"
        "Whole grain" -> "ಸಂಪೂರ್ಣ ಧಾನ್ಯ"
        "Protein" -> "ಪ್ರೋಟೀನ್"
        "Animal protein" -> "ಪ್ರಾಣಿ ಮೂಲದ ಪ್ರೋಟೀನ್"
        "Iron rich greens" -> "ಕಬ್ಬಿಣ ಸಮೃದ್ಧ ಸೊಪ್ಪು"
        "Vegetables" -> "ತರಕಾರಿಗಳು"
        "Fruit" -> "ಹಣ್ಣು"
        "Energy food" -> "ಶಕ್ತಿ ಆಹಾರ"
        "Balanced" -> "ಸಮತೋಲನ"
        "Moderate" -> "ಮಧ್ಯಮ"
        "Needs improvement" -> "ಸುಧಾರಣೆ ಅಗತ್ಯ"
        else -> this
    }
}

private fun Context.writePdf(uri: Uri, content: ByteArray) {
    contentResolver.openOutputStream(uri)?.use { output ->
        output.write(content)
    }
}

private fun String.localizedAdminStatus(language: String): String {
    if (language != "kn") return this
    return when (this) {
        "Date and English menu are required." -> "ದಿನಾಂಕ ಮತ್ತು ಇಂಗ್ಲಿಷ್ ಮೆನು ಕಡ್ಡಾಯವಾಗಿದೆ."
        "Publishing meal update..." -> "ಊಟದ ನವೀಕರಣವನ್ನು ಪ್ರಕಟಿಸಲಾಗುತ್ತಿದೆ..."
        "Meal update already exists for this date." -> "ಈ ದಿನಾಂಕಕ್ಕೆ ಊಟದ ನವೀಕರಣ ಈಗಾಗಲೇ ಇದೆ."
        "Meal update posted." -> "ಊಟದ ನವೀಕರಣ ಪ್ರಕಟಿಸಲಾಗಿದೆ."
        "Facility name is required." -> "ಸೌಲಭ್ಯದ ಹೆಸರು ಕಡ್ಡಾಯವಾಗಿದೆ."
        "Adding facility..." -> "ಸೌಲಭ್ಯ ಸೇರಿಸಲಾಗುತ್ತಿದೆ..."
        "Facility added." -> "ಸೌಲಭ್ಯ ಸೇರಿಸಲಾಗಿದೆ."
        "Saving facility..." -> "ಸೌಲಭ್ಯ ಉಳಿಸಲಾಗುತ್ತಿದೆ..."
        "Facility updated." -> "ಸೌಲಭ್ಯ ನವೀಕರಿಸಲಾಗಿದೆ."
        "Deleting facility..." -> "ಸೌಲಭ್ಯ ಅಳಿಸಲಾಗುತ್ತಿದೆ..."
        "Facility deleted." -> "ಸೌಲಭ್ಯ ಅಳಿಸಲಾಗಿದೆ."
        "Student name and achievement are required." -> "ವಿದ್ಯಾರ್ಥಿಯ ಹೆಸರು ಮತ್ತು ಸಾಧನೆ ಕಡ್ಡಾಯವಾಗಿದೆ."
        "Publishing student star..." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಪ್ರಕಟಿಸಲಾಗುತ್ತಿದೆ..."
        "Student star published." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಪ್ರಕಟಿಸಲಾಗಿದೆ."
        "Saving student star..." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಉಳಿಸಲಾಗುತ್ತಿದೆ..."
        "Student star updated." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ನವೀಕರಿಸಲಾಗಿದೆ."
        "Deleting student star..." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಅಳಿಸಲಾಗುತ್ತಿದೆ..."
        "Student star deleted." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಅಳಿಸಲಾಗಿದೆ."
        "School name, location, and dice code are required." -> "ಶಾಲೆಯ ಹೆಸರು, ಸ್ಥಳ ಮತ್ತು ಡೈಸ್ ಕೋಡ್ ಕಡ್ಡಾಯವಾಗಿದೆ."
        "Creating school..." -> "ಶಾಲೆ ಸೃಷ್ಟಿಸಲಾಗುತ್ತಿದೆ..."
        "School created." -> "ಶಾಲೆ ಸೃಷ್ಟಿಸಲಾಗಿದೆ."
        "Saving school settings..." -> "ಶಾಲೆಯ ಸೆಟ್ಟಿಂಗ್ಸ್ ಉಳಿಸಲಾಗುತ್ತಿದೆ..."
        "School updated." -> "ಶಾಲೆ ನವೀಕರಿಸಲಾಗಿದೆ."
        "Deleting school..." -> "ಶಾಲೆ ಅಳಿಸಲಾಗುತ್ತಿದೆ..."
        "School deleted." -> "ಶಾಲೆ ಅಳಿಸಲಾಗಿದೆ."
        "Deleting feedback..." -> "ಸಲಹೆ ಅಳಿಸಲಾಗುತ್ತಿದೆ..."
        "Feedback deleted." -> "ಸಲಹೆ ಅಳಿಸಲಾಗಿದೆ."
        "Could not add facility. Check Firebase setup." -> "ಸೌಲಭ್ಯ ಸೇರಿಸಲಾಗಲಿಲ್ಲ. Firebase ವ್ಯವಸ್ಥೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
        "Could not post meal. Check Firebase setup." -> "ಊಟ ಪ್ರಕಟಿಸಲಾಗಲಿಲ್ಲ. Firebase ವ್ಯವಸ್ಥೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
        "Could not publish student star. Check Firebase setup." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಪ್ರಕಟಿಸಲಾಗಲಿಲ್ಲ. Firebase ವ್ಯವಸ್ಥೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
        "Could not create school. Check Firebase setup." -> "ಶಾಲೆ ಸೃಷ್ಟಿಸಲಾಗಲಿಲ್ಲ. Firebase ವ್ಯವಸ್ಥೆಯನ್ನು ಪರಿಶೀಲಿಸಿ."
        "Could not update school." -> "ಶಾಲೆ ನವೀಕರಿಸಲಾಗಲಿಲ್ಲ."
        "Could not delete school." -> "ಶಾಲೆ ಅಳಿಸಲಾಗಲಿಲ್ಲ."
        "Could not update facility." -> "ಸೌಲಭ್ಯ ನವೀಕರಿಸಲಾಗಲಿಲ್ಲ."
        "Could not delete facility." -> "ಸೌಲಭ್ಯ ಅಳಿಸಲಾಗಲಿಲ್ಲ."
        "Could not update student star." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ನವೀಕರಿಸಲಾಗಲಿಲ್ಲ."
        "Could not delete student star." -> "ವಿದ್ಯಾರ್ಥಿ ಸ್ಟಾರ್ ಅಳಿಸಲಾಗಲಿಲ್ಲ."
        "Could not delete feedback." -> "ಸಲಹೆ ಅಳಿಸಲಾಗಲಿಲ್ಲ."
        else -> this
    }
}

private fun text(language: String, en: String, kn: String): String = if (language == "kn") kn else en
