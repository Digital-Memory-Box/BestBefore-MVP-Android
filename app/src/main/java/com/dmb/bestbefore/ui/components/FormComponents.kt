package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    tintColor: Color,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isSelected) tintColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(2.dp, if (isSelected) tintColor else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { action() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Gray)
        Text(subtitle, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray)
    }
}

@Composable
fun DurationButton(
    label: String,
    days: Int,
    current: Int,
    tintColor: Color,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (current == days) tintColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable { onClick(days) }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (current == days) Color.White else Color.Gray)
    }
}

@Composable
fun ThemeOption(
    title: String,
    color: Color,
    isSelected: Boolean,
    action: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { action() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Brush.linearGradient(listOf(color.copy(0.5f), color)), CircleShape)
                .border(3.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
        )
        Text(title, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else Color.Gray)
    }
}
