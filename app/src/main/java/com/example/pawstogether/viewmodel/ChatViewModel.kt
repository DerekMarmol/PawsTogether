package com.example.pawstogether.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.pawstogether.model.ChatPreview
import com.example.pawstogether.ui.theme.models.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val _chatPreviews = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatPreviews: StateFlow<List<ChatPreview>> = _chatPreviews.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    @OptIn(UnstableApi::class)
    fun sendMessage(receiverId: String, receiverName: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserName = auth.currentUser?.displayName ?: "Usuario"
        val messageText = messageText.value.trim()

        if (messageText.isEmpty()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            receiverId = receiverId,
            content = messageText,
            senderName = currentUserName,
            timestamp = System.currentTimeMillis()
        )

        // Crear array de participantes para facilitar las consultas
        val participants = listOf(currentUserId, receiverId).sorted()

        val messageData = hashMapOf(
            "id" to message.id,
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "senderName" to message.senderName,
            "participants" to participants
        )

        firestore.collection("messages")
            .document(message.id)
            .set(messageData)
            .addOnSuccessListener {
                _messageText.value = ""

                // Actualizar o crear el chat preview para ambos usuarios
                updateChatPreview(currentUserId, receiverId, receiverName, message)
                updateChatPreview(receiverId, currentUserId, currentUserName, message)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error sending message", e)
            }
    }

    private fun updateChatPreview(
        ownerId: String,
        otherUserId: String,
        otherUserName: String,
        lastMessage: Message
    ) {
        val chatPreviewRef = firestore.collection("chatPreviews")
            .document("${ownerId}_${otherUserId}")

        val previewData = hashMapOf(
            "userId" to otherUserId,
            "userName" to otherUserName,
            "lastMessage" to lastMessage.content,
            "timestamp" to lastMessage.timestamp,
            "ownerId" to ownerId
        )

        chatPreviewRef.set(previewData)
    }

    @OptIn(UnstableApi::class)
    fun loadChatPreviews() {
        val currentUserId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        Log.d("ChatViewModel", "Loading chats for user: $currentUserId")

        // Simplificar la consulta para evitar problemas con índices
        firestore.collection("chatPreviews")
            .whereEqualTo("ownerId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    Log.e("ChatViewModel", "Error loading chats", error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    ChatPreview(
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                } ?: emptyList()

                _chatPreviews.value = chats
                Log.d("ChatViewModel", "Chat previews updated. Count: ${chats.size}")
            }
    }


    @OptIn(UnstableApi::class)
    fun listenToMessages(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val participants = listOf(currentUserId, otherUserId).sorted()

        firestore.collection("messages")
            .whereEqualTo("participants", participants)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error listening to messages", error)
                    return@addSnapshotListener
                }

                val messagesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                _messages.value = messagesList
            }
    }
}