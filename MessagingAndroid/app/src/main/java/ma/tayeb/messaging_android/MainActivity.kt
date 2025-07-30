package ma.tayeb.messaging_android

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ma.tayeb.messaging_android.config.RetrofitClient
import ma.tayeb.messaging_android.enums.ReaderType
import ma.tayeb.messaging_android.enums.SenderType
import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.types.MessageCreationRequest
import ma.tayeb.messaging_android.ui.theme.MessagingAndroidTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
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
        AndroidThreeTen.init(this)
        enableEdgeToEdge()
        setContent {
            MessagingAndroidTheme {
                ChatScreen()
            }
        }
    }
}


@Composable
fun ChatScreen() {
    var customer by remember { mutableStateOf<Customer?>(null) }
    val messages = remember { mutableStateListOf<Message>() }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response

            connectAndSubscribe(response.conversation.id) { newMessage ->
                if (messages.none { it.id == newMessage.id }) {
                    messages.add(0, newMessage) // reverseLayout = true
                }
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

            messages.addAll(paginatedResponse.content.reversed()) // keep latest at top
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MessageInputBar(
                onSend = { messageText ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            fun createFormDataMap(request: MessageCreationRequest): Map<String, RequestBody> {
                                val map = mutableMapOf<String, RequestBody>()

                                fun String.toRequestBody(): RequestBody =
                                    this.toRequestBody("text/plain".toMediaType())

                                map["conversationId"] = request.conversationId.toString().toRequestBody()
                                map["content"] = request.content?.toRequestBody() ?: "".toRequestBody()
                                map["customerId"] = request.customerId.toString().toRequestBody()
                                map["senderType"] = request.senderType.name.toRequestBody()

                                request.replyToId?.let {
                                    map["replyToId"] = it.toRequestBody()
                                }

                                return map
                            }

                            val request = MessageCreationRequest(
                                conversationId = customer!!.conversation.id,
                                content = messageText,
                                customerId = customer!!.id,
                                senderType = SenderType.CUSTOMER,
                                replyToId = null
                            )

                            val formData = createFormDataMap(request)
                            RetrofitClient.apiService.sendMessage(formData)

                        } catch (e: Exception) {
                            println("Send message error: ${e.message}")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                error != null -> {
                    Text("Error: $error", modifier = Modifier.padding(16.dp))
                }
                messages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        reverseLayout = true
                    ) {
                        items(messages, key = { it.id ?: "" }) { message ->
                            MessageBubble(
                                message = message,
                                onEdit = { newContent ->
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.editMessage(message.id!!, newContent)
                                            val index = messages.indexOfFirst { it.id == message.id }
                                            if (index != -1) {
                                                messages[index] = message.copy(content = newContent, edited = true)
                                            }
                                        } catch (e: Exception) {
                                            println("Edit error: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    Text("Loading messages...", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}



@Composable
fun MessageBubble(
    message: Message,
    onEdit: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(message.content ?: "") }

    val isCustomer = message.senderType == SenderType.CUSTOMER
    val senderName = message.agent?.fullName ?: message.customer?.fullName ?: "Unknown"
    val formattedTime = formatTimestamp(message.timestamp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Text(
            "$senderName • $formattedTime",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = if (isCustomer) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCustomer) Color(0xFFD0F8CE) else Color(0xFFE0F7FA)
                ),
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(message.content ?: "")

                        message.fileUrl?.let { fileUrl ->
                            val resolvedUrl = fileUrl.replace("http://localhost:9000", "http://10.0.2.2:9000")
                            AsyncImage(
                                model = resolvedUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (isCustomer) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
        }

    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(editedContent)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Message") },
            text = {
                TextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    singleLine = false,
                    label = { Text("New Content") }
                )
            }
        )
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

fun formatTimestamp(timestampStr: String): String {
    return try {
        // Parse assuming ISO-8601 format (e.g. "2025-07-30T18:10:29")
        val parsed = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME)
        // Format to something like "Jul 30, 18:10"
        parsed.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
    } catch (e: Exception) {
        // Fallback if parsing fails
        println(e)
        timestampStr
    }
}
