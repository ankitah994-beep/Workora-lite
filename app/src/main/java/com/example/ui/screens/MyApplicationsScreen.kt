package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Job
import com.example.model.UserAccount
import com.example.ui.theme.WorkoraBgLight
import com.example.ui.theme.WorkoraBorder
import com.example.ui.theme.WorkoraNavy
import com.example.ui.theme.WorkoraNavyDark
import com.example.ui.theme.WorkoraOrange
import com.example.ui.theme.WorkoraSuccess
import com.example.ui.theme.WorkoraSurface
import com.example.ui.theme.WorkoraTextDark
import com.example.ui.theme.WorkoraTextMuted
import com.example.ui.theme.WorkoraWarning
import com.example.viewmodel.MyApplicationsViewModel
import com.example.viewmodel.WorkerApplicationStatus

@Composable
fun MyApplicationsScreen(
    workerUid: String,
    currentUser: UserAccount? = null,
    onExploreJobs: () -> Unit = {},
    viewModel: MyApplicationsViewModel = viewModel()
) {
    LaunchedEffect(workerUid) {
        viewModel.initWorker(workerUid)
    }

    val appliedJobs by viewModel.appliedJobs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()

    val filteredJobs = when (activeFilter) {
        "Hired" -> appliedJobs.filter { viewModel.getStatusForJob(it) == WorkerApplicationStatus.HIRED }
        "Pending" -> appliedJobs.filter { viewModel.getStatusForJob(it) == WorkerApplicationStatus.PENDING }
        "Closed" -> appliedJobs.filter { viewModel.getStatusForJob(it) == WorkerApplicationStatus.CLOSED }
        else -> appliedJobs
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkoraBgLight)
            .testTag("my_applications_screen")
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WorkoraSurface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Applications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoraNavyDark
                        )
                        Text(
                            text = "Track the status of your submitted job applications",
                            fontSize = 12.sp,
                            color = WorkoraTextMuted
                        )
                    }

                    IconButton(
                        onClick = { viewModel.loadApplications() },
                        modifier = Modifier.testTag("btn_refresh_applications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh applications",
                            tint = WorkoraNavy
                        )
                    }
                }

                if (appliedJobs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // Filter Chips: All, Hired, Pending, Closed
                    val filters = listOf("All", "Hired", "Pending", "Closed")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(filters) { filter ->
                            val isSelected = activeFilter == filter
                            val count = when (filter) {
                                "Hired" -> appliedJobs.count { viewModel.getStatusForJob(it) == WorkerApplicationStatus.HIRED }
                                "Pending" -> appliedJobs.count { viewModel.getStatusForJob(it) == WorkerApplicationStatus.PENDING }
                                "Closed" -> appliedJobs.count { viewModel.getStatusForJob(it) == WorkerApplicationStatus.CLOSED }
                                else -> appliedJobs.size
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = {
                                    Text(
                                        text = "$filter ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WorkoraNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = WorkoraBgLight,
                                    labelColor = WorkoraTextDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) WorkoraNavy else WorkoraBorder,
                                    selectedBorderColor = WorkoraNavy,
                                    borderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("filter_chip_$filter")
                            )
                        }
                    }
                }
            }
        }

        // Content
        if (isLoading && appliedJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = WorkoraNavy,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading your applications...",
                        color = WorkoraTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (appliedJobs.isEmpty()) {
            // Friendly Material 3 Empty State
            EmptyApplicationsState(onExploreJobs = onExploreJobs)
        } else if (filteredJobs.isEmpty()) {
            // Filter produced zero results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WorkoraTextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No applications with status '$activeFilter'",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoraTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch to 'All' to see all your applications.",
                        fontSize = 13.sp,
                        color = WorkoraTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredJobs, key = { it.id }) { job ->
                    val status = viewModel.getStatusForJob(job)
                    WorkerJobApplicationCard(
                        job = job,
                        status = status
                    )
                }
            }
        }
    }
}

/**
 * Job card displaying dynamic status badge according to requirements:
 * - "Hired" (in Green) if assignedWorkerId matches the current worker's UID.
 * - "Closed" (in Grey) if assignedWorkerId exists but differs from current worker's UID.
 * - "Pending" (in Yellow) if assignedWorkerId is null or empty.
 */
@Composable
private fun WorkerJobApplicationCard(
    job: Job,
    status: WorkerApplicationStatus
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("application_card_${job.id}"),
        colors = CardDefaults.cardColors(containerColor = WorkoraSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (status == WorkerApplicationStatus.HIRED) 1.5.dp else 1.dp,
            color = if (status == WorkerApplicationStatus.HIRED) WorkoraSuccess.copy(alpha = 0.5f) else WorkoraBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .background(WorkoraOrange.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = job.category.ifBlank { "Trade Work" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoraOrange
                    )
                }

                // Dynamic Status Badge
                ApplicationStatusBadge(status = status, jobId = job.id)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Job Title
            Text(
                text = job.title.ifBlank { "${job.category} Service Request" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoraNavyDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = job.description,
                    fontSize = 13.sp,
                    color = WorkoraTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = WorkoraTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.location,
                            fontSize = 12.sp,
                            color = WorkoraTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (job.date.isNotBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = WorkoraTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.date,
                            fontSize = 12.sp,
                            color = WorkoraTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Explanation Banner
            when (status) {
                WorkerApplicationStatus.HIRED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFFA7F3D0)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Congratulations! You have been hired for this job.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }
                WorkerApplicationStatus.PENDING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFFFDE68A)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your application is currently pending review by the customer.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
                WorkerApplicationStatus.CLOSED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This position has been filled by another applicant.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Prominent dynamic status tag based on Firestore data:
 * - "Hired" (in Green)
 * - "Closed" (in Grey)
 * - "Pending" (in Yellow)
 */
@Composable
private fun ApplicationStatusBadge(
    status: WorkerApplicationStatus,
    jobId: String
) {
    val (backgroundColor, textColor, borderColor, icon) = when (status) {
        WorkerApplicationStatus.HIRED -> {
            Quad(
                Color(0xFFD1FAE5), // Soft green background
                Color(0xFF047857), // Deep green text
                Color(0xFFA7F3D0), // Green border
                Icons.Default.CheckCircle
            )
        }
        WorkerApplicationStatus.PENDING -> {
            Quad(
                Color(0xFFFEF3C7), // Soft yellow/amber background
                Color(0xFFB45309), // Amber text
                Color(0xFFFDE68A), // Amber border
                Icons.Default.HourglassTop
            )
        }
        WorkerApplicationStatus.CLOSED -> {
            Quad(
                Color(0xFFF1F5F9), // Soft grey background
                Color(0xFF64748B), // Grey text
                Color(0xFFCBD5E1), // Grey border
                Icons.Default.Lock
            )
        }
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("status_badge_$jobId")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Friendly Material 3 empty state displayed if the worker hasn't applied to any jobs yet.
 */
@Composable
private fun EmptyApplicationsState(
    onExploreJobs: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_applications_state"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WorkoraSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, WorkoraBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Friendly Icon Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(WorkoraNavy.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = "No Applications",
                        tint = WorkoraNavy,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "No Applications Yet",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoraNavyDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You haven't applied to any jobs yet. Explore available local trade jobs, submit your interest, and your applications will appear right here with real-time status updates.",
                    fontSize = 13.sp,
                    color = WorkoraTextMuted,
                    lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onExploreJobs,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkoraOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("btn_browse_jobs")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Available Jobs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
