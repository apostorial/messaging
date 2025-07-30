package ma.tayeb.messaging_android

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import ma.tayeb.messaging_android.config.RetrofitClient
import ma.tayeb.messaging_android.enums.ReaderType
import ma.tayeb.messaging_android.enums.SenderType
import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.ui.theme.MessagingAndroidTheme
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.dto.LifecycleEvent
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
            MessagingAndroidTheme {
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
    val messages = remember { mutableStateListOf<Message>() }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response

            connectAndSubscribe(response.conversation.id) { newMessage ->
                messages.add(newMessage)
            }

            RetrofitClient.apiService.markAsRead(
                conversationId = response.conversation.id,
                readerType = ReaderType.CUSTOMER
            )

            val paginatedResponse = RetrofitClient.apiService.findAllMessagesByConversation(
                conversationId = response.conversation.id,
                page = 0,
                size = 20
            )
            messages.addAll(paginatedResponse.content)

        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
        }
    }

    when {
        error != null -> {
            Text("Error: $error", modifier = modifier)
        }

        messages.isNotEmpty() -> {
            LazyColumn(
                modifier = modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        else -> {
            Text("Loading...", modifier = modifier)
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isFromAgent = message.senderType == SenderType.AGENT
    val alignment = if (isFromAgent) Alignment.CenterStart else Alignment.CenterEnd
    val backgroundColor = if (isFromAgent) Color.LightGray else Color(0xFFDCF8C6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content ?: "",
                modifier = Modifier.padding(8.dp),
                color = Color.Black
            )
        }
    }
}

@Composable
fun MessageInputBar(
    modifier: Modifier = Modifier,
    onSend: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type your message") },
            maxLines = 4,
            singleLine = false,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSend(messageText.trim())
                        messageText = ""
                    }
                }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                if (messageText.isNotBlank()) {
                    onSend(messageText.trim())
                    messageText = ""
                }
            }
        ) {
            Text("Send")
        }
    }
}


@SuppressLint("CheckResult")
fun connectAndSubscribe(conversationId: UUID, onMessageReceived: (Message) -> Unit) {
    println("🚀 Starting connection for conversation: $conversationId")

    val stompClient = Stomp.over(
        Stomp.ConnectionProvider.OKHTTP,
        "ws://10.0.2.2:8080/ws-native"
    )

    stompClient.connect()
    println("📞 Connect called")
    println("🔍 Expected topic: /topic/conversation$conversationId")
    println("🔍 Conversation ID: $conversationId")

    stompClient.lifecycle()
        .subscribe({ event ->
            println("🔄 Lifecycle event: ${event.type}")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println("🔗 WebSocket Connected - Now subscribing to topic")

                    // Subscribe to conversation messages once connected
                    val topicPath = "/topic/conversation$conversationId"
                    println("📡 Subscribing to: $topicPath")

                    stompClient.topic(topicPath)
                        .subscribe({ topicMessage ->
                            println("📨 Received message: ${topicMessage.payload}")
                            try {
                                val gson = Gson()
                                val message = gson.fromJson(topicMessage.payload, Message::class.java)
                                onMessageReceived(message)

                            } catch (e: Exception) {
                                println("❌ Failed to parse message: ${e.message}")
                            }
                        }, { error ->
                            println("❌ Topic subscription error: ${error.message}")
                            error.printStackTrace()
                        })
                }
                LifecycleEvent.Type.CLOSED -> {
                    println("❌ WebSocket Closed")
                }
                LifecycleEvent.Type.ERROR -> {
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