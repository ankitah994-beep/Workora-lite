package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreJobRepositoryImpl
import com.example.data.JobRepository
import com.example.model.Job
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Represents the 3 dynamic application statuses required:
 * - Hired (Green) if assignedWorkerId matches the worker's UID.
 * - Closed (Grey) if assignedWorkerId exists but differs from the worker's UID.
 * - Pending (Yellow) if assignedWorkerId is null or empty.
 */
enum class WorkerApplicationStatus(val label: String) {
    HIRED("Hired"),
    PENDING("Pending"),
    CLOSED("Closed");

    companion object {
        fun resolve(assignedWorkerId: String?, currentWorkerUid: String): WorkerApplicationStatus {
            return when {
                !assignedWorkerId.isNullOrBlank() && assignedWorkerId == currentWorkerUid -> HIRED
                !assignedWorkerId.isNullOrBlank() && assignedWorkerId != currentWorkerUid -> CLOSED
                else -> PENDING
            }
        }
    }
}

class MyApplicationsViewModel(
    private val jobRepository: JobRepository = FirestoreJobRepositoryImpl()
) : ViewModel() {

    private val _appliedJobs = MutableStateFlow<List<Job>>(emptyList())
    val appliedJobs: StateFlow<List<Job>> = _appliedJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeFilter = MutableStateFlow("All")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    private var currentWorkerUid: String = ""

    fun initWorker(workerUid: String) {
        val uid = workerUid.ifBlank {
            FirebaseAuth.getInstance().currentUser?.uid ?: "1"
        }
        if (currentWorkerUid == uid && _appliedJobs.value.isNotEmpty()) return
        currentWorkerUid = uid
        loadApplications()
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun loadApplications() {
        if (currentWorkerUid.isBlank()) {
            currentWorkerUid = FirebaseAuth.getInstance().currentUser?.uid ?: "1"
        }
        _isLoading.value = true
        viewModelScope.launch {
            jobRepository.getJobsAppliedByWorker(currentWorkerUid)
                .catch {
                    _isLoading.value = false
                }
                .collect { jobs ->
                    _appliedJobs.value = jobs
                    _isLoading.value = false
                }
        }
    }

    fun getStatusForJob(job: Job): WorkerApplicationStatus {
        return WorkerApplicationStatus.resolve(job.assignedWorkerId, currentWorkerUid)
    }
}
