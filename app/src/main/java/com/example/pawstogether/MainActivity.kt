package com.example.pawstogether

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pawstogether.ui.theme.PawsTogetherTheme
import com.example.pawstogether.ui.theme.screens.HomeScreen
import com.example.pawstogether.ui.theme.screens.LoginScreen
import com.example.pawstogether.ui.theme.screens.RegisterScreen
import com.example.pawstogether.ui.theme.screens.ReportsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
                HomeScreen()
            }
            composable("reports"){
                ReportsScreen()
            }
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

    private fun registerWithEmail(email: String, password: String, petExperience: String, interests: String, services: String, navController: NavHostController) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userInfo = hashMapOf(
                            "email" to email,
                            "petExperience" to petExperience,
                            "interests" to interests,
                            "services" to services
                        )
                        db.collection("users").document(user.uid).set(userInfo)
                            .addOnSuccessListener {
                                navController.navigate("home") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(baseContext, "Error al guardar la información: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(baseContext, "Registro fallido: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }
}