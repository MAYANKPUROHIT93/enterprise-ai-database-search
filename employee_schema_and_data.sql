-- ============================================================================
-- Enterprise Employee Table Schema & Synthetic Data Seed Script
-- Compatible with: MySQL 8.0+, MariaDB, and standard SQL Dialects
-- ============================================================================

-- 1. Create Table: employee
CREATE TABLE IF NOT EXISTS employee (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'Unique organizational identifier e.g. EMP-1001',
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(25) NULL,
    department VARCHAR(50) NOT NULL,
    job_title VARCHAR(80) NOT NULL,
    salary DECIMAL(12, 2) NOT NULL COMMENT 'Annual base salary',
    hire_date DATE NOT NULL,
    employment_status ENUM('ACTIVE', 'ON_LEAVE', 'TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    manager_id BIGINT UNSIGNED NULL COMMENT 'Self-referencing manager ID',
    location VARCHAR(60) NOT NULL DEFAULT 'Bengaluru, India',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for query optimization
    INDEX idx_employee_department (department),
    INDEX idx_employee_status (employment_status),
    INDEX idx_employee_hire_date (hire_date),
    INDEX idx_employee_manager (manager_id),
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employee(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 2. Synthetic Dataset (Leadership & Management Hierarchy)
-- ============================================================================

-- Level 1: Executive / Directors (No manager)
INSERT INTO employee (
    id, employee_code, first_name, last_name, email, phone_number, 
    department, job_title, salary, hire_date, employment_status, manager_id, location
) VALUES
(1, 'EMP-1001', 'Aarav', 'Sharma', 'aarav.sharma@company.com', '+91 98765 43210', 'Engineering', 'VP of Engineering', 185000.00, '2019-03-15', 'ACTIVE', NULL, 'Bengaluru, India'),
(2, 'EMP-1002', 'Priya', 'Nair', 'priya.nair@company.com', '+91 98765 43211', 'Product', 'Head of Product', 165000.00, '2020-01-10', 'ACTIVE', NULL, 'Bengaluru, India'),
(3, 'EMP-1003', 'Vikram', 'Mehta', 'vikram.mehta@company.com', '+91 98765 43212', 'Sales', 'Chief Revenue Officer', 170000.00, '2019-08-01', 'ACTIVE', NULL, 'Mumbai, India'),
(4, 'EMP-1004', 'Ananya', 'Iyer', 'ananya.iyer@company.com', '+91 98765 43213', 'Human Resources', 'Chief People Officer', 145000.00, '2020-06-20', 'ACTIVE', NULL, 'Bengaluru, India')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Level 2: Engineering & Tech Staff (Reporting to Aarav Sharma - ID 1)
INSERT INTO employee (
    id, employee_code, first_name, last_name, email, phone_number, 
    department, job_title, salary, hire_date, employment_status, manager_id, location
) VALUES
(5, 'EMP-1005', 'Rohan', 'Verma', 'rohan.verma@company.com', '+91 98765 43214', 'Engineering', 'Principal Software Architect', 140000.00, '2020-09-01', 'ACTIVE', 1, 'Bengaluru, India'),
(6, 'EMP-1006', 'Sneha', 'Patel', 'sneha.patel@company.com', '+91 98765 43215', 'Engineering', 'Engineering Manager (Backend)', 130000.00, '2021-02-15', 'ACTIVE', 1, 'Pune, India'),
(7, 'EMP-1007', 'Karthik', 'Raman', 'karthik.raman@company.com', '+91 98765 43216', 'Engineering', 'Senior Full Stack Engineer', 95000.00, '2021-07-10', 'ACTIVE', 6, 'Bengaluru, India'),
(8, 'EMP-1008', 'Tanvi', 'Deshmukh', 'tanvi.deshmukh@company.com', '+91 98765 43217', 'Engineering', 'Senior Backend Engineer', 92000.00, '2022-01-18', 'ACTIVE', 6, 'Pune, India'),
(9, 'EMP-1009', 'Aditya', 'Chopra', 'aditya.chopra@company.com', '+91 98765 43218', 'Engineering', 'Frontend Engineer II', 78000.00, '2022-06-01', 'ACTIVE', 6, 'Bengaluru, India'),
(10, 'EMP-1010', 'Meera', 'Reddy', 'meera.reddy@company.com', '+91 98765 43219', 'Engineering', 'DevOps & Site Reliability Lead', 115000.00, '2021-04-12', 'ACTIVE', 5, 'Hyderabad, India'),
(11, 'EMP-1011', 'Devansh', 'Joshi', 'devansh.joshi@company.com', '+91 98765 43220', 'Engineering', 'Cloud Infrastructure Engineer', 82000.00, '2023-02-01', 'ACTIVE', 10, 'Hyderabad, India'),
(12, 'EMP-1012', 'Ishita', 'Saxena', 'ishita.saxena@company.com', '+91 98765 43221', 'Engineering', 'QA Automation Engineer', 68000.00, '2023-05-15', 'ACTIVE', 6, 'Bengaluru, India'),
(13, 'EMP-1013', 'Arjun', 'Gupta', 'arjun.gupta@company.com', '+91 98765 43222', 'Engineering', 'Junior Backend Engineer', 55000.00, '2024-01-10', 'ACTIVE', 6, 'Pune, India')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Level 3: Product, Design & Analytics (Reporting to Priya Nair - ID 2)
INSERT INTO employee (
    id, employee_code, first_name, last_name, email, phone_number, 
    department, job_title, salary, hire_date, employment_status, manager_id, location
) VALUES
(14, 'EMP-1014', 'Kabir', 'Malhotra', 'kabir.malhotra@company.com', '+91 98765 43223', 'Product', 'Senior Product Manager', 110000.00, '2021-05-10', 'ACTIVE', 2, 'Bengaluru, India'),
(15, 'EMP-1015', 'Diya', 'Sengupta', 'diya.sengupta@company.com', '+91 98765 43224', 'Product', 'Lead UI/UX Designer', 98000.00, '2021-11-01', 'ACTIVE', 2, 'Remote / Kolkata, India'),
(16, 'EMP-1016', 'Nikhil', 'Bansal', 'nikhil.bansal@company.com', '+91 98765 43225', 'Product', 'Product Data Analyst', 72000.00, '2022-09-15', 'ACTIVE', 14, 'Bengaluru, India'),
(17, 'EMP-1017', 'Rhea', 'Kulkarni', 'rhea.kulkarni@company.com', '+91 98765 43226', 'Product', 'Associate Product Manager', 62000.00, '2023-08-01', 'ON_LEAVE', 14, 'Bengaluru, India')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Level 4: Sales, Marketing & Account Management (Reporting to Vikram Mehta - ID 3)
INSERT INTO employee (
    id, employee_code, first_name, last_name, email, phone_number, 
    department, job_title, salary, hire_date, employment_status, manager_id, location
) VALUES
(18, 'EMP-1018', 'Siddharth', 'Bhatia', 'siddharth.bhatia@company.com', '+91 98765 43227', 'Sales', 'Enterprise Account Executive', 95000.00, '2021-03-22', 'ACTIVE', 3, 'Mumbai, India'),
(19, 'EMP-1019', 'Natasha', 'Kapoor', 'natasha.kapoor@company.com', '+91 98765 43228', 'Sales', 'Regional Sales Manager', 105000.00, '2020-10-05', 'ACTIVE', 3, 'Delhi NCR, India'),
(20, 'EMP-1020', 'Varun', 'Nambiar', 'varun.nambiar@company.com', '+91 98765 43229', 'Marketing', 'Digital Marketing Strategist', 75000.00, '2022-04-18', 'ACTIVE', 3, 'Bengaluru, India'),
(21, 'EMP-1021', 'Pooja', 'Chawla', 'pooja.chawla@company.com', '+91 98765 43230', 'Sales', 'Sales Development Representative', 48000.00, '2023-11-20', 'ACTIVE', 19, 'Delhi NCR, India'),
(22, 'EMP-1022', 'Gaurav', 'Das', 'gaurav.das@company.com', '+91 98765 43231', 'Sales', 'Customer Success Specialist', 58000.00, '2022-12-01', 'ACTIVE', 18, 'Mumbai, India')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Level 5: HR, Talent Acquisition & Finance (Reporting to Ananya Iyer - ID 4)
INSERT INTO employee (
    id, employee_code, first_name, last_name, email, phone_number, 
    department, job_title, salary, hire_date, employment_status, manager_id, location
) VALUES
(23, 'EMP-1023', 'Rajesh', 'Menon', 'rajesh.menon@company.com', '+91 98765 43232', 'Finance', 'Financial Controller', 118000.00, '2020-04-01', 'ACTIVE', 4, 'Bengaluru, India'),
(24, 'EMP-1024', 'Kavita', 'Soni', 'kavita.soni@company.com', '+91 98765 43233', 'Human Resources', 'Senior Talent Acquisition Specialist', 70000.00, '2022-03-10', 'ACTIVE', 4, 'Bengaluru, India'),
(25, 'EMP-1025', 'Harsh', 'Trivedi', 'harsh.trivedi@company.com', '+91 98765 43234', 'Finance', 'Senior Payroll Accountant', 64000.00, '2022-07-25', 'ACTIVE', 23, 'Bengaluru, India'),
(26, 'EMP-1026', 'Alia', 'D’Souza', 'alia.dsouza@company.com', '+91 98765 43235', 'Human Resources', 'HR Operations & People Partner', 56000.00, '2023-03-01', 'ON_LEAVE', 4, 'Bengaluru, India'),
(27, 'EMP-1027', 'Sameer', 'Khan', 'sameer.khan@company.com', '+91 98765 43236', 'Engineering', 'Software Engineer (Former)', 76000.00, '2021-08-15', 'TERMINATED', 6, 'Bengaluru, India')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Reset Auto-Increment to next value
ALTER TABLE employee AUTO_INCREMENT = 28;

-- ============================================================================
-- Verification Queries (Optional)
-- ============================================================================
-- SELECT department, COUNT(*) AS employee_count, AVG(salary) AS avg_salary FROM employee GROUP BY department;
-- SELECT e.id, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS employee_name, e.job_title, CONCAT(m.first_name, ' ', m.last_name) AS manager_name FROM employee e LEFT JOIN employee m ON e.manager_id = m.id;
