package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreJobRepositoryImpl
import com.example.data.FirestoreWorkerRepositoryImpl
import com.example.data.JobRepository
import com.example.data.WorkerRepository
import com.example.model.Job
import com.example.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing state for JobDetailsScreen:
 * - Fetches real-time Job details by ID from Firestore
 * - Resolves applicants from the job's `appliedWorkers` UIDs list
 * - Executes the hiring action by updating Firestore `status` to "Assigned" and `assignedWorkerId`
 */
class JobDetailsViewModel(
    private val jobRepository: JobRepository = FirestoreJobRepositoryImpl(),
    private val workerRepository: WorkerRepository = FirestoreWorkerRepositoryImpl(),
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) : ViewModel() {

    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()

    private val _applicants = MutableStateFlow<List<User>>(emptyList())
    val applicants: StateFlow<List<User>> = _applicants.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isHiring = MutableStateFlow(false)
    val isHiring: StateFlow<Boolean> = _isHiring.asStateFlow()

    private val _hiredWorkerId = MutableStateFlow<String?>(null)
    val hiredWorkerId: StateFlow<String?> = _hiredWorkerId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    /**
     * Initializes or loads job details by ID and begins observing job changes.
     */
    fun loadJob(jobId: String, initialJob: Job? = null) {
        if (initialJob != null) {
            _job.value = initialJob
            fetchApplicants(initialJob.appliedWorkers)
        }

        viewModelScope.launch {
            _isLoading.value = _job.value == null
            jobRepository.getJobById(jobId).collect { updatedJob ->
                if (updatedJob != null) {
                    _job.value = updatedJob
                    fetchApplicants(updatedJob.appliedWorkers)
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * Resolves worker profiles for the given list of worker UIDs.
     */
    private fun fetchApplicants(workerUids: List<String>) {
        if (workerUids.isEmpty()) {
            _applicants.value = emptyList()
            return
        }

        viewModelScope.launch {
            val workers = workerRepository.getWorkersByIds(workerUids)
            _applicants.value = workers
        }
    }

    /**
     * Executes the hiring action:
     * - Updates that job's document in Firestore:
     *   sets `status` to "Assigned"
     *   creates/sets new field `assignedWorkerId` with that worker's UID
     */
    fun hireWorker(
        jobId: String,
        workerUid: String,
        workerName: String = "Worker",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isHiring.value = true
            _hiredWorkerId.value = workerUid

            // 1. Direct Firestore update as requested
            val db = firestore
            if (db != null) {
                try {
                    db.collection("jobs")
                        .document(jobId)
                        .update(
                            mapOf(
                                "status" to "Assigned",
                                "assignedWorkerId" to workerUid
                            )
                        )
                } catch (e: Exception) {
                    // Handled gracefully through repository update
                }
            }

            // 2. Repository update
            val result = jobRepository.hireWorkerForJob(jobId, workerUid)

            // 3. Optimistic local update
            _job.value = _job.value?.copy(
                status = "Assigned",
                assignedWorkerId = workerUid
            )

            _isHiring.value = false
            val success = result.isSuccess
            if (success) {
                _toastMessage.value = "Successfully hired $workerName for this job!"
            } else {
                _toastMessage.value = "Failed to update hiring status. Please try again."
            }
            onComplete?.invoke(success)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
