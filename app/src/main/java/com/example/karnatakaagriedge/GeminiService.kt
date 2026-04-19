import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    // PASTE YOUR API KEY HERE
    private val apiKey = "AIzaSyBer9FlWoW_GP7QYJyseyftzx0TwJEPwJk"

    // Initializing the "Brain" using Gemini 3 Flash
    private val model = GenerativeModel(
        modelName = "gemini-3-flash",
        apiKey = apiKey
    )

    // The Secret Prompt that defines the AI's personality
    private val systemPrompt = """
        You are an expert for Karnataka farmers. 
        If you see a disease in the image, tell the cure in both Kannada and English.
        Always provide the estimated cost of treatment in Rupees (₹).
        Keep the answer practical and easy for a farmer to follow.
    """.trimIndent()

    suspend fun analyzeCropImage(image: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val inputContent = content {
                image(image)
                text(systemPrompt)
            }

            val response = model.generateContent(inputContent)
            return@withContext response.text ?: "Sorry, I couldn't analyze this image."
        } catch (e: Exception) {
            return@withContext "Error: ${e.localizedMessage}"
        }
    }
}