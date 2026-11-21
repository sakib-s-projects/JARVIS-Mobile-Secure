package com.orbit.assistant;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Offline-first, durable storage for site tasks. No network access or destructive side effects. */
public final class SiteTaskStore {
    private static final String PREFS = "jarvis_site_tasks_v1";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 500;

    public static final class Task {
        public final String id;
        public final String title;
        public final String note;
        public final long createdAt;
        public final boolean completed;
        public final long completedAt;
        public final String reminderId;
        public final long reminderAt;

        public Task(String id, String title, String note, long createdAt,
                   boolean completed, long completedAt) {
            this(id, title, note, createdAt, completed, completedAt, "", 0L);
        }

        public Task(String id, String title, String note, long createdAt,
                   boolean completed, long completedAt, String reminderId, long reminderAt) {
            this.id = id == null ? "" : id.trim();
            this.title = title == null ? "" : title.trim();
            this.note = note == null ? "" : note.trim();
            this.createdAt = createdAt;
            this.completed = completed;
            this.completedAt = completed ? Math.max(0L, completedAt) : 0L;
            this.reminderId = reminderId == null ? "" : reminderId.trim();
            this.reminderAt = this.reminderId.isEmpty() ? 0L : Math.max(0L, reminderAt);
        }

        JSONObject toJson() throws Exception {
            return new JSONObject().put("id", id).put("title", title).put("note", note)
                    .put("createdAt", createdAt).put("completed", completed)
                    .put("completedAt", completedAt).put("reminderId", reminderId)
                    .put("reminderAt", reminderAt);
        }

        static Task fromJson(JSONObject o) {
            if (o == null) return null;
            String id = o.optString("id", "").trim();
            String title = o.optString("title", "").trim();
            if (id.isEmpty() || title.isEmpty()) return null;
            boolean done = o.optBoolean("completed", false);
            return new Task(id, title, o.optString("note", ""), o.optLong("createdAt", 0L),
                    done, o.optLong("completedAt", 0L), o.optString("reminderId", ""),
                    o.optLong("reminderAt", 0L));
        }
    }

    private SiteTaskStore() {}

    public static Task create(String title, String note) {
        return new Task(UUID.randomUUID().toString(), title, note, System.currentTimeMillis(), false, 0L);
    }

    public static synchronized List<Task> list(Context c) {
        ArrayList<Task> out = new ArrayList<>();
        if (c == null) return out;
        try {
            JSONArray array = new JSONArray(prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                Task task = Task.fromJson(array.optJSONObject(i));
                if (task != null) out.add(task);
            }
        } catch (Exception ignored) {}
        Collections.sort(out, (a, b) -> {
            if (a.completed != b.completed) return a.completed ? 1 : -1;
            return Long.compare(b.createdAt, a.createdAt);
        });
        return out;
    }

    public static synchronized boolean upsert(Context c, Task task) {
        if (c == null || task == null || task.id.isEmpty() || task.title.isEmpty()) return false;
        ArrayList<Task> items = new ArrayList<>(list(c));
        boolean found = false;
        for (int i = 0; i < items.size(); i++) {
            if (task.id.equals(items.get(i).id)) { items.set(i, task); found = true; break; }
        }
        if (!found) {
            // Put a just-created task first before trimming. Multiple tasks can share the same
            // millisecond timestamp; stable sorting must not make the newest insert the item
            // discarded when the store reaches MAX_ITEMS.
            items.add(0, task);
        }
        while (items.size() > MAX_ITEMS) items.remove(items.size() - 1);
        return write(c, items);
    }

    public static synchronized boolean setCompleted(Context c, String id, boolean completed) {
        if (id == null || id.trim().isEmpty()) return false;
        for (Task task : list(c)) {
            if (id.equals(task.id)) {
                return upsert(c, new Task(task.id, task.title, task.note, task.createdAt,
                        completed, completed ? System.currentTimeMillis() : 0L,
                        task.reminderId, task.reminderAt));
            }
        }
        return false;
    }

    public static synchronized boolean remove(Context c, String id) {
        if (c == null || id == null || id.trim().isEmpty()) return false;
        ArrayList<Task> items = new ArrayList<>(list(c));
        boolean changed = items.removeIf(t -> id.equals(t.id));
        return changed && write(c, items);
    }

    static synchronized String backupJson(Context c) {
        return c == null ? "[]" : prefs(c).getString(KEY, "[]");
    }

    static synchronized boolean restoreBackupJson(Context c, String json) {
        if (c == null || json == null) return false;
        try {
            JSONArray source = new JSONArray(json);
            JSONArray clean = new JSONArray();
            for (int i = 0; i < source.length() && i < MAX_ITEMS; i++) {
                Task t = Task.fromJson(source.optJSONObject(i));
                if (t != null) clean.put(t.toJson());
            }
            return prefs(c).edit().putString(KEY, clean.toString()).commit();
        } catch (Exception ignored) { return false; }
    }

    private static boolean write(Context c, List<Task> tasks) {
        JSONArray array = new JSONArray();
        for (Task t : tasks) {
            try { array.put(t.toJson()); } catch (Exception ignored) {}
        }
        return prefs(c).edit().putString(KEY, array.toString()).commit();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
