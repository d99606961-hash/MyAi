package com.example.myai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val API_KEY = "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" // <--- ЗАМЕНИ НА СВОЙ КЛЮЧ!
    private var chatHistory = JSONArray()
    private lateinit var chatLayout: LinearLayout
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }

        val clearBtn = Button(this).apply {
            text = "Очистить чат"
            setOnClickListener {
                chatHistory = JSONArray()
                chatLayout.removeAllViews()
                Toast.makeText(this@MainActivity, "История очищена", Toast.LENGTH_SHORT).show()
            }
        }
        mainLayout.addView(clearBtn)

        scrollView = ScrollView(this)
        chatLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(chatLayout)
        mainLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }
        val input = EditText(this).apply {
            hint = "Введите сообщение..."
        }
        val sendBtn = Button(this).apply { text = ">" }

        inputLayout.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputLayout.addView(sendBtn)
        mainLayout.addView(inputLayout)

        setContentView(mainLayout)

        sendBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.text.clear()
                addMessageToUI("Вы: $text")
                sendToAPI(text)
            }
        }
    }

    private fun addMessageToUI(message: String) {
        val tv = TextView(this).apply {
            text = message
            setPadding(0, 10, 0, 10)
            textSize = 16f
        }
        chatLayout.addView(tv)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun sendToAPI(userMessage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                chatHistory.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })

                val jsonBody = JSONObject().apply {
                    put("model", "llama-3.3-70b-versatile")
                    put("messages", chatHistory)
                }

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        addMessageToUI("Ошибка: $responseBody")
                    }
                    return@launch
                }

                val jsonResponse = JSONObject(responseBody)
                val reply = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                chatHistory.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", reply)
                })

                withContext(Dispatchers.Main) {
                    addMessageToUI("ИИ: $reply")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addMessageToUI("Ошибка сети: ${e.message}")
                }
            }
        }
    }
}
