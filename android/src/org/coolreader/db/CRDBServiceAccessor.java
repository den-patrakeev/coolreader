/*
 * CoolReader for Android
 * Copyright (C) 2012,2020 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018,2019,2021 Aleksey Chernov <valexlin@gmail.com>
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

package org.coolreader.db;

import java.util.ArrayList;

import org.coolreader.crengine.MountPathCorrector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class CRDBServiceAccessor {
	private final static String TAG = "cr3db";
	private final Activity mActivity;
    private volatile CRDBService.LocalBinder mService;
    private volatile boolean mServiceBound;
	private volatile boolean bindIsCalled;
	private final ArrayList<Runnable> onConnectCallbacks = new ArrayList<Runnable>();
    private MountPathCorrector pathCorrector;

    public CRDBService.LocalBinder get() {
    	if (mService == null)
    		throw new RuntimeException("no service");
    	return mService;
    }
    
	public CRDBServiceAccessor(Activity activity, MountPathCorrector pathCorrector) {
		mActivity = activity;
		this.pathCorrector = pathCorrector;
	}

	public synchronized void setPathCorrector(MountPathCorrector pathCorrector) {
		this.pathCorrector = pathCorrector;
    	if (mService != null && pathCorrector != null)
    		mService.setPathCorrector(pathCorrector);
	}

    public void bind(final Runnable boundCallback) {
    	synchronized(this) {
			if (mService != null) {
				Log.v(TAG, "CRDBService is already bound");
				if (boundCallback != null)
					boundCallback.run();
				return;
			}
		}
    	//Log.v(TAG, "binding CRDBService");
    	if (boundCallback != null) {
			synchronized(onConnectCallbacks) {
				onConnectCallbacks.add(boundCallback);
			}
		}
    	if (!bindIsCalled) {
    		bindIsCalled = true;
	    	if (mActivity.bindService(new Intent(mActivity, 
	                CRDBService.class), mServiceConnection, Context.BIND_AUTO_CREATE)) {
	            mServiceBound = true;
			    Log.v(TAG, "binding CRDBService in progress...");
	    	} else {
	    		Log.e(TAG, "cannot bind CRDBService");
	    	}
    	}
    }

    public void unbind() {
    	Log.v(TAG, "unbinding CRDBService");
        if (mServiceBound) {
            // Detach our existing connection.
            mActivity.unbindService(mServiceConnection);
            mServiceBound = false;
            bindIsCalled = false;
            mService = null;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
			synchronized(CRDBServiceAccessor.this) {
				mService = ((CRDBService.LocalBinder) service);
				Log.i(TAG, "connected to CRDBService");
				if (pathCorrector != null)
					mService.setPathCorrector(pathCorrector);
			}
        	synchronized(onConnectCallbacks) {
				if (onConnectCallbacks.size() != 0) {
					// run once
					for (Runnable callback : onConnectCallbacks)
						callback.run();
					onConnectCallbacks.clear();
				}
			}
        }

        public void onServiceDisconnected(ComponentName className) {
        	synchronized(CRDBServiceAccessor.this) {
				mServiceBound = false;
				bindIsCalled = false;
				mService = null;
			}
			Log.i(TAG, "disconnected from CRDBService");
        }
    };

}
