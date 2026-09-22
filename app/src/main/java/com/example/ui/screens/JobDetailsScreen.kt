package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Job
import com.example.model.User
import com.example.ui.components.WorkoraToast
import com.example.ui.theme.WorkoraBgLight
import com.example.ui.theme.WorkoraBorder
import com.example.ui.theme.WorkoraNavy
import com.example.ui.theme.WorkoraNavyDark
import com.example.ui.theme.WorkoraNavySoft
import com.example.ui.theme.WorkoraOrange
import com.example.ui.theme.WorkoraOrangeSoft
import com.example.ui.theme.WorkoraSuccess
import com.example.ui.theme.WorkoraSurface
import com.example.ui.theme.WorkoraTextDark
import com.example.ui.theme.WorkoraTextMuted
import com.example.viewmodel.JobDetailsViewModel
import kotlinx.coroutines.launch

/**
 * JobDetailsScreen for Customers to view their posted job details and manage applicants.
 *
 * Requirements satisfied:
 * 1. Navigation: Accessible when clicking any job posted in the Customer's 'My Jobs' tab.
 * 2. View Applicants: Fetches and displays workers who applied using UIDs from `appliedWorkers`.
 * 3. Hire Action: Prominent "Hire Worker" button next to each applicant's card.
 * 4. Database Update: Updates the job document in Firestore with `status = "Assigned"`
 *    and `assignedWorkerId = workerUid`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    initialJob: Job? = null,
    onBack: () -> Unit,
    detailsViewModel: JobDetailsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(jobId) {
        detailsViewModel.loadJob(jobId, initialJob)
    }

    val jobState by detailsViewModel.job.collectAsStateWithLifecycle()
    val applicants by detailsViewModel.applicants.collectAsStateWithLifecycle()
    val isLoading by detailsViewModel.isLoading.collectAsStateWithLifecycle()
    val isHiring by detailsViewModel.isHiring.collectAsStateWithLifecycle()
    val hiredWorkerId by detailsViewModel.hiredWorkerId.collectAsStateWithLifecycle()
    val toastMessage by detailsViewModel.toastMessage.collectAsStateWithLifecycle()

    var workerToHireConfirm by remember { mutableStateOf<User?>(null) }
    var workerToCall by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            detailsViewModel.clearToast()
        }
    }

    val currentJob = jobState ?: initialJob

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("job_details_screen"),
        containerColor = WorkoraBgLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Job Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoraNavyDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("job_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to My Jobs",
                            tint = WorkoraNavyDark
                        )
                    }
                },
                actions = {
                    currentJob?.let { job ->
                        val isAssigned = job.status.equals("Assigned", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(
                                    if (isAssigned) WorkoraNavySoft else WorkoraSuccess.copy(alpha = 0.14f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("job_status_badge")
                        ) {
                            Text(
                                text = if (isAssigned) "Assigned" else "Open",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAssigned) WorkoraNavy else WorkoraSuccess
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WorkoraSurface)
            )
        }
    ) { innerPadding ->
        if (currentJob == null && isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WorkoraNavy,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (currentJob == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Job not found",
                    color = WorkoraTextMuted,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Job Information Card
                item {
                    JobInformationCard(job = currentJob)
                }

                // 2. Assigned Worker Banner (if already assigned)
                if (currentJob.status.equals("Assigned", ignoreCase = true)) {
                    item {
                        AssignedWorkerBanner(
                            assignedWorkerId = currentJob.assignedWorkerId,
                            applicants = applicants
                        )
                    }
                }

                // 3. Applicants Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("applicants_section"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Applicants",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoraNavyDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(WorkoraNavySoft, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${currentJob.appliedWorkers.size.coerceAtLeast(applicants.size)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoraNavy
                                )
                            }
                        }

                        Text(
                            text = if (currentJob.status.equals("Assigned", ignoreCase = true))
                                "Position Filled"
                            else
                                "Ready to hire",
                            fontSize = 12.sp,
                            color = WorkoraTextMuted
                        )
                    }
                }

                // 4. Applicants List or Empty State
                if (currentJob.appliedWorkers.isEmpty() && applicants.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("no_applicants_card"),
                            colors = CardDefaults.cardColors(containerColor = WorkoraSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, WorkoraBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(WorkoraNavySoft, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = WorkoraNavy,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No applicants yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoraTextDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Workers in your area will be notified of this job. Check back soon to review and hire skilled workers!",
                                    fontSize = 13.sp,
                                    color = WorkoraTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(applicants, key = { it.id }) { worker ->
                        val isAssignedToThis = currentJob.assignedWorkerId == worker.id.toString() ||
                                (currentJob.status.equals("Assigned", ignoreCase = true) && currentJob.assignedWorkerId == worker.name)

                        ApplicantWorkerCard(
                            worker = worker,
                            isAssigned = isAssignedToThis,
                            isAnyWorkerAssigned = currentJob.status.equals("Assigned", ignoreCase = true),
                            isHiringThisWorker = isHiring && hiredWorkerId == worker.id.toString(),
                            onHireClick = {
                                workerToHireConfirm = worker
                            },
                            onCallClick = {
                                workerToCall = worker
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Confirmation Dialog before Hiring
    workerToHireConfirm?.let { worker ->
        AlertDialog(
            onDismissRequest = { workerToHireConfirm = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(WorkoraOrangeSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WorkoraOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Hire ${worker.name}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Assign this job to ${worker.name} (${worker.trade})? The job status will be marked as 'Assigned' in Firestore and the worker will be notified.",
                    fontSize = 14.sp,
                    color = WorkoraTextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val workerToHire = worker
                        workerToHireConfirm = null
                        currentJob?.let { job ->
                            detailsViewModel.hireWorker(
                                jobId = job.id,
                                workerUid = workerToHire.id.toString(),
                                workerName = workerToHire.name
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkoraOrange),
                    modifier = Modifier.testTag("confirm_hire_button")
                ) {
                    Text("Confirm Hire", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { workerToHireConfirm = null }) {
                    Text("Cancel", color = WorkoraTextDark)
                }
            }
        )
    }

    // Call Worker Dialog
    workerToCall?.let { worker ->
        AlertDialog(
            onDismissRequest = { workerToCall = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(WorkoraNavySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = WorkoraNavy,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Call ${worker.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Connect directly with this applicant regarding work details.",
                        fontSize = 14.sp,
                        color = WorkoraTextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = worker.phone.ifBlank { "+91 98765 43210" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoraNavyDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phone = worker.phone.ifBlank { "+91 98765 43210" }
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        runCatching { context.startActivity(dialIntent) }
                        workerToCall = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WorkoraNavy)
                ) {
                    Text("Dial Now")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { workerToCall = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Top card presenting full Job Posting details:
 * Category, Title, Date, Location, and full Description.
 */
@Composable
private fun JobInformationCard(
    job: Job,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WorkoraSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WorkoraBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Category & Posted Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(WorkoraNavySoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = WorkoraNavy,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = job.category.ifBlank { "General Work" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoraNavy
                    )
                }

                Text(
                    text = "ID: #${job.id.takeLast(6)}",
                    fontSize = 12.sp,
                    color = WorkoraTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Job Title
            Text(
                text = job.title.ifBlank { "${job.category} Service" },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoraTextDark
            )

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.description,
                    fontSize = 14.sp,
                    color = WorkoraTextDark.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata: Date & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.date.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Date",
                            tint = WorkoraOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = job.date,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoraTextDark
                        )
                    }
                }

                if (job.location.isNotBlank()) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = WorkoraOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = job.location,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoraTextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner displayed when the job has been successfully assigned to a worker.
 */
@Composable
private fun AssignedWorkerBanner(
    assignedWorkerId: String?,
    applicants: List<User>,
    modifier: Modifier = Modifier
) {
    val assignedWorker = applicants.firstOrNull { it.id.toString() == assignedWorkerId }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoraSuccess.copy(alpha = 0.12f))
            .border(1.dp, WorkoraSuccess.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(WorkoraSuccess, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Job Assigned",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoraSuccess
                )
                Text(
                    text = if (assignedWorker != null)
                        "Assigned to ${assignedWorker.name} (${assignedWorker.trade})"
                    else
                        "Worker assigned (ID: ${assignedWorkerId ?: "Unknown"})",
                    fontSize = 12.sp,
                    color = WorkoraTextDark
                )
            }
        }
    }
}

/**
 * Card representing an applicant worker with profile summary and prominent "Hire Worker" button.
 */
@Composable
private fun ApplicantWorkerCard(
    worker: User,
    isAssigned: Boolean,
    isAnyWorkerAssigned: Boolean,
    isHiringThisWorker: Boolean,
    onHireClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("applicant_card_${worker.id}"),
        colors = CardDefaults.cardColors(containerColor = WorkoraSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isAssigned) WorkoraSuccess else WorkoraBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Initials
                val initials = worker.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifBlank { "W" }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(WorkoraNavySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoraNavy
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Worker Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = worker.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoraTextDark
                        )
                        if (worker.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = WorkoraOrange,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${worker.trade} • ${worker.experienceYears} yrs exp",
                        fontSize = 13.sp,
                        color = WorkoraTextMuted
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Rating and wage
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "%.1f".format(worker.rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoraTextDark
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "(${worker.reviewsCount})",
                            fontSize = 11.sp,
                            color = WorkoraTextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ₹${worker.dailyWage}/day",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoraOrange
                        )
                    }
                }

                // Quick Call Button
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(WorkoraNavySoft, CircleShape)
                        .testTag("applicant_call_button_${worker.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call ${worker.name}",
                        tint = WorkoraNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Prominent "Hire Worker" Button
            if (isAssigned) {
                // Already Assigned to this worker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(WorkoraSuccess, RoundedCornerShape(12.dp))
                        .testTag("hired_status_badge_${worker.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hired for this Job ✓",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Button(
                    onClick = onHireClick,
                    enabled = !isHiringThisWorker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("hire_worker_button_${worker.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnyWorkerAssigned) WorkoraNavy else WorkoraOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isHiringThisWorker) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigning...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnyWorkerAssigned) "Reassign & Hire Worker" else "Hire Worker",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
