package com.example.pawstogether.utils

import android.util.Log
import com.example.pawstogether.model.Comment
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.ui.theme.models.PostAction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

suspend fun setupUserAndPosts(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onSetup: (String, String, List<PetPost>) -> Unit
) {
    auth.currentUser?.let { user ->
        val userId = user.uid
        val userDoc = db.collection("users").document(userId).get().await()
        val userName = userDoc.getString("userName") ?: ""

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("HomeScreen", "Error al escuchar cambios en posts", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PetPost::class.java)?.copy(id = doc.id)
                    }
                    onSetup(userId, userName, posts)
                }
            }
    }
}

suspend fun handleNewPost(
    post: PetPost,
    currentUserId: String,
    currentUserName: String,
    db: FirebaseFirestore
) {
    try {
        val postWithDetails = post.copy(
            userId = currentUserId,
            userName = currentUserName,
            timestamp = System.currentTimeMillis()
        )
        db.collection("posts").add(postWithDetails).await()
    } catch (e: Exception) {
        Log.e("HomeScreen", "Error al guardar el nuevo post", e)
    }
}

fun handlePostInteraction(
    action: PostAction,
    currentUserId: String,
    currentUserName: String,
    db: FirebaseFirestore
) {
    try {
        when (action) {
            is PostAction.Like -> {
                Log.d("handlePostInteraction", "Dando like al post: ${action.postId}")
                db.collection("posts").document(action.postId)
                    .update(
                        "likes", FieldValue.increment(1),
                        "likedBy", FieldValue.arrayUnion(currentUserId)
                    )
            }
            is PostAction.Unlike -> {
                Log.d("handlePostInteraction", "Quitando like del post: ${action.postId}")
                db.collection("posts").document(action.postId)
                    .update(
                        "likes", FieldValue.increment(-1),
                        "likedBy", FieldValue.arrayRemove(currentUserId)
                    )
            }
            is PostAction.Comment -> {
                Log.d("handlePostInteraction", "Añadiendo comentario al post: ${action.postId}")
                Log.d("handlePostInteraction", "Comentario: ${action.text}")
                val newComment = Comment(
                    userId = currentUserId,
                    userName = currentUserName,
                    text = action.text,
                    timestamp = System.currentTimeMillis()
                )
                db.collection("posts").document(action.postId)
                    .update("comments", FieldValue.arrayUnion(newComment))
                    .addOnSuccessListener {
                        Log.d("handlePostInteraction", "Comentario añadido exitosamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("handlePostInteraction", "Error al añadir comentario", e)
                    }
            }
        }
    } catch (e: Exception) {
        Log.e("handlePostInteraction", "Error al actualizar el post", e)
    }
}





