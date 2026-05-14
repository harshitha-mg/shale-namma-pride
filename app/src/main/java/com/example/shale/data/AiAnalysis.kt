package com.example.shale.data

import com.example.shale.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class MealNutritionAnalysis(
    val menu: String,
    val calories: Int,
    val protein: Int,
    val iron: Double,
    val balancedScore: Int,
    val nutritionLevel: String,
    val suggestionsEn: List<String>,
    val suggestionsKn: List<String>,
    val detectedGroups: List<String>
)

data class FeedbackAnalysisItem(
    val feedback: Feedback,
    val sentiment: String,
    val category: String,
    val priority: String,
    val summary: String
)

data class FeedbackSentimentAnalysis(
    val items: List<FeedbackAnalysisItem>,
    val positiveCount: Int,
    val neutralCount: Int,
    val negativeCount: Int,
    val urgentCount: Int,
    val categoryCounts: Map<String, Int>,
    val frequentProblems: List<String>
)

object AiAnalysisEngine {
    fun analyzeMeal(meal: Meal): MealNutritionAnalysis {
        val menu = listOf(meal.menuEn, meal.menuKn).joinToString(" ").lowercase()
        val groups = mutableSetOf<String>()
        var calories = 180
        var protein = 4
        var iron = 1.2

        fun containsAny(vararg words: String): Boolean = words.any { menu.contains(it) }

        if (containsAny("rice", "anna", "ಅನ್ನ", "chitranna", "pulav", "ಪಲಾವ್", "ಅಕ್ಕಿ")) {
            groups += "Carbohydrate"
            calories += 220
            protein += 4
            iron += 0.6
        }
        if (containsAny("chapati", "roti", "ರೊಟ್ಟಿ", "ಚಪಾತಿ", "wheat", "ಗೋಧಿ")) {
            groups += "Whole grain"
            calories += 170
            protein += 5
            iron += 1.5
        }
        if (containsAny("dal", "sambar", "ಸಾಂಬಾರ್", "ಬೇಳೆ", "lentil", "ಕಾಳು", "chana", "gram", "peas")) {
            groups += "Protein"
            calories += 140
            protein += 9
            iron += 2.1
        }
        if (containsAny("egg", "ಮೊಟ್ಟೆ", "milk", "ಹಾಲು", "curd", "ಮೊಸರು", "paneer")) {
            groups += "Animal protein"
            calories += 110
            protein += 8
            iron += 0.8
        }
        if (containsAny("spinach", "palak", "ಸೊಪ್ಪು", "greens", "leafy", "ಮೆಂತ್ಯ", "ಮೂಲಂಗಿ")) {
            groups += "Iron rich greens"
            calories += 45
            protein += 3
            iron += 2.7
        }
        if (containsAny("vegetable", "ತರಕಾರಿ", "carrot", "beans", "cabbage", "ಕ್ಯಾರೆಟ್", "ಬೀನ್ಸ್", "ಕೋಸು")) {
            groups += "Vegetables"
            calories += 55
            protein += 2
            iron += 0.9
        }
        if (containsAny("banana", "ಬಾಳೆ", "fruit", "ಹಣ್ಣು", "apple", "orange", "ಮಾವಿನ")) {
            groups += "Fruit"
            calories += 90
            protein += 1
            iron += 0.3
        }
        if (containsAny("sweet", "payasam", "ಪಾಯಸ", "ಸಿಹಿ", "jaggery", "ಬೆಲ್ಲ")) {
            groups += "Energy food"
            calories += 120
        }

        val score = (
            28 +
                groups.count { it in listOf("Carbohydrate", "Whole grain") } * 12 +
                groups.count { it in listOf("Protein", "Animal protein") } * 18 +
                groups.count { it == "Iron rich greens" } * 16 +
                groups.count { it == "Vegetables" } * 14 +
                groups.count { it == "Fruit" } * 8
            ).coerceIn(0, 100)

        val suggestionsEn = buildList {
            if (groups.none { it in listOf("Protein", "Animal protein") }) add("Add dal, chana, egg, milk, or curd to improve protein.")
            if ("Iron rich greens" !in groups) add("Add leafy greens or iron-rich vegetables at least a few times a week.")
            if ("Vegetables" !in groups) add("Include one cooked vegetable portion for vitamins and fiber.")
            if ("Fruit" !in groups) add("Add a fruit when possible for micronutrients.")
            if (score >= 80) add("Meal looks balanced. Keep rotating grains, pulses, vegetables, and fruits.")
        }
        val suggestionsKn = buildList {
            if (groups.none { it in listOf("Protein", "Animal protein") }) add("ಪ್ರೋಟೀನ್ ಹೆಚ್ಚಿಸಲು ಬೇಳೆ, ಕಡಲೆ, ಮೊಟ್ಟೆ, ಹಾಲು ಅಥವಾ ಮೊಸರು ಸೇರಿಸಿ.")
            if ("Iron rich greens" !in groups) add("ಕಬ್ಬಿಣಾಂಶಕ್ಕಾಗಿ ಸೊಪ್ಪು ಅಥವಾ ಕಬ್ಬಿಣ ಸಮೃದ್ಧ ತರಕಾರಿಗಳನ್ನು ವಾರದಲ್ಲಿ ಕೆಲವು ಬಾರಿ ಸೇರಿಸಿ.")
            if ("Vegetables" !in groups) add("ವಿಟಮಿನ್ ಮತ್ತು ಫೈಬರ್‌ಗಾಗಿ ಒಂದು ತರಕಾರಿ ಭಾಗ ಸೇರಿಸಿ.")
            if ("Fruit" !in groups) add("ಸಾಧ್ಯವಾದರೆ ಸೂಕ್ಷ್ಮ ಪೋಷಕಾಂಶಗಳಿಗಾಗಿ ಹಣ್ಣು ಸೇರಿಸಿ.")
            if (score >= 80) add("ಊಟ ಸಮತೋಲನವಾಗಿದೆ. ಧಾನ್ಯ, ಬೇಳೆ, ತರಕಾರಿ ಮತ್ತು ಹಣ್ಣುಗಳನ್ನು ಬದಲಾಯಿಸಿ ನೀಡುತ್ತಿರಿ.")
        }

        return MealNutritionAnalysis(
            menu = if (meal.menuEn.isNotBlank()) meal.menuEn else meal.menuKn,
            calories = calories.coerceIn(200, 850),
            protein = protein.coerceIn(4, 35),
            iron = ((iron * 10).roundToInt() / 10.0).coerceIn(0.5, 8.0),
            balancedScore = score,
            nutritionLevel = when {
                score >= 80 -> "Balanced"
                score >= 60 -> "Moderate"
                else -> "Needs improvement"
            },
            suggestionsEn = suggestionsEn,
            suggestionsKn = suggestionsKn,
            detectedGroups = groups.toList()
        )
    }

    fun analyzeFeedback(feedback: List<Feedback>): FeedbackSentimentAnalysis {
        val items = feedback.map { item ->
            val content = item.content.lowercase()
            val negative = listOf("bad", "poor", "dirty", "late", "problem", "complaint", "unsafe", "not", "delay", "water", "toilet", "bully", "fight", "ಅಸಮಾಧಾನ", "ಕೆಟ್ಟ", "ಸಮಸ್ಯೆ", "ತಡ", "ಅಸ್ವಚ್ಛ", "ನೀರು", "ಶೌಚಾಲಯ")
                .count { content.contains(it) }
            val positive = listOf("good", "great", "happy", "nice", "excellent", "thank", "improve", "clean", "support", "ಚೆನ್ನಾಗಿದೆ", "ಧನ್ಯವಾದ", "ಉತ್ತಮ", "ಸಂತೋಷ", "ಸ್ವಚ್ಛ")
                .count { content.contains(it) }
            val urgent = listOf("urgent", "immediately", "unsafe", "sick", "injury", "harassment", "bully", "fight", "danger", "ತುರ್ತು", "ಅಪಾಯ", "ಗಾಯ", "ಕಿರುಕುಳ")
                .any { content.contains(it) }
            val category = when {
                listOf("meal", "food", "lunch", "ಊಟ", "ಆಹಾರ").any { content.contains(it) } -> "Meal"
                listOf("water", "toilet", "clean", "dirty", "ನೀರು", "ಶೌಚಾಲಯ", "ಸ್ವಚ್ಛ", "ಅಸ್ವಚ್ಛ").any { content.contains(it) } -> "Facilities"
                listOf("teacher", "class", "study", "homework", "ಶಿಕ್ಷಕ", "ತರಗತಿ", "ಪಾಠ").any { content.contains(it) } -> "Academics"
                listOf("bus", "transport", "road", "ಬಸ್", "ಸಾರಿಗೆ").any { content.contains(it) } -> "Transport"
                listOf("bully", "fight", "unsafe", "harassment", "ಕಿರುಕುಳ", "ಜಗಳ", "ಅಪಾಯ").any { content.contains(it) } -> "Safety"
                else -> "General"
            }
            val sentiment = when {
                negative > positive -> "Negative"
                positive > negative -> "Positive"
                else -> "Neutral"
            }
            val priority = when {
                urgent -> "High"
                sentiment == "Negative" -> "Medium"
                else -> "Low"
            }
            FeedbackAnalysisItem(
                feedback = item,
                sentiment = sentiment,
                category = category,
                priority = priority,
                summary = item.content.trim().take(90).ifBlank { "No details provided" }
            )
        }

        val categoryCounts = items.groupingBy { it.category }.eachCount().toSortedMap()
        val frequentProblems = items
            .filter { it.sentiment == "Negative" || it.priority == "High" }
            .groupingBy { it.category }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { "${it.key}: ${it.value}" }

        return FeedbackSentimentAnalysis(
            items = items,
            positiveCount = items.count { it.sentiment == "Positive" },
            neutralCount = items.count { it.sentiment == "Neutral" },
            negativeCount = items.count { it.sentiment == "Negative" },
            urgentCount = items.count { it.priority == "High" },
            categoryCounts = categoryCounts,
            frequentProblems = frequentProblems
        )
    }
}

object GeminiAnalysisApi {
    private val baseUrl: String
        get() = BuildConfig.GEMINI_BACKEND_URL.trim().trimEnd('/')

    suspend fun analyzeMeal(meal: Meal, language: String): MealNutritionAnalysis? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext null
        runCatching {
            val payload = JSONObject()
                .put("date", meal.date)
                .put("menuEn", meal.menuEn)
                .put("menuKn", meal.menuKn)
                .put("language", language)
            val json = postJson("/analyze-meal", payload)
            json.toMealNutritionAnalysis(meal)
        }.getOrNull()
    }

    suspend fun analyzeFeedback(feedback: List<Feedback>, language: String): FeedbackSentimentAnalysis? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext null
        runCatching {
            val feedbackArray = JSONArray()
            feedback.forEach { item ->
                feedbackArray.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("content", item.content)
                )
            }
            val payload = JSONObject()
                .put("language", language)
                .put("feedback", feedbackArray)
            val json = postJson("/analyze-feedback", payload)
            json.toFeedbackSentimentAnalysis(feedback)
        }.getOrNull()
    }

    private fun postJson(path: String, payload: JSONObject): JSONObject {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(payload.toString())
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException(body)
        return JSONObject(body)
    }

    private fun JSONObject.toMealNutritionAnalysis(fallbackMeal: Meal): MealNutritionAnalysis =
        MealNutritionAnalysis(
            menu = optString("menu", fallbackMeal.menuEn.ifBlank { fallbackMeal.menuKn }),
            calories = optInt("calories", 0).coerceIn(0, 1200),
            protein = optInt("protein", 0).coerceIn(0, 80),
            iron = optDouble("iron", 0.0).coerceIn(0.0, 20.0),
            balancedScore = optInt("balancedScore", 0).coerceIn(0, 100),
            nutritionLevel = optString("nutritionLevel", "Moderate"),
            suggestionsEn = optJSONArray("suggestionsEn").toStringList(),
            suggestionsKn = optJSONArray("suggestionsKn").toStringList(),
            detectedGroups = optJSONArray("detectedGroups").toStringList()
        )

    private fun JSONObject.toFeedbackSentimentAnalysis(sourceFeedback: List<Feedback>): FeedbackSentimentAnalysis {
        val sourceByIndex = sourceFeedback.mapIndexed { index, item -> index to item }.toMap()
        val itemsJson = optJSONArray("items") ?: JSONArray()
        val items = List(itemsJson.length()) { index ->
            val item = itemsJson.optJSONObject(index) ?: JSONObject()
            FeedbackAnalysisItem(
                feedback = sourceByIndex[index] ?: Feedback(content = item.optString("summary")),
                sentiment = item.optString("sentiment", "Neutral"),
                category = item.optString("category", "General"),
                priority = item.optString("priority", "Low"),
                summary = item.optString("summary", "No summary")
            )
        }
        val categoryObject = optJSONObject("categoryCounts") ?: JSONObject()
        val categoryCounts = listOf("Meal", "Facilities", "Academics", "Transport", "Safety", "General")
            .associateWith { categoryObject.optInt(it, 0) }
            .filterValues { it > 0 }
        return FeedbackSentimentAnalysis(
            items = items,
            positiveCount = optInt("positiveCount", items.count { it.sentiment == "Positive" }),
            neutralCount = optInt("neutralCount", items.count { it.sentiment == "Neutral" }),
            negativeCount = optInt("negativeCount", items.count { it.sentiment == "Negative" }),
            urgentCount = optInt("urgentCount", items.count { it.priority == "High" }),
            categoryCounts = categoryCounts,
            frequentProblems = optJSONArray("frequentProblems").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }
}

object GeminiDeveloperRestApi {
    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.trim()

    suspend fun analyzeMeal(meal: Meal, language: String): MealNutritionAnalysis? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val prompt = """
                Analyze this Karnataka school mid-day meal menu.

                Date: ${meal.date}
                English menu: ${meal.menuEn}
                Kannada menu: ${meal.menuKn}
                Preferred UI language: $language

                Return only JSON with these exact fields:
                {
                  "menu": "string",
                  "calories": 450,
                  "protein": 14,
                  "iron": 3.2,
                  "balancedScore": 75,
                  "nutritionLevel": "Balanced|Moderate|Needs improvement",
                  "suggestionsEn": ["string"],
                  "suggestionsKn": ["string"],
                  "detectedGroups": ["Carbohydrate", "Protein"]
                }
            """.trimIndent()
            val text = generateText(prompt)
            JSONObject(text.cleanJson()).toMealNutritionAnalysis(meal)
        }.getOrNull()
    }

    suspend fun analyzeFeedback(feedback: List<Feedback>, language: String): FeedbackSentimentAnalysis? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val feedbackText = feedback.take(50).mapIndexed { index, item -> "${index + 1}. ${item.content}" }.joinToString("\n")
            val prompt = """
                Analyze the following school feedback from parents/students.

                Preferred UI language: $language

                Feedback:
                $feedbackText

                Return only JSON with these exact fields:
                {
                  "items": [
                    {
                      "sentiment": "Positive|Neutral|Negative",
                      "category": "Meal|Facilities|Academics|Transport|Safety|General",
                      "priority": "High|Medium|Low",
                      "summary": "one line"
                    }
                  ],
                  "positiveCount": 0,
                  "neutralCount": 0,
                  "negativeCount": 0,
                  "urgentCount": 0,
                  "categoryCounts": {
                    "Meal": 0,
                    "Facilities": 0,
                    "Academics": 0,
                    "Transport": 0,
                    "Safety": 0,
                    "General": 0
                  },
                  "frequentProblems": ["Facilities: 2"]
                }
            """.trimIndent()
            val text = generateText(prompt)
            JSONObject(text.cleanJson()).toFeedbackSentimentAnalysis(feedback)
        }.getOrNull()
    }

    suspend fun translateText(text: String, targetLanguage: String): String? = withContext(Dispatchers.IO) {
        android.util.Log.d("GEMINI_DEBUG", "API KEY = '$apiKey'")
        if (apiKey.isBlank() || text.isBlank()) return@withContext null
        runCatching {
            val prompt = """
                Translate the following school feedback text to ${if (targetLanguage == "kn") "Kannada" else "English"}.
                Keep the meaning natural and concise.
                Return only the translated text, without quotes or notes.

                Text:
                $text
            """.trimIndent()
            generateText(prompt).trim()
        }.getOrNull()
    }

    private fun generateText(prompt: String): String {
        val payload = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("temperature", 0.2)
            )

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(payload.toString())
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException(body)
        return JSONObject(body)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
    }

    private fun String.cleanJson(): String =
        trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
}

private fun JSONObject.toMealNutritionAnalysis(fallbackMeal: Meal): MealNutritionAnalysis =
    MealNutritionAnalysis(
        menu = optString("menu", fallbackMeal.menuEn.ifBlank { fallbackMeal.menuKn }),
        calories = optInt("calories", 0).coerceIn(0, 1200),
        protein = optInt("protein", 0).coerceIn(0, 80),
        iron = optDouble("iron", 0.0).coerceIn(0.0, 20.0),
        balancedScore = optInt("balancedScore", 0).coerceIn(0, 100),
        nutritionLevel = optString("nutritionLevel", "Moderate"),
        suggestionsEn = optJSONArray("suggestionsEn").toStringList(),
        suggestionsKn = optJSONArray("suggestionsKn").toStringList(),
        detectedGroups = optJSONArray("detectedGroups").toStringList()
    )

private fun JSONObject.toFeedbackSentimentAnalysis(sourceFeedback: List<Feedback>): FeedbackSentimentAnalysis {
    val sourceByIndex = sourceFeedback.mapIndexed { index, item -> index to item }.toMap()
    val itemsJson = optJSONArray("items") ?: JSONArray()
    val items = List(itemsJson.length()) { index ->
        val item = itemsJson.optJSONObject(index) ?: JSONObject()
        FeedbackAnalysisItem(
            feedback = sourceByIndex[index] ?: Feedback(content = item.optString("summary")),
            sentiment = item.optString("sentiment", "Neutral"),
            category = item.optString("category", "General"),
            priority = item.optString("priority", "Low"),
            summary = item.optString("summary", "No summary")
        )
    }
    val categoryObject = optJSONObject("categoryCounts") ?: JSONObject()
    val categoryCounts = listOf("Meal", "Facilities", "Academics", "Transport", "Safety", "General")
        .associateWith { categoryObject.optInt(it, 0) }
        .filterValues { it > 0 }
    return FeedbackSentimentAnalysis(
        items = items,
        positiveCount = optInt("positiveCount", items.count { it.sentiment == "Positive" }),
        neutralCount = optInt("neutralCount", items.count { it.sentiment == "Neutral" }),
        negativeCount = optInt("negativeCount", items.count { it.sentiment == "Negative" }),
        urgentCount = optInt("urgentCount", items.count { it.priority == "High" }),
        categoryCounts = categoryCounts,
        frequentProblems = optJSONArray("frequentProblems").toStringList()
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}
