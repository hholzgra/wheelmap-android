/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.activity;

import com.google.inject.Inject;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.LayoutParams;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.holoeverywhere.app.Activity;
import org.wheelmap.android.activity.MyTabListener.OnStateListener;
import org.wheelmap.android.activity.MyTabListener.TabHolder;
import org.wheelmap.android.fragment.DisplayFragment;
import org.wheelmap.android.fragment.DisplayFragmentListener;
import org.wheelmap.android.fragment.ErrorDialogFragment;
import org.wheelmap.android.fragment.POIsListFragment;
import org.wheelmap.android.fragment.POIsMapWorkerFragment;
import org.wheelmap.android.fragment.POIsOsmdroidFragment;
import org.wheelmap.android.fragment.WorkerFragmentListener;
import org.wheelmap.android.manager.MyLocationManager;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.PrepareDatabaseHelper;
import org.wheelmap.android.modules.IAppProperties;
import org.wheelmap.android.online.R;
import org.wheelmap.android.service.RestServiceException;
import org.wheelmap.android.tracker.TrackerWrapper;

import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

import de.akquinet.android.androlog.Log;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshAttacher;

@Activity.Addons(value = {Activity.ADDON_SHERLOCK, "MyRoboguice"})
public class MainSinglePaneActivity extends MapActivity implements
        DisplayFragmentListener, WorkerFragmentListener, OnStateListener,
        PullToRefreshAttacher.OnRefreshListener {

    private static final String TAG = MainSinglePaneActivity.class.getSimpleName();


    @Inject
    IAppProperties appProperties;

    private MyTabListener mTabListener;

    private final static int DEFAULT_SELECTED_TAB = 0;

    private int mSelectedTab = DEFAULT_SELECTED_TAB;

    private TrackerWrapper mTrackerWrapper;

    public boolean mFirstStart;

    private PullToRefreshAttacher mPullToRefreshHelper;

    private TabHolder mActiveTabHolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setSupportProgressBarIndeterminateVisibility(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setContentView(R.layout.activity_frame_empty);
        FragmentManager.enableDebugLogging(true);

        mTrackerWrapper = new TrackerWrapper(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        createSearchModeCustomView(actionBar);

        mTabListener = new MyTabListener(this);

        Tab tab = actionBar
                .newTab()
                .setText(R.string.title_pois_list)
                .setIcon(
                        getResources().getDrawable(
                                R.drawable.ic_location_list_wheelmap))
                .setTag(POIsListFragment.TAG)
                .setTabListener(mTabListener);
        actionBar.addTab(tab, MyTabListener.TAB_LIST, false);

        tab = actionBar
                .newTab()
                .setText(R.string.title_pois_map)
                .setIcon(
                        getResources().getDrawable(
                                R.drawable.ic_location_map_wheelmap))
                .setTag(POIsOsmdroidFragment.TAG)
                .setTabListener(mTabListener);
        actionBar.addTab(tab, MyTabListener.TAB_MAP, false);

        if (savedInstanceState != null) {
            executeState(savedInstanceState);
        } else {
            executeDefaultInstanceState();
        }

        configureRefresh();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        executeIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void executeIntent(Intent intent) {
        Log.d(TAG, "executeIntent intent = " + intent);
        Bundle extras = intent.getExtras();
        if (extras == null || (!mFirstStart && extras.containsKey(Extra.REQUEST))) {
            return;
        }

        executeState(extras);
    }

    private void executeState(Bundle state) {
        mSelectedTab = state.getInt(Extra.SELECTED_TAB, DEFAULT_SELECTED_TAB);
        mFirstStart = false;

        TabHolder holder = TabHolder.findActiveHolderByTab(mSelectedTab);
        holder.setExecuteBundle(state);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem(mSelectedTab);
    }

    private void executeDefaultInstanceState() {
        mSelectedTab = DEFAULT_SELECTED_TAB;
        mFirstStart = true;
        ActionBar actionBar = getSupportActionBar();
        Log.d( TAG, "executeDefaultInstanceState: selectedNavigationIndex = " + actionBar.getSelectedNavigationIndex());
        if ( actionBar.getSelectedNavigationIndex() != mSelectedTab) {
            actionBar.setSelectedNavigationItem(mSelectedTab);
        }
    }

    public void onStateChange(String tag) {
        if (tag == null) {
            return;
        }

        Log.d(TAG, "onStateChange " + tag);
        mActiveTabHolder = mTabListener.getTabHolder(tag);

        mSelectedTab = getSupportActionBar().getSelectedNavigationIndex();
        String readableName = tag.replaceAll("Fragment", "");
        mTrackerWrapper.track(readableName);

        getSupportActionBar().setDisplayShowCustomEnabled(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Extra.SELECTED_TAB, mSelectedTab);
        super.onSaveInstanceState(outState);
    }

    private void configureRefresh() {
        // As we're modifying some of the options, create an instance of
        // PullToRefreshAttacher.Options
        PullToRefreshAttacher.Options ptrOptions = new PullToRefreshAttacher.Options();

        // Here we make the refresh scroll distance to 75% of the GridView height
        ptrOptions.refreshScrollDistance = 0.75f;

        // Here we customise the animations which are used when showing/hiding the header view
        // ptrOptions.headerInAnimation = R.anim.slide_in_top;
        // ptrOptions.headerOutAnimation = R.anim.slide_out_top;

        // Here we define a custom header layout which will be inflated and used
        ptrOptions.headerLayout = R.layout.ptr_header;

        mPullToRefreshHelper = PullToRefreshAttacher.get(this, ptrOptions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.ab_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_filter:
                showFilterSettings();
                return true;
            case R.id.menu_about:
                showInfo();
                return true;
            case R.id.menu_new_poi:
                createNewPoi();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createSearchModeCustomView(final ActionBar bar) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View customView = inflater.inflate(R.layout.item_ab_searchmodebutton,
                null);
        ImageButton button = (ImageButton) customView.findViewById(R.id.image);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Fragment f = getSupportFragmentManager().findFragmentByTag(
                        POIsMapWorkerFragment.TAG);
                if (f == null) {
                    return;
                }

                ((POIsMapWorkerFragment) f).setSearchMode(false);
                bar.setDisplayShowCustomEnabled(false);
            }
        });

        bar.setCustomView(customView, new ActionBar.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    private void showInfo() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    private void showFilterSettings() {
        Intent intent = new Intent(this, NewSettingsActivity.class);
        startActivity(intent);
    }

    private long insertNewPoi() {
        Location location = MyLocationManager.getLastLocation();
        String name = getString(R.string.poi_new_default_name);

        return PrepareDatabaseHelper.insertNew(getContentResolver(), name,
                location.getLatitude(), location.getLongitude());
    }

    private void createNewPoi() {
        long poiId = insertNewPoi();
        Intent i = new Intent(this, POIDetailEditableActivity.class);
        i.putExtra(Extra.POI_ID, poiId);
        startActivity(i);
    }

    @Override
    public void onError(RestServiceException e) {

        if (e.isNetworkError()) {
            Crouton.makeText(this, e.getRessourceString(), Style.ALERT).show();
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        ErrorDialogFragment errorDialog = ErrorDialogFragment.newInstance(e,
                Extra.UNKNOWN);
        if (errorDialog == null) {
            return;
        }

        errorDialog.show(fm);
    }

    @Override
    public void onShowDetail(Fragment fragment, ContentValues values) {
        long copyId = PrepareDatabaseHelper.createCopyFromContentValues(
                getContentResolver(), values, false);
        Intent intent = new Intent(this, POIDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(Extra.POI_ID, copyId);
        startActivity(intent);
    }

    @Override
    public void onRefreshing(boolean isRefreshing) {
        Log.d(TAG, "onRefreshing isRefreshing = " + isRefreshing);
        mPullToRefreshHelper.setRefreshing(isRefreshing);
    }

    @Override
    public void onSearchModeChange(boolean isSearchMode) {
        Log.d(TAG, "onSearchModeChange: showing custom view in actionbar");
        getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    @Override
    public void refreshRegisterList(ListView listView) {
        mPullToRefreshHelper.addRefreshableView(listView, this);
    }

    @Override
    public void onRefreshEnabled(boolean refreshEnabled) {
        mPullToRefreshHelper.setEnabled(refreshEnabled);
    }

    @Override
    public void onRefreshStarted(View view) {
        DisplayFragment f = (DisplayFragment) mActiveTabHolder.fragment;
        if ( f != null) {
            f.onRefreshStarted();
        }
    }
}
