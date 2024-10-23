package com.example.pawstogether.ui.theme.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pawstogether.ui.theme.models.DrawerItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    navController: NavHostController,
    scope: CoroutineScope
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        DrawerItems.values().forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = false,
                onClick = {
                    navController.navigate(item.route)
                    scope.launch { drawerState.close() }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}