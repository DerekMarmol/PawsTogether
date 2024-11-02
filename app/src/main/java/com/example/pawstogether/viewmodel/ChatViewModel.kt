package com.example.pawstogether.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private suspend fun getCurrentUserDisplayName(): String {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return "Usuario"
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            userDoc.getString("displayName") ?: "Usuario"
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting display name", e)
            "Usuario"
        }
    }

    @OptIn(UnstableApi::class)
    fun sendMessage(receiverId: String, receiverName: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: run {
                    Log.e("ChatViewModel", "No current user found")
                    return@launch
                }
                val displayName = getCurrentUserDisplayName()
                val messageText = _messageText.value.trim()
                if (messageText.isEmpty()) return@launch

                Log.d("ChatViewModel", "Sending message to $receiverName (ID: $receiverId)")

                val message = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUser.uid,
                    receiverId = receiverId,
                    content = messageText,
                    senderName = displayName,
                    timestamp = System.currentTimeMillis()
                )

                val chatId = getChatId(currentUser.uid, receiverId)
                Log.d("ChatViewModel", "Generated chatId: $chatId")

                val batch = firestore.batch()

                val chatRef = firestore.collection("chats").document(chatId)
                batch.set(
                    chatRef,
                    hashMapOf(
                        "participants" to listOf(currentUser.uid, receiverId),
                        "lastMessage" to messageText,
                        "timestamp" to message.timestamp,
                        "otherUserName" to receiverName
                    )
                )

                val messageRef = chatRef.collection("messages").document(message.id)
                batch.set(messageRef, message)

                val currentUserPreviewRef = firestore.collection("chatPreviews")
                    .document(currentUser.uid)
                    .collection("userChats")
                    .document(receiverId)

                batch.set(
                    currentUserPreviewRef,
                    hashMapOf(
                        "userId" to receiverId,
                        "userName" to receiverName,
                        "lastMessage" to messageText,
                        "timestamp" to message.timestamp
                    )
                )

                val receiverPreviewRef = firestore.collection("chatPreviews")
                    .document(receiverId)
                    .collection("userChats")
                    .document(currentUser.uid)

                batch.set(
                    receiverPreviewRef,
                    hashMapOf(
                        "userId" to currentUser.uid,
                        "userName" to displayName, // Usar el displayName de Firestore
                        "lastMessage" to messageText,
                        "timestamp" to message.timestamp
                    )
                )

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ChatViewModel", "Message and chat previews successfully saved")
                        _messageText.value = ""
                        loadChatPreviews()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Error saving message and chat previews", e)
                    }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in sendMessage", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun startAdoptionChat(otherUserId: String, otherUserName: String, petName: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val initialMessage = "¡Hola! Me interesa adoptar a $petName. ¿Podríamos hablar sobre el proceso de adopción?"
                _messageText.value = initialMessage
                sendMessage(otherUserId, otherUserName)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error starting adoption chat", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun startServiceChat(otherUserId: String, otherUserName: String, serviceType: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val initialMessage = "¡Hola! Me interesa el servicio de $serviceType que ofreces. ¿Podríamos hablar sobre los detalles?"
                _messageText.value = initialMessage
                sendMessage(otherUserId, otherUserName)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error starting service chat", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun loadChatPreviews() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: run {
                    Log.e("ChatViewModel", "No current user found in loadChatPreviews")
                    return@launch
                }

                Log.d("ChatViewModel", "Loading chat previews for user: $currentUserId")
                _isLoading.value = true

                firestore.collection("chatPreviews")
                    .document(currentUserId)
                    .collection("userChats")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ChatViewModel", "Error loading chat previews", error)
                            _isLoading.value = false
                            return@addSnapshotListener
                        }

                        val chats = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                ChatPreview(
                                    userId = doc.getString("userId") ?: "",
                                    userName = doc.getString("userName") ?: "",
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0
                                ).also {
                                    Log.d("ChatViewModel", "Loaded chat preview: $it")
                                }
                            } catch (e: Exception) {
                                Log.e("ChatViewModel", "Error parsing chat preview", e)
                                null
                            }
                        } ?: emptyList()

                        Log.d("ChatViewModel", "Loaded ${chats.size} chat previews")
                        _chatPreviews.value = chats
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in loadChatPreviews", e)
                _isLoading.value = false
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun listenToMessages(otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: run {
                    Log.e("ChatViewModel", "No current user found in listenToMessages")
                    return@launch
                }

                val chatId = getChatId(currentUserId, otherUserId)
                Log.d("ChatViewModel", "Listening to messages for chatId: $chatId")

                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ChatViewModel", "Error listening to messages", error)
                            return@addSnapshotListener
                        }

                        val messagesList = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                doc.toObject(Message::class.java)
                            } catch (e: Exception) {
                                Log.e("ChatViewModel", "Error parsing message", e)
                                null
                            }
                        } ?: emptyList()

                        Log.d("ChatViewModel", "Received ${messagesList.size} messages")
                        _messages.value = messagesList
                    }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in listenToMessages", e)
            }
        }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
}
