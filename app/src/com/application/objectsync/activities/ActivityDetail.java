package com.application.objectsync.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.application.objectsync.R;
import com.application.objectsync.rest_service.ConstantsSync;
import com.application.objectsync.soup_operations.GenericLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityDetail extends Activity implements LoaderManager.LoaderCallbacks<List<JSONObject>>  {

    SharedPreferences settings;
    String curObj,sortField;
    List<String> colApiNames;
    GenericLoader genLoader;
    List<JSONObject> originalData;
    ListView objectsList;
    CustomListAdapter listAdapter;
    private LoadCompleteReceiver loadCompleteReceiver;
    public static final String LOAD_COMPLETE_INTENT_ACTION = "com.salesforce.samples.smartsyncexplorer.loaders.LIST_LOAD_COMPLETE";
    private static final int OBJECT_LOADER_ID = 1;
    private AtomicBoolean isRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        isRegistered = new AtomicBoolean(false);
        objectsList=(ListView)findViewById(R.id.objectsList);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        curObj=getIntent().getStringExtra(ConstantsSync.PASS_DETAIL_INTENT_KEY);
        colApiNames= Arrays.asList(settings.getString(curObj,"").split(","));
        sortField=settings.getString(curObj,"").split(",")[0];
        listAdapter = new CustomListAdapter(this, R.layout.list_item);
        loadCompleteReceiver=new LoadCompleteReceiver();

        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);
        getLoaderManager().initLoader(OBJECT_LOADER_ID, null, this);
        objectsList.setAdapter(listAdapter);
    }

    @Override
    public Loader<List<JSONObject>> onCreateLoader(int i, Bundle bundle) {
        genLoader = new GenericLoader(this, curObj,sortField);
        genLoader.syncDown(curObj,sortField,this);
        return genLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<JSONObject>> loader, List<JSONObject> jsonObjects) {
        originalData = jsonObjects;
        listAdapter.setData(originalData);
    }

    @Override
    public void onLoaderReset(Loader<List<JSONObject>> loader) {
        originalData = null;
    }


    private class CustomListAdapter extends ArrayAdapter<JSONObject> {

        private int listItemLayoutId;
        private List<JSONObject> sObjects;
        /**
         * Parameterized constructor.
         *
         * @param context Context.
         * @param listItemLayoutId List item view resource ID.
         */
        public CustomListAdapter(Context context, int listItemLayoutId) {
            super(context, listItemLayoutId);
            this.listItemLayoutId = listItemLayoutId;

        }

        /**
         * Sets data to this adapter.
         *
         * @param data Data.
         */
        public void setData(List<JSONObject> data) {
            clear();
            sObjects = data;
            if (data != null) {
                addAll(data);
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(listItemLayoutId, null);
            }
            if (sObjects != null) {
                try{
                    final JSONObject sObject = sObjects.get(position);
                    if (sObject != null) {
                        final TextView objName = (TextView) convertView.findViewById(R.id.obj_name);

                        if (objName != null) {

                                objName.setText(sObject.getString(sortField));//initially Id here.

                        }
                        /*for(String colApi : colApiNames)
                        {
                            this.addTextView(parent,sObject.getString(colApi));
                        }*/

                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

            }
            return convertView;
        }


       /* private void addTextView(ViewGroup parent,String text)
        {

            TextView button  = new TextView(ActivityDetail.this);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
            button.setLayoutParams(lp);
            button.setText(text);
            parent.addView(button);

        }*/


    }

    private void refreshList() {
        getLoaderManager().getLoader(OBJECT_LOADER_ID).forceLoad();
    }


    private class LoadCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (LOAD_COMPLETE_INTENT_ACTION.equals(action)) {
                    refreshList();
                }
            }
        }
    }
}
