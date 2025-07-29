package com.example.test

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.test.ui.theme.TestTheme
import com.google.gson.Gson
import ua.naiksoftware.stomp.Stomp
import java.util.UUID

val request = CustomerCreationRequest(
    fullName = "Customer",
    prospectId = "123",
    clientId = "456"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CallApiOnLaunch(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CallApiOnLaunch(modifier: Modifier = Modifier) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response

            // Connect to WebSocket once customer is received
            connectAndSubscribe(response.conversation.id)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
        }
    }

    when {
        customer != null -> {
            Text(
                text = "Customer: ${customer!!.fullName}\nID: ${customer!!.id}\nProspect ID: ${customer!!.prospectId}\nClient ID: ${customer!!.clientId}\nConversation ID: ${customer!!.conversation.id}",
                modifier = modifier
            )
        }

        error != null -> {
            Text("Error: $error", modifier = modifier)
        }

        else -> {
            Text("Loading...", modifier = modifier)
        }
    }
}

@SuppressLint("CheckResult")
fun connectAndSubscribe(conversationId: UUID) {
    println("🚀 Starting connection for conversation: $conversationId")

    val stompClient = Stomp.over(
        Stomp.ConnectionProvider.OKHTTP,
        "ws://10.0.2.2:8080/ws-native"
    )

    // Connect
    stompClient.connect()
    println("📞 Connect called")
    println("🔍 Expected topic: /topic/conversation$conversationId")
    println("🔍 Conversation ID: $conversationId")

    // Log WebSocket lifecycle
    stompClient.lifecycle()
        .subscribe({ event ->
            println("🔄 Lifecycle event: ${event.type}")
            when (event.type) {
                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED -> {
                    println("🔗 WebSocket Connected - Now subscribing to topic")

                    // Subscribe to conversation messages once connected
                    val topicPath = "/topic/conversation$conversationId"
                    println("📡 Subscribing to: $topicPath")

                    stompClient.topic(topicPath)
                        .subscribe({ topicMessage ->
                            println("📨 Received message: ${topicMessage.payload}")
                        }, { error ->
                            println("❌ Topic subscription error: ${error.message}")
                            error.printStackTrace()
                        })
                }
                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.CLOSED -> {
                    println("❌ WebSocket Closed")
                }
                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.ERROR -> {
                    println("⚠️ WebSocket Error: ${event.exception}")
                    event.exception?.printStackTrace()
                }
                else -> println("🔄 Other lifecycle event: ${event.type}")
            }
        }, { throwable ->
            println("💥 Lifecycle error: ${throwable.message}")
            throwable.printStackTrace()
        })
}