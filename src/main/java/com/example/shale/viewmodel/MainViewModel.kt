package com.example.shale.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shale.data.Feedback
import com.example.shale.data.Facility
import com.example.shale.data.Meal
import com.example.shale.data.School
import com.example.shale.data.ShaleRepository
import com.example.shale.data.StudentStar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = ShaleRepository()

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals

    private val _facilities = MutableStateFlow<List<Facility>>(emptyList())
    val facilities: StateFlow<List<Facility>> = _facilities

    private val _studentStars = MutableStateFlow<List<StudentStar>>(emptyList())
    val studentStars: StateFlow<List<StudentStar>> = _studentStars

    private val _feedbackStatus = MutableStateFlow<String?>(null)
    val feedbackStatus: StateFlow<String?> = _feedbackStatus

    private val _feedback = MutableStateFlow<List<Feedback>>(emptyList())
    val feedback: StateFlow<List<Feedback>> = _feedback

    private val _adminStatus = MutableStateFlow<String?>(null)
    val adminStatus: StateFlow<String?> = _adminStatus

    private var schoolDataJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getSchools().collectLatest { _schools.value = it }
        }
    }

    fun selectSchool(school: School) {
        _selectedSchool.value = school
        schoolDataJob?.cancel()
        _meals.value = emptyList()
        _facilities.value = emptyList()
        _studentStars.value = emptyList()
        _feedback.value = emptyList()

        schoolDataJob = viewModelScope.launch {
            launch {
                repository.getMeals(school.id).collectLatest { _meals.value = it }
            }
            launch {
                repository.getFacilities(school.id).collectLatest { _facilities.value = it }
            }
            launch {
                repository.getStudentStars(school.id).collectLatest { _studentStars.value = it }
            }
            launch {
                repository.getFeedback(school.id).collectLatest { _feedback.value = it }
            }
        }
    }

    fun clearSchool() {
        _selectedSchool.value = null
        schoolDataJob?.cancel()
        _meals.value = emptyList()
        _facilities.value = emptyList()
        _studentStars.value = emptyList()
        _feedback.value = emptyList()
    }

    fun sendFeedback(content: String, anonymous: Boolean, authorName: String?) {
        val school = _selectedSchool.value ?: return
        if (content.isBlank()) {
            _feedbackStatus.value = "Please enter feedback before submitting."
            return
        }

        viewModelScope.launch {
            try {
                repository.sendFeedback(
                    Feedback(
                        schoolId = school.id,
                        content = content.trim(),
                        isAnonymous = anonymous,
                        authorName = authorName?.takeUnless { anonymous || it.isBlank() }
                    )
                )
                _feedbackStatus.value = "Feedback submitted. Thank you."
            } catch (error: Exception) {
                _feedbackStatus.value = "Could not submit feedback. Check Firebase setup."
            }
        }
    }

    fun clearFeedbackStatus() {
        _feedbackStatus.value = null
    }

    fun deleteFeedback(feedbackId: String) {
        if (feedbackId.isBlank()) return
        viewModelScope.launch {
            try {
                _adminStatus.value = "Deleting feedback..."
                repository.deleteFeedback(feedbackId)
                _adminStatus.value = "Feedback deleted."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not delete feedback."
            }
        }
    }

    fun postMeal(date: String, menuEn: String, menuKn: String, imageUrl: String, imageUri: Uri?) {
        val school = _selectedSchool.value ?: return
        if (date.isBlank() || menuEn.isBlank()) {
            _adminStatus.value = "Date and English menu are required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Publishing meal update..."
                if (repository.mealExists(school.id, date.trim())) {
                    _adminStatus.value = "Meal update already exists for this date."
                    return@launch
                }
                val finalImageUrl = if (imageUri != null) {
                    repository.uploadMealImage(school.id, date.trim(), imageUri)
                } else {
                    imageUrl.trim()
                }
                repository.postMeal(
                    Meal(
                        schoolId = school.id,
                        date = date.trim(),
                        menuEn = menuEn.trim(),
                        menuKn = menuKn.trim(),
                        imageUrl = finalImageUrl
                    )
                )
                _adminStatus.value = "Meal update posted."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not post meal. Check Firebase setup."
            }
        }
    }

    fun addFacility(
        nameEn: String,
        nameKn: String,
        descriptionEn: String,
        descriptionKn: String,
        imageUrl: String,
        imageUri: Uri?
    ) {
        val school = _selectedSchool.value ?: return
        if (nameEn.isBlank()) {
            _adminStatus.value = "Facility name is required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Adding facility..."
                val finalImageUrl = if (imageUri != null) repository.uploadFacilityImage(school.id, imageUri) else imageUrl.trim()
                repository.addFacility(
                    Facility(
                        schoolId = school.id,
                        nameEn = nameEn.trim(),
                        nameKn = nameKn.trim(),
                        descriptionEn = descriptionEn.trim(),
                        descriptionKn = descriptionKn.trim(),
                        imageUrl = finalImageUrl
                    )
                )
                _adminStatus.value = "Facility added."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not add facility. Check Firebase setup."
            }
        }
    }

    fun updateFacility(
        facilityId: String,
        nameEn: String,
        nameKn: String,
        descriptionEn: String,
        descriptionKn: String,
        imageUrl: String,
        imageUri: Uri? = null
    ) {
        val school = _selectedSchool.value ?: return
        if (facilityId.isBlank() || nameEn.isBlank()) {
            _adminStatus.value = "Facility name is required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Saving facility..."
                val finalImageUrl = if (imageUri != null) repository.uploadFacilityImage(school.id, imageUri) else imageUrl.trim()
                repository.updateFacility(
                    Facility(
                        id = facilityId,
                        schoolId = school.id,
                        nameEn = nameEn.trim(),
                        nameKn = nameKn.trim(),
                        descriptionEn = descriptionEn.trim(),
                        descriptionKn = descriptionKn.trim(),
                        imageUrl = finalImageUrl
                    )
                )
                _adminStatus.value = "Facility updated."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not update facility."
            }
        }
    }

    fun deleteFacility(facilityId: String) {
        if (facilityId.isBlank()) return
        viewModelScope.launch {
            try {
                _adminStatus.value = "Deleting facility..."
                repository.deleteFacility(facilityId)
                _adminStatus.value = "Facility deleted."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not delete facility."
            }
        }
    }

    fun addStudentStar(
        name: String,
        nameKn: String,
        titleEn: String,
        titleKn: String,
        achievementEn: String,
        achievementKn: String,
        quote: String,
        imageUrl: String,
        imageUri: Uri?,
        date: String
    ) {
        val school = _selectedSchool.value ?: return
        if (name.isBlank() || achievementEn.isBlank()) {
            _adminStatus.value = "Student name and achievement are required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Publishing student star..."
                val finalImageUrl = if (imageUri != null) repository.uploadStudentImage(school.id, imageUri) else imageUrl.trim()
                repository.addStudentStar(
                    StudentStar(
                        schoolId = school.id,
                        name = name.trim(),
                        nameKn = nameKn.trim(),
                        titleEn = titleEn.trim(),
                        titleKn = titleKn.trim(),
                        achievementEn = achievementEn.trim(),
                        achievementKn = achievementKn.trim(),
                        achievement = achievementEn.trim(),
                        quote = quote.trim(),
                        imageUrl = finalImageUrl,
                        date = date.trim()
                    )
                )
                _adminStatus.value = "Student star published."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not publish student star. Check Firebase setup."
            }
        }
    }

    fun updateStudentStar(
        studentId: String,
        name: String,
        nameKn: String,
        titleEn: String,
        titleKn: String,
        achievementEn: String,
        achievementKn: String,
        quote: String,
        imageUrl: String,
        date: String,
        imageUri: Uri? = null
    ) {
        val school = _selectedSchool.value ?: return
        if (studentId.isBlank() || name.isBlank() || achievementEn.isBlank()) {
            _adminStatus.value = "Student name and achievement are required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Saving student star..."
                val finalImageUrl = if (imageUri != null) repository.uploadStudentImage(school.id, imageUri) else imageUrl.trim()
                repository.updateStudentStar(
                    StudentStar(
                        id = studentId,
                        schoolId = school.id,
                        name = name.trim(),
                        nameKn = nameKn.trim(),
                        titleEn = titleEn.trim(),
                        titleKn = titleKn.trim(),
                        achievementEn = achievementEn.trim(),
                        achievementKn = achievementKn.trim(),
                        achievement = achievementEn.trim(),
                        quote = quote.trim(),
                        imageUrl = finalImageUrl,
                        date = date.trim()
                    )
                )
                _adminStatus.value = "Student star updated."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not update student star."
            }
        }
    }

    fun deleteStudentStar(studentId: String) {
        if (studentId.isBlank()) return
        viewModelScope.launch {
            try {
                _adminStatus.value = "Deleting student star..."
                repository.deleteStudentStar(studentId)
                _adminStatus.value = "Student star deleted."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not delete student star."
            }
        }
    }

    fun createSchool(
        adminUid: String,
        adminEmail: String?,
        nameEn: String,
        nameKn: String,
        locationEn: String,
        locationKn: String,
        diceCode: String
    ) {
        if (nameEn.isBlank() || locationEn.isBlank() || diceCode.isBlank()) {
            _adminStatus.value = "School name, location, and dice code are required."
            return
        }
        viewModelScope.launch {
            try {
                _adminStatus.value = "Creating school..."
                repository.createSchool(
                    nameEn = nameEn.trim(),
                    nameKn = nameKn.trim(),
                    locationEn = locationEn.trim(),
                    locationKn = locationKn.trim(),
                    diceCode = diceCode.trim(),
                    adminUid = adminUid,
                    adminEmail = adminEmail
                )
                _adminStatus.value = "School created."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not create school. Check Firebase setup."
            }
        }
    }

    fun updateSchoolDetails(
        adminUid: String,
        schoolId: String,
        nameEn: String,
        nameKn: String,
        locationEn: String,
        locationKn: String,
        diceCode: String
    ) {
        if (schoolId.isBlank()) return
        viewModelScope.launch {
            try {
                _adminStatus.value = "Saving school settings..."
                repository.updateSchool(
                    schoolId = schoolId,
                    adminUid = adminUid,
                    updates = mapOf(
                        "nameEn" to nameEn.trim(),
                        "nameKn" to nameKn.trim(),
                        "locationEn" to locationEn.trim(),
                        "locationKn" to locationKn.trim(),
                        "diceCode" to diceCode.trim()
                    )
                )
                _adminStatus.value = "School updated."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not update school."
            }
        }
    }

    fun deleteSchool(adminUid: String, schoolId: String) {
        if (schoolId.isBlank()) return
        viewModelScope.launch {
            try {
                _adminStatus.value = "Deleting school..."
                repository.deleteSchool(schoolId, adminUid)
                _adminStatus.value = "School deleted."
            } catch (error: Exception) {
                _adminStatus.value = error.message ?: "Could not delete school."
            }
        }
    }
}
