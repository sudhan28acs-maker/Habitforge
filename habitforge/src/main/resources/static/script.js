const API = "/api/habits";
const $ = id => document.getElementById(id);
const DAYS = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"];

$("dayChips").innerHTML = DAYS.map(d =>
  `<label class="chip"><input type="checkbox" value="${d}"><span>${DAY_SHORT[d]}</span></label>`).join("");

function toggleDays() {
  $("daysBox").style.display = $("frequency").value === "SPECIFIC_WEEKDAYS" ? "block" : "none";
}

async function createHabit() {
  const name = $("name").value.trim();
  if (!name) { showToast("Name is required", true); return; }
  const frequency = $("frequency").value;
  const targetDays = [...document.querySelectorAll("#dayChips input:checked")].map(c => c.value);
  await apiFetch(API, { method: "POST", headers: JSON_H, body: JSON.stringify({
    name, description: $("description").value.trim() || null, frequency,
    targetDays: frequency === "SPECIFIC_WEEKDAYS" ? targetDays : [], startDate: $("startDate").value || null }) });
  ["name", "description", "startDate"].forEach(i => $(i).value = "");
  document.querySelectorAll("#dayChips input").forEach(c => c.checked = false);
  showToast("Habit created");
  load();
}

async function markToday(id) {
  const s = await apiFetch(`${API}/${id}/logs`, { method: "POST", headers: JSON_H, body: "{}" });
  showToast(`Done! Current streak: ${s.currentStreak} 🔥`);
  load();
}

async function undoToday(id) {
  await apiFetch(`${API}/${id}/logs?date=${todayStr()}`, { method: "DELETE" });
  showToast("Today's log removed");
  load();
}

async function logDay() {
  const id = $("logHabit").value;
  if (!id) { showToast("Create a habit first", true); return; }
  const s = await apiFetch(`${API}/${id}/logs`, { method: "POST", headers: JSON_H,
    body: JSON.stringify({ date: $("logDate").value || null, note: $("logNote").value.trim() || null }) });
  $("logNote").value = "";
  showToast(`Logged! Current streak: ${s.currentStreak}, best: ${s.bestStreak}`);
  load();
}

async function deleteHabit(id) {
  if (!confirm("Delete this habit and all its history?")) return;
  await apiFetch(`${API}/${id}`, { method: "DELETE" });
  showToast("Habit deleted");
  load();
}

async function checkMissed() {
  const r = await apiFetch(`${API}/refresh-streaks`, { method: "POST" });
  showToast(`Checked ${r.habitsChecked} habit(s) — ${r.streaksReset} streak(s) reset`);
  load();
}

async function load() {
  const habits = (await apiFetch(API).catch(() => [])) || [];
  const rows = $("rows");
  rows.innerHTML = "";
  $("empty").classList.toggle("show", habits.length === 0);

  habits.forEach(h => {
    const sched = h.frequency === "DAILY" ? `<span class="mini-chip">Every day</span>`
      : h.targetDays.map(d => `<span class="mini-chip">${DAY_SHORT[d]}</span>`).join("");
    let today;
    if (!h.requiredToday) today = `<span class="badge" style="background:#f3f4f6;color:#6b7280">Rest day</span>`;
    else today = h.completedToday ? `<span class="badge badge-paid">DONE</span>` : `<span class="badge badge-pending">DUE</span>`;
    const action = !h.requiredToday ? "" : h.completedToday
      ? `<button class="btn-sm btn-swap" onclick="undoToday(${h.id})">Undo</button>`
      : `<button class="btn-sm btn-invoice" onclick="markToday(${h.id})">Done today</button>`;
    const tr = document.createElement("tr");
    tr.innerHTML = `<td><strong>${h.name}</strong><br><small style="color:var(--muted)">${h.description || ""}</small></td>
      <td>${sched}</td><td><span class="streak">${h.currentStreak} 🔥</span></td><td><span class="streak">${h.bestStreak} 🏆</span></td>
      <td>${today}</td>
      <td><div class="actions">${action}
        <a class="btn-sm btn-invoice" style="text-decoration:none" href="calendar.html?id=${h.id}">Calendar</a>
        <button class="btn-sm btn-danger" onclick="deleteHabit(${h.id})">Delete</button></div></td>`;
    rows.appendChild(tr);
  });

  const due = habits.filter(h => h.requiredToday);
  const pending = due.filter(h => !h.completedToday);
  $("reminders").innerHTML = due.length === 0
    ? `<div class="reminder done">Nothing scheduled today 🌿</div>`
    : pending.length === 0 ? `<div class="reminder done">All habits done for today 🎉</div>`
    : pending.map(h => `<div class="reminder">🔔 ${h.name} <button class="btn-sm btn-invoice" onclick="markToday(${h.id})">Done</button></div>`).join("");

  $("statHabits").textContent = habits.length;
  $("statDone").textContent = `${due.length - pending.length}/${due.length}`;
  $("statCurrent").textContent = habits.reduce((m, h) => Math.max(m, h.currentStreak), 0);
  $("statBest").textContent = habits.reduce((m, h) => Math.max(m, h.bestStreak), 0);

  const prev = $("logHabit").value;
  $("logHabit").innerHTML = habits.map(h => `<option value="${h.id}">${h.name}</option>`).join("");
  if (prev) $("logHabit").value = prev;
}

$("logDate").value = todayStr();
load();
