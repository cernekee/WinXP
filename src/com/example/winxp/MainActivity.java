/*
 * Win XPrivacy: Bypassing XPrivacy hooks using JNI
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

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
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
        
        if (!Native.init() ||
        		!Native.getMagic().equals("some magic string that XPrivacy will never guess")) {
        	throw new IllegalArgumentException("error initializing native library; reboot and try again");
        }
        
        TelephonyManager tm;
        AccountManager am;
        WifiManager wm;

        tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        writeField(R.id.imei_0, tm.getDeviceId());
        writeField(R.id.num_0, tm.getLine1Number());

        am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        writeField(R.id.acct_0, listAccounts(am.getAccounts()));
        
        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        writeField(R.id.mac_0, wm.getConnectionInfo().getMacAddress());

        Native.nukeXposed("android/app/ContextImpl");
        Native.nukeXposed("android/telephony/TelephonyManager");
        Native.nukeXposed("android/accounts/AccountManager");
        Native.nukeXposed("android/net/wifi/WifiManager");

        tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        writeField(R.id.imei_1, tm.getDeviceId());
        writeField(R.id.num_1, tm.getLine1Number());

        am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        writeField(R.id.acct_1, listAccounts(am.getAccounts()));

        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        writeField(R.id.mac_1, wm.getConnectionInfo().getMacAddress());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
