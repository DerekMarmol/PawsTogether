package com.example.pawstogether

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pawstogether.model.UserRating
import com.example.pawstogether.ui.theme.PawsTogetherTheme
import com.example.pawstogether.ui.theme.screens.AdoptionScreen
import com.example.pawstogether.ui.theme.screens.HomeScreen
import com.example.pawstogether.ui.theme.screens.LoginScreen
import com.example.pawstogether.ui.theme.screens.PetCareScreen
import com.example.pawstogether.ui.theme.screens.RatingScreen
import com.example.pawstogether.ui.theme.screens.RegisterScreen
import com.example.pawstogether.ui.theme.screens.ReportsScreen
import com.example.pawstogether.ui.theme.screens.ServicesScreen
import com.example.pawstogether.ui.theme.screens.VeterinaryListScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @kotlin.OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            PawsTogetherTheme {
                val navController = rememberNavController()
                AppNavigator(navController)
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun AppNavigator(navController: NavHostController) {
        NavHost(navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onEmailLogin = { email, password -> signInWithEmail(email, password, navController) },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegister = { email, password, petExperience, interests, services ->
                        registerWithEmail(email, password, petExperience, interests, services, navController)
                    }
                )
            }
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("reports") {
                ReportsScreen()
            }
            composable("adoption") {
                AdoptionScreen()
            }
            composable("services") {
                ServicesScreen(navController)
            }
            composable("PetCare") {
                PetCareScreen(navController)
            }
            composable("veterinary_list") {
                VeterinaryListScreen(navController) }
            composable(
                route = "rating/{userId}/{serviceType}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("serviceType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val serviceType = backStackEntry.arguments?.getString("serviceType") ?: ""

                // Aquí deberías obtener el `userName` desde alguna fuente, por ejemplo, desde Firestore o pasándolo desde la pantalla anterior.
                // Si lo obtienes de Firestore, tendrías que realizar una consulta asincrónica.
                val userName = "NombreUsuario" // Aquí deberías sustituirlo con el nombre real

                RatingScreen(
                    toUserId = userId,
                    serviceType = serviceType,
                    userName = userName, // Pasa el `userName` aquí
                    onRatingSubmit = { rating ->
                        saveRatingToFirebase(rating) {
                            navController.popBackStack()
                        }
                    },
                    onClose = {
                        navController.popBackStack()
                    }
                )
            }

        }
    }

    @OptIn(UnstableApi::class)
    private fun saveRatingToFirebase(rating: UserRating, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("ratings")
            .add(rating)
            .addOnSuccessListener {
                // Actualizar el promedio de calificaciones del usuario calificado
                updateUserRating(rating.toUserId)
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al guardar la reseña", e)
                onComplete()
            }
    }

    @OptIn(UnstableApi::class)
    private fun updateUserRating(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("ratings")
            .whereEqualTo("toUserId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val totalStars = documents.sumOf { it.getLong("stars")?.toInt() ?: 0 }
                val averageRating = if (documents.size() > 0) totalStars.toFloat() / documents.size() else 0f

                db.collection("users").document(userId)
                    .update("averageRating", averageRating)
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al actualizar el promedio de calificaciones", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al calcular el promedio de calificaciones", e)
            }
    }

    private fun signInWithEmail(email: String, password: String, navController: NavHostController) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(baseContext, "Inicio de sesión fallido: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerWithEmail(
        email: String,
        password: String,
        petExperience: String,
        interests: String,
        services: String,
        navController: NavHostController
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Extraer el nombre de usuario del email (antes del @)
                        val userName = email.substringBefore("@")

                        val userInfo = hashMapOf(
                            "email" to email,
                            "userName" to userName, // Agregamos el userName
                            "petExperience" to petExperience,
                            "interests" to interests,
                            "services" to services,
                            "averageRating" to 0f // Inicializamos el rating
                        )

                        db.collection("users").document(user.uid).set(userInfo)
                            .addOnSuccessListener {
                                navController.navigate("home") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    baseContext,
                                    "Error al guardar la información: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(
                        baseContext,
                        "Registro fallido: $errorMessage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}