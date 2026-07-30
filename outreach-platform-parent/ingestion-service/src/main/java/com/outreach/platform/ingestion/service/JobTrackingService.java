package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.FileMetadataDocument;
import com.outreach.platform.ingestion.model.JobStatus;
import com.outreach.platform.ingestion.model.JobTrackingDocument;
import com.outreach.platform.ingestion.repo.FileMetadataRepository;
import com.outreach.platform.ingestion.repo.JobTrackingRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing import job lifecycle, progress tracking, and file metadata.
 */
@Named
public class JobTrackingService {

    private static final Logger log = LoggerFactory.getLogger(JobTrackingService.class);

    private final JobTrackingRepository jobTrackingRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Inject
    public JobTrackingService(JobTrackingRepository jobTrackingRepository,
                              FileMetadataRepository fileMetadataRepository) {
        this.jobTrackingRepository = jobTrackingRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    /**
     * Creates a new job tracking record with PENDING status and stores file metadata.
     */
    public JobTrackingDocument createJob(String jobId, String jobType, String fileName,
                                         String extension, long fileSizeBytes) {
        Instant now = Instant.now();

        JobTrackingDocument job = new JobTrackingDocument();
        job.setId(jobId);
        job.setJobType(jobType);
        job.setFileName(fileName);
        job.setStatus(JobStatus.PENDING);
        job.setProgress(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        jobTrackingRepository.save(job);
        log.info("Created job tracking record: jobId={}, type={}, file={}", jobId, jobType, fileName);

        FileMetadataDocument metadata = new FileMetadataDocument();
        metadata.setJobId(jobId);
        metadata.setOriginalFileName(fileName);
        metadata.setExtension(extension);
        metadata.setFileSizeBytes(fileSizeBytes);
        metadata.setStatus(JobStatus.PENDING);
        metadata.setUploadedAt(now);

        fileMetadataRepository.save(metadata);
        log.info("Stored file metadata for jobId={}", jobId);

        return job;
    }

    /**
     * Marks a job as RUNNING.
     */
    public void startJob(String jobId, int totalRows) {
        jobTrackingRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.RUNNING);
            job.setTotalRows(totalRows);
            job.setStartedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            jobTrackingRepository.save(job);
            log.info("Job started: jobId={}, totalRows={}", jobId, totalRows);
        });
    }

    /**
     * Updates job progress (processed rows and percentage).
     */
    public void updateProgress(String jobId, int processedRows, int totalRows) {
        jobTrackingRepository.findById(jobId).ifPresent(job -> {
            job.setProcessedRows(processedRows);
            job.setTotalRows(totalRows);
            int progress = totalRows > 0 ? (processedRows * 100) / totalRows : 0;
            job.setProgress(Math.min(progress, 100));
            job.setUpdatedAt(Instant.now());
            jobTrackingRepository.save(job);
        });
    }

    /**
     * Marks a job as COMPLETED.
     */
    public void completeJob(String jobId, int processedRows, int errorCount, List<String> errors) {
        jobTrackingRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setProcessedRows(processedRows);
            job.setErrorCount(errorCount);
            job.setErrors(errors);
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            jobTrackingRepository.save(job);
            log.info("Job completed: jobId={}, processed={}, errors={}", jobId, processedRows, errorCount);
        });
    }

    /**
     * Marks a job as FAILED.
     */
    public void failJob(String jobId, String errorMessage) {
        jobTrackingRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.getErrors().add(errorMessage);
            job.setErrorCount(job.getErrors().size());
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            jobTrackingRepository.save(job);
            log.error("Job failed: jobId={}, error={}", jobId, errorMessage);
        });
    }

    /**
     * Cancels a job if it is still PENDING or RUNNING.
     * @return true if cancelled, false if already in a terminal state
     */
    public boolean cancelJob(String jobId) {
        Optional<JobTrackingDocument> optJob = jobTrackingRepository.findById(jobId);
        if (optJob.isEmpty()) {
            return false;
        }
        JobTrackingDocument job = optJob.get();
        if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.RUNNING) {
            job.setStatus(JobStatus.CANCELLED);
            job.setCompletedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
            jobTrackingRepository.save(job);
            log.info("Job cancelled: jobId={}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Retrieves a job by its ID.
     */
    public Optional<JobTrackingDocument> getJob(String jobId) {
        return jobTrackingRepository.findById(jobId);
    }

    /**
     * Lists all jobs, paginated and sorted by creation date descending.
     */
    public Page<JobTrackingDocument> listJobs(Pageable pageable) {
        return jobTrackingRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
