package com.firebirdberlin.tinytimetracker.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

import de.greenrobot.event.EventBus;

import com.firebirdberlin.tinytimetracker.R;
import com.firebirdberlin.tinytimetracker.LogDataSource;
import com.firebirdberlin.tinytimetracker.LogSummaryAdapter;
import com.firebirdberlin.tinytimetracker.TinyTimeTracker;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryChanged;
import com.firebirdberlin.tinytimetracker.events.OnLogEntryDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerDeleted;
import com.firebirdberlin.tinytimetracker.events.OnTrackerSelected;
import com.firebirdberlin.tinytimetracker.events.OnWifiUpdateCompleted;
import com.firebirdberlin.tinytimetracker.models.LogDailySummary;
import com.firebirdberlin.tinytimetracker.models.LogSummary;
import com.firebirdberlin.tinytimetracker.models.TrackerEntry;
import com.firebirdberlin.tinytimetracker.models.UnixTimestamp;

public class CardFragment extends Fragment {
    private static String TAG = TinyTimeTracker.TAG + ".CardFragment";
    private TrackerEntry currentTracker = null;
    private Context mContext = null;
    private EventBus bus = EventBus.getDefault();
    private RecyclerView recyclerView = null;
    private LogSummaryAdapter logSummaryAdapter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(TAG, "onCreateView()");
        mContext = (Context) getActivity();

        View v = inflater.inflate(R.layout.recycler_view, container, false);
        recyclerView = (RecyclerView) v.findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(mContext);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        logSummaryAdapter = new LogSummaryAdapter(getData());
        recyclerView.setAdapter(logSummaryAdapter);

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume()");
        bus.register(this);

        OnTrackerSelected event = bus.getStickyEvent(OnTrackerSelected.class);
        if ( event != null ) {
            Log.i(TAG, event.newTracker.verbose_name);
            this.currentTracker = event.newTracker;
            refresh();
        }
    }

    private void refresh() {
        if ( this.currentTracker == null ) {
            return;
        }
        logSummaryAdapter = new LogSummaryAdapter(getData());
        recyclerView.swapAdapter(logSummaryAdapter, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    private List<LogSummary> getData() {
        List<LogSummary> result = new ArrayList<LogSummary>();

        if (currentTracker != null) {
            UnixTimestamp start = UnixTimestamp.startOfToday();
            start.set(Calendar.DAY_OF_YEAR, 1);
            start.add(Calendar.YEAR, -1);
            List< Pair<Long, Long> > values = fetchData(start);

            long workingHoursInSeconds = (int) (currentTracker.working_hours * 3600.f);
            int weekOfYear = -1;
            LogSummary summary = null;
            for (Pair<Long, Long> e : values) {
                UnixTimestamp timestamp = new UnixTimestamp(e.first.longValue());
                UnixTimestamp duration = new UnixTimestamp(e.second.longValue());

                LogDailySummary logEntry = new LogDailySummary(e.first.longValue(), e.second.longValue());
                logEntry.calculateSaldo(workingHoursInSeconds);

                int currentWeek = timestamp.getWeekOfYear();
                if ( currentWeek != weekOfYear ) {
                    weekOfYear = currentWeek;
                    if (summary != null) {
                        Collections.reverse(summary.dailySummaries);
                        result.add(summary);
                        summary = null;
                    }
                    summary = new LogSummary(currentTracker);
                }
                summary.dailySummaries.add(logEntry);
            }

            if (summary != null) {
                Collections.reverse(summary.dailySummaries);
                result.add(summary);
                summary = null;
            }
        }
        return result;
    }

    private List< Pair<Long, Long> > fetchData(UnixTimestamp start_timestamp) {
        LogDataSource datasource = new LogDataSource(mContext);
        List< Pair<Long, Long> > values = datasource.getTotalDurationAggregated(
                currentTracker.id, LogDataSource.AGGRETATION_DAY, start_timestamp.getTimestamp());
        datasource.close();
        return values;
    }

    private void updateCurrentWeek() {
        if ( currentTracker == null ) return;
        UnixTimestamp start = UnixTimestamp.startOfWeek();
        List< Pair<Long, Long> > values = fetchData(start);
        long workingHoursInSeconds = (int) (currentTracker.working_hours * 3600.f);
        LogSummary summary = new LogSummary(currentTracker);
        for (Pair<Long, Long> e : values) {
            UnixTimestamp timestamp = new UnixTimestamp(e.first.longValue());
            UnixTimestamp duration = new UnixTimestamp(e.second.longValue());
            LogDailySummary logEntry = new LogDailySummary(e.first.longValue(), e.second.longValue());
            logEntry.calculateSaldo(workingHoursInSeconds);
            summary.dailySummaries.add(logEntry);
        }

        Collections.reverse(summary.dailySummaries);
        LogSummaryAdapter adapter = (LogSummaryAdapter) recyclerView.getAdapter();

        int position = adapter.replace(summary);
        if ( position >= 0 ) {
            adapter.notifyItemChanged(position);
        } else {
            adapter.add(0, summary);
            adapter.notifyItemInserted(0);
        }
    }

    public void onEvent(OnTrackerSelected event) {
        Log.i(TAG, "OnTrackerSelected");
        this.currentTracker = event.newTracker;
        refresh();
    }

    public void onEvent(OnWifiUpdateCompleted event) {
        if ( currentTracker == null ) return;
        if (event.success && currentTracker.equals(event.tracker)) {
            updateCurrentWeek();
        }
    }

    public void onEvent(OnTrackerDeleted event) {
        this.currentTracker = null;
        refresh();
    }

    public void onEvent(OnLogEntryChanged event) {
        if ( currentTracker != null && currentTracker.id == event.entry.tracker_id ) {
            refresh();
        }
    }

    public void onEvent(OnLogEntryDeleted event) {
        if ( currentTracker != null ) {
            refresh();
        }
    }
}
