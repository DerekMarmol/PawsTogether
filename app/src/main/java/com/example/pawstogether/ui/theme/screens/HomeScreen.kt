package com.example.pawstogether.ui.theme.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.pawstogether.model.PetPost
import com.example.pawstogether.ui.theme.components.*
import com.example.pawstogether.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var currentUserId by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("") }
    var petPosts by remember { mutableStateOf(listOf<PetPost>()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        setupUserAndPosts(auth, db) { userId, userName, posts ->
            currentUserId = userId
            currentUserName = userName
            petPosts = posts
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                navController = navController,
                scope = scope
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(onMenuClick = { scope.launch { drawerState.open() } })
            }
        ) { paddingValues ->
            HomeContent(
                paddingValues = paddingValues,
                petPosts = petPosts,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                onNewPost = { mediaUrl, description ->
                    scope.launch {
                        val newPost = PetPost(
                            id = "",
                            userId = currentUserId,
                            userName = currentUserName,
                            mediaUrl = mediaUrl,
                            description = description,
                            isVideo = false,
                            likes = 0,
                            likedBy = emptyList(),
                            comments = emptyList(),
                            timestamp = System.currentTimeMillis()
                        )
                        handleNewPost(newPost, currentUserId, currentUserName, db)
                    }
                },
                onPostInteraction = { action ->
                    scope.launch {
                        handlePostInteraction(action, currentUserId, currentUserName, db)
                    }
                }
            )
        }
    }
}