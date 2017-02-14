/**
 *  @version 1.0
 * COPYRIGHTS COPELABS/ULHT, LGPLv3.0, 2017-02-14
 * Main Activity of the NDN-Opp app.
 * @author Seweryn Dynerowicz (COPELABS/ULHT)
 */
package pt.ulusofona.copelabs.ndn.android.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;

import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuInflater;

import android.widget.Switch;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import pt.ulusofona.copelabs.ndn.R;

import pt.ulusofona.copelabs.ndn.android.CsEntry;
import pt.ulusofona.copelabs.ndn.android.Face;
import pt.ulusofona.copelabs.ndn.android.FibEntry;
import pt.ulusofona.copelabs.ndn.android.Name;

import pt.ulusofona.copelabs.ndn.android.PitEntry;
import pt.ulusofona.copelabs.ndn.android.SctEntry;
import pt.ulusofona.copelabs.ndn.android.service.ForwardingDaemon;

import pt.ulusofona.copelabs.ndn.android.ui.dialog.AddRoute;
import pt.ulusofona.copelabs.ndn.android.ui.dialog.CreateFace;

import pt.ulusofona.copelabs.ndn.android.ui.fragment.Table;

public class Main extends AppCompatActivity {
    // ForwardingDaemon service
    private Intent mDaemonIntent;
    private IntentFilter mDaemonBroadcastedIntents;
    private BroadcastReceiver mReceiver;
    private ForwardingDaemon mDaemon;

    private Switch mNfdSwitch;

	// Fragments
	private Table<Name> mNametree;
	private Table<CsEntry> mContentStore;

    private WifiP2p mWifiP2p;
    private Overview mOverview;
	private ForwarderConfiguration mFwdCfg;

	// Navigation
	private ListView mNavigation;
	private String mNavigationEntries[];

	private Fragment mCurrent;
    private int mSelection;

	// For automatic refreshing of screen.
	private static final int UPDATE_DELAY = 1000;
	private final Handler mUpdater = new Handler();
	private final Runnable mRefresher = new Runnable() {
		@Override
		public void run() {
            /* @TODO: this runs all the time. Even when the Daemon is stopped. */
			refresh();
			mUpdater.postDelayed(this, UPDATE_DELAY);
           }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mDaemonIntent = new Intent(getApplicationContext(), ForwardingDaemon.class);
        mDaemonBroadcastedIntents = new IntentFilter();
        mDaemonBroadcastedIntents.addAction(ForwardingDaemon.STARTED);
        mDaemonBroadcastedIntents.addAction(ForwardingDaemon.STOPPED);
        mReceiver = new BroadcastReceiver();

        mNametree = new Table<>();
        mNametree.setArguments(Name.TABLE_ARGUMENTS);

		mContentStore = new Table<>();
        mContentStore.setArguments(CsEntry.TABLE_ARGUMENTS);

        mWifiP2p = new WifiP2p();
		mOverview = new Overview();
		mFwdCfg = new ForwarderConfiguration();

		mNfdSwitch = (Switch) findViewById(R.id.nfdSwitch);
		mNfdSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isOn) {
				if(isOn) startService(mDaemonIntent);
				else {
                    if(mConnection != null)
                        unbindService(mConnection);
                    stopService(mDaemonIntent);
                }
			}
		});

		mNavigationEntries = getResources().getStringArray(R.array.navigationEntries);
		mNavigation = (ListView) findViewById(R.id.navigation);
		mNavigation.setAdapter(
			new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mNavigationEntries));
		mNavigation.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				select(position);
				mNavigation.setItemChecked(position, true);
				((DrawerLayout) findViewById(R.id.root)).closeDrawer(mNavigation);
			}
		});

		select(1);
	}

    private void clear() {
        mWifiP2p.clear();
        mOverview.clear();
        mFwdCfg.clear();
        mNametree.clear();
        mContentStore.clear();
    }

    private void refresh() {
        List<Face> faces;
		if(mDaemon != null) {
			switch (mSelection) {
                case 0:
                    mWifiP2p.refresh(mDaemon.getUmobilePeers());
                    break;
				case 1:
                    faces = mDaemon.getFaceTable();
                    List<PitEntry> pit = mDaemon.getPendingInterestTable();
                    Collections.sort(faces); Collections.sort(pit);
					mOverview.refresh(mDaemon.getVersion(), mDaemon.getUptime(), faces, pit);
					break;
				case 2:
                    faces = mDaemon.getFaceTable();
                    List<FibEntry> fib = mDaemon.getForwardingInformationBase();
                    List<SctEntry> sct = mDaemon.getStrategyChoiceTable();
                    Collections.sort(faces); Collections.sort(fib); Collections.sort(sct);
					mFwdCfg.refresh(faces, fib, sct);
					break;
				case 3:
                    List<Name> nametree = mDaemon.getNameTree();
                    Collections.sort(nametree);
					mNametree.refresh(nametree);
					break;
				case 4:
                    List<CsEntry> contentStore = mDaemon.getContentStore();
                    Collections.sort(contentStore);
					mContentStore.refresh(mDaemon.getContentStore());
					break;
			}
		}
    }

	private void select(int position) {
		switch(position) {
            case 0: mCurrent = mWifiP2p; break;
            case 1: mCurrent = mOverview; break;
		    case 2: mCurrent = mFwdCfg; break;
		    case 3: mCurrent = mNametree; break;
		    case 4: mCurrent = mContentStore; break;
		}

		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.container, mCurrent)
			.commit();
        mSelection = position;

		getSupportActionBar().setTitle(mNavigationEntries[position]);
	}

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mDaemonBroadcastedIntents);
        mUpdater.post(mRefresher);
    }

    @Override
	protected void onPause() {
        unregisterReceiver(mReceiver);
		mUpdater.removeCallbacks(mRefresher);
        super.onPause();
	}

    @Override
    protected void onDestroy() {
        if(mConnection != null) unbindService(mConnection);
        stopService(mDaemonIntent);
        super.onDestroy();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		    case R.id.createFace:
			    CreateFace cf = new CreateFace(mDaemon);
			    cf.setTargetFragment(mCurrent, 0);
			    cf.show(getSupportFragmentManager(), cf.getTag());
			    break;
            case R.id.addRoute:
                AddRoute ar = new AddRoute(mDaemon);
                ar.setTargetFragment(mCurrent, 0);
                ar.show(getSupportFragmentManager(), ar.getTag());
                break;
        }
		return true;
	}

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName cn, IBinder bndr) {
            mDaemon = ((ForwardingDaemon.DaemonBinder) bndr).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName cn) {
            mDaemon = null;
        }
    };

    private class BroadcastReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
			if(action.equals(ForwardingDaemon.STARTED)) {
                mUpdater.post(mRefresher);
                mNfdSwitch.setChecked(true);
                bindService(mDaemonIntent, mConnection, Context.BIND_AUTO_CREATE);
            } else if (action.equals(ForwardingDaemon.STOPPED)) {
                mUpdater.removeCallbacks(mRefresher);
                clear();
                mNfdSwitch.setChecked(false);
            }
        }
    }
}