package com.matburt.mobileorg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import android.net.Uri;

import com.matburt.mobileorg.Gui.Outline.OutlineAdapter;
import com.matburt.mobileorg.Gui.Wizard.WizardActivity;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.util.PreferenceUtils;
import com.matburt.mobileorg.Services.SyncService;


/**
 * An activity representing a list of OrgNodes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link OrgNodeDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class OrgNodeListActivity extends AppCompatActivity {

    public final static String NODE_ID = "node_id";
    private final static String OUTLINE_NODES = "nodes";
    private final static String OUTLINE_CHECKED_POS = "selection";
    private final static String OUTLINE_SCROLL_POS = "scrollPosition";

    public final static String SYNC_FAILED = "com.matburt.mobileorg.SYNC_FAILED";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    private Long node_id;

    private SynchServiceReceiver syncReceiver;
    private MenuItem synchronizerMenuItem;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orgnode_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        Intent intent = getIntent();
        node_id = intent.getLongExtra(NODE_ID, -1);

        if (this.node_id == -1) displayNewUserDialogs();

        recyclerView = (RecyclerView) findViewById(R.id.orgnode_list);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        if (findViewById(R.id.orgnode_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            ((OutlineAdapter)recyclerView.getAdapter()).setHasTwoPanes(true);
        }

        this.syncReceiver = new SynchServiceReceiver();
        registerReceiver(this.syncReceiver, new IntentFilter(
                Synchronizer.SYNC_UPDATE));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), EditNodeActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.outline_menu, menu);
        synchronizerMenuItem = menu.findItem(R.id.menu_sync);

        return true;
    }

    // TODO: Add onSaveInstanceState and onRestoreInstanceState

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                listView.collapseCurrent();
                return true;

            case R.id.menu_sync:
                runSynchronize(null);
                return true;

            case R.id.menu_settings:
                runShowSettings(null);
                return true;

            case R.id.menu_outline:
                runExpandableOutline(-1);
                return true;

            case R.id.menu_capturechild:
//                OutlineActionMode.runCaptureActivity(listView.getCheckedNodeId(), this);
                return true;

            case R.id.menu_search:
                return runSearch();

            case R.id.menu_help:
                runHelp(null);
                return true;
        }
        return false;
    }

    public void runHelp(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
        startActivity(intent);
    }

    public void runSynchronize(View view) {
        startService(new Intent(this, SyncService.class));
    }

    public void runShowSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void runShowWizard(View view) {
        startActivity(new Intent(this, WizardActivity.class));
    }


    private void runExpandableOutline(long id) {
        Intent intent = new Intent(this, OrgNodeListActivity.class);
        intent.putExtra(OrgNodeListActivity.NODE_ID, id);
        startActivity(intent);
    }

    private boolean runSearch() {
        return onSearchRequested();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new OutlineAdapter(this));
    }

    private void showUpgradePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(OrgUtils.getStringFromResource(R.raw.upgrade, this));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }


    private void displayNewUserDialogs() {
        if (PreferenceUtils.isSyncConfigured() == false)
            runShowWizard(null);

        if (PreferenceUtils.isUpgradedVersion())
            showUpgradePopup();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(this.syncReceiver);
        super.onDestroy();
    }

    public void refreshDisplay() {
//        this.listView.refresh();
        refreshTitle();
    }


    private void refreshTitle() {
        this.getSupportActionBar().setTitle("MobileOrg " + getChangesString());
    }

    private String getChangesString() {
        int changes = OrgProviderUtils.getChangesCount(getContentResolver());
        if(changes > 0)
            return "[" + changes + "]";
        else
            return "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTitle();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(SYNC_FAILED)) {
            Bundle extrasBundle = intent.getExtras();
            String errorMsg = extrasBundle.getString("ERROR_MESSAGE");
            showSyncFailPopup(errorMsg);
        }
    }

    private void showSyncFailPopup(String errorMsg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMsg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }


    private class SynchServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean syncStart = intent.getBooleanExtra(Synchronizer.SYNC_START, false);
            boolean syncDone = intent.getBooleanExtra(Synchronizer.SYNC_DONE, false);
            boolean showToast = intent.getBooleanExtra(Synchronizer.SYNC_SHOW_TOAST, false);
            int progress = intent.getIntExtra(Synchronizer.SYNC_PROGRESS_UPDATE, -1);

            if(syncStart) {
                synchronizerMenuItem.setVisible(false);
            } else if (syncDone) {
                ((OutlineAdapter)recyclerView.getAdapter()).refresh();
                synchronizerMenuItem.setVisible(true);

                if (showToast)
                    Toast.makeText(context,
                            R.string.sync_successful,
                            Toast.LENGTH_SHORT).show();
            } else if (progress >= 0 && progress <= 100) {
//                int normalizedProgress = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * progress;
            }
        }
    }
}
