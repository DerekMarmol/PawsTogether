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

suspend fun handlePostInteraction(
    action: PostAction,
    currentUserId: String,
    currentUserName: String,
    db: FirebaseFirestore
) {
    try {
        when (action) {
            is PostAction.Like -> {
                db.collection("posts").document(action.postId)
                    .update(
                        "likes", FieldValue.increment(1),
                        "likedBy", FieldValue.arrayUnion(currentUserId)
                    )
            }
            is PostAction.Unlike -> {
                db.collection("posts").document(action.postId)
                    .update(
                        "likes", FieldValue.increment(-1),
                        "likedBy", FieldValue.arrayRemove(currentUserId)
                    )
            }
            is PostAction.Comment -> {
                val newComment = Comment(
                    userId = currentUserId,
                    userName = currentUserName,
                    text = action.text
                )
                db.collection("posts").document(action.postId)
                    .update("comments", FieldValue.arrayUnion(newComment))
            }
        }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Error al actualizar el post", e)
    }
}