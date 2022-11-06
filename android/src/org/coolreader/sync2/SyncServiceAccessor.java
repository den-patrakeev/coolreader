/*
 * CoolReader for Android
 * Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.sync2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class SyncServiceAccessor {
	private final static String TAG = "sync2acc";
	private final Activity mActivity;
	private volatile SyncServiceBinder mServiceBinder;
	private volatile boolean mServiceBound;
	private final ArrayList<SyncServiceBinder.Callback> onConnectCallbacks = new ArrayList<>();
	private boolean bindIsCalled;
	private final Object mLocker = new Object();

	public interface Callback {
		void run(SyncServiceAccessor acc);
	}

	public SyncServiceAccessor(Activity activity) {
		mActivity = activity;
	}

	public void bind(final SyncServiceBinder.Callback boundCallback) {
		synchronized (this) {
			if (mServiceBinder != null && mServiceBound) {
				Log.v(TAG, "SyncService is already bound");
				if (boundCallback != null)
					boundCallback.run(mServiceBinder);
				return;
			}
		}
		//Log.v(TAG, "binding SyncService");
		if (boundCallback != null) {
			synchronized (mLocker) {
				onConnectCallbacks.add(boundCallback);
			}
		}
		if (!bindIsCalled) {
			bindIsCalled = true;
			if (mActivity.bindService(new Intent(mActivity, SyncService.class), mServiceConnection, Context.BIND_AUTO_CREATE)) {
				mServiceBound = true;
				Log.v(TAG, "binding SyncService in progress...");
			} else {
				Log.e(TAG, "cannot bind SyncService");
			}
		}
	}

	public void unbind() {
		Log.v(TAG, "unbinding SyncService");
		if (mServiceBound) {
			// Detach our existing connection.
			mActivity.unbindService(mServiceConnection);
			mServiceBound = false;
			bindIsCalled = false;
			mServiceBinder = null;
		}
	}

	public boolean isServiceBound() {
		synchronized (SyncServiceAccessor.this) {
			return mServiceBound;
		}
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			synchronized (SyncServiceAccessor.this) {
				mServiceBinder = ((SyncServiceBinder)service);
				Log.i(TAG, "connected to SyncService");
			}
			synchronized (mLocker) {
				if (onConnectCallbacks.size() != 0) {
					// run once
					for (SyncServiceBinder.Callback callback : onConnectCallbacks)
						callback.run(mServiceBinder);
					onConnectCallbacks.clear();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			synchronized (SyncServiceAccessor.this) {
				mServiceBinder = null;
				mServiceBound = false;
				bindIsCalled = false;
			}
			Log.i(TAG, "Connection to the SyncService has been lost (abnormal termination)");
		}
	};

}
