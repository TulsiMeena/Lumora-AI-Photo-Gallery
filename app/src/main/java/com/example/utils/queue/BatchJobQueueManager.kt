package com.example.utils.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID

enum class QueueJobStatus {
  QUEUED,
  PROCESSING,
  COMPLETED,
  FAILED,
  CANCELLED
}

enum class BatchQueueOperation {
  RESIZE,
  COMPRESS,
  FORMAT_CONVERT,
  WATERMARK,
  PDF_GENERATION,
  DOCUMENT_SCAN_OCR,
  AI_OFFLINE_INDEX
}

data class QueueJobItem(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val operation: BatchQueueOperation,
  val inputUri: String,
  val outputUri: String? = null,
  val status: QueueJobStatus = QueueJobStatus.QUEUED,
  val progress: Float = 0f, // 0.0f to 1.0f
  val errorMessage: String? = null,
  val createdMillis: Long = System.currentTimeMillis(),
  val completedMillis: Long? = null,
  val isRetryable: Boolean = true
)

/**
 * Controlled Concurrency Queue Manager for large media operations.
 * Dynamically limits concurrency based on processor cores to prevent OOM and UI freezing.
 */
object BatchJobQueueManager {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  // Hardware-aware concurrency: min 2, max 4 parallel tasks to protect memory
  private val maxParallelism = maxOf(2, minOf(Runtime.getRuntime().availableProcessors() - 1, 4))
  private val semaphore = Semaphore(maxParallelism)

  private val _jobs = MutableStateFlow<List<QueueJobItem>>(emptyList())
  val jobs: StateFlow<List<QueueJobItem>> = _jobs.asStateFlow()

  private val activeCoroutines = mutableMapOf<String, Job>()

  /**
   * Enqueues a new background media job.
   */
  fun enqueueJob(
    title: String,
    operation: BatchQueueOperation,
    inputUri: String,
    action: suspend (updateProgress: (Float) -> Unit) -> String?
  ): String {
    val jobItem = QueueJobItem(
      title = title,
      operation = operation,
      inputUri = inputUri
    )

    _jobs.update { listOf(jobItem) + it.take(49) } // Keep last 50 jobs

    val coroutineJob = scope.launch {
      semaphore.withPermit {
        updateJobStatus(jobItem.id, QueueJobStatus.PROCESSING, progress = 0.05f)
        try {
          val resultOutput = action { progressValue ->
            updateJobProgress(jobItem.id, progressValue)
          }

          _jobs.update { list ->
            list.map {
              if (it.id == jobItem.id) {
                it.copy(
                  status = QueueJobStatus.COMPLETED,
                  progress = 1.0f,
                  outputUri = resultOutput,
                  completedMillis = System.currentTimeMillis()
                )
              } else it
            }
          }
        } catch (e: Exception) {
          _jobs.update { list ->
            list.map {
              if (it.id == jobItem.id) {
                it.copy(
                  status = QueueJobStatus.FAILED,
                  errorMessage = e.localizedMessage ?: "Operation failed",
                  completedMillis = System.currentTimeMillis()
                )
              } else it
            }
          }
        } finally {
          activeCoroutines.remove(jobItem.id)
        }
      }
    }

    activeCoroutines[jobItem.id] = coroutineJob
    return jobItem.id
  }

  fun cancelJob(jobId: String) {
    activeCoroutines[jobId]?.cancel()
    activeCoroutines.remove(jobId)
    _jobs.update { list ->
      list.map {
        if (it.id == jobId && (it.status == QueueJobStatus.QUEUED || it.status == QueueJobStatus.PROCESSING)) {
          it.copy(status = QueueJobStatus.CANCELLED, completedMillis = System.currentTimeMillis())
        } else it
      }
    }
  }

  fun clearCompletedJobs() {
    _jobs.update { list ->
      list.filter { it.status == QueueJobStatus.PROCESSING || it.status == QueueJobStatus.QUEUED }
    }
  }

  private fun updateJobStatus(jobId: String, status: QueueJobStatus, progress: Float) {
    _jobs.update { list ->
      list.map { if (it.id == jobId) it.copy(status = status, progress = progress) else it }
    }
  }

  private fun updateJobProgress(jobId: String, progress: Float) {
    _jobs.update { list ->
      list.map { if (it.id == jobId) it.copy(progress = progress.coerceIn(0f, 1f)) else it }
    }
  }
}
