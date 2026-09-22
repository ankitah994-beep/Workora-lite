package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.WorkoraNavy
import com.example.ui.theme.WorkoraOrange
import com.example.ui.theme.WorkoraTextMuted

data class NavTabItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Material 3 NavigationBar (BottomNavigationBar) with role-based tabs:
 * - Customer: [Home, My Jobs, Profile]
 * - Worker: [Find Work, My Apps, Profile]
 */
@Composable
fun WorkoraBottomNavigationBar(
    role: UserRole,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = if (role == UserRole.CUSTOMER) {
        listOf(
            NavTabItem(label = "Home", icon = Icons.Default.Home, testTag = "nav_tab_customer_home"),
            NavTabItem(label = "My Jobs", icon = Icons.Default.Work, testTag = "nav_tab_customer_my_jobs"),
            NavTabItem(label = "Profile", icon = Icons.Default.Person, testTag = "nav_tab_customer_profile")
        )
    } else {
        listOf(
            NavTabItem(label = "Find Work", icon = Icons.Default.Search, testTag = "nav_tab_worker_find_work"),
            NavTabItem(label = "My Apps", icon = Icons.Default.Assignment, testTag = "nav_tab_worker_my_apps"),
            NavTabItem(label = "Profile", icon = Icons.Default.Person, testTag = "nav_tab_worker_profile")
        )
    }

    val primaryColor = if (role == UserRole.CUSTOMER) WorkoraOrange else WorkoraNavy

    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    unselectedIconColor = WorkoraTextMuted,
                    unselectedTextColor = WorkoraTextMuted,
                    indicatorColor = primaryColor.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
