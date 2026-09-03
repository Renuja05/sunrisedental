# Sunrise Dental Clinic — Appointment & Patient Management System
### Web version: HTML/CSS/JS → REST → Java backend → JDBC → MySQL (WAMP)

```
Browser (HTML/CSS/JS)  →  HTTP/REST  →  Java backend  →  JDBC  →  MySQL (via WAMP)
```

One Java process (`server.Main`) does two jobs at once: it serves the
REST API under `/api/...`, **and** it serves the HTML/CSS/JS files
themselves. That means WAMP only needs to do one job for you — running
MySQL — there's no separate Apache/PHP setup required.

---

## 1. Requirements

- **JDK 17+**, NetBeans.
- **WAMP** (or any MySQL 5.7+/8.x install) — used for the database only.
- **MySQL Connector/J** — same as before: download it, put the `.jar`
  in `lib/`, add it to the project's Libraries in NetBeans.
- A modern browser (Chrome, Edge, Firefox) to actually use the system.

---

## 2. Setup

1. **Start WAMP** and make sure MySQL is running (the WAMP tray icon
   should be green).
2. **Create the database**: run `sql/schema.sql` once — either
   `mysql -u root -p < sql/schema.sql` from a terminal, or paste it
   into phpMyAdmin (WAMP's built-in DB admin tool, usually at
   `http://localhost/phpmyadmin`) and execute it there.
3. **Check the connection settings** in `src/db/DBConnection.java` —
   WAMP's MySQL usually defaults to username `root` with **no
   password**, which already matches this file. Change it if yours is
   different.
4. **Open in NetBeans**: File → New Project → Java with Existing
   Sources → point it at this project's `src` folder.
5. **Add the MySQL connector jar**: right-click the project →
   Properties → Libraries → Add JAR/Folder → select the jar.
6. **Test the database connection**: right-click `db/TestConnection.java`
   → Run File. You should see `SUCCESS: connected to the sunrise_dental
   database.`
7. **Run the server**: right-click `server/Main.java` → Run File. Wait for:
   ```
   Database connection OK.
   Server running: open http://localhost:8080/ in your browser.
   ```
8. **Open your browser** to `http://localhost:8080/` — you'll land on
   the login page.

---

## 3. Default accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `password123` | Administrator |
| `reception1` | `password123` | Receptionist |

---

## 4. Project structure

```
SunriseDentalClinicWeb/
├── sql/schema.sql          MySQL schema + seed data
├── lib/                    put mysql-connector-j-*.jar here
├── src/
│   ├── model/               User → Receptionist/Administrator, Patient,
│   │                        Dentist, Treatment, Appointment, Bill
│   ├── dao/                 One concrete class per entity (PatientDAO,
│   │                        DentistDAO, TreatmentDAO, AppointmentDAO,
│   │                        BillDAO, UserDAO) — all SQL lives here
│   ├── db/                  DBConnection (Singleton) + TestConnection
│   ├── util/                Validator, PasswordUtil, IdGenerator, Json
│   ├── web/                 SessionManager, BaseHandler, StaticFileHandler
│   ├── web/handlers/        One REST handler per resource
│   └── server/Main.java     <- run this file to start everything
└── webapp/                  the browser-side frontend
    ├── login.html
    ├── dashboard.html
    ├── register-appointment.html
    ├── appointment-details.html
    ├── reports.html
    ├── manage-staff.html
    ├── help.html
    ├── css/style.css
    ├── js/api.js             fetch() wrapper + session handling
    └── img/                  logo + icons (same set used in the JFrame version)
```

---

## 5. Design pattern used

**Singleton** — `db.DBConnection` is the single, application-wide
access point for the database configuration (private constructor,
`getInstance()`), same as in the JFrame version. `web.SessionManager`
follows the identical shape on the server side, tracking which login
token belongs to which user.

No Factory, Strategy, or Service layer — REST handlers call the DAO
classes directly, the same way the JFrame screens did.

---

## 6. How authentication works without a JFrame session object

There's no `CurrentSession` class this time, because a browser can't
hold a Java object between page loads — instead:

1. `POST /api/login` returns a random **token** plus the user's details.
2. The browser stores both in `sessionStorage` (see `js/api.js`) —
   cleared automatically when the tab closes.
3. Every later request attaches that token in an `X-Auth-Token` header.
4. The server's `SessionManager` looks up which user that token
   belongs to; `BaseHandler.rejectIfNotLoggedIn()` /
   `rejectIfNotAdmin()` enforce access control on every endpoint,
   independently of whatever the browser's UI shows or hides.

---

## 7. Where the old Service-layer logic went

Same approach as the JFrame version — it lives directly inside the DAO
methods or the REST handler:

- **Double bookings**: `AppointmentDAO.register(...)` checks slot
  availability immediately before inserting.
- **Duplicate patients**: `PatientDAO.findOrCreate(...)` reuses a
  patient record by contact number instead of creating a new one.
- **Bill calculation**: done inline in `BillHandler` — consultation fee
  (fixed) + treatment cost − an optional 10% returning-patient discount.

---

## 8. Printing

Every "Print" button just calls the browser's own `window.print()`.
Each screen has a small `@media print` rule that hides the navigation
bar and buttons so only the actual report or receipt prints.
