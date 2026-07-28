--liquibase formatted sql

--changeset outreach-platform:20240102-001-seed-users
--comment: Seed platform users (ADMIN, PMO, POC)
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "users" WHERE "username" = 'admin'
INSERT INTO "users" ("id", "username", "email_encrypted", "password_hash", "role", "enabled", "force_password_change", "created_by")
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'admin', 'admin@outreach-platform.com', '$2a$10$dummyhashforadmin000000000000000000000000000000000000', 'ADMIN', TRUE, FALSE, 'system'),
    ('a0000000-0000-0000-0000-000000000002', 'priya_sharma', 'priya.sharma@outreach-platform.com', '$2a$10$dummyhashforuser0000000000000000000000000000000000000', 'PMO', TRUE, FALSE, 'system'),
    ('a0000000-0000-0000-0000-000000000003', 'vikram_singh', 'vikram.singh@outreach-platform.com', '$2a$10$dummyhashforuser0000000000000000000000000000000000000', 'PMO', TRUE, FALSE, 'system'),
    ('a0000000-0000-0000-0000-000000000004', 'anita_desai', 'anita.desai@outreach-platform.com', '$2a$10$dummyhashforuser0000000000000000000000000000000000000', 'POC', TRUE, FALSE, 'system'),
    ('a0000000-0000-0000-0000-000000000005', 'rahul_verma', 'rahul.verma@outreach-platform.com', '$2a$10$dummyhashforuser0000000000000000000000000000000000000', 'POC', TRUE, FALSE, 'system');

--changeset outreach-platform:20240102-001-seed-events
--comment: Seed events across lifecycle states
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "events" WHERE "event_code" = 'EVT-2024-001'
INSERT INTO "events" ("id", "event_code", "event_name", "description", "status", "event_date", "event_end_date", "city", "venue", "category", "max_volunteers", "registered_count", "attended_count", "created_by")
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'EVT-2024-001', 'Digital Literacy Workshop', 'Teaching basic digital skills to underprivileged communities', 'DRAFT', '2024-06-15', '2024-06-16', 'Mumbai', 'Community Hall, Andheri West', 'Education', 30, 0, 0, 'admin'),
    ('b0000000-0000-0000-0000-000000000002', 'EVT-2024-002', 'Green Mumbai Tree Plantation', 'Mass tree plantation drive across suburban areas', 'DRAFT', '2024-07-01', '2024-07-01', 'Mumbai', 'Sanjay Gandhi National Park', 'Environment', 50, 0, 0, 'priya_sharma'),
    ('b0000000-0000-0000-0000-000000000003', 'EVT-2024-003', 'Blood Donation Camp - Bangalore', 'Quarterly corporate blood donation initiative', 'PUBLISHED', '2024-05-20', '2024-05-20', 'Bangalore', 'Infosys Campus, Electronic City', 'Health', 100, 45, 0, 'vikram_singh'),
    ('b0000000-0000-0000-0000-000000000004', 'EVT-2024-004', 'Rural School Library Setup', 'Setting up libraries in 5 rural schools near Pune', 'PUBLISHED', '2024-05-25', '2024-05-27', 'Pune', 'Multiple Locations, Mulshi Taluka', 'Education', 25, 18, 0, 'priya_sharma'),
    ('b0000000-0000-0000-0000-000000000005', 'EVT-2024-005', 'Women Empowerment Seminar', 'Financial literacy and career guidance for women', 'ACTIVE', '2024-04-10', '2024-04-12', 'Delhi', 'India Habitat Centre', 'Social', 40, 35, 28, 'anita_desai'),
    ('b0000000-0000-0000-0000-000000000006', 'EVT-2024-006', 'Coastal Cleanup Drive', 'Beach cleanup and marine awareness campaign', 'ACTIVE', '2024-04-15', '2024-04-15', 'Chennai', 'Marina Beach', 'Environment', 80, 72, 65, 'vikram_singh'),
    ('b0000000-0000-0000-0000-000000000007', 'EVT-2024-007', 'Tech for Good Hackathon', '48-hour hackathon building solutions for NGOs', 'ACTIVE', '2024-04-20', '2024-04-22', 'Hyderabad', 'T-Hub, IIIT Campus', 'Technology', 60, 55, 50, 'rahul_verma'),
    ('b0000000-0000-0000-0000-000000000008', 'EVT-2024-008', 'Old Age Home Visit', 'Spending time with elderly residents and organizing activities', 'COMPLETED', '2024-03-01', '2024-03-01', 'Kolkata', 'Missionaries of Charity Home', 'Social', 20, 18, 16, 'anita_desai'),
    ('b0000000-0000-0000-0000-000000000009', 'EVT-2024-009', 'Marathon for Education', '10K charity run supporting underprivileged students', 'COMPLETED', '2024-02-15', '2024-02-15', 'Pune', 'Shivaji Park', 'Health', 200, 180, 165, 'priya_sharma'),
    ('b0000000-0000-0000-0000-000000000010', 'EVT-2024-010', 'Diwali Gift Distribution', 'Distributing gifts and sweets to orphanages', 'ARCHIVED', '2023-11-10', '2023-11-12', 'Jaipur', 'Multiple Orphanages', 'Social', 35, 32, 30, 'admin');

--changeset outreach-platform:20240102-001-seed-volunteers
--comment: Seed volunteer profiles
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "volunteers" WHERE "employee_id" = 'EMP001'
INSERT INTO "volunteers" ("id", "employee_id", "full_name_encrypted", "email_encrypted", "phone_encrypted", "base_location", "department", "designation", "skills", "availability", "total_events_participated")
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'EMP001', 'Arun Kumar', 'arun.kumar@company.com', '9876543210', 'Mumbai', 'Engineering', 'Senior Developer', 'Java,Spring Boot,Mentoring', 'AVAILABLE', 8),
    ('c0000000-0000-0000-0000-000000000002', 'EMP002', 'Sneha Patel', 'sneha.patel@company.com', '9876543211', 'Mumbai', 'Design', 'UX Lead', 'UI Design,Workshop Facilitation,Photography', 'AVAILABLE', 5),
    ('c0000000-0000-0000-0000-000000000003', 'EMP003', 'Rajesh Iyer', 'rajesh.iyer@company.com', '9876543212', 'Bangalore', 'Engineering', 'Staff Engineer', 'Python,ML,Teaching', 'AVAILABLE', 12),
    ('c0000000-0000-0000-0000-000000000004', 'EMP004', 'Meera Nair', 'meera.nair@company.com', '9876543213', 'Bangalore', 'Product', 'Product Manager', 'Project Management,Communication,Event Planning', 'BUSY', 6),
    ('c0000000-0000-0000-0000-000000000005', 'EMP005', 'Sanjay Gupta', 'sanjay.gupta@company.com', '9876543214', 'Delhi', 'Sales', 'Regional Manager', 'Public Speaking,Fundraising,Networking', 'AVAILABLE', 10),
    ('c0000000-0000-0000-0000-000000000006', 'EMP006', 'Kavitha Reddy', 'kavitha.reddy@company.com', '9876543215', 'Hyderabad', 'HR', 'HR Business Partner', 'Training,Counseling,Event Coordination', 'AVAILABLE', 7),
    ('c0000000-0000-0000-0000-000000000007', 'EMP007', 'Amit Shah', 'amit.shah@company.com', '9876543216', 'Pune', 'Engineering', 'DevOps Engineer', 'Infrastructure,Cloud,First Aid', 'ON_LEAVE', 4),
    ('c0000000-0000-0000-0000-000000000008', 'EMP008', 'Lakshmi Venkat', 'lakshmi.venkat@company.com', '9876543217', 'Chennai', 'Finance', 'Senior Analyst', 'Financial Literacy,Data Analysis,Teaching', 'AVAILABLE', 9),
    ('c0000000-0000-0000-0000-000000000009', 'EMP009', 'Deepak Joshi', 'deepak.joshi@company.com', '9876543218', 'Kolkata', 'Marketing', 'Marketing Lead', 'Content Creation,Social Media,Photography', 'AVAILABLE', 3),
    ('c0000000-0000-0000-0000-000000000010', 'EMP010', 'Priyanka Das', 'priyanka.das@company.com', '9876543219', 'Kolkata', 'Engineering', 'QA Lead', 'Testing,Documentation,Teaching', 'AVAILABLE', 6),
    ('c0000000-0000-0000-0000-000000000011', 'EMP011', 'Vikash Tiwari', 'vikash.tiwari@company.com', '9876543220', 'Delhi', 'Legal', 'Legal Counsel', 'Legal Aid,Advocacy,Public Speaking', 'AVAILABLE', 5),
    ('c0000000-0000-0000-0000-000000000012', 'EMP012', 'Ananya Bose', 'ananya.bose@company.com', '9876543221', 'Pune', 'Engineering', 'Frontend Developer', 'React,Accessibility,Workshop Facilitation', 'AVAILABLE', 7),
    ('c0000000-0000-0000-0000-000000000013', 'EMP013', 'Ravi Shankar', 'ravi.shankar@company.com', '9876543222', 'Chennai', 'Operations', 'Operations Manager', 'Logistics,Event Management,First Aid', 'BUSY', 11),
    ('c0000000-0000-0000-0000-000000000014', 'EMP014', 'Sunita Rao', 'sunita.rao@company.com', '9876543223', 'Hyderabad', 'Data', 'Data Scientist', 'ML,Statistics,Teaching', 'AVAILABLE', 4),
    ('c0000000-0000-0000-0000-000000000015', 'EMP015', 'Manoj Pillai', 'manoj.pillai@company.com', '9876543224', 'Jaipur', 'Admin', 'Facilities Manager', 'Logistics,Coordination,Driving', 'AVAILABLE', 8);


--changeset outreach-platform:20240102-001-seed-enrollments
--comment: Seed event enrollment records linking volunteers to events
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "event_enrollment" WHERE "event_id" = 'b0000000-0000-0000-0000-000000000005' AND "volunteer_id" = 'c0000000-0000-0000-0000-000000000001'
INSERT INTO "event_enrollment" ("id", "event_id", "volunteer_id", "attendance_status", "email_status")
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000011', 'NOT_ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000003', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000008', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000013', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000001', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000006', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000012', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000014', 'REGISTERED', 'SENT'),
    ('d0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000009', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000010', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000004', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000016', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000002', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000017', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000005', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000018', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000007', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000019', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000012', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000020', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000015', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000021', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000006', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000022', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000011', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000023', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000013', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000024', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000015', 'ATTENDED', 'DELIVERED'),
    ('d0000000-0000-0000-0000-000000000025', 'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004', 'REGISTERED', 'SENT');


--changeset outreach-platform:20240102-001-seed-feedback
--comment: Seed volunteer feedback entries across events
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "volunteer_feedback" WHERE "event_id" = 'b0000000-0000-0000-0000-000000000005' AND "volunteer_id" = 'c0000000-0000-0000-0000-000000000001'
INSERT INTO "volunteer_feedback" ("id", "event_id", "volunteer_id", "score", "answer1", "answer2", "answer3", "category", "sentiment", "status", "anonymous", "submitted_at")
VALUES
    -- Women Empowerment Seminar (event 5)
    ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 5, 'Excellent speakers and well-organized sessions', 'Could have included more Q&A time', 'Would love to volunteer again', 'Communication', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-13 10:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 4, 'Great venue and logistics arrangement', 'Registration process was slow at start', NULL, 'Organization', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-13 11:15:00+05:30'),
    ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 4, 'Meaningful interactions with participants', 'Need better signage for directions', 'The panel discussion was highlight', 'Content', 'POSITIVE', 'SUBMITTED', FALSE, '2024-04-13 14:00:00+05:30'),
    -- Coastal Cleanup Drive (event 6)
    ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000003', 5, 'Amazing team spirit and visible impact', 'More water stations needed along the beach', NULL, 'Logistics', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-16 09:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000008', 3, 'Good awareness campaign material', 'Gloves ran out within first hour', 'Safety gear should be pre-distributed', 'Logistics', 'NEUTRAL', 'SUBMITTED', FALSE, '2024-04-16 09:45:00+05:30'),
    ('e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000013', 4, 'Well-coordinated effort with local authorities', 'Start time was too early for some', NULL, 'Organization', 'POSITIVE', 'REVIEWED', TRUE, '2024-04-16 10:30:00+05:30'),
    -- Tech for Good Hackathon (event 7)
    ('e0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000001', 5, 'Incredible learning experience and networking', 'WiFi was intermittent on day 2', 'Built a donation tracking app for an NGO', 'Content', 'POSITIVE', 'SUBMITTED', FALSE, '2024-04-23 16:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003', 4, 'Great mentors and problem statements', 'Food quality could be improved', NULL, 'Overall', 'POSITIVE', 'SUBMITTED', FALSE, '2024-04-23 16:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000006', 3, 'Good concept but rushed timeline', 'Need more diverse problem statements', 'Would prefer 72 hours next time', 'Content', 'NEUTRAL', 'FLAGGED', FALSE, '2024-04-23 17:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000012', 5, 'Perfect mix of tech and social impact', 'Presentation time limit was too short', NULL, 'Overall', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-23 17:30:00+05:30'),
    -- Old Age Home Visit (event 8)
    ('e0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000009', 5, 'Heartwarming experience with the residents', 'Need more structured activities planned', 'The music session was a big hit', 'Content', 'POSITIVE', 'REVIEWED', FALSE, '2024-03-02 14:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000010', 4, 'Well-organized transport and schedule', 'Could bring more interactive games', NULL, 'Organization', 'POSITIVE', 'REVIEWED', FALSE, '2024-03-02 15:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000004', 4, 'Great coordination with the care home staff', 'Visit duration was too short', 'Residents really appreciated the company', 'Communication', 'POSITIVE', 'SUBMITTED', TRUE, '2024-03-02 15:30:00+05:30'),
    -- Marathon for Education (event 9)
    ('e0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000002', 3, 'Good cause and route planning', 'Medal distribution was chaotic', 'Hydration stations were well spaced', 'Logistics', 'NEUTRAL', 'REVIEWED', FALSE, '2024-02-16 08:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000005', 5, 'Fantastic turnout and great community spirit', 'Parking was a nightmare', NULL, 'Overall', 'POSITIVE', 'REVIEWED', FALSE, '2024-02-16 09:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000016', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000007', 2, 'The cause was worthy', 'Poor route marking led to confusion', 'Some runners went off course', 'Logistics', 'NEGATIVE', 'FLAGGED', FALSE, '2024-02-16 09:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000017', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000012', 4, 'Energetic atmosphere and good support', 'Need more first aid stations', NULL, 'Organization', 'POSITIVE', 'SUBMITTED', FALSE, '2024-02-16 10:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000018', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000015', 4, 'Smooth registration and bib collection', 'Start was delayed by 30 minutes', 'Great initiative for education', 'Organization', 'POSITIVE', 'REVIEWED', TRUE, '2024-02-16 10:30:00+05:30'),
    -- Diwali Gift Distribution (event 10)
    ('e0000000-0000-0000-0000-000000000019', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000006', 5, 'Seeing children smile made it all worth it', 'Gift packaging could be more festive', NULL, 'Overall', 'POSITIVE', 'ARCHIVED', FALSE, '2023-11-13 11:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000020', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000011', 4, 'Great team coordination across orphanages', 'Transport between locations was tight', 'Kids loved the interactive games', 'Logistics', 'POSITIVE', 'ARCHIVED', FALSE, '2023-11-13 11:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000021', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000013', 5, 'Perfectly planned and executed', 'None - everything was great', NULL, 'Overall', 'POSITIVE', 'ARCHIVED', FALSE, '2023-11-13 12:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000022', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000015', 3, 'Good intent and decent execution', 'Some gifts were age-inappropriate', 'Need better age group analysis', 'Content', 'NEUTRAL', 'ARCHIVED', FALSE, '2023-11-13 12:30:00+05:30'),
    -- Additional feedback for active events
    ('e0000000-0000-0000-0000-000000000023', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000011', 2, 'Content was not relevant to the audience', 'Speakers were not engaging', 'Would not recommend', 'Content', 'NEGATIVE', 'FLAGGED', FALSE, '2024-04-13 16:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000024', 'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004', 4, 'Great community engagement and support', 'Registration queue was too long initially', NULL, 'Overall', 'POSITIVE', 'SUBMITTED', FALSE, '2024-05-21 11:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000025', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000014', 3, 'Interesting problems to solve', 'Judging criteria was unclear', 'Need clearer evaluation rubric', 'Communication', 'NEUTRAL', 'SUBMITTED', TRUE, '2024-04-23 18:00:00+05:30');


--changeset outreach-platform:20240102-001-seed-notification-templates
--comment: Seed notification templates for platform communications
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "notification_templates" WHERE "name" = 'Event Registration Confirmation'
INSERT INTO "notification_templates" ("id", "name", "type", "subject_template", "body_template", "engine", "variables_schema", "active", "created_by")
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'Event Registration Confirmation', 'EMAIL', 'You are registered for {{eventName}}', '<p>Hi {{volunteerName}},</p><p>You have been successfully registered for <strong>{{eventName}}</strong> on {{eventDate}} at {{venue}}, {{city}}.</p><p>Please mark your calendar and reach the venue 15 minutes before the start time.</p><p>Thank you for volunteering!</p>', 'THYMELEAF', '{"type":"object","properties":{"volunteerName":{"type":"string"},"eventName":{"type":"string"},"eventDate":{"type":"string"},"venue":{"type":"string"},"city":{"type":"string"}}}', TRUE, 'admin'),
    ('f0000000-0000-0000-0000-000000000002', 'Event Reminder', 'EMAIL', 'Reminder: {{eventName}} is tomorrow', '<p>Hi {{volunteerName}},</p><p>This is a friendly reminder that <strong>{{eventName}}</strong> is scheduled for tomorrow ({{eventDate}}).</p><p><strong>Venue:</strong> {{venue}}, {{city}}</p><p><strong>Time:</strong> {{startTime}}</p><p>Looking forward to seeing you there!</p>', 'THYMELEAF', '{"type":"object","properties":{"volunteerName":{"type":"string"},"eventName":{"type":"string"},"eventDate":{"type":"string"},"venue":{"type":"string"},"city":{"type":"string"},"startTime":{"type":"string"}}}', TRUE, 'admin'),
    ('f0000000-0000-0000-0000-000000000003', 'Feedback Request', 'EMAIL', 'Share your feedback for {{eventName}}', '<p>Hi {{volunteerName}},</p><p>Thank you for participating in <strong>{{eventName}}</strong>! Your feedback helps us improve future events.</p><p>Please take 2 minutes to share your experience:</p><p><a href="{{feedbackLink}}">Submit Feedback</a></p><p>Your response can be anonymous if you prefer.</p>', 'THYMELEAF', '{"type":"object","properties":{"volunteerName":{"type":"string"},"eventName":{"type":"string"},"feedbackLink":{"type":"string"}}}', TRUE, 'admin'),
    ('f0000000-0000-0000-0000-000000000004', 'Event Completion Summary', 'EMAIL', '{{eventName}} - Event Completed Successfully', '<p>Hi {{recipientName}},</p><p>The event <strong>{{eventName}}</strong> has been successfully completed.</p><p><strong>Attendance:</strong> {{attendedCount}}/{{registeredCount}} volunteers attended</p><p><strong>Feedback received:</strong> {{feedbackCount}} responses</p><p>Detailed reports are available on the platform dashboard.</p>', 'THYMELEAF', '{"type":"object","properties":{"recipientName":{"type":"string"},"eventName":{"type":"string"},"attendedCount":{"type":"integer"},"registeredCount":{"type":"integer"},"feedbackCount":{"type":"integer"}}}', TRUE, 'admin'),
    ('f0000000-0000-0000-0000-000000000005', 'Welcome to Outreach Platform', 'EMAIL', 'Welcome to the Outreach Volunteer Platform', '<p>Hi {{volunteerName}},</p><p>Welcome to the Outreach Volunteer Platform! Your profile has been set up successfully.</p><p><strong>Employee ID:</strong> {{employeeId}}</p><p><strong>Department:</strong> {{department}}</p><p>You can now browse upcoming events and register as a volunteer. Visit the platform to explore opportunities that match your skills.</p><p>Happy volunteering!</p>', 'THYMELEAF', '{"type":"object","properties":{"volunteerName":{"type":"string"},"employeeId":{"type":"string"},"department":{"type":"string"}}}', TRUE, 'admin');


--changeset outreach-platform:20240102-001-seed-feedback-additional
--comment: Additional volunteer feedback entries
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM "volunteer_feedback" WHERE "id" = 'e0000000-0000-0000-0000-000000000026'
INSERT INTO "volunteer_feedback" ("id", "event_id", "volunteer_id", "score", "answer1", "answer2", "answer3", "category", "sentiment", "status", "anonymous", "submitted_at")
VALUES
    -- More feedback for Marathon for Education (event 9)
    ('e0000000-0000-0000-0000-000000000026', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000001', 5, 'Great energy and purpose-driven run', 'More shade along the route needed', NULL, 'Overall', 'POSITIVE', 'REVIEWED', FALSE, '2024-02-16 11:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000027', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000003', 4, 'Well-organized start and finish', 'Timing chip collection was confusing', 'Great cause for education', 'Logistics', 'POSITIVE', 'SUBMITTED', FALSE, '2024-02-16 11:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000028', 'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000006', 3, 'Decent organization overall', 'Start was delayed significantly', NULL, 'Organization', 'NEUTRAL', 'REVIEWED', FALSE, '2024-02-16 12:00:00+05:30'),
    -- More feedback for Old Age Home Visit (event 8)
    ('e0000000-0000-0000-0000-000000000029', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000001', 5, 'Touching experience spending time with elders', 'Would love longer visits', 'Planning to volunteer regularly', 'Content', 'POSITIVE', 'REVIEWED', FALSE, '2024-03-02 16:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000030', 'b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000015', 4, 'Great coordination with care home staff', 'Bring more board games next time', NULL, 'Organization', 'POSITIVE', 'SUBMITTED', TRUE, '2024-03-02 16:30:00+05:30'),
    -- More feedback for Diwali Gift Distribution (event 10)
    ('e0000000-0000-0000-0000-000000000031', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000001', 5, 'Most heartwarming event of the year', 'Need more volunteers for each location', NULL, 'Overall', 'POSITIVE', 'ARCHIVED', FALSE, '2023-11-13 13:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000032', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000003', 4, 'Well-planned multi-location event', 'Some gifts were duplicated across locations', 'Kids were so happy to receive them', 'Logistics', 'POSITIVE', 'ARCHIVED', FALSE, '2023-11-13 13:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000033', 'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000008', 3, 'Good effort but needed better planning', 'Time allocation per orphanage was too short', NULL, 'Organization', 'NEUTRAL', 'ARCHIVED', FALSE, '2023-11-13 14:00:00+05:30'),
    -- Feedback for Women Empowerment Seminar (event 5) - more volunteers
    ('e0000000-0000-0000-0000-000000000034', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000008', 4, 'Inspiring speakers and good networking', 'Room temperature was uncomfortable', 'Would bring my team next year', 'Content', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-13 17:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000035', 'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000014', 5, 'Data-driven presentations were exceptional', 'Need more breakout session options', NULL, 'Content', 'POSITIVE', 'SUBMITTED', FALSE, '2024-04-13 17:30:00+05:30'),
    -- Feedback for Coastal Cleanup (event 6) - more volunteers
    ('e0000000-0000-0000-0000-000000000036', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000001', 4, 'Visible impact on the beach cleanliness', 'Need better sunscreen availability', 'Marine life awareness segment was great', 'Content', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-16 12:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000037', 'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000010', 2, 'Good cause but poor execution', 'Waste bags ran out early and no backup plan', 'Safety protocols were inadequate', 'Logistics', 'NEGATIVE', 'FLAGGED', FALSE, '2024-04-16 12:30:00+05:30'),
    -- Feedback for Tech for Good Hackathon (event 7) - more
    ('e0000000-0000-0000-0000-000000000038', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000008', 4, 'Loved collaborating with diverse teams', 'Power outlets were insufficient', NULL, 'Logistics', 'POSITIVE', 'SUBMITTED', FALSE, '2024-04-23 18:30:00+05:30'),
    ('e0000000-0000-0000-0000-000000000039', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005', 5, 'Best hackathon experience ever', 'More snack variety needed', 'Built a volunteer matching algorithm', 'Overall', 'POSITIVE', 'REVIEWED', FALSE, '2024-04-23 19:00:00+05:30'),
    ('e0000000-0000-0000-0000-000000000040', 'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000015', 3, 'Interesting concept and good mentors', 'Network connectivity issues throughout', 'Infrastructure needs improvement', 'Communication', 'NEUTRAL', 'SUBMITTED', TRUE, '2024-04-23 19:30:00+05:30');
