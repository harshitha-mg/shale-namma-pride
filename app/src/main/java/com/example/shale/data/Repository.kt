package com.example.shale.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class School(
    val id: String = "",
    val nameEn: String = "",
    val nameKn: String = "",
    val locationEn: String = "",
    val locationKn: String = "",
    val diceCode: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val name: String = "",
    val location: String = ""
) {
    fun displayName(language: String): String =
        if (language == "kn") nameKn.ifBlank { nameEn.ifBlank { name.ifBlank { "School" } } }
        else nameEn.ifBlank { name.ifBlank { nameKn.ifBlank { "School" } } }

    fun displayLocation(language: String): String =
        if (language == "kn") locationKn.ifBlank { locationEn.ifBlank { location } }
        else locationEn.ifBlank { location.ifBlank { locationKn } }
}

data class Meal(
    val id: String = "",
    val schoolId: String = "",
    val date: String = "",
    val menuEn: String = "",
    val menuKn: String = "",
    val imageUrl: String = ""
)

data class Facility(
    val id: String = "",
    val schoolId: String = "",
    val name: String = "",
    val nameEn: String = "",
    val nameKn: String = "",
    val descriptionEn: String = "",
    val descriptionKn: String = "",
    val imageUrl: String = ""
) {
    fun displayName(language: String): String =
        if (language == "kn") nameKn.ifBlank { nameEn.ifBlank { name.ifBlank { "Facility" } } }
        else nameEn.ifBlank { name.ifBlank { nameKn.ifBlank { "Facility" } } }

    fun displayDescription(language: String): String =
        if (language == "kn") descriptionKn.ifBlank { descriptionEn }
        else descriptionEn.ifBlank { descriptionKn }
}

data class StudentStar(
    val id: String = "",
    val schoolId: String = "",
    val name: String = "",
    val nameKn: String = "",
    val achievement: String = "",
    val achievementEn: String = "",
    val achievementKn: String = "",
    val titleEn: String = "",
    val titleKn: String = "",
    val quote: String = "",
    val imageUrl: String = "",
    val date: String = ""
) {
    fun displayName(language: String): String =
        if (language == "kn") nameKn.ifBlank { name.ifBlank { "Student" } }
        else name.ifBlank { nameKn.ifBlank { "Student" } }

    fun displayTitle(language: String): String =
        if (language == "kn") titleKn.ifBlank { titleEn.ifBlank { "Student Star" } }
        else titleEn.ifBlank { titleKn.ifBlank { "Student Star" } }

    fun displayAchievement(language: String): String =
        if (language == "kn") achievementKn.ifBlank { achievementEn.ifBlank { achievement } }
        else achievementEn.ifBlank { achievement.ifBlank { achievementKn } }
}

data class Feedback(
    val id: String = "",
    val schoolId: String = "",
    val content: String = "",
    val isAnonymous: Boolean = true,
    val authorName: String? = null,
    val timestamp: Timestamp? = null
)

class ShaleRepository {
    private companion object {
        const val FIRESTORE_DATABASE_ID = "ai-studio-63469dff-58db-465d-aead-0d809c39f872"
    }

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID)
        } catch (error: IllegalStateException) {
            null
        }

    private val storage: FirebaseStorage?
        get() = try {
            FirebaseStorage.getInstance()
        } catch (error: IllegalStateException) {
            null
        }

    fun getSchools(): Flow<List<School>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("schools")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val schools = snapshot.documents.map { it.toSchool() }
                    trySend(schools)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createSchool(
        nameEn: String,
        nameKn: String,
        locationEn: String,
        locationKn: String,
        diceCode: String,
        adminUid: String,
        adminEmail: String?
    ): String {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        val ref = db.collection("schools").add(
            mapOf(
                "nameEn" to nameEn,
                "nameKn" to nameKn,
                "locationEn" to locationEn,
                "locationKn" to locationKn,
                "diceCode" to diceCode,
                "adminUid" to adminUid,
                "adminEmail" to (adminEmail ?: ""),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return ref.id
    }

    suspend fun updateSchool(
        schoolId: String,
        adminUid: String,
        updates: Map<String, Any?>
    ) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        val ref = db.collection("schools").document(schoolId)
        val snapshot = ref.get().await()
        val ownerUid = snapshot.getString("adminUid") ?: ""
        if (ownerUid.isNotBlank() && ownerUid != adminUid) {
            throw IllegalStateException("Only the school admin can edit this school.")
        }
        ref.set(updates.filterValues { it != null }, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteSchool(schoolId: String, adminUid: String) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        val ref = db.collection("schools").document(schoolId)
        val snapshot = ref.get().await()
        val ownerUid = snapshot.getString("adminUid") ?: ""
        if (ownerUid.isNotBlank() && ownerUid != adminUid) {
            throw IllegalStateException("Only the school admin can delete this school.")
        }
        ref.delete().await()
    }

    fun getMeals(schoolId: String): Flow<List<Meal>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("meals")
            .whereEqualTo("schoolId", schoolId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.map { it.toMeal() }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getFacilities(schoolId: String): Flow<List<Facility>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("facilities")
            .whereEqualTo("schoolId", schoolId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.map { it.toFacility() }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getStudentStars(schoolId: String): Flow<List<StudentStar>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("students")
            .whereEqualTo("schoolId", schoolId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.map { it.toStudentStar() }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getFeedback(schoolId: String): Flow<List<Feedback>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("feedback")
            .whereEqualTo("schoolId", schoolId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.map { it.toFeedback() }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendFeedback(feedback: Feedback) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")

        db.collection("feedback").add(
            mapOf(
                "schoolId" to feedback.schoolId,
                "content" to feedback.content,
                "isAnonymous" to feedback.isAnonymous,
                "authorName" to feedback.authorName,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteFeedback(feedbackId: String) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("feedback").document(feedbackId).delete().await()
    }

    suspend fun postMeal(meal: Meal) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        val mealId = "${meal.schoolId}_${meal.date}"
        val mealRef = db.collection("meals").document(mealId)
        if (mealRef.get().await().exists()) {
            throw IllegalStateException("Meal update already exists for this date.")
        }
        mealRef.set(
            mapOf(
                "schoolId" to meal.schoolId,
                "date" to meal.date,
                "menuEn" to meal.menuEn,
                "menuKn" to meal.menuKn,
                "imageUrl" to meal.imageUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun mealExists(schoolId: String, date: String): Boolean {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        return db.collection("meals").document("${schoolId}_${date}").get().await().exists()
    }

    suspend fun uploadMealImage(schoolId: String, date: String, imageUri: Uri): String {
        val storageRef = storage?.reference ?: throw IllegalStateException("Firebase Storage is not configured.")
        val imageRef = storageRef.child("meal_images/$schoolId/$date.jpg")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    suspend fun uploadFacilityImage(schoolId: String, imageUri: Uri): String {
        val storageRef = storage?.reference ?: throw IllegalStateException("Firebase Storage is not configured.")
        val imageRef = storageRef.child("facility_images/$schoolId/${System.currentTimeMillis()}.jpg")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    suspend fun uploadStudentImage(schoolId: String, imageUri: Uri): String {
        val storageRef = storage?.reference ?: throw IllegalStateException("Firebase Storage is not configured.")
        val imageRef = storageRef.child("student_images/$schoolId/${System.currentTimeMillis()}.jpg")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    suspend fun addFacility(facility: Facility) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("facilities").add(
            mapOf(
                "schoolId" to facility.schoolId,
                "nameEn" to facility.nameEn,
                "nameKn" to facility.nameKn,
                "descriptionEn" to facility.descriptionEn,
                "descriptionKn" to facility.descriptionKn,
                "imageUrl" to facility.imageUrl,
                "category" to "Classroom"
            )
        ).await()
    }

    suspend fun updateFacility(facility: Facility) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("facilities").document(facility.id).set(
            mapOf(
                "schoolId" to facility.schoolId,
                "nameEn" to facility.nameEn,
                "nameKn" to facility.nameKn,
                "descriptionEn" to facility.descriptionEn,
                "descriptionKn" to facility.descriptionKn,
                "imageUrl" to facility.imageUrl
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun deleteFacility(facilityId: String) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("facilities").document(facilityId).delete().await()
    }

    suspend fun addStudentStar(student: StudentStar) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("students").add(
            mapOf(
                "schoolId" to student.schoolId,
                "name" to student.name,
                "nameKn" to student.nameKn,
                "titleEn" to student.titleEn,
                "titleKn" to student.titleKn,
                "achievementEn" to student.achievementEn,
                "achievementKn" to student.achievementKn,
                "achievement" to student.achievement,
                "quote" to student.quote,
                "imageUrl" to student.imageUrl,
                "date" to student.date
            )
        ).await()
    }

    suspend fun updateStudentStar(student: StudentStar) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("students").document(student.id).set(
            mapOf(
                "schoolId" to student.schoolId,
                "name" to student.name,
                "nameKn" to student.nameKn,
                "titleEn" to student.titleEn,
                "titleKn" to student.titleKn,
                "achievementEn" to student.achievementEn,
                "achievementKn" to student.achievementKn,
                "achievement" to student.achievementEn,
                "quote" to student.quote,
                "imageUrl" to student.imageUrl,
                "date" to student.date
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun deleteStudentStar(studentId: String) {
        val db = firestore ?: throw IllegalStateException("Firebase is not configured. Add google-services.json to android/app.")
        db.collection("students").document(studentId).delete().await()
    }

    private fun DocumentSnapshot.stringValue(field: String): String =
        getString(field) ?: get(field)?.toString().orEmpty()

    private fun DocumentSnapshot.toSchool(): School = School(
        id = id,
        nameEn = stringValue("nameEn"),
        nameKn = stringValue("nameKn"),
        locationEn = stringValue("locationEn"),
        locationKn = stringValue("locationKn"),
        diceCode = stringValue("diceCode"),
        adminUid = stringValue("adminUid"),
        adminEmail = stringValue("adminEmail"),
        name = stringValue("name"),
        location = stringValue("location")
    )

    private fun DocumentSnapshot.toMeal(): Meal = Meal(
        id = id,
        schoolId = stringValue("schoolId"),
        date = stringValue("date"),
        menuEn = stringValue("menuEn"),
        menuKn = stringValue("menuKn"),
        imageUrl = stringValue("imageUrl")
    )

    private fun DocumentSnapshot.toFacility(): Facility = Facility(
        id = id,
        schoolId = stringValue("schoolId"),
        name = stringValue("name"),
        nameEn = stringValue("nameEn"),
        nameKn = stringValue("nameKn"),
        descriptionEn = stringValue("descriptionEn"),
        descriptionKn = stringValue("descriptionKn"),
        imageUrl = stringValue("imageUrl")
    )

    private fun DocumentSnapshot.toStudentStar(): StudentStar = StudentStar(
        id = id,
        schoolId = stringValue("schoolId"),
        name = stringValue("name"),
        nameKn = stringValue("nameKn"),
        achievement = stringValue("achievement"),
        achievementEn = stringValue("achievementEn"),
        achievementKn = stringValue("achievementKn"),
        titleEn = stringValue("titleEn"),
        titleKn = stringValue("titleKn"),
        quote = stringValue("quote"),
        imageUrl = stringValue("imageUrl"),
        date = stringValue("date")
    )

    private fun DocumentSnapshot.toFeedback(): Feedback = Feedback(
        id = id,
        schoolId = stringValue("schoolId"),
        content = stringValue("content"),
        isAnonymous = getBoolean("isAnonymous") ?: true,
        authorName = getString("authorName"),
        timestamp = getTimestamp("timestamp")
    )
}
