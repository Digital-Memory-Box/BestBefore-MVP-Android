package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mock model for collaborator
data class Collaborator(val email: String)

@Composable
fun CollaboratorListView(
    collaborators: List<Collaborator>,
    accentColor: Color,
    applyAccentToAll: Boolean,
    selectedTheme: String,
    onTap: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp), // Space before the main owner profile
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        collaborators.forEach { collaborator ->
            // Extract the nickname before the "@" symbol, fallback to "user"
            val nickname = collaborator.email.substringBefore("@").ifEmpty { "user" }
            
            Row(
                modifier = Modifier
                    .clickable { onTap(collaborator.email) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar Profile
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        )
                        .background(color = accentColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Person",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp) // Matching font system size 18 mapping
                    )
                }
                
                // Username Text with dynamic theme color validation
                val textColor = if (applyAccentToAll && selectedTheme != "Default") accentColor else Color.White
                
                Text(
                    text = "@$nickname",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollaboratorListViewPreview() {
    Box(
        modifier = Modifier
            .background(Color.DarkGray)
            .padding(16.dp)
    ) {
        CollaboratorListView(
            collaborators = listOf(
                Collaborator("john@example.com"),
                Collaborator("jane@example.com")
            ),
            accentColor = Color(0xFFFF9800), // Amber
            applyAccentToAll = true,
            selectedTheme = "Sunset"
        )
    }
}
