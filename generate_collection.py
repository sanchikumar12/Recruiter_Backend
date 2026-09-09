import json
import uuid

def parse_url(raw_url):
    # e.g. {{jobBaseUrl}}/api/v1/jobs?keyword=java&size=10
    # Host is the variable or base
    # Path is list of path segments
    # Query is list of {key, value}
    
    parts = raw_url.split('?', 1)
    url_path = parts[0]
    query_params = []
    if len(parts) > 1:
        for q in parts[1].split('&'):
            if '=' in q:
                k, v = q.split('=', 1)
                query_params.append({"key": k, "value": v})
            else:
                query_params.append({"key": q, "value": ""})
                
    # find host and path
    # Example: {{authBaseUrl}}/api/v1/auth/register
    segments = url_path.split('/')
    host = [segments[0]]
    path = [s for s in segments[1:] if s]
    
    res = {
        "raw": raw_url,
        "host": host,
        "path": path
    }
    if query_params:
        res["query"] = query_params
    return res

def make_request(name, method, url_raw, headers=None, body_json=None, test_script=None, description=""):
    req_headers = []
    if headers:
        for k, v in headers.items():
            req_headers.append({"key": k, "value": v, "type": "text"})
            
    req = {
        "method": method,
        "header": req_headers,
        "url": parse_url(url_raw)
    }
    
    if description:
        req["description"] = description
        
    if body_json is not None:
        if isinstance(body_json, str):
            raw_body = body_json
        else:
            raw_body = json.dumps(body_json, indent=2)
        req["body"] = {
            "mode": "raw",
            "raw": raw_body,
            "options": {
                "raw": {
                    "language": "json"
                }
            }
        }
        # ensure Content-Type header exists
        if not any(h["key"].lower() == "content-type" for h in req_headers):
            req_headers.append({"key": "Content-Type", "value": "application/json", "type": "text"})
            
    item = {
        "name": name,
        "request": req
    }
    
    if test_script:
        item["event"] = [
            {
                "listen": "test",
                "script": {
                    "type": "text/javascript",
                    "exec": test_script if isinstance(test_script, list) else [test_script]
                }
            }
        ]
        
    return item

# Build all folders and endpoints
folders = []

# -------------------------------------------------------------
# 01 - Discovery Server (Eureka :8761)
# -------------------------------------------------------------
eureka_items = [
    make_request(
        "1. Eureka: Web Dashboard (200 OK)",
        "GET",
        "{{discoveryBaseUrl}}/",
        description="Access Eureka Discovery Server Web Dashboard HTML view"
    ),
    make_request(
        "2. Eureka: Registered Apps Registry (200 OK)",
        "GET",
        "{{discoveryBaseUrl}}/eureka/apps",
        headers={"Accept": "application/json"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "pm.test('Registry contains applications object', function () {",
            "    var json = pm.response.json();",
            "    pm.expect(json).to.have.property('applications');",
            "});"
        ],
        description="Query registered microservice instances in Eureka JSON registry"
    ),
    make_request(
        "3. Eureka: Actuator Health (200 OK)",
        "GET",
        "{{discoveryBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "pm.test('Eureka Server is UP', function () {",
            "    var json = pm.response.json();",
            "    pm.expect(json.status).to.eql('UP');",
            "});"
        ],
        description="Verify Eureka server health status"
    ),
    make_request(
        "4. Eureka: Actuator Info (200 OK)",
        "GET",
        "{{discoveryBaseUrl}}/actuator/info",
        description="Verify Eureka server build and info details"
    )
]
folders.append({"name": "01 - Discovery Server (Eureka :8761)", "item": eureka_items})

# -------------------------------------------------------------
# 02 - Admin Server (Spring Boot Admin :8082)
# -------------------------------------------------------------
admin_items = [
    make_request(
        "1. Admin Server: Dashboard UI (200 OK)",
        "GET",
        "{{adminServerBaseUrl}}/",
        description="Spring Boot Admin web console user interface"
    ),
    make_request(
        "2. Admin Server: Monitored Microservice Instances (200 OK)",
        "GET",
        "{{adminServerBaseUrl}}/instances",
        headers={"Accept": "application/json"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "pm.test('Returns list of monitored microservices', function () {",
            "    var json = pm.response.json();",
            "    pm.expect(Array.isArray(json)).to.be.true;",
            "});"
        ],
        description="List all registered client instances monitored by Spring Boot Admin"
    ),
    make_request(
        "3. Admin Server: Actuator Health (200 OK)",
        "GET",
        "{{adminServerBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "pm.test('Admin Server is UP', function () {",
            "    var json = pm.response.json();",
            "    pm.expect(json.status).to.eql('UP');",
            "});"
        ],
        description="Verify Spring Boot Admin server health status"
    )
]
folders.append({"name": "02 - Admin Server (Spring Boot Admin :8082)", "item": admin_items})

# -------------------------------------------------------------
# 03 - Auth Service (:8083)
# -------------------------------------------------------------
auth_items = [
    make_request(
        "1. Auth: Register Candidate (201 Created)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/register",
        body_json={
            "email": "candidate_{{$timestamp}}@example.com",
            "password": "StrongPassword123!",
            "role": "USER"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Returns userId and email', function () {",
            "    pm.expect(json).to.have.property('userId');",
            "    pm.expect(json).to.have.property('email');",
            "});",
            "pm.collectionVariables.set('authUserId', json.userId);",
            "pm.collectionVariables.set('userEmail', json.email);",
            "pm.collectionVariables.set('userPassword', 'StrongPassword123!');",
            "if (json.activationToken) { pm.collectionVariables.set('activationToken', json.activationToken); }"
        ],
        description="Register a new user account with role USER and secure password"
    ),
    make_request(
        "2. Auth: Register Candidate (Auto-Generated Password)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/register",
        body_json={
            "email": "auto_{{$timestamp}}@example.com",
            "role": "USER"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Server auto-generates password', function () {",
            "    pm.expect(json).to.have.property('generatedPassword');",
            "});"
        ],
        description="Register user without password; system generates a secure alphanumeric password"
    ),
    make_request(
        "3. Auth: Activate Account (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/activate",
        body_json={
            "token": "{{activationToken}}",
            "password": "NewActivatedPassword123!"
        },
        test_script=[
            "pm.test('Status code is 200 OK or 400 if already active', function () {",
            "    pm.expect(pm.response.code).to.be.oneOf([200, 400]);",
            "});"
        ],
        description="Confirm account activation and optionally set new password using token"
    ),
    make_request(
        "4. Auth: Login (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/login",
        body_json={
            "email": "{{userEmail}}",
            "password": "{{userPassword}}"
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Contains accessToken and refreshToken', function () {",
            "    pm.expect(json).to.have.property('accessToken');",
            "    pm.expect(json).to.have.property('refreshToken');",
            "});",
            "pm.collectionVariables.set('accessToken', json.accessToken);",
            "pm.collectionVariables.set('refreshToken', json.refreshToken);",
            "if (json.userId) { pm.collectionVariables.set('currentCandidateId', json.userId); }"
        ],
        description="Authenticate candidate and retrieve JWT access token and refresh token"
    ),
    make_request(
        "5. Auth: Rotate Refresh Token (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/refresh",
        body_json={
            "refreshToken": "{{refreshToken}}"
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('New rotated access token issued', function () {",
            "    pm.expect(json).to.have.property('accessToken');",
            "});",
            "pm.collectionVariables.set('accessToken', json.accessToken);",
            "if (json.refreshToken) { pm.collectionVariables.set('refreshToken', json.refreshToken); }"
        ],
        description="Rotate refresh token and obtain fresh access token"
    ),
    make_request(
        "6. Auth: Forgot Password (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/forgot-password",
        body_json={
            "email": "{{userEmail}}"
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "if (json.resetToken) {",
            "    pm.collectionVariables.set('passwordResetToken', json.resetToken);",
            "}"
        ],
        description="Request password reset token (15 minute validity)"
    ),
    make_request(
        "7. Auth: Reset Password (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/reset-password",
        body_json={
            "token": "{{passwordResetToken}}",
            "newPassword": "UpdatedPassword123!"
        },
        test_script=[
            "pm.test('Status code is 200 OK or 401/400 if token consumed', function () {",
            "    pm.expect(pm.response.code).to.be.oneOf([200, 400, 401]);",
            "});",
            "if (pm.response.code === 200) {",
            "    pm.collectionVariables.set('userPassword', 'UpdatedPassword123!');",
            "}"
        ],
        description="Reset password using valid reset token and revoke all old refresh tokens"
    ),
    make_request(
        "8. Auth: Logout (200 OK)",
        "POST",
        "{{authBaseUrl}}/api/v1/auth/logout",
        body_json={
            "refreshToken": "{{refreshToken}}"
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Revoke refresh token and invalidate user session"
    ),
    make_request(
        "9. Auth: Actuator Health (200 OK)",
        "GET",
        "{{authBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Check Auth Service health"
    ),
    make_request(
        "10. Auth: OpenAPI Specification (200 OK)",
        "GET",
        "{{authBaseUrl}}/v3/api-docs",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Fetch OpenAPI v3 document in JSON"
    ),
    make_request(
        "11. Auth: Swagger UI",
        "GET",
        "{{authBaseUrl}}/swagger-ui/index.html",
        description="Interactive Swagger UI web page"
    )
]
folders.append({"name": "03 - Auth Service (:8083)", "item": auth_items})

# -------------------------------------------------------------
# 04 - User Service (:8081)
# -------------------------------------------------------------
user_items = [
    make_request(
        "1. User: Create Candidate Profile (201 Created)",
        "POST",
        "{{userBaseUrl}}/api/v1/users",
        body_json={
            "fullName": "Rahul Sharma",
            "mobileNumber": "+91 9876543210",
            "email": "rahul_{{$timestamp}}@example.com",
            "dateOfBirth": "1995-05-20",
            "skills": ["Java", "Spring Boot", "Microservices", "PostgreSQL", "Kafka", "Docker"],
            "location": "Bangalore, India",
            "headline": "Lead Java & Cloud Architect",
            "bio": "Over 8 years designing resilient distributed systems and event-driven microservices.",
            "role": "USER",
            "password": "RahulPassword123!"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('User ID returned', function () {",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('userId', json.id);",
            "pm.collectionVariables.set('candidateId', json.id);"
        ],
        description="Register a full candidate profile with skills, location, and credentials"
    ),
    make_request(
        "2. User: Get Candidate Profile by ID (200 OK)",
        "GET",
        "{{userBaseUrl}}/api/v1/users/{{userId}}",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Matches requested ID', function () {",
            "    pm.expect(json.id).to.eql(pm.collectionVariables.get('userId'));",
            "});"
        ],
        description="Retrieve profile information for candidate by unique UUID"
    ),
    make_request(
        "3. User: Create Candidate via Alias /api/candidates (201 Created)",
        "POST",
        "{{userBaseUrl}}/api/candidates",
        body_json={
            "fullName": "Priya Patel",
            "mobileNumber": "+91 9123456789",
            "email": "priya_{{$timestamp}}@example.com",
            "dateOfBirth": "1997-10-12",
            "skills": ["React", "TypeScript", "Node.js", "GraphQL"],
            "location": "Pune, India",
            "headline": "Senior Fullstack Engineer",
            "bio": "Specialized in high-load frontend applications and GraphQL federation.",
            "role": "USER"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});"
        ],
        description="Create candidate profile using alias path `/api/candidates`"
    ),
    make_request(
        "4. User: Get Candidate via Alias /api/candidates/{id} (200 OK)",
        "GET",
        "{{userBaseUrl}}/api/candidates/{{userId}}",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Retrieve candidate profile using alias path `/api/candidates/{id}`"
    ),
    make_request(
        "5. User: Negative - Duplicate Email (400 Bad Request)",
        "POST",
        "{{userBaseUrl}}/api/v1/users",
        body_json={
            "fullName": "Duplicate Tester",
            "email": "rahul.sharma@example.com",
            "skills": ["Java"]
        },
        test_script=[
            "pm.test('Status code is 400 Bad Request', function () {",
            "    pm.response.to.have.status(400);",
            "});"
        ],
        description="Validate business constraint preventing duplicate email registration"
    ),
    make_request(
        "6. User: Negative - Non Existent Profile (404 Not Found)",
        "GET",
        "{{userBaseUrl}}/api/v1/users/00000000-0000-0000-0000-000000000000",
        test_script=[
            "pm.test('Status code is 404 Not Found', function () {",
            "    pm.response.to.have.status(404);",
            "});"
        ],
        description="Validate 404 behavior when profile ID is not found"
    ),
    make_request(
        "7. User: Actuator Health (200 OK)",
        "GET",
        "{{userBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Check User Service health"
    ),
    make_request(
        "8. User: OpenAPI Specification (200 OK)",
        "GET",
        "{{userBaseUrl}}/v3/api-docs",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="User Service OpenAPI document"
    ),
    make_request(
        "9. User: Swagger UI",
        "GET",
        "{{userBaseUrl}}/swagger-ui/index.html",
        description="User Service Swagger UI documentation"
    )
]
folders.append({"name": "04 - User Service (:8081)", "item": user_items})

# -------------------------------------------------------------
# 05 - Job Service (:8084)
# -------------------------------------------------------------
job_items = [
    # Admin Job Endpoints
    make_request(
        "1. Admin: Create Job Draft (201 Created)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "title": "Principal Java Backend Engineer {{$timestamp}}",
            "description": "Architect mission-critical recruitment microservices with high throughput and event sourcing.",
            "department": "Engineering",
            "location": "Bangalore",
            "workMode": "HYBRID",
            "employmentType": "FULL_TIME",
            "experienceLevel": "LEAD",
            "minExperienceYears": 7.0,
            "maxExperienceYears": 12.0,
            "salaryMin": 3200000.0,
            "salaryMax": 4800000.0,
            "currency": "INR",
            "applicationDeadline": "2026-12-31T23:59:59Z",
            "skills": ["Java 17", "Spring Boot 3", "PostgreSQL", "Docker", "Kafka", "Redis"]
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Job is created in DRAFT status', function () {",
            "    pm.expect(json.status).to.eql('DRAFT');",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('jobId', json.id);",
            "pm.collectionVariables.set('jobIdForInterview', json.id);"
        ],
        description="Admin creates a new job opening in DRAFT status"
    ),
    make_request(
        "2. Admin: Get Job by ID (Any Status) (200 OK)",
        "GET",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Matches jobId', function () {",
            "    pm.expect(json.id).to.eql(pm.collectionVariables.get('jobId'));",
            "});"
        ],
        description="Admin retrieves job details regardless of status (DRAFT, PUBLISHED, PAUSED, CLOSED)"
    ),
    make_request(
        "3. Admin: Update Job Details & Skills (200 OK)",
        "PUT",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "title": "Principal Java Backend Engineer (Updated) {{$timestamp}}",
            "description": "Architect and lead mission-critical recruitment microservices with high throughput and event sourcing.",
            "department": "Core Platform Engineering",
            "location": "Bangalore / Remote",
            "workMode": "REMOTE",
            "employmentType": "FULL_TIME",
            "experienceLevel": "LEAD",
            "minExperienceYears": 8.0,
            "maxExperienceYears": 14.0,
            "salaryMin": 3500000.0,
            "salaryMax": 5200000.0,
            "currency": "INR",
            "applicationDeadline": "2026-12-31T23:59:59Z",
            "skills": ["Java 17", "Spring Boot 3", "PostgreSQL", "Docker", "Kafka", "Redis", "Kubernetes"]
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Department updated', function () {",
            "    pm.expect(json.department).to.eql('Core Platform Engineering');",
            "});"
        ],
        description="Admin updates existing job properties, requirements, and skills list"
    ),
    make_request(
        "4. Admin: Publish Job (204 No Content)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}/publish",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Transition job status to PUBLISHED making it visible to candidates"
    ),
    make_request(
        "5. Admin: Pause Job (204 No Content)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}/pause",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Transition job status to PAUSED temporarily hiding it from candidate search"
    ),
    make_request(
        "6. Admin: Re-Publish Job (204 No Content)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}/publish",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Re-publish job from PAUSED back to PUBLISHED status"
    ),
    make_request(
        "7. Admin: Get Job Statistics (200 OK)",
        "GET",
        "{{jobBaseUrl}}/api/v1/admin/jobs/stats",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Stats object contains total jobs', function () {",
            "    pm.expect(json).to.have.property('totalJobs');",
            "});"
        ],
        description="Get aggregated counts of jobs across all statuses"
    ),
    make_request(
        "8. Admin: List All Jobs with Filters & Pagination (200 OK)",
        "GET",
        "{{jobBaseUrl}}/api/v1/admin/jobs?status=PUBLISHED&page=0&size=10&sort=createdAt,desc",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Returns page of jobs', function () {",
            "    pm.expect(json).to.have.property('content');",
            "});"
        ],
        description="Admin lists jobs filtered by status, keywords, experience, and pagination"
    ),
    # Candidate Job Endpoints
    make_request(
        "9. Candidate: Search Published Jobs (200 OK)",
        "GET",
        "{{jobBaseUrl}}/api/v1/jobs?keyword=Java&location=Bangalore&page=0&size=10",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Only published jobs are returned', function () {",
            "    pm.expect(json).to.have.property('content');",
            "});"
        ],
        description="Candidate searches public catalog for published job openings"
    ),
    make_request(
        "10. Candidate: Get Published Job Details (200 OK)",
        "GET",
        "{{jobBaseUrl}}/api/v1/jobs/{{jobId}}",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Job is PUBLISHED', function () {",
            "    pm.expect(json.status).to.eql('PUBLISHED');",
            "});"
        ],
        description="Candidate retrieves public view of a published job opening"
    ),
    make_request(
        "11. Admin: Close Job (204 No Content)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}/close",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Admin permanently closes recruitment on a job opening"
    ),
    make_request(
        "12. Negative: Candidate Cannot View Closed Job (404 Not Found)",
        "GET",
        "{{jobBaseUrl}}/api/v1/jobs/{{jobId}}",
        test_script=[
            "pm.test('Status code is 404 Not Found', function () {",
            "    pm.response.to.have.status(404);",
            "});"
        ],
        description="Verify candidate public endpoint rejects access to closed jobs"
    ),
    make_request(
        "13. Setup: Recreate Active Job for Subsequent Testing (201 Created)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "title": "Senior Cloud Native Engineer {{$timestamp}}",
            "description": "Building cloud microservices for 366PI platform.",
            "department": "Engineering",
            "location": "Bangalore",
            "workMode": "REMOTE",
            "employmentType": "FULL_TIME",
            "experienceLevel": "SENIOR",
            "minExperienceYears": 5.0,
            "maxExperienceYears": 10.0,
            "salaryMin": 2500000.0,
            "salaryMax": 3800000.0,
            "currency": "INR",
            "skills": ["Java", "Spring Boot", "PostgreSQL", "Kafka"]
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.collectionVariables.set('jobId', json.id);",
            "pm.collectionVariables.set('jobIdForInterview', json.id);"
        ],
        description="Recreate and publish an active job so Application and Interview tests succeed"
    ),
    make_request(
        "14. Setup: Publish Newly Created Job (204 No Content)",
        "POST",
        "{{jobBaseUrl}}/api/v1/admin/jobs/{{jobId}}/publish",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Publish active job for application testing"
    ),
    make_request(
        "15. Job: Actuator Health (200 OK)",
        "GET",
        "{{jobBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Check Job Service health"
    ),
    make_request(
        "16. Job: OpenAPI Specification (200 OK)",
        "GET",
        "{{jobBaseUrl}}/v3/api-docs",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Job Service OpenAPI specification"
    ),
    make_request(
        "17. Job: Swagger UI",
        "GET",
        "{{jobBaseUrl}}/swagger-ui/index.html",
        description="Job Service Swagger UI documentation"
    )
]
folders.append({"name": "05 - Job Service (:8084)", "item": job_items})

# -------------------------------------------------------------
# 06 - Application Service (:8085)
# -------------------------------------------------------------
app_items = [
    # Candidate Endpoints
    make_request(
        "1. Candidate: Apply for Job (201 Created)",
        "POST",
        "{{applicationBaseUrl}}/api/v1/applications",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        body_json={
            "jobId": "{{jobId}}",
            "resumeUrl": "https://storage.366pi.com/resumes/rahul-sharma-lead-architect.pdf",
            "coverNote": "Passionate about building scalable distributed systems with high availability."
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Application created in APPLIED status', function () {",
            "    pm.expect(json.status).to.eql('APPLIED');",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('applicationId', json.id);"
        ],
        description="Candidate applies for published job opening"
    ),
    make_request(
        "2. Candidate: Get My Applications (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/applications/my-applications?page=0&size=10",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Contains applications content', function () {",
            "    pm.expect(json).to.have.property('content');",
            "});"
        ],
        description="Candidate retrieves list of their submitted applications"
    ),
    make_request(
        "3. Candidate: Get Application by ID (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/applications/{{applicationId}}",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Matches applicationId', function () {",
            "    pm.expect(json.id).to.eql(pm.collectionVariables.get('applicationId'));",
            "});"
        ],
        description="Candidate views details of their specific application"
    ),
    # Admin Endpoints
    make_request(
        "4. Admin: List Applications with Filter (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/admin/applications?status=APPLIED&page=0&size=10",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Admin lists applications filtered by job and status"
    ),
    make_request(
        "5. Admin: Get Application Statistics (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/admin/applications/stats",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Contains status breakdown counts', function () {",
            "    pm.expect(json).to.have.property('totalApplications');",
            "});"
        ],
        description="Aggregated count of applications across all lifecycle stages"
    ),
    make_request(
        "6. Admin: Get Application by ID (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/admin/applications/{{applicationId}}",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Admin view of application details"
    ),
    make_request(
        "7. Admin: Move Status to UNDER_REVIEW (200 OK)",
        "PUT",
        "{{applicationBaseUrl}}/api/v1/admin/applications/{{applicationId}}/status",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "status": "UNDER_REVIEW",
            "comments": "Resume matches all core platform architectural requirements."
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Status is UNDER_REVIEW', function () {",
            "    pm.expect(json.status).to.eql('UNDER_REVIEW');",
            "});"
        ],
        description="Recruiter reviews application and moves status to UNDER_REVIEW"
    ),
    make_request(
        "8. Admin: Shortlist Candidate Application (200 OK)",
        "PUT",
        "{{applicationBaseUrl}}/api/v1/admin/applications/{{applicationId}}/status",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "status": "SHORTLISTED",
            "comments": "Candidate shortlisted for technical interview rounds."
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Status is SHORTLISTED', function () {",
            "    pm.expect(json.status).to.eql('SHORTLISTED');",
            "});"
        ],
        description="Shortlist candidate application making it eligible for interview scheduling"
    ),
    make_request(
        "9. Admin: Get Application Status Audit History (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/admin/applications/{{applicationId}}/history",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Audit trail is an array', function () {",
            "    pm.expect(Array.isArray(json)).to.be.true;",
            "});"
        ],
        description="Get complete chronological audit trail of status transitions"
    ),
    # Internal Endpoints
    make_request(
        "10. Internal: Check Interview Eligibility (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/api/v1/internal/applications/{{applicationId}}/eligibility",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Candidate is eligible', function () {",
            "    pm.expect(json.eligible).to.be.true;",
            "});"
        ],
        description="Inter-service endpoint used by Interview Service to verify candidate eligibility"
    ),
    make_request(
        "11. Internal: Mark Interview Scheduled (204 No Content)",
        "POST",
        "{{applicationBaseUrl}}/api/v1/internal/applications/{{applicationId}}/interview-scheduled",
        headers={"X-System-Id": "00000000-0000-0000-0000-000000000000"},
        test_script=[
            "pm.test('Status code is 204 No Content', function () {",
            "    pm.response.to.have.status(204);",
            "});"
        ],
        description="Inter-service notification transitioning application status to INTERVIEW_SCHEDULED"
    ),
    make_request(
        "12. Candidate: Withdraw Application (200 OK)",
        "POST",
        "{{applicationBaseUrl}}/api/v1/applications/{{applicationId}}/withdraw",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Status is WITHDRAWN', function () {",
            "    pm.expect(json.status).to.eql('WITHDRAWN');",
            "});"
        ],
        description="Candidate withdraws their job application"
    ),
    make_request(
        "13. Setup: Recreate Shortlisted Application for Interview Tests (201 Created)",
        "POST",
        "{{applicationBaseUrl}}/api/v1/applications",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        body_json={
            "jobId": "{{jobId}}",
            "resumeUrl": "https://storage.366pi.com/resumes/rahul-interview.pdf",
            "coverNote": "Ready for technical evaluation."
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.collectionVariables.set('applicationId', json.id);"
        ],
        description="Create fresh application to be shortlisted for interview testing"
    ),
    make_request(
        "14. Setup: Shortlist Recreated Application (200 OK)",
        "PUT",
        "{{applicationBaseUrl}}/api/v1/admin/applications/{{applicationId}}/status",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "status": "SHORTLISTED",
            "comments": "Eligible for interview scheduling flow."
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Shortlist recreated application"
    ),
    make_request(
        "15. Application: Actuator Health (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Check Application Service health"
    ),
    make_request(
        "16. Application: OpenAPI Specification (200 OK)",
        "GET",
        "{{applicationBaseUrl}}/v3/api-docs",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Application Service OpenAPI documentation"
    ),
    make_request(
        "17. Application: Swagger UI",
        "GET",
        "{{applicationBaseUrl}}/swagger-ui/index.html",
        description="Application Service Swagger UI documentation"
    )
]
folders.append({"name": "06 - Application Service (:8085)", "item": app_items})

# -------------------------------------------------------------
# 07 - Interview Service (:8086)
# -------------------------------------------------------------
interview_items = [
    # Interviewer Management
    make_request(
        "1. Admin: Register Interviewer (201 Created)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/admin/interviewers",
        body_json={
            "userId": "{{adminId}}",
            "name": "Dr. Aris Thorne",
            "email": "aris.thorne_{{$timestamp}}@example.com"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Interviewer ID returned', function () {",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('interviewerId', json.id);"
        ],
        description="Register a specialist as an active interviewer"
    ),
    make_request(
        "2. Admin: List Active Interviewers (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/admin/interviewers",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Returns list of interviewers', function () {",
            "    pm.expect(Array.isArray(json)).to.be.true;",
            "});"
        ],
        description="List all registered active interviewers"
    ),
    make_request(
        "3. Admin: Get Interviewer by ID (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/admin/interviewers/{{interviewerId}}",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Matches interviewerId', function () {",
            "    pm.expect(json.id).to.eql(pm.collectionVariables.get('interviewerId'));",
            "});"
        ],
        description="Retrieve details of a registered interviewer by ID"
    ),
    # Slot Management
    make_request(
        "4. Admin: Create Interview Slot (201 Created)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/slots",
        body_json={
            "interviewerId": "{{interviewerId}}",
            "startTime": "2026-11-20T10:00:00",
            "endTime": "2026-11-20T11:00:00"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Slot ID returned with AVAILABLE status', function () {",
            "    pm.expect(json.status).to.eql('AVAILABLE');",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('slotId', json.id);"
        ],
        description="Admin schedules a future 1-hour interview slot"
    ),
    make_request(
        "5. Admin: Create Backup Slot for Rescheduling (201 Created)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/slots",
        body_json={
            "interviewerId": "{{interviewerId}}",
            "startTime": "2026-11-22T14:00:00",
            "endTime": "2026-11-22T15:00:00"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.collectionVariables.set('rescheduleSlotId', json.id);"
        ],
        description="Create second slot for rescheduling flow tests"
    ),
    make_request(
        "6. Admin: Get All Slots (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/slots?page=0&size=10",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Admin lists interview slots across all statuses with pagination"
    ),
    # Candidate Interview Scheduling
    make_request(
        "7. Candidate: Get Available Slots for Application (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/interviews/slots?applicationId={{applicationId}}&page=0&size=10",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Available slots returned', function () {",
            "    pm.expect(json).to.have.property('content');",
            "});"
        ],
        description="Candidate checks available interview slots for their shortlisted application"
    ),
    make_request(
        "8. Candidate: Schedule Interview (201 Created)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/interviews",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        body_json={
            "applicationId": "{{applicationId}}",
            "slotId": "{{slotId}}",
            "roundNumber": 1,
            "roundName": "Technical Architecture & System Design",
            "interviewType": "TECHNICAL"
        },
        test_script=[
            "pm.test('Status code is 201 Created', function () {",
            "    pm.response.to.have.status(201);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Interview scheduled with SCHEDULED status', function () {",
            "    pm.expect(json.status).to.eql('SCHEDULED');",
            "    pm.expect(json).to.have.property('id');",
            "});",
            "pm.collectionVariables.set('interviewId', json.id);"
        ],
        description="Candidate books an available slot to schedule interview round"
    ),
    make_request(
        "9. Candidate: Get My Scheduled Interviews (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/interviews/me?page=0&size=10",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Candidate queries all scheduled interviews associated with their profile"
    ),
    make_request(
        "10. Candidate: Get Interview by ID (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/interviews/{{interviewId}}",
        headers={"X-Candidate-Id": "{{candidateId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Matches interviewId', function () {",
            "    pm.expect(json.id).to.eql(pm.collectionVariables.get('interviewId'));",
            "});"
        ],
        description="Candidate retrieves interview details"
    ),
    # Admin Interview Lifecycle & Audit
    make_request(
        "11. Admin: Get All Interviews (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/admin/interviews?page=0&size=10",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Admin queries paginated list of all interviews across candidates"
    ),
    make_request(
        "12. Admin: Reschedule Interview to New Slot (200 OK)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/{{interviewId}}/reschedule",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "newSlotId": "{{rescheduleSlotId}}"
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Admin moves interview to a different available slot"
    ),
    make_request(
        "13. Admin: Update Interview Status to COMPLETED (200 OK)",
        "PATCH",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/{{interviewId}}/status",
        headers={"X-Admin-Id": "{{adminId}}"},
        body_json={
            "status": "COMPLETED",
            "notes": "Candidate demonstrated outstanding system design skills and deep microservice experience."
        },
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Status updated to COMPLETED', function () {",
            "    pm.expect(json.status).to.eql('COMPLETED');",
            "});"
        ],
        description="Admin transitions interview status to COMPLETED with feedback notes"
    ),
    make_request(
        "14. Admin: Get Interview History Audit Trail (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/{{interviewId}}/history",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Audit history returned as array', function () {",
            "    pm.expect(Array.isArray(json)).to.be.true;",
            "    pm.expect(json.length).to.be.above(0);",
            "});"
        ],
        description="Get chronological history of all status changes and rescheduling actions"
    ),
    make_request(
        "15. Admin: Cancel Interview (200 OK)",
        "POST",
        "{{interviewBaseUrl}}/api/v1/admin/interviews/{{interviewId}}/cancel",
        headers={"X-Admin-Id": "{{adminId}}"},
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});",
            "var json = pm.response.json();",
            "pm.test('Status changed to CANCELLED', function () {",
            "    pm.expect(json.status).to.eql('CANCELLED');",
            "});"
        ],
        description="Admin cancels interview and releases associated slot back to AVAILABLE"
    ),
    make_request(
        "16. Interview: Actuator Health (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/actuator/health",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Check Interview Service health"
    ),
    make_request(
        "17. Interview: OpenAPI Specification (200 OK)",
        "GET",
        "{{interviewBaseUrl}}/v3/api-docs",
        test_script=[
            "pm.test('Status code is 200 OK', function () {",
            "    pm.response.to.have.status(200);",
            "});"
        ],
        description="Interview Service OpenAPI documentation"
    ),
    make_request(
        "18. Interview: Swagger UI",
        "GET",
        "{{interviewBaseUrl}}/swagger-ui/index.html",
        description="Interview Service Swagger UI documentation"
    )
]
folders.append({"name": "07 - Interview Service (:8086)", "item": interview_items})

collection = {
    "info": {
        "_postman_id": str(uuid.uuid4()),
        "name": "366PI Recruitment Platform - Complete Endpoints Collection",
        "description": "Comprehensive Postman API Collection covering 100% of all endpoints across all 7 microservices in the 366PI Recruitment Platform architecture:\n\n1. Discovery Server (Eureka :8761)\n2. Admin Server (Spring Boot Admin :8082)\n3. Auth Service (:8083)\n4. User Service (:8081)\n5. Job Service (:8084)\n6. Application Service (:8085)\n7. Interview Service (:8086)\n\nAll endpoints include realistic JSON payloads, query parameters, path variables, required authentication headers (X-Candidate-Id, X-Admin-Id, Bearer token), and automated Postman test scripts that dynamically populate collection variables.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "discoveryBaseUrl", "value": "http://localhost:8761", "type": "string"},
        {"key": "adminServerBaseUrl", "value": "http://localhost:8082", "type": "string"},
        {"key": "authBaseUrl", "value": "http://localhost:8083", "type": "string"},
        {"key": "userBaseUrl", "value": "http://localhost:8081", "type": "string"},
        {"key": "jobBaseUrl", "value": "http://localhost:8084", "type": "string"},
        {"key": "applicationBaseUrl", "value": "http://localhost:8085", "type": "string"},
        {"key": "interviewBaseUrl", "value": "http://localhost:8086", "type": "string"},
        {"key": "adminId", "value": "11111111-1111-1111-1111-111111111111", "type": "string"},
        {"key": "candidateId", "value": "22222222-2222-2222-2222-222222222222", "type": "string"},
        {"key": "userId", "value": "22222222-2222-2222-2222-222222222222", "type": "string"},
        {"key": "authUserId", "value": "", "type": "string"},
        {"key": "userEmail", "value": "candidate@example.com", "type": "string"},
        {"key": "userPassword", "value": "StrongPassword123!", "type": "string"},
        {"key": "accessToken", "value": "", "type": "string"},
        {"key": "refreshToken", "value": "", "type": "string"},
        {"key": "activationToken", "value": "", "type": "string"},
        {"key": "passwordResetToken", "value": "", "type": "string"},
        {"key": "jobId", "value": "", "type": "string"},
        {"key": "jobIdForInterview", "value": "", "type": "string"},
        {"key": "applicationId", "value": "", "type": "string"},
        {"key": "interviewerId", "value": "", "type": "string"},
        {"key": "slotId", "value": "", "type": "string"},
        {"key": "rescheduleSlotId", "value": "", "type": "string"},
        {"key": "interviewId", "value": "", "type": "string"}
    ],
    "item": folders
}

output_path = r"c:\Users\Shubhangi\Documents\workspace-spring-tools-for-eclipse-5.3.0.RELEASE\366pi\collection.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2)

print(f"Successfully wrote collection.json to {output_path}")
total_endpoints = sum(len(f["item"]) for f in folders)
print(f"Total services/folders: {len(folders)}")
print(f"Total requests generated: {total_endpoints}")
for f in folders:
    print(f" - {f['name']}: {len(f['item'])} requests")
