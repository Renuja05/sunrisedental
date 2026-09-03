-- ============================================================
-- Sunrise Dental Clinic - Appointment and Patient Management System
-- MySQL schema
--
-- Setup:
--   1. Start MySQL (e.g. via XAMPP / WAMP / MySQL Workbench).
--   2. Run this whole script, e.g. from a terminal:
--        mysql -u root -p < schema.sql
--      or paste it into phpMyAdmin / MySQL Workbench and execute.
--   3. Update the URL / username / password in
--      src/com/sunrisedental/db/DatabaseConnection.java if they
--      differ from the defaults below (root / no password).
-- ============================================================

DROP DATABASE IF EXISTS sunrise_dental;
CREATE DATABASE sunrise_dental;
USE sunrise_dental;

-- ---------------------------------------------------------------
-- Users (Receptionist / Administrator accounts)
-- ---------------------------------------------------------------
CREATE TABLE users (
    user_id         VARCHAR(10)  PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(64)  NOT NULL,   -- SHA-256 hex digest
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('RECEPTIONIST', 'ADMINISTRATOR')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- Patients
-- ---------------------------------------------------------------
CREATE TABLE patients (
    patient_id      VARCHAR(10)  PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(200) NOT NULL,
    contact_number  VARCHAR(15)  NOT NULL,
    UNIQUE KEY uq_patient_contact (contact_number)
);

-- ---------------------------------------------------------------
-- Dentists
-- ---------------------------------------------------------------
CREATE TABLE dentists (
    dentist_id      VARCHAR(10)  PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    specialization  VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- Treatment types and their standard cost
-- ---------------------------------------------------------------
CREATE TABLE treatments (
    treatment_id    VARCHAR(10)   PRIMARY KEY,
    treatment_name  VARCHAR(100)  NOT NULL,
    cost            DECIMAL(10,2) NOT NULL
);

-- ---------------------------------------------------------------
-- Appointments
-- ---------------------------------------------------------------
CREATE TABLE appointments (
    appointment_number  VARCHAR(15)  PRIMARY KEY,
    patient_id          VARCHAR(10)  NOT NULL,
    dentist_id           VARCHAR(10)  NOT NULL,
    treatment_id           VARCHAR(10)  NOT NULL,
    appointment_date        DATE         NOT NULL,
    appointment_time         TIME         NOT NULL,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
                                 CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')),
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    FOREIGN KEY (dentist_id)   REFERENCES dentists(dentist_id),
    FOREIGN KEY (treatment_id) REFERENCES treatments(treatment_id),
    -- A dentist cannot be double-booked for the same date and time
    UNIQUE KEY uq_dentist_slot (dentist_id, appointment_date, appointment_time)
);

-- ---------------------------------------------------------------
-- Bills
-- ---------------------------------------------------------------
CREATE TABLE bills (
    bill_number         VARCHAR(15)   PRIMARY KEY,
    appointment_number  VARCHAR(15)   NOT NULL UNIQUE,
    consultation_fee    DECIMAL(10,2) NOT NULL,
    treatment_cost        DECIMAL(10,2) NOT NULL,
    discount               DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount             DECIMAL(10,2) NOT NULL,
    bill_date                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_number) REFERENCES appointments(appointment_number)
);

-- ============================================================
-- Seed data
-- ============================================================

-- Default accounts (password for both is: "password123")
-- SHA-256("password123") = ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94
INSERT INTO users (user_id, username, password_hash, full_name, role) VALUES
 ('U0001', 'admin',      'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94', 'System Administrator', 'ADMINISTRATOR'),
 ('U0002', 'reception1', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94', 'Nimali Perera',        'RECEPTIONIST');

INSERT INTO dentists (dentist_id, name, specialization) VALUES
 ('D0001', 'Dr. S. Fernando',  'General Dentistry'),
 ('D0002', 'Dr. K. Jayasuriya','Orthodontics'),
 ('D0003', 'Dr. R. Wickramasinghe', 'Oral Surgery');

INSERT INTO treatments (treatment_id, treatment_name, cost) VALUES
 ('T0001', 'Dental Check-up',        1500.00),
 ('T0002', 'Scaling and Polishing',  4000.00),
 ('T0003', 'Tooth Filling',          6000.00),
 ('T0004', 'Tooth Extraction',       5000.00),
 ('T0005', 'Root Canal Treatment',  25000.00),
 ('T0006', 'Braces Fitting',        45000.00);
