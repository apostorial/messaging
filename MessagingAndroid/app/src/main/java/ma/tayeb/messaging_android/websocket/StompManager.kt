package ma.tayeb.messaging_android.websocket

import android.util.Log
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ma.tayeb.messaging_android.types.Message
import ma.tayeb.messaging_android.config.LocalDateTimeAdapter
import org.threeten.bp.LocalDateTime
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.UUID
import java.util.concurrent.TimeUnit

class StompManager private constructor() {
    
    companion object {
        private const val TAG = "StompManager"
        private const val WS_URL = "ws://10.0.2.2:8080/ws"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 3000L
        
        @Volatile
        private var INSTANCE: StompManager? = null
        
        fun getInstance(): StompManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StompManager().also { INSTANCE = it }
            }
        }
    }
    
    private var stompClient: StompClient? = null
    private val disposables = CompositeDisposable()
    private var isConnected = false
    private var reconnectAttempts = 0
    private var shouldReconnect = true
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    
    // Callbacks for real-time updates
    private var onNewMessage: ((Message) -> Unit)? = null
    private var onConversationUpdate: ((UUID) -> Unit)? = null
    private var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    
    fun connect() {
        if (isConnected) return
        
        Log.d(TAG, "Connecting to STOMP server: $WS_URL")
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)
            .withClientHeartbeat(10000)
            .withServerHeartbeat(10000)
        
        disposables.add(
            stompClient!!.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { lifecycleEvent ->
                        when (lifecycleEvent.type) {
                            LifecycleEvent.Type.OPENED -> {
                                Log.d(TAG, "STOMP Connected")
                                isConnected = true
                                reconnectAttempts = 0
                                onConnectionStatusChanged?.invoke(true)
                            }
                            LifecycleEvent.Type.CLOSED -> {
                                Log.d(TAG, "STOMP Disconnected")
                                isConnected = false
                                onConnectionStatusChanged?.invoke(false)
                                if (shouldReconnect) {
                                    scheduleReconnect()
                                }
                            }
                            LifecycleEvent.Type.ERROR -> {
                                Log.e(TAG, "STOMP Connection Error: ${lifecycleEvent.exception?.message}")
                                isConnected = false
                                onConnectionStatusChanged?.invoke(false)
                                if (shouldReconnect) {
                                    scheduleReconnect()
                                }
                            }

                            LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                                Log.w(TAG, "STOMP Server heartbeat failed")
                            }
                        }
                    },
                    { throwable ->
                        Log.e(TAG, "STOMP Lifecycle Error: ${throwable.message}", throwable)
                        isConnected = false
                        onConnectionStatusChanged?.invoke(false)
                        if (shouldReconnect) {
                            scheduleReconnect()
                        }
                    }
                )
        )
        
        Log.d(TAG, "Initiating STOMP connection...")
        stompClient!!.connect()
    }
    
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }
        
        reconnectAttempts++
        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${RECONNECT_DELAY_MS}ms")
        
        disposables.add(
            io.reactivex.Observable.timer(RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d(TAG, "Attempting to reconnect...")
                    connect()
                }
        )
    }
    
    fun subscribeToConversation(conversationId: UUID) {
        if (!isConnected) {
            Log.w(TAG, "Cannot subscribe: not connected")
            return
        }
        
        val topic = "/topic/conversation$conversationId"
        Log.d(TAG, "Subscribing to conversation topic: $topic")
        
        disposables.add(
            stompClient!!.topic(topic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { topicMessage ->
                        try {
                            val message = gson.fromJson(topicMessage.payload, Message::class.java)
                            Log.d(TAG, "Received new message: ${message.id}")
                            onNewMessage?.invoke(message)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing message", e)
                        }
                    },
                    { throwable ->
                        Log.e(TAG, "Error subscribing to conversation topic", throwable)
                    }
                )
        )
    }
    
    fun subscribeToConversationUpdates() {
        if (!isConnected) {
            Log.w(TAG, "Cannot subscribe: not connected")
            return
        }
        
        val topic = "/topic/conversation-updates"
        Log.d(TAG, "Subscribing to conversation updates topic: $topic")
        
        disposables.add(
            stompClient!!.topic(topic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { topicMessage ->
                        try {
                            val conversationId = UUID.fromString(topicMessage.payload)
                            Log.d(TAG, "Received conversation update: $conversationId")
                            onConversationUpdate?.invoke(conversationId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing conversation update", e)
                        }
                    },
                    { throwable ->
                        Log.e(TAG, "Error subscribing to conversation updates topic", throwable)
                    }
                )
        )
    }
    
    fun setOnNewMessage(callback: ((Message) -> Unit)?) {
        onNewMessage = callback
    }
    
    fun setOnConversationUpdate(callback: ((UUID) -> Unit)?) {
        onConversationUpdate = callback
    }
    
    fun setOnConnectionStatusChanged(callback: ((Boolean) -> Unit)?) {
        onConnectionStatusChanged = callback
    }
    
    fun disconnect() {
        shouldReconnect = false
        isConnected = false
        disposables.clear()
        stompClient?.disconnect()
        stompClient = null
        Log.d(TAG, "STOMP Disconnected and cleanup completed")
    }
    
    fun isConnected(): Boolean = isConnected
    
    fun sendMessage(destination: String, message: String) {
        if (!isConnected) {
            Log.w(TAG, "Cannot send message: not connected")
            return
        }
        
        Log.d(TAG, "Sending message to $destination: $message")
        
        disposables.add(
            stompClient!!.send(destination, message)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Log.d(TAG, "Message sent successfully") },
                    { throwable ->
                        Log.e(TAG, "Error sending message", throwable)
                    }
                )
        )
    }
} 