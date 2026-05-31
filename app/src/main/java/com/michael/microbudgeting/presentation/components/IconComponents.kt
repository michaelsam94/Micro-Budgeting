package com.michael.microbudgeting.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

fun getSafeIcon(name: String): ImageVector {
    return when (name) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "restaurant" -> Icons.Default.Favorite
        "directions_car" -> Icons.Default.Home
        "local_hospital" -> Icons.Default.Face
        "bolt" -> Icons.Default.Build
        "local_mall" -> Icons.Default.ShoppingCart
        "settings" -> Icons.Default.Settings
        "add" -> Icons.Default.Add
        "delete" -> Icons.Default.Delete
        else -> Icons.Default.Star
    }
}

@Composable
fun CategoryBadge(
    iconName: String,
    hexColor: String,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .background(bgColor.copy(alpha = 0.15f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getSafeIcon(iconName),
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}
