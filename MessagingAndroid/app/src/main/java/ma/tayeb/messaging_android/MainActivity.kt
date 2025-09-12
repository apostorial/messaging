package ma.tayeb.messaging_android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import okhttp3.MultipartBody
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import android.content.SharedPreferences
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


val request = CustomerCreationRequest(
    fullName = "Amine Bennani",
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
fun ChatScreen(
    onFileSelectionChanged: (android.net.Uri?) -> Unit = {}
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    val messages = remember { mutableStateListOf<Message>() }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var replyToMessage by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()

    var fullImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.findOrCreate(request)
            customer = response

            connectAndSubscribe(
                response.conversation.id,
                onMessageReceived = { newMessage ->
                    val idxById = newMessage.id?.let { id -> messages.indexOfFirst { it.id == id } } ?: -1
                    val idx = if (idxById != -1) idxById else messages.indexOfFirst {
                        it.id == null && it.timestamp == newMessage.timestamp && it.fileUrl == newMessage.fileUrl
                    }
                    if (idx == -1) {
                        messages.add(newMessage)
                    } else {
                        messages[idx] = newMessage
                    }
                },
                onReadIds = { ids ->
                    // Update read flag for messages whose IDs are in the payload
                    for (i in messages.indices) {
                        val m = messages[i]
                        if (m.id != null && ids.contains(m.id)) {
                            messages[i] = m.copy(read = true)
                        }
                    }
                }
            )

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
        } finally {
            isLoading = false
        }
    }

// Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars), // Add this line
            bottomBar = {
                Column {
                    // Reply preview above input bar if replying
                    replyToMessage?.let { message ->
                        ReplyPreview(
                            message = message,
                            onCancel = { replyToMessage = null }
                        )
                    }

                    // Selected file preview above input bar
                    var selectedUriState by remember { mutableStateOf<android.net.Uri?>(null) }
                    SelectedFilePreview(
                        uri = selectedUriState,
                        onRemove = { 
                            selectedUriState = null
                            onFileSelectionChanged(null)
                        }
                    )

                    MessageInputBar(
                        onSend = { messageText, imageUri ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    fun createFormDataMap(request: MessageCreationRequest): Map<String, RequestBody> {
                                        val map = mutableMapOf<String, RequestBody>()

                                        fun String.toRequestBody(): RequestBody =
                                            this.toRequestBody("text/plain".toMediaType())

                                        map["conversationId"] = request.conversationId.toString().toRequestBody()
                                        map["content"] = (request.content ?: "").toRequestBody()
                                        map["customerId"] = request.customerId.toString().toRequestBody()
                                        map["senderType"] = request.senderType.name.toRequestBody()

                                        request.replyToId?.let {
                                            map["replyToId"] = it.toRequestBody()
                                        }

                                        return map
                                    }

                                    fun createFilePart(context: Context, uri: android.net.Uri?): MultipartBody.Part? {
                                        if (uri == null) return null
                                        val cr = context.contentResolver
                                        val mime = cr.getType(uri) ?: "application/octet-stream"

                                        // Resolve filename
                                        var filename = "upload"
                                        val cursor = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                                        cursor?.use {
                                            if (it.moveToFirst()) {
                                                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                                if (idx >= 0) filename = it.getString(idx)
                                            }
                                        }

                                        val inputStream = cr.openInputStream(uri) ?: return null
                                        val bytes = inputStream.readBytes()
                                        inputStream.close()

                                        val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull()) // Use toMediaTypeOrNull for safety
                                        return MultipartBody.Part.createFormData(
                                            name = "file", // This 'name' must match the parameter name in your backend service
                                            filename = filename,
                                            body = requestBody
                                        )
                                    }

                                    val request = MessageCreationRequest(
                                        conversationId = customer!!.conversation.id,
                                        content = messageText.ifBlank { null },
                                        customerId = customer!!.id,
                                        senderType = SenderType.CUSTOMER,
                                        replyToId = replyToMessage?.id
                                    )

                                    val formData = createFormDataMap(request)
                                    val filePart = createFilePart(context, imageUri)

                                    RetrofitClient.apiService.sendMessage(formData, filePart)
                                    replyToMessage = null

                                } catch (e: Exception) {
                                    println("Send message error: ${e.message}")
                                }
                            }
                        },
                        onPickChanged = { 
                            selectedUriState = it
                            onFileSelectionChanged(it)
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
                    isLoading -> {
                        Text("Loading messages...", modifier = Modifier.padding(16.dp))
                    }
                    messages.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 8.dp),
                            state = listState
                        ) {
                            items(messages, key = { it.id ?: it.timestamp }) { message ->
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
                                    },
                                    onOpenImage = { url -> fullImageUrl = url }
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Start the conversation",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Send a message to start chatting with one of our agents.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        

    // Full-screen image viewer
    fullImageUrl?.let { url ->
        Dialog(onDismissRequest = { fullImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable { fullImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Inside
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val previewText = when {
                    !message.content.isNullOrBlank() -> message.content!!
                    message.fileType == "IMAGE" -> "🖼️ Image"
                    message.fileType == "PDF" -> "📄 PDF"
                    else -> "Attachment"
                }
                Text(
                    text = previewText,
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
    onReply: (Message) -> Unit,
    onOpenImage: (String) -> Unit
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
                containerColor = if (isCustomer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 280.dp)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Sender name + timestamp + action icons on the same line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Action icons on the right side
                    if (isCustomer) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = if (isCustomer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = { onReply(message) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Reply, 
                            contentDescription = "Reply",
                            modifier = Modifier.size(16.dp),
                            tint = if (isCustomer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Reply preview inside bubble
                message.replyTo?.let { replied ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                    ) {
                        val repliedPreview = when {
                            !replied.content.isNullOrBlank() -> replied.content!!
                            replied.fileType == "IMAGE" -> "🖼️ Image"
                            replied.fileType == "PDF" -> "📄 PDF"
                            else -> "Attachment"
                        }
                        Text(
                            text = repliedPreview,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Message content and optional image
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Only show message content if it's not empty
                    if (!message.content.isNullOrBlank()) {
                        Text(message.content ?: "")
                        
                        if (message.edited) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "(edited)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    message.fileUrl?.let { fileUrl ->
                        val resolvedUrl = fileUrl.replace("http://localhost:9000", "http://192.168.11.115:9000")
                        val context = LocalContext.current
                        val type = message.fileType
                        if (type == "IMAGE") {
                            AsyncImage(
                                model = resolvedUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = if (message.content.isNullOrBlank()) 0.dp else 8.dp)
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenImage(resolvedUrl) },
                                contentScale = ContentScale.Crop
                            )
                        } else if (type == "PDF") {
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(android.net.Uri.parse(resolvedUrl), "application/pdf")
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        println("Failed to open PDF: ${e.message}")
                                    }
                                },
                                modifier = Modifier.padding(top = if (message.content.isNullOrBlank()) 0.dp else 8.dp)
                            ) {
                                Text("Open PDF")
                            }
                        }
                        
                        // Read receipt below the attachment for customer messages
                        if (isCustomer) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (message.read) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
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
                    } ?: run {
                        // If no attachment but still customer message, show read receipt aligned right
                        if (isCustomer) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (message.read) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
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
fun SelectedFilePreview(
    uri: android.net.Uri?,
    onRemove: () -> Unit
) {
    if (uri == null) return
    val context = LocalContext.current
    val mime = context.contentResolver.getType(uri) ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            if (mime.startsWith("image")) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .height(60.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Image selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else if (mime == "application/pdf") {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "PDF",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MessageInputBar(
    modifier: Modifier = Modifier,
    onSend: (String, android.net.Uri?) -> Unit,
    onPickChanged: (android.net.Uri?) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current

    // File picker (images and PDFs)
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
            }
            selectedImageUri = uri
            onPickChanged(uri)
        }
    )



    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attach image button
        IconButton(onClick = { imagePicker.launch(arrayOf("image/*", "application/pdf")) }) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
        }
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
                    if (messageText.isNotBlank() || selectedImageUri != null) {
                        onSend(messageText.trim(), selectedImageUri)
                        messageText = ""
                        selectedImageUri = null
                        onPickChanged(null)
                    }
                }
            )
        )

        Spacer(modifier = Modifier.width(2.dp))



        IconButton(
            onClick = {
                if (messageText.isNotBlank() || selectedImageUri != null) {
                    onSend(messageText.trim(), selectedImageUri)
                    messageText = ""
                    selectedImageUri = null
                    onPickChanged(null)
                }
            }
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send message")
        }
    }
}


@SuppressLint("CheckResult")
fun connectAndSubscribe(
    conversationId: UUID,
    onMessageReceived: (Message) -> Unit,
    onReadIds: (List<String>) -> Unit
) {
    println("🚀 Starting connection for conversation: $conversationId")

    val stompClient = Stomp.over(
        Stomp.ConnectionProvider.OKHTTP,
        "ws://192.168.11.115:8080/ws-native"
    )

    stompClient.connect()
    println("📞 Connect called")
    println("🔍 Conversation ID: $conversationId")

    stompClient.lifecycle()
        .subscribe({ event ->
            println("🔄 Lifecycle event: ${event.type}")
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    println("🔗 WebSocket Connected - Now subscribing to topics")

                    // Subscribe to conversation messages (support both topic variants)
                    val topicPathWithSlash = "/topic/conversation/$conversationId"
                    val topicPathNoSlash = "/topic/conversation$conversationId"
                    println("📡 Subscribing to: $topicPathWithSlash and $topicPathNoSlash")

                    listOf(topicPathWithSlash, topicPathNoSlash).forEach { path ->
                        stompClient.topic(path)
                            .subscribe({ topicMessage ->
                                println("📨 Received message on $path: ${topicMessage.payload}")
                                try {
                                    val gson = Gson()
                                    val message = gson.fromJson(topicMessage.payload, Message::class.java)
                                    onMessageReceived(message)
                                } catch (e: Exception) {
                                    println("❌ Failed to parse message: ${e.message}")
                                }
                            }, { error ->
                                println("❌ Topic subscription error ($path): ${error.message}")
                                error.printStackTrace()
                            })
                    }

                    // Subscribe to read receipts
                    val readTopicPath = "/topic/conversation/$conversationId/read"
                    println("📡 Subscribing to: $readTopicPath")
                    stompClient.topic(readTopicPath)
                        .subscribe({ topicMessage ->
                            println("📨 Received read IDs: ${topicMessage.payload}")
                            try {
                                val gson = Gson()
                                val idsArray = gson.fromJson(topicMessage.payload, Array<String>::class.java)
                                onReadIds(idsArray.toList())
                            } catch (e: Exception) {
                                println("❌ Failed to parse read IDs: ${e.message}")
                            }
                        }, { error ->
                            println("❌ Read topic subscription error: ${error.message}")
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
fun FloatingRagBubble(
    onClick: () -> Unit,
    hasFileSelected: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.padding(
                bottom = if (hasFileSelected) 140.dp else 80.dp // Moved up slightly
            )
        ) {
            Icon(Icons.Default.Search, contentDescription = "Ask FAQ")
        }
    }
}
@Composable
fun RagChatScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // List of chat messages: Pair<isUser:Boolean, content:String>
    val chatMessages = remember { mutableStateListOf<Pair<Boolean, String>>() }

    var currentQuestion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Load saved messages on first launch
    LaunchedEffect(Unit) {
        loadChatMessages(context, chatMessages)
    }

    // Save messages whenever they change
    DisposableEffect(chatMessages.size) {
        onDispose {
            saveChatMessages(context, chatMessages)
        }
    }

    // Scroll to bottom when chatMessages changes
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Ask FAQ", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
            }

            // Chat messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                state = listState,
                reverseLayout = false,
            ) {
                items(chatMessages) { (isUser, content) ->
                    ChatBubble(isUser = isUser, text = content)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = currentQuestion,
                    onValueChange = { currentQuestion = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your question") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (currentQuestion.isNotBlank() && !isLoading) {
                                coroutineScope.launch {
                                    // Add user question to chat
                                    chatMessages.add(true to currentQuestion)
                                    val questionToSend = currentQuestion
                                    currentQuestion = ""
                                    isLoading = true

                                    // Add empty answer message to update streaming chunks
                                    chatMessages.add(false to "")

                                    streamAnswer(questionToSend) { chunk ->
                                        // Append chunk to last answer bubble
                                        val lastIndex = chatMessages.lastIndex
                                        val old = chatMessages[lastIndex]
                                        chatMessages[lastIndex] = old.copy(second = old.second + chunk)
                                    }

                                    isLoading = false
                                }
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (currentQuestion.isNotBlank() && !isLoading) {
                            coroutineScope.launch {
                                chatMessages.add(true to currentQuestion)
                                val questionToSend = currentQuestion
                                currentQuestion = ""
                                isLoading = true

                                chatMessages.add(false to "")
                                streamAnswer(questionToSend) { chunk ->
                                    val lastIndex = chatMessages.lastIndex
                                    val old = chatMessages[lastIndex]
                                    chatMessages[lastIndex] = old.copy(second = old.second + chunk)
                                }
                                isLoading = false
                            }
                        }
                    },
                    enabled = currentQuestion.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.Default.Send, 
                        contentDescription = if (isLoading) "Loading..." else "Send"
                    )
                }
            }
        }

        // Add blur effect for edge-to-edge areas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Top blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        RoundedCornerShape(0.dp)
                    )
                    .graphicsLayer {
                        // Use graphicsLayer for blur effect
                        alpha = 0.8f
                    }
            )
            
            // Bottom blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        RoundedCornerShape(0.dp)
                    )
                    .graphicsLayer {
                        // Use graphicsLayer for blur effect
                        alpha = 0.8f
                    }
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun ChatBubble(
    isUser: Boolean,
    text: String
) {
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(4.dp)
        ) {
            Text(
                text = parseBoldText(text, textColor),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

fun parseBoldText(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        var lastIndex = 0
        
        regex.findAll(text).forEach { matchResult ->
            // Add text before the bold section
            if (matchResult.range.first > lastIndex) {
                append(text.substring(lastIndex, matchResult.range.first))
            }
            
            // Add bold text
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = defaultColor
                )
            ) {
                append(matchResult.groupValues[1])
            }
            
            lastIndex = matchResult.range.last + 1
        }
        
        // Add remaining text after the last bold section
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
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
                .url("http://192.168.11.115:8000/ask")
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
                    
                    // Parse JSON and extract only the "response" field
                    try {
                        val json = JSONObject(chunk)
                        val responseText = json.optString("response", "")
                        if (responseText.isNotEmpty()) {
                            onChunk(responseText)
                        }
                    } catch (e: Exception) {
                        // If JSON parsing fails, send the raw chunk
                        onChunk(chunk)
                    }
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
    var selectedUriState by remember { mutableStateOf<android.net.Uri?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        ChatScreen(onFileSelectionChanged = { selectedUriState = it }) // Your existing main chat UI

        FloatingRagBubble(
            onClick = onOpenRagChat,
            hasFileSelected = selectedUriState != null
        )
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

// Helper functions for saving and loading chat messages
fun saveChatMessages(context: Context, messages: List<Pair<Boolean, String>>) {
    val prefs: SharedPreferences = context.getSharedPreferences("rag_chat", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    
    // Convert messages to JSON format
    val messagesJson = messages.joinToString("|||") { (isUser, content) ->
        "$isUser||$content"
    }
    
    editor.putString("chat_messages", messagesJson)
    editor.apply()
}

fun loadChatMessages(context: Context, messages: MutableList<Pair<Boolean, String>>) {
    val prefs: SharedPreferences = context.getSharedPreferences("rag_chat", Context.MODE_PRIVATE)
    val messagesJson = prefs.getString("chat_messages", "")
    
    if (messagesJson?.isNotEmpty() == true) {
        messages.clear()
        messagesJson.split("|||").forEach { messageStr ->
            if (messageStr.isNotEmpty()) {
                val parts = messageStr.split("||", limit = 2)
                if (parts.size == 2) {
                    val isUser = parts[0].toBoolean()
                    val content = parts[1]
                    messages.add(isUser to content)
                }
            }
        }
    }
}

// Optional: Add a function to clear chat history
fun clearChatHistory(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("rag_chat", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}
