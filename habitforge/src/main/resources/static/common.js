async function apiFetch(url, options = {}) {
  try {
    const res = await fetch(url, options);
    const ct = res.headers.get("content-type") || "";
    const payload = ct.includes("json") ? await res.json().catch(() => null) : null;
    if (!res.ok) {
      let msg = (payload && payload.message) || `Request failed (${res.status})`;
      if (payload && payload.details && typeof payload.details === "object") {
        msg += ": " + Object.values(payload.details).join(", ");
      }
      throw new Error(msg);
    }
    return payload;
  } catch (err) {
    showToast(err.message || "Something went wrong", true);
    throw err;
  }
}

function showToast(message, isError = false) {
  const t = document.getElementById("toast");
  if (!t) return;
  t.textContent = message;
  t.className = "toast show" + (isError ? " error" : "");
  setTimeout(() => { t.className = "toast"; }, 3200);
}

const todayStr = () => new Date().toLocaleDateString("en-CA");   // yyyy-MM-dd
const DAY_SHORT = { MONDAY: "Mon", TUESDAY: "Tue", WEDNESDAY: "Wed", THURSDAY: "Thu", FRIDAY: "Fri", SATURDAY: "Sat", SUNDAY: "Sun" };
const JSON_H = { "Content-Type": "application/json" };

function highlightNav() {
  const path = location.pathname.split("/").pop() || "index.html";
  document.querySelectorAll(".navbar a").forEach(a => { if (a.getAttribute("href") === path) a.classList.add("active"); });
}
document.addEventListener("DOMContentLoaded", highlightNav);
