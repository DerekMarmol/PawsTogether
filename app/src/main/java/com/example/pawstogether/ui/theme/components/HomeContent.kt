package com.example.pawstogether.ui.theme.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.ui.theme.models.PostAction

@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    petPosts: List<PetPost>,
    currentUserId: String,
    currentUserName: String,
    onNewPost: (String, String) -> Unit,
    onPostInteraction: (PostAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            NewPostCard(
                onNewPost = onNewPost,
                currentUserId = currentUserId,
                currentUserName = currentUserName
            )
        }

        items(petPosts) { post ->
            PetPostItem(
                post = post,
                currentUserId = currentUserId,
                onPostInteraction = onPostInteraction
            )
        }
    }
}