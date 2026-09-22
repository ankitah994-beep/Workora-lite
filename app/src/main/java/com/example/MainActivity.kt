package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.ScreenState
import com.example.model.UserRole
import com.example.ui.components.WorkoraBottomNavigationBar
import com.example.ui.screens.AccountSelectScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.JobDetailsScreen
import com.example.ui.screens.LabourDashboardScreen
import com.example.ui.screens.MyApplicationsScreen
import com.example.ui.screens.PostWorkScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.WorkoraTheme
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.JobViewModel
import com.example.viewmodel.PostWorkViewModel
import com.example.viewmodel.ProfileViewModel
import com.example.viewmodel.ReviewViewModel
import com.example.viewmodel.WorkoraViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "Workora app initialized")
        enableEdgeToEdge()
        setContent {
            WorkoraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkoraApp()
                }
            }
        }
    }
}

@Composable
fun WorkoraApp(
    viewModel: WorkoraViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    jobViewModel: JobViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    postWorkViewModel: PostWorkViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val availableWorkers by homeViewModel.availableWorkers.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isWorkerAvailable by viewModel.isWorkerAvailable.collectAsStateWithLifecycle()
    val customerTab by viewModel.customerTab.collectAsStateWithLifecycle()
    val labourTab by viewModel.labourTab.collectAsStateWithLifecycle()
    val homeSelectedTab by homeViewModel.selectedTab.collectAsStateWithLifecycle()

    // Bottom Navigation tab states:
    // Customer: 0 = Home, 1 = My Jobs, 2 = Profile
    // Worker: 0 = Find Work, 1 = My Apps, 2 = Profile
    var customerNavTab by rememberSaveable { mutableIntStateOf(0) }
    var workerNavTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedJobId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }

    // Sync Customer home screen segmented tab with bottom navigation
    LaunchedEffect(homeSelectedTab) {
        if (customerNavTab != 2 && customerNavTab != homeSelectedTab) {
            customerNavTab = homeSelectedTab
        }
    }

    // Sync Worker dashboard tab with bottom navigation
    LaunchedEffect(labourTab) {
        if (workerNavTab != 2 && workerNavTab != labourTab) {
            workerNavTab = labourTab
        }
    }

    // Determine user role
    val isWorker = currentUser?.role.equals("LABOUR", ignoreCase = true) || selectedRole == UserRole.LABOUR
    val activeRole = if (isWorker) UserRole.LABOUR else UserRole.CUSTOMER

    // Sync viewModel state when user logs in or out
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val role = if (user.role.equals("LABOUR", ignoreCase = true)) UserRole.LABOUR else UserRole.CUSTOMER
            viewModel.onUserLoggedIn(role)
        }
    }

    // If user is not authenticated, show Auth or Account Selection screen
    if (currentUser == null) {
        AnimatedContent(
            targetState = if (screenState == ScreenState.ACCOUNT_SELECTION) ScreenState.ACCOUNT_SELECTION else ScreenState.AUTH,
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
            },
            label = "auth_screen_transition"
        ) { currentScreen ->
            when (currentScreen) {
                ScreenState.ACCOUNT_SELECTION -> {
                    AccountSelectScreen(
                        onSelectRole = { role -> viewModel.selectRole(role) },
                        toastMessage = toastMessage
                    )
                }
                else -> {
                    AuthScreen(
                        authViewModel = authViewModel,
                        selectedRole = selectedRole ?: UserRole.CUSTOMER,
                        onRoleChanged = { role -> viewModel.setRole(role) },
                        onBackToRoleSelection = { viewModel.goToAccountType() },
                        onLoginSuccess = { user ->
                            val role = if (user.role.equals("LABOUR", ignoreCase = true)) UserRole.LABOUR else UserRole.CUSTOMER
                            viewModel.onUserLoggedIn(role)
                            customerNavTab = 0
                            workerNavTab = 0
                        },
                        toastMessage = toastMessage
                    )
                }
            }
        }
        return
    }

    // If Post Work screen is explicitly active for Customer, show PostWorkScreen
    if (screenState == ScreenState.POST_WORK) {
        PostWorkScreen(
            postWorkViewModel = postWorkViewModel,
            currentUser = currentUser,
            onBack = { viewModel.closePostWork() },
            onJobSubmittedSuccessfully = { savedJob ->
                viewModel.postNewJob(
                    title = savedJob.title,
                    category = savedJob.category,
                    description = savedJob.description,
                    dailyRate = 800,
                    location = savedJob.location,
                    workersNeeded = 1,
                    urgency = "Today",
                    dateTime = savedJob.date
                )
                jobViewModel.createJobRequest(
                    title = savedJob.title,
                    workType = savedJob.category,
                    description = savedJob.description,
                    offeredWage = 800,
                    location = savedJob.location,
                    dateTime = savedJob.date
                )
                viewModel.closePostWork()
                homeViewModel.setSelectedTab(1)
                customerNavTab = 1
                viewModel.showToast("Job posted successfully!")
            },
            toastMessage = toastMessage
        )
        return
    }

    // Authenticated App Shell with Material 3 Scaffold & Role-Based Bottom Navigation Bar
    val currentBottomIndex = if (activeRole == UserRole.CUSTOMER) customerNavTab else workerNavTab

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            WorkoraBottomNavigationBar(
                role = activeRole,
                selectedTab = currentBottomIndex,
                onTabSelected = { selectedIndex ->
                    selectedJobId = null
                    selectedJob = null
                    if (activeRole == UserRole.CUSTOMER) {
                        customerNavTab = selectedIndex
                        if (selectedIndex == 0) homeViewModel.setSelectedTab(0)
                        else if (selectedIndex == 1) homeViewModel.setSelectedTab(1)
                    } else {
                        workerNavTab = selectedIndex
                        if (selectedIndex == 0) viewModel.setLabourTab(0)
                        else if (selectedIndex == 1) viewModel.setLabourTab(1)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (activeRole == UserRole.CUSTOMER) {
                // Customer Views: Tab 0 (Home), Tab 1 (My Jobs), Tab 2 (Profile), or JobDetailsScreen
                val currentJobId = selectedJobId
                if (currentJobId != null) {
                    JobDetailsScreen(
                        jobId = currentJobId,
                        initialJob = selectedJob,
                        onBack = {
                            selectedJobId = null
                            selectedJob = null
                        }
                    )
                } else {
                    when (customerNavTab) {
                        0, 1 -> {
                            CustomerHomeScreen(
                                homeViewModel = homeViewModel,
                                currentUser = currentUser,
                                onOpenProfile = { customerNavTab = 2 },
                                onOpenPostWork = { viewModel.openPostWork() },
                                onJobClick = { job ->
                                    selectedJob = job
                                    selectedJobId = job.id
                                },
                                onHireWorker = { worker ->
                                    viewModel.hireWorkerDirectly(worker)
                                    jobViewModel.createJobRequest(
                                        title = "${worker.trade} Hiring Request",
                                        workType = worker.trade,
                                        description = "Direct hire request for ${worker.name}",
                                        offeredWage = worker.dailyWage,
                                        location = worker.location,
                                        worker = worker
                                    )
                                },
                                onPostWorkSubmitted = { title, cat, desc, rate, loc ->
                                    viewModel.postNewJob(title, cat, desc, rate, loc, 1, "Today", "Today, 9:00 AM")
                                    jobViewModel.createJobRequest(
                                        title = title,
                                        workType = cat,
                                        description = desc,
                                        offeredWage = rate,
                                        location = loc
                                    )
                                    customerNavTab = 1
                                    homeViewModel.setSelectedTab(1)
                                },
                                toastMessage = toastMessage
                            )
                        }
                        2 -> {
                            ProfileScreen(
                                profileViewModel = profileViewModel,
                                currentUser = currentUser,
                                role = UserRole.CUSTOMER,
                                onBack = { customerNavTab = 0 },
                                onSwitchRole = {
                                    viewModel.switchRole()
                                    customerNavTab = 0
                                    workerNavTab = 0
                                },
                                onLogout = {
                                    runCatching { FirebaseAuth.getInstance().signOut() }
                                    authViewModel.logout()
                                    viewModel.logout()
                                    customerNavTab = 0
                                    workerNavTab = 0
                                },
                                toastMessage = toastMessage,
                                onProfileSaved = { msg ->
                                    viewModel.showToast(msg)
                                }
                            )
                        }
                    }
                }
            } else {
                // Worker Views: Tab 0 (Find Work), Tab 1 (My Apps), Tab 2 (Profile)
                when (workerNavTab) {
                    0 -> {
                        LabourDashboardScreen(
                            jobViewModel = jobViewModel,
                            currentUser = currentUser,
                            jobs = jobs,
                            applications = applications,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.setCategoryFilter(it) },
                            activeTab = workerNavTab,
                            onTabSelected = { tab ->
                                workerNavTab = tab
                                viewModel.setLabourTab(tab)
                            },
                            isAvailable = isWorkerAvailable,
                            onToggleAvailability = { viewModel.toggleWorkerAvailability() },
                            onApplyJob = { job -> viewModel.applyForJob(job) },
                            onAcceptJob = { job ->
                                jobViewModel.acceptJob(job.id.toString())
                                viewModel.acceptJob(job)
                            },
                            onRejectJob = { job ->
                                jobViewModel.rejectJob(job.id.toString())
                                viewModel.rejectJob(job)
                            },
                            onCompleteJob = { jobId ->
                                jobViewModel.completeJob(jobId.toString())
                                viewModel.completeJob(jobId)
                            },
                            onSwitchRole = {
                                viewModel.switchRole()
                                customerNavTab = 0
                                workerNavTab = 0
                            },
                            onOpenProfile = { workerNavTab = 2 },
                            toastMessage = toastMessage
                        )
                    }
                    1 -> {
                        val currentWorkerUid = currentUser?.id?.toString()
                            ?: FirebaseAuth.getInstance().currentUser?.uid
                            ?: "1"
                        MyApplicationsScreen(
                            workerUid = currentWorkerUid,
                            currentUser = currentUser,
                            onExploreJobs = {
                                workerNavTab = 0
                                viewModel.setLabourTab(0)
                            }
                        )
                    }
                    2 -> {
                        ProfileScreen(
                            profileViewModel = profileViewModel,
                            currentUser = currentUser,
                            role = UserRole.LABOUR,
                            onBack = { workerNavTab = 0 },
                            onSwitchRole = {
                                viewModel.switchRole()
                                customerNavTab = 0
                                workerNavTab = 0
                            },
                            onLogout = {
                                runCatching { FirebaseAuth.getInstance().signOut() }
                                authViewModel.logout()
                                viewModel.logout()
                                customerNavTab = 0
                                workerNavTab = 0
                            },
                            toastMessage = toastMessage,
                            onProfileSaved = { msg ->
                                viewModel.showToast(msg)
                            }
                        )
                    }
                }
            }
        }
    }
}
