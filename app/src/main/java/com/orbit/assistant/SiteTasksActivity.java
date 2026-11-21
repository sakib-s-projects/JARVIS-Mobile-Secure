package com.orbit.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;

/** Offline-first site task list. Changes are saved locally as they are made. */
public final class SiteTasksActivity extends Activity {
    private LinearLayout pending, completed;
    private OrbitPredictiveBack navigation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.syncTheme(this);
        Window w = getWindow();
        w.setStatusBarColor(UiKit.BG);
        w.setNavigationBarColor(UiKit.BG);
        View content = build();
        setContentView(content);
        UiKit.applyActivityInsets(this, content, true);
        navigation = OrbitPredictiveBack.install(this);
    }

    @Override protected void onResume() {
        super.onResume();
        UiPresence.enter(this);
        refresh();
    }

    @Override protected void onPause() {
        UiPresence.leave(this);
        super.onPause();
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        OrbitBackground.applyPage(scroll);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int p = UiKit.dp(this, 20);
        page.setPadding(p, UiKit.dp(this, 24), p, UiKit.dp(this, 48));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = iconButton(R.drawable.ic_back, "Back");
        back.setOnClickListener(v -> navigation.performBack());
        header.addView(back, new LinearLayout.LayoutParams(UiKit.dp(this, 48), UiKit.dp(this, 48)));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.addView(UiKit.text(this, "Site Tasks", 25, UiKit.TEXT, true));
        title.addView(UiKit.text(this, "Offline task list", 12, UiKit.MUTED, false));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
        titleLp.leftMargin = UiKit.dp(this, 10);
        header.addView(title, titleLp);
        page.addView(header);

        TextView intro = UiKit.text(this,
                "Save site work here. Tasks stay on this phone and are available without internet.",
                14, UiKit.MUTED, false);
        intro.setPadding(0, UiKit.dp(this, 14), 0, UiKit.dp(this, 14));
        page.addView(intro);
        Button add = primaryButton("＋  Add task");
        add.setOnClickListener(v -> showEditor(null));
        page.addView(add, fullButtonLp(48));

        TextView pendingTitle = section("PENDING");
        page.addView(pendingTitle);
        pending = new LinearLayout(this);
        pending.setOrientation(LinearLayout.VERTICAL);
        page.addView(pending);

        TextView completedTitle = section("COMPLETED");
        completedTitle.setPadding(0, UiKit.dp(this, 22), 0, UiKit.dp(this, 8));
        page.addView(completedTitle);
        completed = new LinearLayout(this);
        completed.setOrientation(LinearLayout.VERTICAL);
        page.addView(completed);
        return scroll;
    }

    private void refresh() {
        if (pending == null || completed == null) return;
        pending.removeAllViews();
        completed.removeAllViews();
        List<SiteTaskStore.Task> tasks = SiteTaskStore.list(this);
        int open = 0, done = 0;
        for (SiteTaskStore.Task task : tasks) {
            if (task.completed) { done++; completed.addView(taskCard(task), cardLp()); }
            else { open++; pending.addView(taskCard(task), cardLp()); }
        }
        if (open == 0) pending.addView(emptyCard("No pending tasks"));
        if (done == 0) completed.addView(emptyCard("No completed tasks yet"));
    }

    private View taskCard(SiteTaskStore.Task task) {
        LinearLayout card = card();
        CheckBox check = new CheckBox(this);
        check.setText(task.title);
        check.setTextSize(16);
        check.setTextColor(UiKit.TEXT);
        check.setButtonTintList(ColorStateList.valueOf(UiKit.accent(this)));
        check.setChecked(task.completed);
        check.setOnCheckedChangeListener((b, checked) -> {
            if (checked && !task.reminderId.isEmpty()) ReminderScheduler.cancel(this, task.reminderId);
            SiteTaskStore.setCompleted(this, task.id, checked);
            if (checked && !task.reminderId.isEmpty()) {
                SiteTaskStore.upsert(this, new SiteTaskStore.Task(task.id, task.title, task.note,
                        task.createdAt, true, System.currentTimeMillis(), "", 0L));
            }
            refresh();
        });
        card.addView(check);
        if (!task.note.isEmpty()) {
            TextView note = UiKit.text(this, task.note, 13, UiKit.MUTED, false);
            note.setPadding(UiKit.dp(this, 36), 0, 0, UiKit.dp(this, 4));
            card.addView(note);
        }
        TextView created = UiKit.text(this, "Created " + DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(new Date(task.createdAt)), 11, UiKit.MUTED, false);
        created.setPadding(UiKit.dp(this, 36), 0, 0, UiKit.dp(this, 10));
        card.addView(created);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button edit = secondaryButton("Edit");
        Button delete = secondaryButton("Delete");
        actions.addView(edit, new LinearLayout.LayoutParams(0, UiKit.dp(this, 42), 1));
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(0, UiKit.dp(this, 42), 1);
        delLp.leftMargin = UiKit.dp(this, 8);
        actions.addView(delete, delLp);
        edit.setOnClickListener(v -> showEditor(task));
        delete.setOnClickListener(v -> {
            AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(this)
                    .setTitle("Delete task?")
                    .setMessage("This task will be removed from the list.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        if (!task.reminderId.isEmpty()) ReminderScheduler.cancel(this, task.reminderId);
                        SiteTaskStore.remove(this, task.id);
                        refresh();
                    });
            AlertDialog deleteDialog = deleteBuilder.create();
            UiKit.styleOrbitDialog(deleteDialog, this, false);
            deleteDialog.show();
        });
        card.addView(actions);
        Button reminder = secondaryButton(task.reminderAt > 0L
                ? "Reminder: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(task.reminderAt))
                : "Set reminder");
        LinearLayout.LayoutParams reminderLp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, 42));
        reminderLp.topMargin = UiKit.dp(this, 8);
        card.addView(reminder, reminderLp);
        reminder.setEnabled(!task.completed);
        reminder.setOnClickListener(v -> chooseReminderTime(task));
        return card;
    }

    private void showEditor(SiteTaskStore.Task existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(UiKit.dp(this, 4), UiKit.dp(this, 4), UiKit.dp(this, 4), 0);
        EditText title = new EditText(this);
        title.setSingleLine(true);
        title.setHint("Task title (Bangla or English)");
        title.setText(existing == null ? "" : existing.title);
        form.addView(title, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 52)));
        EditText note = new EditText(this);
        note.setHint("Site / details (optional)");
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        note.setText(existing == null ? "" : existing.note);
        form.addView(note, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 90)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add site task" : "Edit task")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        UiKit.styleOrbitDialog(dialog, this, false, () -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = title.getText().toString().trim();
            if (name.isEmpty()) {
                title.setError("Task title required");
                return;
            }
            SiteTaskStore.Task saved = existing == null
                    ? SiteTaskStore.create(name, note.getText().toString())
                    : new SiteTaskStore.Task(existing.id, name, note.getText().toString(),
                        existing.createdAt, existing.completed, existing.completedAt,
                        existing.reminderId, existing.reminderAt);
            if (SiteTaskStore.upsert(this, saved)) {
                dialog.dismiss();
                refresh();
            } else Toast.makeText(this, "Could not save task", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void chooseReminderTime(SiteTaskStore.Task task) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog date = new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, day, 9, 0, 0);
            chosen.set(Calendar.MILLISECOND, 0);
            TimePickerDialog time = new TimePickerDialog(this, (view, hour, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hour);
                chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0);
                long trigger = chosen.getTimeInMillis();
                if (trigger <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Choose a future time", Toast.LENGTH_SHORT).show();
                    return;
                }
                String id = task.reminderId.isEmpty() ? "site-task-" + task.id : task.reminderId;
                ReminderStore.Item item = new ReminderStore.Item(id, "Site task: " + task.title,
                        trigger, System.currentTimeMillis());
                ReminderScheduler.ScheduleResult result = ReminderScheduler.schedule(this, item);
                if (!result.scheduled) {
                    Toast.makeText(this, "Could not schedule reminder", Toast.LENGTH_LONG).show();
                    return;
                }
                SiteTaskStore.Task updated = new SiteTaskStore.Task(task.id, task.title, task.note,
                        task.createdAt, task.completed, task.completedAt, id, trigger);
                if (!SiteTaskStore.upsert(this, updated)) {
                    ReminderScheduler.cancel(this, id);
                    Toast.makeText(this, "Reminder scheduled but task update failed; alarm cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, result.exact ? "Reminder scheduled" : "Approximate reminder scheduled",
                            Toast.LENGTH_SHORT).show();
                    refresh();
                }
            }, 9, 0, false);
            time.show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        date.show();
    }

    private TextView section(String text) {
        TextView v = UiKit.text(this, text, 11, UiKit.MUTED, true);
        v.setLetterSpacing(.12f);
        v.setPadding(0, UiKit.dp(this, 22), 0, UiKit.dp(this, 8));
        return v;
    }
    private View emptyCard(String text) {
        LinearLayout c = card();
        c.addView(UiKit.text(this, text, 14, UiKit.MUTED, false));
        return c;
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 14));
        c.setBackground(UiKit.outlined(UiKit.SURFACE, UiKit.withAlpha(UiKit.accent(this), 36), 18, this));
        return c;
    }
    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = UiKit.dp(this, 9);
        return lp;
    }
    private LinearLayout.LayoutParams fullButtonLp(int height) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, UiKit.dp(this, height));
        lp.topMargin = UiKit.dp(this, 4);
        return lp;
    }
    private Button primaryButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false); b.setTextColor(UiKit.TEXT); b.setTextSize(14);
        b.setBackground(UiKit.ripple(UiKit.accent(this), UiKit.TEXT, 16, this));
        return b;
    }
    private Button secondaryButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false); b.setTextSize(13); b.setTextColor(UiKit.accent(this));
        b.setMinHeight(0); b.setMinimumHeight(0); b.setStateListAnimator(null);
        b.setBackground(UiKit.rippleOutlined(UiKit.SURFACE_2, UiKit.withAlpha(UiKit.accent(this), 72),
                UiKit.accent(this), 14, this));
        UiKit.pressScale(b);
        return b;
    }
    private ImageButton iconButton(int res, String desc) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(res); b.setImageTintList(ColorStateList.valueOf(UiKit.accent(this)));
        b.setBackground(UiKit.ripple(UiKit.SURFACE, UiKit.accent(this), 18, this));
        b.setContentDescription(desc);
        b.setPadding(UiKit.dp(this, 11), UiKit.dp(this, 11), UiKit.dp(this, 11), UiKit.dp(this, 11));
        UiKit.pressScale(b);
        return b;
    }
}
