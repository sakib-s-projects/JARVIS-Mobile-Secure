package com.orbit.assistant;

import static org.junit.Assert.*;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/** Robolectric checks for persistence and task state changes. */
@RunWith(RobolectricTestRunner.class)
public class SiteTaskStoreTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("jarvis_site_tasks_v1", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void newTaskStartsIncompleteAndCanBeSaved() {
        SiteTaskStore.Task t = SiteTaskStore.create("Pour concrete", "Column C2");
        assertFalse(t.completed);
        assertEquals(0L, t.completedAt);
        assertTrue(SiteTaskStore.upsert(context, t));
        List<SiteTaskStore.Task> items = SiteTaskStore.list(context);
        assertEquals(1, items.size());
        assertEquals("Pour concrete", items.get(0).title);
        assertEquals("Column C2", items.get(0).note);
    }

    @Test public void completionCanBeToggledAndReopened() {
        SiteTaskStore.Task t = SiteTaskStore.create("Install rebar", "");
        assertTrue(SiteTaskStore.upsert(context, t));
        assertTrue(SiteTaskStore.setCompleted(context, t.id, true));
        SiteTaskStore.Task done = SiteTaskStore.list(context).get(0);
        assertTrue(done.completed);
        assertTrue(done.completedAt > 0L);
        assertTrue(SiteTaskStore.setCompleted(context, t.id, false));
        SiteTaskStore.Task reopened = SiteTaskStore.list(context).get(0);
        assertFalse(reopened.completed);
        assertEquals(0L, reopened.completedAt);
    }

    @Test public void editUsesSameIdAndDoesNotDuplicate() {
        SiteTaskStore.Task original = SiteTaskStore.create("Check formwork", "");
        assertTrue(SiteTaskStore.upsert(context, original));
        SiteTaskStore.Task edited = new SiteTaskStore.Task(original.id, "Check formwork C3",
                "Before casting", original.createdAt, false, 0L);
        assertTrue(SiteTaskStore.upsert(context, edited));
        assertEquals(1, SiteTaskStore.list(context).size());
        assertEquals("Check formwork C3", SiteTaskStore.list(context).get(0).title);
    }

    @Test public void removeOnlySelectedTask() {
        SiteTaskStore.Task first = SiteTaskStore.create("Task A", "");
        SiteTaskStore.Task second = SiteTaskStore.create("Task B", "");
        assertTrue(SiteTaskStore.upsert(context, first));
        assertTrue(SiteTaskStore.upsert(context, second));
        assertTrue(SiteTaskStore.remove(context, first.id));
        assertEquals(1, SiteTaskStore.list(context).size());
        assertEquals(second.id, SiteTaskStore.list(context).get(0).id);
    }

    @Test public void backupJsonRestoresTasks() {
        SiteTaskStore.Task base = SiteTaskStore.create("Backup test", "Site note");
        SiteTaskStore.Task task = new SiteTaskStore.Task(base.id, base.title, base.note, base.createdAt,
                false, 0L, "site-task-" + base.id, System.currentTimeMillis() + 60000L);
        assertTrue(SiteTaskStore.upsert(context, task));
        String backup = SiteTaskStore.backupJson(context);
        assertTrue(SiteTaskStore.remove(context, task.id));
        assertTrue(SiteTaskStore.restoreBackupJson(context, backup));
        assertEquals("Backup test", SiteTaskStore.list(context).get(0).title);
        assertEquals(task.reminderId, SiteTaskStore.list(context).get(0).reminderId);
        assertEquals(task.reminderAt, SiteTaskStore.list(context).get(0).reminderAt);
    }

    @Test public void restoreRejectsMalformedJsonWithoutChangingExistingData() {
        SiteTaskStore.Task saved = SiteTaskStore.create("Keep this task", "Preserve");
        assertTrue(SiteTaskStore.upsert(context, saved));
        assertFalse(SiteTaskStore.restoreBackupJson(context, "{bad json"));
        assertEquals("Keep this task", SiteTaskStore.list(context).get(0).title);
    }

    @Test public void storeRejectsEmptyTitle() {
        SiteTaskStore.Task blank = SiteTaskStore.create("   ", "");
        assertFalse(SiteTaskStore.upsert(context, blank));
        assertTrue(SiteTaskStore.list(context).isEmpty());
    }

    @Test public void storeCapsTaskCountAndKeepsRecentTasks() {
        SiteTaskStore.Task newest = null;
        for (int i = 0; i < 505; i++) {
            SiteTaskStore.Task task = SiteTaskStore.create("Task " + i, "");
            assertTrue(SiteTaskStore.upsert(context, task));
            if (i == 504) newest = task;
        }
        assertEquals(500, SiteTaskStore.list(context).size());
        assertNotNull(newest);
        final String newestId = newest.id;
        assertTrue(SiteTaskStore.list(context).stream().anyMatch(t -> t.id.equals(newestId)));
    }
}
