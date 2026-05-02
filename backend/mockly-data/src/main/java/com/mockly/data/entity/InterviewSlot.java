package com.mockly.data.entity;

import com.mockly.data.enums.InterviewSlotStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_slots", indexes = {
        @Index(name = "idx_interview_slots_status_scheduled_at", columnList = "status, scheduled_at"),
        @Index(name = "idx_interview_slots_interviewer", columnList = "interviewer_id"),
        @Index(name = "idx_interview_slots_booked_by", columnList = "booked_by"),
        @Index(name = "idx_interview_slots_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(length = 150)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InterviewSlotStatus status = InterviewSlotStatus.OPEN;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", insertable = false, updatable = false)
    private User interviewer;

    @Column(name = "booked_by")
    private UUID bookedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by", insertable = false, updatable = false)
    private User bookedByUser;

    @Column(name = "session_id")
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private Session session;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
