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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.application.objectsync.R;
import com.application.objectsync.rest_service.ConstantsSync;
import com.application.objectsync.soup_operations.GenericLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    //Dynamic view contect
    /*View itemView;
    LinearLayout dynamicView;
    ViewGroup itemViewGroup;
    private List<View> allViews = new ArrayList<View>();*/
    private static int viewsCount = 0;

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
    }


    @Override
    protected void onResume() {
        if (!isRegistered.get()) {
            registerReceiver(loadCompleteReceiver,
                    new IntentFilter(LOAD_COMPLETE_INTENT_ACTION));
        }
        isRegistered.set(true);
        getLoaderManager().initLoader(OBJECT_LOADER_ID, null, this);
        super.onResume();
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

    @Override
    protected void onPause() {
        if (isRegistered.get()) {
            unregisterReceiver(loadCompleteReceiver);
        }
        isRegistered.set(false);
        getLoaderManager().destroyLoader(OBJECT_LOADER_ID);
        genLoader = null;
        super.onPause();
    }

    /*//configure dynamic view
    private void configureDynamicView()
    {
        dynamicView = (LinearLayout) findViewById(R.id.obj_layout);
        for(String colApi : colApiNames)
        {
            this.addTextView();
        }
    }*/




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
                        final TextView objName = (TextView) convertView.findViewById(R.id.obj_name);
                        objName.setText(sObject.getString(sortField));//initially Id here.
                        for(String colApi : colApiNames)
                        {
                            TextView view=(TextView) convertView.findViewById(idMap.get(colApi));
                            view.setText(colApi+" : "+sObject.getString(colApi));
                            /*((TextView)convertView.findViewById(idMap.get(colApi))).
                                    setText(((TextView)convertView.findViewById(idMap.get(colApi))).getText()+" : "+sObject.getString(colApi));*/
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
            textView.setText(text);
            //textView.setTextColor(getColor(R.color.black));
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
