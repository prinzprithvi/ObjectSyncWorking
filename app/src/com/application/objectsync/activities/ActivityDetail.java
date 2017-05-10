package com.application.objectsync.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.application.objectsync.R;
import com.application.objectsync.rest_service.ConstantsSync;
import com.application.objectsync.soup_operations.GenericLoader;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityDetail extends SalesforceActivity implements LoaderManager.LoaderCallbacks<List<JSONObject>>  {

    public static final String PASS_OBJECT_KEY="s_object";
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
        //itemView=LayoutInflater.from(this).inflate(R.layout.list_item, null);
        isRegistered = new AtomicBoolean(false);
        objectsList=(ListView)findViewById(R.id.objectsList);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        curObj=getIntent().getStringExtra(ConstantsSync.PASS_DETAIL_INTENT_KEY);
        colApiNames= Arrays.asList(settings.getString(curObj,"").split(","));

        //configureDynamicView();
        sortField=settings.getString(curObj,"").split(",")[0];
        listAdapter = new CustomListAdapter(this, R.layout.list_item);
        loadCompleteReceiver=new LoadCompleteReceiver();
        objectsList.setAdapter(listAdapter);


        objectsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                    final JSONObject sObject = listAdapter.getItem(position);
                    launchDetailActivity(sObject);

            }
        });
    }

    @Override
    public void onResume(RestClient client) {
        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);
        getLoaderManager().initLoader(OBJECT_LOADER_ID, null, this);
        refreshList();


    }

    private void launchDetailActivity(JSONObject sObject) {

        Intent detailIntent = new Intent(this, EditActivity.class);
        detailIntent.addCategory(Intent.CATEGORY_DEFAULT);
        detailIntent.putExtra(ConstantsSync.PASS_DETAIL_INTENT_KEY,curObj);
        if(sObject!=null) {
            detailIntent.putExtra(PASS_OBJECT_KEY, sObject.toString());
            startActivity(detailIntent);
        }
        else
        {
            detailIntent.putExtra(PASS_OBJECT_KEY, "");
            startActivity(detailIntent);
        }

    }




    @Override
    public Loader<List<JSONObject>> onCreateLoader(int i, Bundle bundle) {
        genLoader = new GenericLoader(this, curObj,sortField);
        //genLoader.syncDown(curObj,this);

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

    @Override
    public void onPause() {
        if (isRegistered.get()) {
            unregisterReceiver(loadCompleteReceiver);
        }
        isRegistered.set(false);
        getLoaderManager().destroyLoader(OBJECT_LOADER_ID);
        genLoader = null;
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        final MenuItem logOut = menu.findItem(R.id.action_logout);
        logOut.setVisible(false);
        final MenuItem actionIDb = menu.findItem(R.id.action_inspect_db);
        actionIDb.setVisible(false);
        final MenuItem addItem = menu.findItem(R.id.action_switch_user);
        addItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_add:
                launchDetailActivity(null);
                return true;
           case R.id.action_refresh:
                syncUpRecords();
                return true;
             /*case R.id.act:
                launchSmartStoreInspectorActivity();
                return true;*/

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void syncUpRecords() {
        genLoader.syncUp(curObj,colApiNames,this);
        Toast.makeText(this, "Sync up complete!", Toast.LENGTH_LONG).show();
    }


    private class CustomListAdapter extends ArrayAdapter<JSONObject> {

        private int listItemLayoutId;
        private List<JSONObject> sObjects;
        private Map<String,Integer> idMap=new HashMap<String,Integer>();
        /**
         * Parameterized constructor.
         *
         * @param context Context.
         * @param listItemLayoutId List item view resource ID.
         */
        public CustomListAdapter(Context context, int listItemLayoutId) {
            super(context, listItemLayoutId);
            this.listItemLayoutId = listItemLayoutId;
            generateIds();


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
                RelativeLayout layout = (RelativeLayout)LayoutInflater.from(getContext()).inflate(listItemLayoutId, null);
                LinearLayout nestedLayout=(LinearLayout) layout.findViewById(R.id.obj_layout);

                for(String colApi : colApiNames)
                {
                    this.addTextView(nestedLayout,colApi);

                }
                //convertView = LayoutInflater.from(getContext()).inflate(listItemLayoutId, null);
                convertView=layout;
            }
            if (sObjects != null) {
                try{
                    final JSONObject sObject = sObjects.get(position);
                    if (sObject != null) {
                        //final TextView objName = (TextView) convertView.findViewById(R.id.obj_name);
                        //objName.setText(sObject.getString(sortField));//initially Id here.
                        final TextView objImage = (TextView) convertView.findViewById(R.id.obj_image);
                        for(String colApi : colApiNames)
                        {
                            TextView view=(TextView) convertView.findViewById(idMap.get(colApi));
                            view.setText(colApi+" : "+sObject.getString(colApi));
                        }
                        final ImageView syncImage = (ImageView) convertView.findViewById(R.id.sync_status_view);
                        if (syncImage != null && (sObject.optBoolean(SyncManager.LOCALLY_UPDATED) ||
                                sObject.optBoolean(SyncManager.LOCALLY_CREATED) ||
                                sObject.optBoolean(SyncManager.LOCALLY_DELETED))) {
                            syncImage.setImageResource(R.drawable.sync_local);
                        } else {
                            syncImage.setImageResource(R.drawable.sync_success);
                        }



                    }
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }

            }
            return convertView;
        }

        private void addTextView(LinearLayout viewGroup,String text)
        {

            TextView textView  = new TextView(ActivityDetail.this);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
            textView.setLayoutParams(lp);
            textView.setId(idMap.get(text));
            //textView.setText(text);
            textView.setTextColor(Color.parseColor("#000000"));
            //allViews.add(textView);
            viewGroup.addView(textView);

        }

        private void generateIds()
        {
            for(String colApi : colApiNames)
            {
                idMap.put(colApi,View.generateViewId());
            }
        }



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
