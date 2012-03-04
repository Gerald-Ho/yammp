/*
 *  YAMMP - Yet Another Multi Media Player for android
 *  Copyright (C) 2011-2012  Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This file is part of YAMMP.
 *
 *  YAMMP is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAMMP is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with YAMMP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.yammp.app;

import java.util.ArrayList;

import org.mariotaku.actionbarcompat.app.ActionBarCompat;
import org.mariotaku.actionbarcompat.app.FragmentActivity;
import org.yammp.Constants;
import org.yammp.IMusicPlaybackService;
import org.yammp.R;
import org.yammp.dialog.ScanningProgress;
import org.yammp.util.MusicUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceToken;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MusicBrowserActivity extends FragmentActivity implements Constants,
		ViewSwitcher.ViewFactory, ServiceConnection {

	private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		public Drawable doInBackground(Void... params) {

			if (mService != null) {
				try {
					Bitmap bitmap = MusicUtils.getArtwork(getApplicationContext(),
							mService.getAudioId(), mService.getAlbumId());
					if (bitmap == null) return null;
					int value = 0;
					if (bitmap.getHeight() <= bitmap.getWidth()) {
						value = bitmap.getHeight();
					} else {
						value = bitmap.getWidth();
					}
					Bitmap result = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - value) / 2,
							(bitmap.getHeight() - value) / 2, value, value);
					return new BitmapDrawable(getResources(), result);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void onPostExecute(Drawable result) {
			if (mAlbumArt != null) {
				if (result != null) {
					mAlbumArt.setImageDrawable(result);
				} else {
					mAlbumArt.setImageResource(R.drawable.ic_mp_albumart_unknown);
				}
			}
		}
	}

	private class TabsAdapter extends FragmentPagerAdapter implements TitleProvider {

		private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
		private final ArrayList<String> mTitles = new ArrayList<String>();

		public TabsAdapter(FragmentManager manager) {
			super(manager);
		}

		public void addFragment(Fragment fragment, String name) {
			mFragments.add(fragment);
			mTitles.add(name);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

		@Override
		public String getTitle(int position) {
			return mTitles.get(position);
		}

	}

	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private ServiceToken mToken;
	private IMusicPlaybackService mService;
	private PreferencesEditor mPrefs;
	private ImageSwitcher mAlbumArt;
	private TextView mTrackName, mTrackDetail;
	private ImageButton mPlayPauseButton, mNextButton;

	private AsyncAlbumArtLoader mAlbumArtLoader;

	private TitlePageIndicator mIndicator;

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (BROADCAST_META_CHANGED.equals(intent.getAction())
					|| BROADCAST_META_CHANGED.equals(intent.getAction())) {
				updateNowplaying();
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(intent.getAction())) {
				updatePlayPauseButton();
			}

		}

	};

	private OnClickListener mActionBarClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				if (mService.getAudioId() > -1 || mService.getPath() != null) {
					Intent intent = new Intent(INTENT_PLAYBACK_VIEWER);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} else {
					MusicUtils.shuffleAll(getApplicationContext());
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnClickListener mPlayPauseClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnClickListener mNextClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mService == null) return;
			try {
				mService.next();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	};

	private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}

		@Override
		public void onPageSelected(int position) {

			mPrefs.setIntState(STATE_KEY_CURRENTTAB, position);
		}
	};

	@Override
	public View makeView() {
		ImageView i = new ImageView(this);
		i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		i.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT));
		return i;
	}

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mPrefs = new PreferencesEditor(getApplicationContext());

		String mount_state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(mount_state)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
			startActivity(new Intent(this, ScanningProgress.class));
			finish();
		}

		configureActivity();
		configureTabs(icicle);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.music_browser, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case GOTO_PLAYBACK:
				intent = new Intent(INTENT_PLAYBACK_VIEWER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
			case SHUFFLE_ALL:
				MusicUtils.shuffleAll(getApplicationContext());
				break;
			case SETTINGS:
				intent = new Intent(INTENT_MUSIC_SETTINGS);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMusicPlaybackService.Stub.asInterface(service);
		updateNowplaying();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		filter.addAction(BROADCAST_PLAYSTATE_CHANGED);
		registerReceiver(mMediaStatusReceiver, filter);

	}

	@Override
	public void onStop() {

		unregisterReceiver(mMediaStatusReceiver);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	private void configureActivity() {

		setContentView(R.layout.music_browser);

		ActionBarCompat mActionBar = getActionBarCompat();

		mActionBar.setCustomView(R.layout.actionbar_music_browser);

		View mCustomView = mActionBar.getCustomView();

		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);

		mCustomView.setOnClickListener(mActionBarClickListener);

		mAlbumArt = (ImageSwitcher) mCustomView.findViewById(R.id.album_art);
		mAlbumArt.setFactory(this);
		mTrackName = (TextView) mCustomView.findViewById(R.id.track_name);
		mTrackDetail = (TextView) mCustomView.findViewById(R.id.track_detail);
		mPlayPauseButton = (ImageButton) mCustomView.findViewById(R.id.play_pause);
		mPlayPauseButton.setOnClickListener(mPlayPauseClickListener);
		mNextButton = (ImageButton) mCustomView.findViewById(R.id.next);
		mNextButton.setOnClickListener(mNextClickListener);

		mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);

		mIndicator = (TitlePageIndicator) mCustomView.findViewById(R.id.indicator);
		if (mIndicator == null) {
			mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
		}

	}

	private void configureTabs(Bundle args) {

		mTabsAdapter.addFragment(new ArtistFragment(args), getString(R.string.artists)
				.toUpperCase());
		mTabsAdapter.addFragment(new AlbumFragment(args), getString(R.string.albums).toUpperCase());
		mTabsAdapter.addFragment(new TrackFragment(args), getString(R.string.tracks).toUpperCase());
		mTabsAdapter.addFragment(new PlaylistFragment(args), getString(R.string.playlists)
				.toUpperCase());
		mTabsAdapter.addFragment(new GenreFragment(args), getString(R.string.genres).toUpperCase());

		mViewPager.setAdapter(mTabsAdapter);
		mIndicator.setViewPager(mViewPager);
		int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
		mIndicator.setCurrentItem(currenttab < mTabsAdapter.getCount() ? currenttab : mTabsAdapter
				.getCount() - 1);
		mIndicator.setOnPageChangeListener(mOnPageChangeListener);
	}

	private void updateNowplaying() {
		if (mService == null) return;
		try {
			if (mService.getAudioId() > -1 || mService.getPath() != null) {
				mPlayPauseButton.setVisibility(View.VISIBLE);
				mNextButton.setVisibility(View.VISIBLE);
				mTrackName.setText(mService.getTrackName());
				if (mService.getArtistName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getArtistName())) {
					mTrackDetail.setText(mService.getArtistName());
				} else if (mService.getAlbumName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getAlbumName())) {
					mTrackDetail.setText(mService.getAlbumName());
				} else {
					mTrackDetail.setText(R.string.unknown_artist);
				}
			} else {
				mPlayPauseButton.setVisibility(View.GONE);
				mNextButton.setVisibility(View.GONE);
				mTrackName.setText(R.string.music_library);
				mTrackDetail.setText(R.string.touch_to_shuffle_all);
			}
			if (mAlbumArtLoader != null) {
				mAlbumArtLoader.cancel(true);
			}
			mAlbumArtLoader = new AsyncAlbumArtLoader();
			mAlbumArtLoader.execute();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void updatePlayPauseButton() {
		if (mService == null) return;
		try {
			if (mService.isPlaying()) {
				mPlayPauseButton.setImageResource(R.drawable.ic_action_media_pause);
			} else {
				mPlayPauseButton.setImageResource(R.drawable.ic_action_media_play);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
