package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.WorkoraDao
import com.example.data.WorkoraDatabase
import com.example.data.WorkoraRepository
import com.example.model.ScreenState
import com.example.model.UserAccount
import com.example.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: WorkoraDatabase
    private lateinit var dao: WorkoraDao
    private lateinit var repository: WorkoraRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.workoraDao()
        repository = WorkoraRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Workora", appName)
    }

    @Test
    fun prototypeScreenNavigationStatesExist() {
        val auth = ScreenState.AUTH
        val accountSelection = ScreenState.ACCOUNT_SELECTION
        val customerHome = ScreenState.CUSTOMER_HOME
        val postWork = ScreenState.POST_WORK
        val labourHome = ScreenState.LABOUR_HOME
        val profile = ScreenState.PROFILE

        assertNotNull(auth)
        assertNotNull(accountSelection)
        assertNotNull(customerHome)
        assertNotNull(postWork)
        assertNotNull(labourHome)
        assertNotNull(profile)
    }

    @Test
    fun sampleWorkerCardRetrieval() = runBlocking {
        val workers = repository.getWorkersByTrade("All").first()
        // Ensure worker profile queries work smoothly
        assertNotNull(workers)
    }

    @Test
    fun userProfileAccountDetailsWork() = runBlocking {
        val user = UserAccount(
            fullName = "Ramesh Verma",
            mobileNumber = "+91 98765 43210",
            email = "ramesh.verma@workora.com",
            password = "demoPassword123",
            location = "Sector 14, Gurugram",
            role = "CUSTOMER",
            isLoggedIn = true
        )
        val id = repository.registerUser(user)
        assertTrue(id > 0)

        val retrieved = repository.getLoggedInUserSync()
        assertNotNull(retrieved)
        assertEquals("Ramesh Verma", retrieved?.fullName)
        assertEquals("Sector 14, Gurugram", retrieved?.location)
    }

    @Test
    fun postWorkViewModelInputAndValidationTest() {
        val postWorkVm = com.example.viewmodel.PostWorkViewModel()
        assertEquals("Mason", postWorkVm.category.value)
        assertEquals("Sector 14, Gurugram", postWorkVm.location.value)

        // Validation fails if title is blank
        val initialValidation = postWorkVm.validate()
        org.junit.Assert.assertFalse(initialValidation)

        // Set inputs
        postWorkVm.setTitle("Need a plumber for pipe repair")
        postWorkVm.setCategory("Plumber")
        postWorkVm.setLocation("Sector 14, Gurugram")
        postWorkVm.setDateTime("Tomorrow, 10:00 AM")
        postWorkVm.setDescription("Fix leaking kitchen sink pipe")
        postWorkVm.setDailyWage("900")

        val valid = postWorkVm.validate()
        assertTrue(valid)
        assertEquals("Need a plumber for pipe repair", postWorkVm.title.value)
        assertEquals("Plumber", postWorkVm.category.value)
        assertEquals("900", postWorkVm.dailyWage.value)
    }

    @Test
    fun postWorkViewModelSubmitJobTest() = runBlocking {
        val mockJobRepo = com.example.data.MockJobRepository()
        val postWorkVm = com.example.viewmodel.PostWorkViewModel(jobRepository = mockJobRepo)

        postWorkVm.setTitle("Fix kitchen sink leak")
        postWorkVm.setCategory("Plumber")
        postWorkVm.setLocation("Sector 14, Gurugram")
        postWorkVm.setDateTime("Today, 2:00 PM")
        postWorkVm.setDescription("Pipe joint leaking water")

        var savedJobResult: com.example.model.Job? = null
        postWorkVm.submitJob(
            fallbackCustomerId = "test_customer_uid_123",
            onSuccess = { job ->
                savedJobResult = job
            },
            onError = { }
        )

        // Allow coroutine to complete
        kotlinx.coroutines.delay(100)

        assertNotNull(savedJobResult)
        assertEquals("Fix kitchen sink leak", savedJobResult?.title)
        assertEquals("Plumber", savedJobResult?.category)
        assertEquals("Sector 14, Gurugram", savedJobResult?.location)
        assertEquals("Today, 2:00 PM", savedJobResult?.date)
        assertEquals("Pipe joint leaking water", savedJobResult?.description)
        assertEquals("test_customer_uid_123", savedJobResult?.customerId)
        assertEquals("open", savedJobResult?.status)
    }

    @Test
    fun jobViewModelHireWorkerTest() = runBlocking {
        val mockJobRepo = com.example.data.MockJobRepository()
        val mockWorkerRepo = com.example.data.MockWorkerRepository()
        val jobVm = com.example.viewmodel.JobViewModel(
            jobRepository = mockJobRepo,
            workerRepository = mockWorkerRepo
        )

        // Seed a job with applicants
        val testJob = com.example.model.Job(
            id = "job_hire_test_101",
            title = "Painter Needed for 2BHK",
            category = "Painter",
            description = "Full home wall painting",
            location = "Sohna Road, Gurugram",
            date = "Tomorrow",
            dailyRate = 950,
            status = "open",
            customerId = "cust_101",
            appliedWorkers = listOf("worker_uid_1", "worker_uid_2")
        )
        mockJobRepo.saveJob(testJob)

        // Select the job
        jobVm.selectJob(testJob)
        assertEquals("job_hire_test_101", jobVm.selectedJob.value?.id)

        // Test hiring worker_uid_2
        var hireCompleted = false
        var hireSuccess = false
        jobVm.hireWorker(
            jobId = "job_hire_test_101",
            workerUid = "worker_uid_2",
            onComplete = { success ->
                hireCompleted = true
                hireSuccess = success
            }
        )

        // Give coroutines time
        kotlinx.coroutines.delay(100)

        assertTrue(hireCompleted)
        assertTrue(hireSuccess)

        // Verify updated job
        val updatedJob = mockJobRepo.getJobById("job_hire_test_101").first()
        assertNotNull(updatedJob)
        assertEquals("Assigned", updatedJob?.status)
        assertEquals("worker_uid_2", updatedJob?.assignedWorkerId)

        // Verify ViewModel's selectedJob state was also updated
        assertEquals("Assigned", jobVm.selectedJob.value?.status)
        assertEquals("worker_uid_2", jobVm.selectedJob.value?.assignedWorkerId)
    }

    @Test
    fun workerApplicationsQueryTest() = runBlocking {
        val mockJobRepo = com.example.data.MockJobRepository()
        val mockWorkerRepo = com.example.data.MockWorkerRepository()
        val jobVm = com.example.viewmodel.JobViewModel(
            jobRepository = mockJobRepo,
            workerRepository = mockWorkerRepo
        )

        val job1 = com.example.model.Job(
            id = "job_app_1",
            title = "Carpentry fitting",
            category = "Carpenter",
            appliedWorkers = listOf("worker_alpha", "worker_beta")
        )
        val job2 = com.example.model.Job(
            id = "job_app_2",
            title = "Electrical wiring",
            category = "Electrician",
            appliedWorkers = listOf("worker_gamma")
        )
        mockJobRepo.saveJob(job1)
        mockJobRepo.saveJob(job2)

        val appliedJobs = mockJobRepo.getJobsAppliedByWorker("worker_alpha").first()
        assertEquals(1, appliedJobs.size)
        assertEquals("job_app_1", appliedJobs[0].id)
    }

    @Test
    fun workoraViewModelNavigationRoutesTest() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val workoraVm = com.example.viewmodel.WorkoraViewModel(app)
        assertEquals(ScreenState.AUTH, workoraVm.screenState.value)

        // Test opening profile
        workoraVm.openProfile()
        assertEquals(ScreenState.PROFILE, workoraVm.screenState.value)
        workoraVm.closeProfile()
        assertEquals(ScreenState.CUSTOMER_HOME, workoraVm.screenState.value)

        // Test opening applications
        workoraVm.openMyApplications()
        assertEquals(ScreenState.MY_APPLICATIONS, workoraVm.screenState.value)
        workoraVm.closeMyApplications()
        assertEquals(ScreenState.LABOUR_HOME, workoraVm.screenState.value)

        // Test opening job details
        val sampleJob = com.example.model.Job(id = "job_test_nav", title = "Tiling")
        workoraVm.openJobDetails(sampleJob)
        assertEquals(ScreenState.JOB_DETAILS, workoraVm.screenState.value)
        assertEquals("job_test_nav", workoraVm.selectedJob.value?.id)

        workoraVm.closeJobDetails()
        assertEquals(ScreenState.CUSTOMER_HOME, workoraVm.screenState.value)
        org.junit.Assert.assertNull(workoraVm.selectedJob.value)
    }
}
