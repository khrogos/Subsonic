/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2010 (C) Sindre Mehus
 */
package github.daneren2005.dsub.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import github.daneren2005.dsub.R;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.TabBackgroundTask;

public abstract class SelectListFragment<T> extends SubsonicFragment implements AdapterView.OnItemClickListener {
	private static final String TAG = SelectListFragment.class.getSimpleName();
	protected ListView listView;
	protected ArrayAdapter adapter;
	protected BackgroundTask<List<T>> currentTask;
	protected List<T> objects;
	protected boolean serialize = true;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		if(bundle != null && serialize) {
			objects = (List<T>) bundle.getSerializable(Constants.FRAGMENT_LIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(serialize) {
			outState.putSerializable(Constants.FRAGMENT_LIST, (Serializable) objects);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.abstract_list_fragment, container, false);

		refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
		refreshLayout.setOnRefreshListener(this);

		listView = (ListView)rootView.findViewById(R.id.fragment_list);
		listView.setOnItemClickListener(this);
		setupScrollList(listView);
		registerForContextMenu(listView);

		if(objects == null) {
			refresh(false);
		} else {
			listView.setAdapter(adapter = getAdapter(objects));
		}

		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		if(!primaryFragment) {
			return;
		}

		menuInflater.inflate(getOptionsMenu(), menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void refresh(final boolean refresh) {
		int titleRes = getTitleResource();
		if(titleRes != 0) {
			setTitle(getTitleResource());
		}
		listView.setVisibility(View.GONE);
		
		// Cancel current running task before starting another one
		if(currentTask != null) {
			currentTask.cancel();
		}

		currentTask = new TabBackgroundTask<List<T>>(this) {
			@Override
			protected List<T> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);

				objects = new ArrayList<T>();

				try {
					objects = getObjects(musicService, refresh, this);
				} catch (Exception x) {
					Log.e(TAG, "Failed to load", x);
				}

				return objects;
			}

			@Override
			protected void done(List<T> result) {
				if (result != null && !result.isEmpty()) {
					// Toggle fast scroll to get around issue when length of list changes
					listView.setFastScrollEnabled(false);
					listView.setAdapter(adapter = getAdapter(result));
					listView.setFastScrollEnabled(true);
					
					onFinishRefresh();
					listView.setVisibility(View.VISIBLE);
				} else {
					setEmpty(true);
				}
				
				currentTask = null;
			}
		};
		currentTask.execute();
	}

	public abstract int getOptionsMenu();
	public abstract ArrayAdapter getAdapter(List<T> objs);
	public abstract List<T> getObjects(MusicService musicService, boolean refresh, ProgressListener listener) throws Exception;
	public abstract int getTitleResource();
	
	public void onFinishRefresh() {
		
	}
}
