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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.tayeb.messaging_android.config.RetrofitClient
import ma.tayeb.messaging_android.enums.ReaderType
import ma.tayeb.messaging_android.enums.SenderType
import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.types.MessageCreationRequest
import ma.tayeb.messaging_android.ui.theme.MessagingAndroidTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.io.BufferedReader
import java.io.InputStreamReader
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
                App()
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

    var replyToMessage by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response

            connectAndSubscribe(response.conversation.id) { newMessage ->
                if (messages.none { it.id == newMessage.id }) {
                    messages.add(newMessage)
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
            messages.addAll(paginatedResponse.content)

        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
        }
    }

// Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Reply preview above input bar if replying
                replyToMessage?.let { message ->
                    ReplyPreview(
                        message = message,
                        onCancel = { replyToMessage = null }
                    )
                }

                MessageInputBar(
                    onSend = { messageText ->
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                fun createFormDataMap(request: MessageCreationRequest): Map<String, RequestBody> {
                                    val map = mutableMapOf<String, RequestBody>()

                                    fun String.toRequestBody(): RequestBody =
                                        this.toRequestBody("text/plain".toMediaType())

                                    map["conversationId"] = request.conversationId.toString().toRequestBody()
                                    map["content"] = request.content?.toRequestBody() as RequestBody
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
                                    replyToId = replyToMessage?.id // set replyToId here
                                )

                                val formData = createFormDataMap(request)

                                RetrofitClient.apiService.sendMessage(formData)
                                replyToMessage = null // clear reply after send

                            } catch (e: Exception) {
                                println("Send message error: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                error != null -> {
                    Text("Error: $error", modifier = Modifier.padding(16.dp))
                }
                messages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
                        state = listState
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
                                },
                                onReply = {
                                    replyToMessage = it
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
fun ReplyPreview(
    message: Message,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray
                )
                Text(
                    text = message.content ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel reply")
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onEdit: (String) -> Unit,
    onReply: (Message) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(message.content ?: "") }

    val isCustomer = message.senderType == SenderType.CUSTOMER
    val senderName = message.agent?.fullName ?: message.customer?.fullName ?: "Unknown"
    val formattedTime = formatTimestamp(message.timestamp)

    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalAlignment = if (isCustomer) Alignment.End else Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isCustomer) Color(0xFFD0F8CE) else Color(0xFFE0F7FA)
            ),
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 280.dp)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Sender name + timestamp (no read receipt here now)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }

                // Reply preview inside bubble
                message.replyTo?.let { replied ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECECEC)),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = replied.content ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }

                // Message content and optional image with edited and read receipt on same line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(message.content ?: "")

                        if (message.edited) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "(edited)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                if (isCustomer) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (message.read) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Read",
                                            tint = Color(0xFF4FC3F7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Sent",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // If not edited but still customer message, show read receipt aligned right
                            if (isCustomer) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (message.read) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Read",
                                            tint = Color(0xFF4FC3F7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Sent",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

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

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        if (isCustomer) {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }

                        IconButton(
                            onClick = { onReply(message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Reply")
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

@Composable
fun FloatingRagBubble(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.Search, contentDescription = "Ask FAQ")
        }
    }
}

@Composable
fun RagChatScreen(
    onBack: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Ask FAQ", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Your question") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (question.isNotBlank() && !loading) {
                    coroutineScope.launch {
                        answer = ""
                        loading = true
                        streamAnswer(question) { chunk ->
                            answer += chunk
                        }
                        loading = false
                    }
                }
            })
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (question.isNotBlank() && !loading) {
                    coroutineScope.launch {
                        answer = ""
                        loading = true
                        streamAnswer(question) { chunk ->
                            answer += chunk
                        }
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = question.isNotBlank() && !loading
        ) {
            Text(if (loading) "Loading..." else "Ask")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = answer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

suspend fun streamAnswer(
    question: String,
    onChunk: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // no timeout on read
                .build()

            val jsonBody = JSONObject().apply { put("question", question) }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("http://10.0.2.2:8000/ask")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val source = response.body?.source()
                if (source == null) throw Exception("Response body source is null")

                val buffer = okio.Buffer()
                while (true) {
                    val bytesRead = source.read(buffer, 8192)
                    if (bytesRead == -1L) break

                    val chunk = buffer.readUtf8()
                    onChunk(chunk)
                }
            }

        } catch (e: Exception) {
            onChunk("\n\nError: ${e.message}")
        }
    }
}

@Composable
fun MainScreen(
    onOpenRagChat: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ChatScreen() // Your existing main chat UI

        FloatingRagBubble(onClick = onOpenRagChat) // Floating bubble top-right
    }
}


@Composable
fun App() {
    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> MainScreen(onOpenRagChat = { currentScreen = "ragChat" })
        "ragChat" -> RagChatScreen(onBack = { currentScreen = "main" })
    }
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
