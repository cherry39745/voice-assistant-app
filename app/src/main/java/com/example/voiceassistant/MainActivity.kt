package com.example.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            }
        }

        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches.isNullOrEmpty()) return@registerForActivityResult

            val spokenText = matches.firstOrNull() ?: ""
            if (spokenText.isBlank()) return@registerForActivityResult

            val response = buildAssistantReply(spokenText)
            val state = voiceAssistantState
            state.transcript = spokenText
            state.reply = response
            textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "voice-assistant")
        }

        setContent {
            val context = LocalContext.current
            var transcript by rememberSaveable { mutableStateOf("Say something like 'What time is it?' ") }
            var reply by rememberSaveable { mutableStateOf("Ready to help.") }

            DisposableEffect(context) {
                textToSpeech = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.language = Locale.US
                    }
                }

                onDispose {
                    textToSpeech?.stop()
                    textToSpeech?.shutdown()
                }
            }

            VoiceAssistantTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start voice input"
                            )
                        }
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Voice Assistant",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "You said:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = transcript,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text = "Assistant:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = reply,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val sampleText = "What time is it?"
                                    transcript = sampleText
                                    reply = buildAssistantReply(sampleText)
                                    textToSpeech?.speak(
                                        reply,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "voice-assistant-sample"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Text(text = "Try sample command")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to your assistant")
        }
        speechLauncher.launch(intent)
    }
}

private object voiceAssistantState {
    var transcript: String = "Say something like 'What time is it?' "
    var reply: String = "Ready to help."
}

private fun buildAssistantReply(command: String): String {
    val normalized = command.trim()
    if (normalized.isEmpty()) {
        return "I didn't catch anything. Please try again."
    }

    return when {
        normalized.contains("hello", ignoreCase = true) || normalized.contains("hi", ignoreCase = true) ->
            "Hello! How can I help you today?"

        normalized.contains("time", ignoreCase = true) ->
            "The current time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."

        normalized.contains("date", ignoreCase = true) ->
            "Today is ${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())}."

        normalized.contains("joke", ignoreCase = true) ->
            "Why did the scarecrow win an award? Because he was outstanding in his field."

        normalized.contains("weather", ignoreCase = true) ->
            "I can connect to a weather API for live updates in a future version of this app."

        normalized.contains("open", ignoreCase = true) ->
            "I can trigger app actions or navigation once you connect your backend or device services."

        else ->
            "I heard: \"$normalized\". I can handle simple commands like greetings, time, date, and jokes."
    }
}
