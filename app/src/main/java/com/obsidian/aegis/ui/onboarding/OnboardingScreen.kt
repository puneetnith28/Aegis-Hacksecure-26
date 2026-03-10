package com.obsidian.aegis.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.RealTime,
        OnboardingPage.RiskDetection,
        OnboardingPage.HealthScore
    )
    val pagerState = rememberPagerState(initialPage = 0) {
        pages.size
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Skip Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onFinished) {
                    Text(text = "Skip", color = Color(0xFF1976D2))
                }
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { position ->
            OnboardingPagerItem(page = pages[position])
        }

        // Bottom Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFF1976D2) else Color.LightGray
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Buttons
            if (pagerState.currentPage == pages.size - 1) {
                Button(
                    onClick = onFinished,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Get Started", color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Continue", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OnboardingPagerItem(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simplified Illustration/Icon Placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFF0F7FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        if (page.highlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.Start) {
                page.highlights.forEach { highlight ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = highlight, fontSize = 14.sp, color = Color(0xFF555555))
                    }
                }
            }
        }
    }
}

sealed class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlights: List<String> = emptyList()
) {
    object Welcome : OnboardingPage(
        title = "Protect Your Privacy",
        description = "Aegis monitors camera, microphone, and location access in real time to protect your device from hidden tracking.",
        icon = Icons.Default.Security
    )
    object RealTime : OnboardingPage(
        title = "Know When Apps Access Sensors",
        description = "Aegis instantly alerts you when apps use your camera, microphone, or location.",
        icon = Icons.Default.GraphicEq,
        highlights = listOf("Camera Indicator", "Microphone Indicator", "Location Indicator")
    )
    object RiskDetection : OnboardingPage(
        title = "Detect Suspicious Activity",
        description = "Aegis analyzes app behavior and warns you when apps use sensors unexpectedly or in the background.",
        icon = Icons.Default.Warning,
        highlights = listOf("Risk Analysis", "Sensor Analysis", "Suspicious Activity Detection")
    )
    object HealthScore : OnboardingPage(
        title = "Understand Your Privacy Health",
        description = "Aegis gives your device a privacy score and helps you manage permissions to stay protected.",
        icon = Icons.Default.Assessment,
        highlights = listOf("Privacy Health Score", "Indicator Logs", "Custom Alerts")
    )
}
