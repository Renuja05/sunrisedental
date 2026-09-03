/**
 * Shared front-end helpers.
 *
 * SESSION STORAGE: after login we keep the auth token and basic user
 * details in sessionStorage (cleared when the browser tab closes).
 * Every API call attaches the token via the X-Auth-Token header so the
 * Java backend can identify who's making the request — plain HTTP has
 * no memory of who you are between requests otherwise.
 */

const Session = {
  KEY: "sunrise_session",

  save(token, user) {
    sessionStorage.setItem(this.KEY, JSON.stringify({ token, user }));
  },

  get() {
    const raw = sessionStorage.getItem(this.KEY);
    return raw ? JSON.parse(raw) : null;
  },

  clear() {
    sessionStorage.removeItem(this.KEY);
  },

  requireLogin() {
    const session = this.get();
    if (!session) {
      window.location.href = "login.html";
      return null;
    }
    return session;
  }
};

/**
 * Calls the REST API and returns the parsed JSON body. Throws an Error
 * with a readable message on any non-2xx response (the same shape of
 * error handling used throughout, whatever screen calls this).
 */
async function apiFetch(method, path, body) {
  const session = Session.get();
  const headers = { "Content-Type": "application/json" };
  if (session) headers["X-Auth-Token"] = session.token;

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch (networkError) {
    throw new Error("Could not reach the server. Is server/Main.java running?");
  }

  let data = {};
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { /* non-JSON response, ignore */ }
  }

  if (!response.ok) {
    throw new Error(data.error || ("Request failed with status " + response.status));
  }
  return data;
}

/** Renders "Signed in as ..." plus a Log out link in any page's #topbar-who element. */
function renderUserBar() {
  const session = Session.get();
  const el = document.getElementById("topbar-who");
  if (!el || !session) return;
  el.innerHTML = "Signed in as " + escapeHtml(session.user.fullName) +
      " (" + escapeHtml(session.user.role) + ") &nbsp;|&nbsp; " +
      '<a href="#" id="logout-link" style="color:#dce6f5;">Log out</a>';
  document.getElementById("logout-link").addEventListener("click", async (e) => {
    e.preventDefault();
    try { await apiFetch("POST", "/api/logout"); } catch (err) { /* best-effort */ }
    Session.clear();
    window.location.href = "login.html";
  });
}

function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
}

function showStatus(elementId, message, type) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = message || "";
  el.className = "status" + (type ? " " + type : "");
}

/** Hides the "Manage Staff Accounts" menu entry unless the user is an Administrator. */
function applyRoleVisibility() {
  const session = Session.get();
  if (!session) return;
  document.querySelectorAll("[data-admin-only]").forEach((el) => {
    el.style.display = session.user.role === "ADMINISTRATOR" ? "" : "none";
  });
}
