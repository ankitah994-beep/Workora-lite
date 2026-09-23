package com.example.data

import com.example.model.Job
import com.example.model.JobRequest
import com.example.model.JobStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * FirestoreJobRepositoryImpl implements JobRepository using Firebase Firestore.
 * Provides fallback in-memory state if Firebase is not initialized or offline.
 */
class FirestoreJobRepositoryImpl(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) : JobRepository {

    private val jobsCollection = "jobs"

    // Default fallback seed data
    private val fallbackJobs = MutableStateFlow(
        listOf(
            JobRequest(
                id = "1",
                customerId = "customer_1",
                customerName = "Ramesh Verma",
                customerPhone = "+91 98765 43210",
                workerId = "1",
                workerName = "Rajesh Sharma",
                title = "Wall Plastering & Brickwork",
                workType = "Mason",
                description = "Need experienced mason for boundary wall plastering and cement repair work.",
                location = "Sector 14, Main Market, Gurugram",
                dateTime = "Today, 9:00 AM",
                offeredWage = 800,
                workersNeeded = 1,
                urgency = "Today",
                status = JobStatus.PENDING
            ),
            JobRequest(
                id = "2",
                customerId = "customer_1",
                customerName = "Ramesh Verma",
                customerPhone = "+91 98765 43210",
                workerId = "2",
                workerName = "Sunil Kumar",
                title = "Interior Painting 2BHK",
                workType = "Painter",
                description = "Living room and 2 bedrooms wall putty and plastic paint work.",
                location = "Sector 12, Gurugram",
                dateTime = "Tomorrow, 8:30 AM",
                offeredWage = 650,
                workersNeeded = 2,
                urgency = "Flexible",
                status = JobStatus.ACCEPTED
            ),
            JobRequest(
                id = "3",
                customerId = "customer_1",
                customerName = "Ramesh Verma",
                customerPhone = "+91 98765 43210",
                workerId = "3",
                workerName = "Amit Verma",
                title = "Wooden Door & Frame Fitting",
                workType = "Carpenter",
                description = "Door frame installation and lock fixing for main entrance.",
                location = "DLF Phase 3, Gurugram",
                dateTime = "Yesterday, 10:00 AM",
                offeredWage = 750,
                workersNeeded = 1,
                urgency = "Done",
                status = JobStatus.COMPLETED
            )
        )
    )

    private val localJobsFallback = MutableStateFlow<List<Job>>(
        listOf(
            Job(
                id = "1",
                title = "Wall Plastering & Brickwork",
                category = "Mason",
                location = "Sector 14, Main Market, Gurugram",
                date = "Today, 9:00 AM",
                description = "Need experienced mason for boundary wall plastering and cement repair work.",
                customerId = "customer_1",
                status = "open",
                timestamp = System.currentTimeMillis() - 7200000,
                appliedWorkers = listOf("1", "2"),
                assignedWorkerId = null
            ),
            Job(
                id = "2",
                title = "Interior Painting 2BHK",
                category = "Painter",
                location = "Sector 12, Gurugram",
                date = "Tomorrow, 8:30 AM",
                description = "Living room and 2 bedrooms wall putty and plastic paint work.",
                customerId = "customer_1",
                status = "open",
                timestamp = System.currentTimeMillis() - 3600000,
                appliedWorkers = listOf("2"),
                assignedWorkerId = null
            ),
            Job(
                id = "3",
                title = "Wooden Door & Frame Fitting",
                category = "Carpenter",
                location = "DLF Phase 3, Gurugram",
                date = "Yesterday, 10:00 AM",
                description = "Door frame installation and lock fixing for main entrance.",
                customerId = "customer_1",
                status = "Assigned",
                timestamp = System.currentTimeMillis() - 86400000,
                appliedWorkers = listOf("3", "1"),
                assignedWorkerId = "3"
            ),
            Job(
                id = "4",
                title = "Bathroom Plumbing & Pipe Replacement",
                category = "Plumber",
                location = "Sohna Road, Gurugram",
                date = "Today, 2:00 PM",
                description = "Fix dripping pipes and install new taps in master bathroom.",
                customerId = "customer_2",
                status = "Assigned",
                timestamp = System.currentTimeMillis() - 1800000,
                appliedWorkers = listOf("1"),
                assignedWorkerId = "1"
            )
        )
    )

    override suspend fun saveJob(job: Job): Result<Job> {
        val db = firestore
        val finalId = if (job.id.isNotBlank()) {
            job.id
        } else {
            db?.collection(jobsCollection)?.document()?.id ?: System.currentTimeMillis().toString()
        }
        val jobToSave = job.copy(id = finalId)

        // Always update local fallback
        val currentLocal = localJobsFallback.value.toMutableList()
        currentLocal.removeAll { it.id == jobToSave.id }
        currentLocal.add(0, jobToSave)
        localJobsFallback.value = currentLocal

        return try {
            if (db != null) {
                val docRef = db.collection(jobsCollection).document(finalId)
                val map = hashMapOf<String, Any>(
                    "id" to jobToSave.id,
                    "title" to jobToSave.title,
                    "category" to jobToSave.category,
                    "location" to jobToSave.location,
                    "date" to jobToSave.date,
                    "description" to jobToSave.description,
                    "customerId" to jobToSave.customerId,
                    "status" to jobToSave.status,
                    "timestamp" to jobToSave.timestamp,
                    "appliedWorkers" to jobToSave.appliedWorkers
                )
                if (jobToSave.assignedWorkerId != null) {
                    map["assignedWorkerId"] = jobToSave.assignedWorkerId
                }
                docRef.set(map, SetOptions.merge()).awaitTask()
            }
            Result.success(jobToSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPostedJobsByCustomer(customerId: String): Flow<List<Job>> {
        val db = firestore
        if (db == null) {
            return localJobsFallback.asStateFlow().map { list ->
                if (customerId.isBlank()) list
                else list.filter { it.customerId == customerId }
            }
        }

        return callbackFlow {
            val query = if (customerId.isNotBlank()) {
                db.collection(jobsCollection).whereEqualTo("customerId", customerId)
            } else {
                db.collection(jobsCollection)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = localJobsFallback.value.filter { customerId.isBlank() || it.customerId == customerId }
                    trySend(fallback)
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents?.mapNotNull { it.toJob() } ?: emptyList()
                if (jobs.isEmpty() && localJobsFallback.value.isNotEmpty()) {
                    val fallback = localJobsFallback.value.filter { customerId.isBlank() || it.customerId == customerId }
                    trySend(fallback)
                } else {
                    trySend(jobs.sortedByDescending { it.timestamp })
                }
            }

            awaitClose {
                listener.remove()
            }
        }
    }

    override suspend fun createJobRequest(jobRequest: JobRequest): Boolean {
        val db = firestore
        val newRequest = if (jobRequest.status != JobStatus.PENDING) {
            jobRequest.copy(status = JobStatus.PENDING)
        } else {
            jobRequest
        }

        // Always update in-memory fallback
        val currentList = fallbackJobs.value.toMutableList()
        currentList.removeAll { it.id == newRequest.id }
        currentList.add(0, newRequest)
        fallbackJobs.value = currentList

        if (db == null) return true

        return try {
            val map = hashMapOf<String, Any>(
                "id" to newRequest.id,
                "customerId" to newRequest.customerId,
                "customerName" to newRequest.customerName,
                "customerPhone" to newRequest.customerPhone,
                "workerId" to newRequest.workerId,
                "workerName" to newRequest.workerName,
                "title" to newRequest.title,
                "workType" to newRequest.workType,
                "description" to newRequest.description,
                "location" to newRequest.location,
                "dateTime" to newRequest.dateTime,
                "offeredWage" to newRequest.offeredWage,
                "workersNeeded" to newRequest.workersNeeded,
                "urgency" to newRequest.urgency,
                "status" to newRequest.status.name,
                "timestamp" to newRequest.timestamp
            )
            db.collection(jobsCollection)
                .document(newRequest.id)
                .set(map, SetOptions.merge())
                .awaitTask()
            true
        } catch (e: Exception) {
            // In-memory fallback already has the request
            true
        }
    }

    override fun getCustomerJobs(customerId: String): Flow<List<JobRequest>> {
        val db = firestore
        if (db == null) {
            return fallbackJobs.asStateFlow().map { list ->
                if (customerId.isBlank()) list
                else list.filter { it.customerId == customerId }
            }
        }

        return callbackFlow {
            val query = if (customerId.isNotBlank()) {
                db.collection(jobsCollection).whereEqualTo("customerId", customerId)
            } else {
                db.collection(jobsCollection)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = fallbackJobs.value.filter { customerId.isBlank() || it.customerId == customerId }
                    trySend(fallback)
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents?.mapNotNull { it.toJobRequest() } ?: emptyList()
                if (jobs.isEmpty() && fallbackJobs.value.isNotEmpty()) {
                    val fallback = fallbackJobs.value.filter { customerId.isBlank() || it.customerId == customerId }
                    trySend(fallback)
                } else {
                    trySend(jobs)
                }
            }

            awaitClose {
                listener.remove()
            }
        }
    }

    override fun getWorkerJobs(workerId: String): Flow<List<JobRequest>> {
        val db = firestore
        if (db == null) {
            return fallbackJobs.asStateFlow().map { list ->
                if (workerId.isBlank()) list
                else list.filter { it.workerId.isBlank() || it.workerId == workerId }
            }
        }

        return callbackFlow {
            val query = if (workerId.isNotBlank()) {
                db.collection(jobsCollection).whereEqualTo("workerId", workerId)
            } else {
                db.collection(jobsCollection)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = fallbackJobs.value.filter { workerId.isBlank() || it.workerId.isBlank() || it.workerId == workerId }
                    trySend(fallback)
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents?.mapNotNull { it.toJobRequest() } ?: emptyList()
                if (jobs.isEmpty() && fallbackJobs.value.isNotEmpty()) {
                    val fallback = fallbackJobs.value.filter { workerId.isBlank() || it.workerId.isBlank() || it.workerId == workerId }
                    trySend(fallback)
                } else {
                    trySend(jobs)
                }
            }

            awaitClose {
                listener.remove()
            }
        }
    }

    override suspend fun updateJobStatus(jobId: String, newStatus: JobStatus): Boolean {
        // Update fallback in-memory
        val currentList = fallbackJobs.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == jobId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = newStatus)
            fallbackJobs.value = currentList
        }

        val db = firestore ?: return index != -1

        return try {
            db.collection(jobsCollection)
                .document(jobId)
                .update("status", newStatus.name)
                .awaitTask()
            true
        } catch (e: Exception) {
            index != -1
        }
    }

    private fun DocumentSnapshot.toJobRequest(): JobRequest? {
        val data = this.data ?: return null
        return try {
            val statusStr = data["status"] as? String ?: "PENDING"
            val parsedStatus = runCatching { JobStatus.valueOf(statusStr) }.getOrDefault(JobStatus.PENDING)

            JobRequest(
                id = data["id"] as? String ?: id,
                customerId = data["customerId"] as? String ?: "customer_1",
                customerName = data["customerName"] as? String ?: "Customer",
                customerPhone = data["customerPhone"] as? String ?: "",
                workerId = data["workerId"] as? String ?: "",
                workerName = data["workerName"] as? String ?: "",
                title = data["title"] as? String ?: "",
                workType = data["workType"] as? String ?: "",
                description = data["description"] as? String ?: "",
                location = data["location"] as? String ?: "",
                dateTime = data["dateTime"] as? String ?: "Today, 9:00 AM",
                offeredWage = (data["offeredWage"] as? Number)?.toInt() ?: 700,
                workersNeeded = (data["workersNeeded"] as? Number)?.toInt() ?: 1,
                urgency = data["urgency"] as? String ?: "Today",
                status = parsedStatus,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun getJobById(jobId: String): Flow<Job?> {
        val db = firestore
        if (db == null) {
            return localJobsFallback.asStateFlow().map { list ->
                list.firstOrNull { it.id == jobId }
            }
        }

        return callbackFlow {
            val docRef = db.collection(jobsCollection).document(jobId)
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val fallback = localJobsFallback.value.firstOrNull { it.id == jobId }
                    trySend(fallback)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val job = snapshot.toJob()
                    if (job != null) {
                        trySend(job)
                        return@addSnapshotListener
                    }
                }
                val fallback = localJobsFallback.value.firstOrNull { it.id == jobId }
                trySend(fallback)
            }

            awaitClose {
                listener.remove()
            }
        }
    }

    override suspend fun hireWorkerForJob(jobId: String, workerId: String): Result<Unit> {
        // Update local fallback immediately
        val current = localJobsFallback.value.toMutableList()
        val index = current.indexOfFirst { it.id == jobId }
        if (index != -1) {
            current[index] = current[index].copy(
                status = "Assigned",
                assignedWorkerId = workerId
            )
            localJobsFallback.value = current
        }

        val db = firestore ?: return Result.success(Unit)

        return try {
            val updates = mapOf<String, Any>(
                "status" to "Assigned",
                "assignedWorkerId" to workerId
            )
            db.collection(jobsCollection)
                .document(jobId)
                .update(updates)
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if network failed or document didn't exist in cloud, return success for local state
            Result.success(Unit)
        }
    }

    override suspend fun applyForJob(jobId: String, workerUid: String): Boolean {
        // Update local fallback immediately
        val current = localJobsFallback.value.toMutableList()
        val index = current.indexOfFirst { it.id == jobId }
        if (index != -1) {
            val job = current[index]
            val updatedWorkers = if (!job.appliedWorkers.contains(workerUid)) {
                job.appliedWorkers + workerUid
            } else {
                job.appliedWorkers
            }
            current[index] = job.copy(appliedWorkers = updatedWorkers)
            localJobsFallback.value = current
        }

        val db = firestore ?: return true

        return try {
            db.collection(jobsCollection)
                .document(jobId)
                .update("appliedWorkers", FieldValue.arrayUnion(workerUid))
                .awaitTask()
            true
        } catch (e: Exception) {
            try {
                val doc = db.collection(jobsCollection).document(jobId).get().awaitTask()
                if (doc.exists()) {
                    val currentWorkers = (doc.get("appliedWorkers") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val updated = if (!currentWorkers.contains(workerUid)) currentWorkers + workerUid else currentWorkers
                    db.collection(jobsCollection).document(jobId).update("appliedWorkers", updated).awaitTask()
                } else {
                    db.collection(jobsCollection).document(jobId).set(
                        mapOf("appliedWorkers" to listOf(workerUid)),
                        SetOptions.merge()
                    ).awaitTask()
                }
                true
            } catch (inner: Exception) {
                index != -1
            }
        }
    }

    override suspend fun hireWorker(jobId: String, workerUid: String): Boolean {
        hireWorkerForJob(jobId, workerUid)
        return true
    }

    override fun getJobsAppliedByWorker(workerUid: String): Flow<List<Job>> {
        val db = firestore
        if (db == null) {
            return localJobsFallback.asStateFlow().map { list ->
                list.filter { it.appliedWorkers.contains(workerUid) }
            }
        }

        return callbackFlow {
            val listener = db.collection(jobsCollection)
                .whereArrayContains("appliedWorkers", workerUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val fallback = localJobsFallback.value.filter { it.appliedWorkers.contains(workerUid) }
                        trySend(fallback)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val jobs = snapshot.documents.mapNotNull { it.toJob() }
                        trySend(jobs)
                    } else if (snapshot != null && snapshot.isEmpty) {
                        val fallback = localJobsFallback.value.filter { it.appliedWorkers.contains(workerUid) }
                        trySend(fallback)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    private fun DocumentSnapshot.toJob(): Job? {
        val data = this.data ?: return null
        return try {
            val appliedList = (data["appliedWorkers"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            Job(
                id = data["id"] as? String ?: id,
                title = data["title"] as? String ?: "",
                category = data["category"] as? String ?: data["workType"] as? String ?: "",
                location = data["location"] as? String ?: "",
                date = data["date"] as? String ?: data["dateTime"] as? String ?: "",
                description = data["description"] as? String ?: "",
                customerId = data["customerId"] as? String ?: "",
                status = data["status"] as? String ?: "open",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                appliedWorkers = appliedList,
                assignedWorkerId = data["assignedWorkerId"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
}
