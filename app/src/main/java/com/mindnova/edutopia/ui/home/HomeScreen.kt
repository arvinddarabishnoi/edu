package com.mindnova.edutopia.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mindnova.edutopia.core.components.EdutopiaCard
import com.mindnova.edutopia.core.components.EdutopiaGradientCard
import com.mindnova.edutopia.core.components.ErrorStateView
import com.mindnova.edutopia.core.components.LoadingStateView
import com.mindnova.edutopia.core.components.SubjectProgressBar
import com.mindnova.edutopia.core.components.XpProgressBar
import com.mindnova.edutopia.core.navigation.Screen
import com.mindnova.edutopia.core.theme.AccentAmber
import com.mindnova.edutopia.core.theme.AccentCyan
import com.mindnova.edutopia.core.theme.AccentEmerald
import com.mindnova.edutopia.core.theme.AccentRose
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.BrandIndigo
import com.mindnova.edutopia.core.theme.BrandPurple
import com.mindnova.edutopia.core.theme.ChemistryColor
import com.mindnova.edutopia.core.theme.MathematicsColor
import com.mindnova.edutopia.core.theme.PhysicsColor
import com.mindnova.edutopia.core.theme.SurfaceDark
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary
import com.mindnova.edutopia.core.theme.TextWhiteSecondary
import com.mindnova.edutopia.core.utils.DateTimeUtils

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    if (uiState.loading) {
        LoadingStateView()
        return
    }

    if (uiState.error != null && uiState.user == null) {
        ErrorStateView(
            errorMessage = uiState.error,
            onRetry = { viewModel.loadHomeData() }
        )
        return
    }

    val user = uiState.user ?: return
    val greeting = DateTimeUtils.getGreeting()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        color = TextWhiteSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = user.name,
                        color = TextWhitePrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { navController.navigate(Screen.Announcements.route) }
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhiteSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // ── XP / Level Hero Card ────────────────────────────────────
        item {
            EdutopiaGradientCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                gradientBrush = Brush.linearGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF1A0B2E))
                ),
                borderColor = Color(0xFF3730A3).copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.batchName?.let { "Batch: $it • " } ?: "",
                                color = TextWhiteSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Level ${user.level}",
                                color = AccentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${user.xp} XP",
                                color = TextWhiteSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        XpProgressBar(progress = uiState.levelProgress)
                        Text(
                            text = "${uiState.xpToNextLevel} XP to next level",
                            color = TextWhiteMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // Streak badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = AccentAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "${user.streak}",
                            color = TextWhitePrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "day streak",
                            color = TextWhiteMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ── Quick Actions ───────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Quick Actions",
                color = TextWhitePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    icon = Icons.Default.AutoStories,
                    label = "Continue\nLearning",
                    color = BrandIndigo,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Courses.route) }
                )
                QuickActionItem(
                    icon = Icons.Default.Quiz,
                    label = "Take a\nTest",
                    color = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.TestsList.route) }
                )
                QuickActionItem(
                    icon = Icons.Default.Assessment,
                    label = "PYQ\nPractice",
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.PyqList.route) }
                )
                QuickActionItem(
                    icon = Icons.Default.EmojiEvents,
                    label = "Daily\nGoal",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        uiState.dailyGoal?.test?.id?.let { testId ->
                            if (testId.isNotBlank()) {
                                navController.navigate(Screen.TestInstructions.createRoute(testId))
                            }
                        }
                    }
                )
            }
        }

        // ── Today's Goal ────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Today's Goal",
                color = TextWhitePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            val goal = uiState.dailyGoal
            if (goal != null) {
                EdutopiaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    backgroundColor = Color(0xFF18233C)
                ) {
                    Column {
                        // Lecture goal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.lecture.title,
                                    color = TextWhitePrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${goal.lecture.subject} • ${goal.lecture.chapter}",
                                    color = TextWhiteMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Test goal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Quiz, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.test.title,
                                    color = TextWhitePrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${goal.test.totalQuestions} Questions • ${goal.test.durationMinutes} min • ${goal.test.scheduledStartTime}",
                                    color = TextWhiteMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Series ───────────────────────────────────────────
        if (uiState.recentSeries.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Courses",
                        color = TextWhitePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See All",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { navController.navigate(Screen.Courses.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentSeries) { series ->
                        EdutopiaCard(
                            modifier = Modifier.width(180.dp),
                            backgroundColor = Color(0xFF18233C),
                            onClick = {
                                navController.navigate(Screen.SeriesDetail.createRoute(series.id))
                            }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                when (series.subject) {
                                                    "Physics" -> listOf(PhysicsColor.copy(alpha = 0.3f), PhysicsColor.copy(alpha = 0.1f))
                                                    "Chemistry" -> listOf(ChemistryColor.copy(alpha = 0.3f), ChemistryColor.copy(alpha = 0.1f))
                                                    else -> listOf(MathematicsColor.copy(alpha = 0.3f), MathematicsColor.copy(alpha = 0.1f))
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = series.title,
                                    color = TextWhitePrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${series.subject} • ${series.lectureCount} lectures",
                                    color = TextWhiteMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Results ──────────────────────────────────────────
        if (uiState.recentResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Recent Results",
                    color = TextWhitePrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(uiState.recentResults) { result ->
                EdutopiaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    backgroundColor = Color(0xFF18233C),
                    onClick = {
                        navController.navigate(Screen.TestReview.createRoute(result.id, result.testId))
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.testTitle,
                                color = TextWhitePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${result.score}/${result.maxScore} • ${result.accuracy}% accuracy",
                                color = TextWhiteSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(
                            text = "${result.score}",
                            color = if (result.score > result.maxScore / 2) AccentEmerald else AccentRose,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Stats Row ───────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    value = "${user.testsCompleted}",
                    label = "Tests",
                    color = BrandIndigo,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    value = "${user.lecturesWatched}",
                    label = "Lectures",
                    color = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    value = "${user.pyqsSolved}",
                    label = "PYQs",
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    value = "${user.points}",
                    label = "Points",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Leaderboard snapshot ───────────────────────────────────
        if (uiState.topStudents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Achievers",
                        color = TextWhitePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Full rankings",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Leaderboard.route)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.topStudents.forEachIndexed { idx, topper ->
                        EdutopiaCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFF18233C)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = listOf("🥇", "🥈", "🥉")[idx],
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = topper.name.ifBlank { "Student" }
                                        .split(" ").firstOrNull().orEmpty().take(9),
                                    color = TextWhitePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${topper.points} pts",
                                    color = AccentAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Announcements ───────────────────────────────────────────
        if (uiState.announcements.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Announcements",
                    color = TextWhitePrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(uiState.announcements) { announcement ->
                EdutopiaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    backgroundColor = Color(0xFF18233C),
                    borderColor = when (announcement.priority) {
                        "high" -> AccentRose.copy(alpha = 0.5f)
                        "low" -> TextWhiteMuted.copy(alpha = 0.3f)
                        else -> Color(0xFF2B3C62)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (announcement.priority) {
                                        "high" -> AccentRose
                                        "low" -> TextWhiteMuted
                                        else -> AccentCyan
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = announcement.title,
                                color = TextWhitePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = announcement.description,
                                color = TextWhiteMuted,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextWhiteSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    EdutopiaCard(
        modifier = modifier,
        backgroundColor = Color(0xFF18233C)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextWhiteMuted,
                fontSize = 11.sp
            )
        }
    }
}
