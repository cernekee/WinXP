/*
 * Win XPrivacy: Bypassing XPrivacy hooks using reflection
 *
 * Copyright (c) 2014, Kevin Cernekee <cernekee@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.example.winxp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.android.internal.telephony.IPhoneSubInfo;

import android.os.Bundle;
import android.os.IBinder;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.IAccountManager;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	public static final String TAG = "WinXP";
	
	private void writeField(int id, String value) {
		TextView tv = (TextView)this.findViewById(id);
		tv.setText(value);
	}
	
	private String listAccounts(Account[] list) {
		String out = "";
		
		for (Account a : list) {
			try {
				if (!out.equals("")) {
					out = out + "\n" + a.name;
				} else {
					out = a.name;
				}
			} catch (Exception e) {
			}
		}
		return out;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        writeField(R.id.imei_0, tm.getDeviceId());
        writeField(R.id.num_0, tm.getLine1Number());
        
        String imei = "ERROR";
        String num = "ERROR";
        try {
        	Class<?> sm = Class.forName("android.os.ServiceManager");
        	Method getService = sm.getMethod("getService", String.class);
        	IBinder serv = (IBinder)getService.invoke(null, "iphonesubinfo");
        	IPhoneSubInfo sub = IPhoneSubInfo.Stub.asInterface(serv);
        	imei = sub.getDeviceId();
        	num = sub.getLine1Number();
        } catch (Exception e) {
        	Log.e(TAG, "Error invoking getSubscriberInfo via reflection", e);
        }
        writeField(R.id.imei_1, imei);
        writeField(R.id.num_1, num);
        
        AccountManager am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        String acctinfo = listAccounts(am.getAccounts());
        writeField(R.id.acct_0, acctinfo);

        try {
        	Field f = AccountManager.class.getDeclaredField("mService");
        	f.setAccessible(true);
        	IAccountManager intf = (IAccountManager)f.get(am);
        	acctinfo = listAccounts(intf.getAccounts(null));
        } catch (Exception e) {
        	acctinfo = "ERROR";
        	Log.e(TAG, "Error invoking getAccounts via reflection", e);
        }
        writeField(R.id.acct_1, acctinfo);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
