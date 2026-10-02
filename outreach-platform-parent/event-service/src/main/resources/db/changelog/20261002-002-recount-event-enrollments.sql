--liquibase formatted sql

--changeset event-service:20261002-002-recount-event-enrollments
--comment: Registered and attended counts were bumped by manual enrollment only, so events filled by
--         file import showed 0. They are now recomputed from event_enrollment; this fixes stored rows.
UPDATE events e SET
    registered_count = (SELECT count(*) FROM event_enrollment x WHERE x.event_id = e.id AND x.attendance_status <> 'UNREGISTERED'),
    attended_count = (SELECT count(*) FROM event_enrollment x WHERE x.event_id = e.id AND x.attendance_status = 'ATTENDED');
