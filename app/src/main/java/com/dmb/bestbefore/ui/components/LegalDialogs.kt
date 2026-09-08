package com.dmb.bestbefore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmb.bestbefore.ui.theme.LocalBestBeforeColors

private data class LegalSection(val title: String, val content: String)

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val colors = LocalBestBeforeColors.current
    val sections = listOf(
        LegalSection(
            "1. Overview",
            "BestBefore (\"we\", \"our\", or \"us\") is dedicated to protecting your privacy. This Privacy Policy explains how our time-capsule and memory sharing service collects, uses, stores, and protects your information."
        ),
        LegalSection(
            "2. Information We Collect",
            "• Account Data: Your email address, display name, user type (Normal or Artist), and profile picture.\n" +
            "• Room & Memory Content: Photographs, videos, descriptions, tags, and time-capsule dates you submit to rooms.\n" +
            "• Background Audio: SoundCloud audio track references associated with your time-capsule rooms.\n" +
            "• Diagnostic Information: Push notification device tokens (Firebase Cloud Messaging) and error logs to maintain app stability."
        ),
        LegalSection(
            "3. How We Use Information",
            "• To host and synchronize your collaborative memory rooms and scheduled time-capsules.\n" +
            "• To deliver automated notifications when a time-capsule unlocks or collaborators add content.\n" +
            "• To enforce safety guidelines, prevent abuse, and process content moderation reports.\n" +
            "• We never sell, rent, or trade your personal data to third parties."
        ),
        LegalSection(
            "4. Third-Party Services",
            "We rely on trusted cloud infrastructure:\n" +
            "• Google Firebase: Authentication, security checks, and push notification dispatch.\n" +
            "• Cloud Hosting: Secure cloud-hosted backend APIs for encrypted data storage.\n" +
            "• SoundCloud API: Streaming background music playback as linked by room owners."
        ),
        LegalSection(
            "5. Data Retention & Account Deletion",
            "You retain full control of your data. You may remove individual memories or delete your account entirely at any time via Settings > Delete Account. Account deletion permanently purges your credentials, profile, owned rooms, and media from our servers."
        ),
        LegalSection(
            "6. Contact Us",
            "If you have questions regarding this Privacy Policy or your personal data, please contact our support team at support@bestbefore.app."
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(
                text = "Privacy Policy",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Last updated: September 2026",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = section.title,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = section.content,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    val colors = LocalBestBeforeColors.current
    val sections = listOf(
        LegalSection(
            "1. Agreement to Terms",
            "By accessing or using BestBefore, you agree to be bound by these Terms of Service. If you do not agree with any part of these terms, you must discontinue use of the application."
        ),
        LegalSection(
            "2. Eligibility & Account Security",
            "You must be at least 16 years of age to register an account. You are responsible for safeguarding your login credentials and for any activities or actions conducted under your account."
        ),
        LegalSection(
            "3. User-Generated Content & Conduct",
            "You retain ownership of any media, text, or memories you upload. You represent that you own or have the necessary rights to share your content.\n\n" +
            "Prohibited behavior includes:\n" +
            "• Uploading illegal, defamatory, harassing, sexually explicit, or infringing content.\n" +
            "• Impersonating other individuals or misrepresenting identity.\n" +
            "• Attempting to disrupt, reverse engineer, or compromise the service's integrity."
        ),
        LegalSection(
            "4. Time-Capsule Rooms",
            "BestBefore enables time-delayed memory sharing. By creating or joining a room, you acknowledge that memories scheduled for future release remain sealed until the designated unlock date configured by the room creator."
        ),
        LegalSection(
            "5. Moderation & Termination",
            "We reserve the right to review, moderate, and remove any content that violates these Terms or our community guidelines. We may suspend or terminate accounts that repeatedly engage in prohibited conduct."
        ),
        LegalSection(
            "6. Disclaimer & Limitation of Liability",
            "BestBefore is provided on an \"AS IS\" and \"AS AVAILABLE\" basis without warranties of any kind. To the maximum extent permitted by applicable law, we shall not be liable for indirect, incidental, or consequential damages."
        ),
        LegalSection(
            "7. Governing Law & Contact",
            "These Terms are governed by applicable laws. For inquiries regarding our terms, please contact legal@bestbefore.app."
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(
                text = "Terms of Service",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Last updated: September 2026",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = section.title,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = section.content,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
