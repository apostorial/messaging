package ma.tayeb.messaging_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ma.tayeb.messaging_android.api.RetrofitClient
import ma.tayeb.messaging_android.types.Customer
import ma.tayeb.messaging_android.types.CustomerCreationRequest
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.ui.theme.MessagingAndroidTheme
import ma.tayeb.messaging_android.websocket.StompManager

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID


val request = CustomerCreationRequest(
    fullName = "Customer",
    prospectId = "123",
    clientId = "456")
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
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response
        } catch (e: Exception) {
            error = e.localizedMessage
        }
    }

    when {
        customer != null -> ChatScreen(conversationId = customer!!.conversation.id, modifier)
        error != null -> Text("Error: $error", modifier = modifier)
        else -> Text("Loading...", modifier = modifier)
    }
}

@Composable
fun ChatScreen(conversationId: UUID, modifier: Modifier = Modifier) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var currentPage by remember { mutableStateOf(0) }
    var isLastPage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val stompManager = remember { StompManager.getInstance() }

    LaunchedEffect(conversationId) {
        // Load initial messages
        loadMessages(conversationId, currentPage) { newMessages, last ->
            messages = newMessages
            isLastPage = last
        }
        
        // Setup STOMP connection and subscriptions
        stompManager.setOnNewMessage { newMessage ->
            if (newMessage.conversation.id == conversationId) {
                messages = messages + newMessage
            }
        }
        
        stompManager.setOnConversationUpdate { updatedConversationId ->
            if (updatedConversationId == conversationId) {
                // Reload messages when conversation is updated
                coroutineScope.launch {
                    loadMessages(conversationId, 0) { newMessages, last ->
                        messages = newMessages
                        isLastPage = last
                    }
                }
            }
        }
        
        stompManager.setOnConnectionStatusChanged { connected ->
            isConnected = connected
            if (connected) {
                // Subscribe to topics when connected
                stompManager.subscribeToConversation(conversationId)
                stompManager.subscribeToConversationUpdates()
            }
        }
        
        // Connect to STOMP
        stompManager.connect()
    }
    
    // Cleanup on dispose
    DisposableEffect(conversationId) {
        onDispose {
            // Don't disconnect here as it's a singleton, just remove callbacks
            stompManager.setOnNewMessage(null)
            stompManager.setOnConversationUpdate(null)
            stompManager.setOnConnectionStatusChanged(null)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection status indicator
        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFF9800))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Connecting to real-time updates...",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Connected to real-time updates",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Debug reconnect button
        Button(
            onClick = {
                stompManager.disconnect()
                stompManager.connect()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Reconnect STOMP")
        }
        
        LazyColumn(
            reverseLayout = true,
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }

        item {
            if (!isLastPage && !isLoading) {
                SideEffect {
                    coroutineScope.launch {
                        isLoading = true
                        currentPage += 1
                        loadMessages(conversationId, currentPage) { newMsgs, last ->
                            messages = messages + newMsgs
                            isLastPage = last
                            isLoading = false
                        }
                    }
                }
            }
        }
        }
    }
}

suspend fun loadMessages(
    conversationId: UUID,
    page: Int,
    onResult: (List<Message>, Boolean) -> Unit
) {
    try {
        val response = RetrofitClient.apiService.getMessagesByConversationId(conversationId, page, 20)
        onResult(response.content, response.last)
    } catch (e: Exception) {
        Log.e("ChatScreen", "Failed to load messages: ${e.localizedMessage}")
        onResult(emptyList(), true)
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCustomer = message.senderType.name == "CUSTOMER"
    val alignment = if (isCustomer) Alignment.End else Alignment.Start
    val bgColor = if (isCustomer) Color(0xFFD0F0C0) else Color(0xFFE0E0E0)
    val textAlign = if (isCustomer) TextAlign.End else TextAlign.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(bgColor, shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            // Show message content if present
            message.content?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    textAlign = textAlign
                )
            }

            // Show image if fileUrl is present
            message.fileUrl?.let { fullUrl ->
                val fileId = fullUrl.substringAfterLast("/") // Extracts the last segment
                val imageUrl = "http://10.0.2.2:9000/files/$fileId"

                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Image message",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

        }
    }
}
