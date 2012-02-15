/*
 * Copyright (C) 2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.quicinc.fmradio;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Handler.Callback;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class StationListActivity extends Activity implements
        View.OnCreateContextMenuListener {

    private static final String LOGTAG = "StationList";
    private ListView mStationList;
    private PresetList mPresetList;
    private SimpleAdapter mAdapter;
    private static IFMRadioService mService = null;
    private static final int CONTEXT_MENU_RENAME = 1;
    private static final int CONTEXT_MENU_DELETE = 2;

    static final int DIALOG_RENAME_ID = 0;
    static final int DIALOG_DELETE_ID = 1;
    // private static String mDialogTitle;
    private static String mDialogStationName;
    private static int mItemId;
    private Dialog mDialog;
    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = IFMRadioService.Stub.asInterface(obj);
            Log.e(LOGTAG, "ServiceConnection: onServiceConnected: ");
            if (mService != null) {
                // try
                // {
                // mService.registerCallbacks(mServiceCallbacks);
                //
                // } catch (RemoteException e)
                // {
                // e.printStackTrace();
                // }

                return;
            } else {
                Log.e(LOGTAG, "IFMRadioService onServiceConnected failed");
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };
    private BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                boolean mHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                if (!mHeadsetPlugged) {
                    finish();
                }
            }
        }
    };
    ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
    HashMap<Integer, Integer> mIndex = new HashMap<Integer, Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_list);

        bindService((new Intent()).setClass(this, FMRadioService.class), osc, 0);

        mStationList = (ListView) findViewById(R.id.station_list);
        // mPresetList = new PresetList("StationList");
        mStationList.setOnCreateContextMenuListener(this);
        mStationList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // Log.d(LOGTAG,"v.getId is ----------------" + arg2);
                String freq = ((HashMap<String, String>) mAdapter.getItem(arg2))
                        .get("freq");
                Float fFreq = Float.parseFloat(freq);
                if (mService != null) {
                    try {
                        mService.tune((int) ((fFreq * 1000)));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(LOGTAG, "mService is null........");
                }
            }
        });
      IntentFilter filter = new IntentFilter();
      filter.addAction(Intent.ACTION_HEADSET_PLUG);
      registerReceiver(mHeadsetReceiver, filter);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case CONTEXT_MENU_RENAME:
            showDialog(DIALOG_RENAME_ID);
            break;
        case CONTEXT_MENU_DELETE:
            showDialog(DIALOG_DELETE_ID);
            break;

        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
        menu.add(0, CONTEXT_MENU_RENAME, 0, "Rename");
        menu.add(0, CONTEXT_MENU_DELETE, 0, "Delete");
        mItemId = mi.position;
        // super.onCreateContextMenu(menu, v, menuInfo);
        // Log.d(LOGTAG,"mItemId is :" + mItemId);
        menu.setHeaderTitle(getNameFromId(mItemId));
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        load();
        super.onResume();

    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle b) {
        // TODO Auto-generated method stub
        // super.onPrepareDialog(id, dialog);
        switch (id) {
        case DIALOG_RENAME_ID:
            mDialog.setTitle(getNameFromId(mItemId));
            final EditText editText = (EditText) mDialog
                    .findViewById(R.id.name);
            editText.setText(getNameFromId(mItemId));
            Button bOk = (Button) mDialog.findViewById(R.id.save);

            bOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    saveStationName(mItemId, editText.getText().toString());
                    mDialog.dismiss();
                }

            });
            Button bCancel = (Button) mDialog.findViewById(R.id.cancel);
            bCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    mDialog.dismiss();
                }
            });
            break;
        case DIALOG_DELETE_ID:
            mDialog.setTitle("Delete:" + getNameFromId(mItemId));
            TextView prompt = (TextView) mDialog.findViewById(R.id.prompt);
            prompt.setText("Are you sure to delete " + getNameFromId(mItemId));
            Button bDelete = (Button) mDialog.findViewById(R.id.delete);

            bDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    deleteStation(mItemId);
                    mDialog.dismiss();
                }
            });
            Button bCancelDelete = (Button) mDialog.findViewById(R.id.cancel);
            bCancelDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    mDialog.dismiss();
                }
            });
            break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle b) {
        // TODO Auto-generated method stub
        switch (id) {
        case DIALOG_RENAME_ID:
            mDialog = new Dialog(this);
            mDialog.setContentView(R.layout.rename_dialog);
            // Log.d(LOGTAG, "mItemId is2 :" + mItemId);
            break;
        case DIALOG_DELETE_ID:
            mDialog = new Dialog(this);
            mDialog.setContentView(R.layout.delete_dialog);
            break;
        default:
            mDialog = null;
        }
        // return super.onCreateDialog(id);
        return mDialog;
    }

    private void saveStationName(int id, String name) {
        // Log.d(LOGTAG,"id :" + id);
        // Log.d(LOGTAG,"name :" + name);
        Integer stationIndex = mIndex.get(new Integer(id));
        SharedPreferences sp = getSharedPreferences(
                FMRadio.SCAN_STATION_PREFS_NAME, 0);
        SharedPreferences.Editor editor = sp.edit();
        editor
                .putString(FMRadio.STATION_NAME + (stationIndex.intValue()),
                        name);
        editor.commit();
        load();
    }

    private void deleteStation(int id) {
        Log.d(LOGTAG, "id :" + id);
        SharedPreferences sp = getSharedPreferences(
                FMRadio.SCAN_STATION_PREFS_NAME, 0);
        Integer stationIndex = mIndex.get(new Integer(id));
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(FMRadio.STATION_NAME + (stationIndex));
        editor.remove(FMRadio.STATION_FREQUENCY + (stationIndex));
        editor.commit();
        load();
    }

    private int getFrequencyFromId(int id) {
        String freq = ((HashMap<String, String>) mAdapter.getItem(id))
                .get("freq");
        Float fFreq = Float.parseFloat(freq);
        return (int) ((fFreq * 1000));
    }

    private String getNameFromId(int id) {
        String name = ((HashMap<String, String>) mAdapter.getItem(id))
                .get("name");
        return name;

    }

    protected void load() {
        list.clear();
        mIndex.clear();
        SharedPreferences sp = getSharedPreferences(
                FMRadio.SCAN_STATION_PREFS_NAME, 0);
        int station_number = sp.getInt(FMRadio.NUM_OF_STATIONS, 0);
        for (int i = 1; i <= station_number; i++) {
            HashMap<String, String> item = new HashMap<String, String>();
            String name = sp.getString(FMRadio.STATION_NAME + i, "");

            int frequency = sp.getInt(FMRadio.STATION_FREQUENCY + i, 0);

            if (!name.equals("") && frequency != 0) {
                item.put("name", name);
                item.put("freq", String.valueOf(frequency / 1000.0f));
                // Log.d(LOGTAG,"list.size :" + list.size());
                // Log.d(LOGTAG,"i is:" + i);
                mIndex.put(new Integer(list.size()), new Integer(i));
                list.add(item);
            }
            // list.add(index, item);
        }

        mAdapter = new SimpleAdapter(this, list, R.layout.station_list_item,
                new String[] { "name", "freq" }, new int[] { R.id.name,
                        R.id.freq });

        mStationList.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mHeadsetReceiver);
        super.onDestroy();
    }

}
