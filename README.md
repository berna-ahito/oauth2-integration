# OAuth2 Integration (Google & GitHub)

A Spring Boot web application that implements **OAuth2 login** using **Google** and **GitHub** providers.  
Users can sign in securely, view their profile, and update personal details such as **Display Name** and **Bio**.  
The app uses **Spring Security**, **Thymeleaf**, and an **H2 in-memory database**.

---
## 🚀 Features

-  OAuth2 login via **Google** and **GitHub**
-  Authenticated profile page with user info
-  Edit and save `Display Name` and `Bio`
-  Secure logout that redirects back to home
- Persistent data using **H2 database**
-  Clean and modern UI (navy blue theme)

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-------------|
| Backend | Spring Boot 3 (Java) |
| Security | Spring Security (OAuth2 Client) |
| View | Thymeleaf + HTML + CSS |
| Database | H2 (in-memory) |
| Build Tool | Maven |

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/berna-ahito/oauth2-integration.git
cd oauth2-integration
```

### 3️⃣ Run the Application
```
mvn spring-boot:run
```

Then open your browser and go to:
👉 http://localhost:8080

4️⃣ Access the H2 Database (optional)

Go to: http://localhost:8080/h2-console
```
JDBC URL: jdbc:h2:mem:testdb

Username: sa

Password: (leave blank)
```
## 🌐 Endpoints Summary

| HTTP Method | Endpoint | Description | Authentication Required |
|--------------|-----------|--------------|--------------------------|
| **GET** | `/` | Displays home page with “Login with Google” and “Login with GitHub” buttons | ❌ No |
| **GET** | `/profile` | Displays logged-in user's profile (display name, bio, email, avatar) | ✅ Yes |
| **POST** | `/profile` | Updates user’s display name and bio | ✅ Yes |
| **GET** | `/logout` | Logs out user and redirects to home page with success message | ❌ No |
| **GET** | `/h2-console` | Opens the in-memory H2 database console | ❌ No |

✅ = Requires Login  ❌ = Public Access

## ⏱️ Project Flow

User visits `/` → sees **Login with Google / GitHub**  
After authentication → redirected to `/profile`  
User edits **Display Name** or **Bio** → clicks **Save Changes**  
Changes persist in the **H2 database**  
Clicking **Logout** ends the session and returns to home with a success message  

---

### 🧭 a) High-level Flow
```mermaid
flowchart LR
  A[Browser] -->|GET /| B[Spring MVC (HomeController)]
  B --> C[Login Buttons]
  A -->|/oauth2/authorization/google| D[Spring Security OAuth2 Client]
  A -->|/oauth2/authorization/github| D
  D -->|Auth Code Flow| E[Google / GitHub]
  E --> D --> F[OAuth2LoginAuthenticationFilter]
  F --> G[SecurityContext holds Principal]
  G -->|GET /profile| H[ProfileController]
  H --> I[Service + JPA]
  I --> J[(H2 Database)]
  H -->|View| K[Thymeleaf Templates]
```
### 🧩 b) Module / Layer Diagram
```mermaid
graph TD
  UI[Thymeleaf Views<br/>home.html, profile.html, error.html] --> MVC[Controllers<br/>HomeController, ProfileController]
  MVC --> SEC[Spring Security Config]
  MVC --> SVC[Profile/User Services]
  SVC --> JPA[Spring Data JPA Repositories]
  JPA --> DB[(H2 In-Memory Database)]
  SEC --> OIDC[OAuth2 Client (Google, GitHub)]
```
