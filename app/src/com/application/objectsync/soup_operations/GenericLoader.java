package com.application.objectsync.soup_operations;

import android.app.Activity;
import android.content.Context;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.SOQLBuilder;
import com.salesforce.androidsdk.smartsync.util.SoqlSyncDownTarget;
import com.salesforce.androidsdk.smartsync.util.SyncDownTarget;
import com.salesforce.androidsdk.smartsync.util.SyncOptions;
import com.salesforce.androidsdk.smartsync.util.SyncState;
import com.salesforce.androidsdk.smartsync.util.SyncUpTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.application.objectsync.rest_service.ServerUtils.LOAD_COMPLETE_INTENT_ACTION;

/**
 * Created by Admin on 30-03-2017.
 */

public class GenericLoader extends AsyncTaskLoader<List<JSONObject>> {

    private SmartStore smartStore;
    private SyncManager syncMgr;
    private long syncId = -1;
    private String soupToLoad;
    private final static int LIMIT=100;
    private String orderPath;
    private static final String TAG = "Loader";

    public GenericLoader(Context context,String soupToLoad,String orderPath) {
        super(context);
        this.soupToLoad=soupToLoad;
        this.orderPath=orderPath;
        smartStore = SmartSyncSDKManager.getInstance().getSmartStore();
        syncMgr = SyncManager.getInstance();
    }



    public synchronized void syncDown(String soup/*, String sortField*/, Activity context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            //Uncomment and add loop to sync all soups at once roadmap
        //Map<String, ?> allEntries = settings.getAll();
        //for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            //Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());


            long syncId=-1;
            final SyncManager.SyncUpdateCallback callback = new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        fireLoadCompleteIntent();

                        Log.e(TAG,"Done");
                    }
                }
            };
            try {
                if (syncId == -1) {
                    final SyncOptions options = SyncOptions.optionsForSyncDown(SyncState.MergeMode.LEAVE_IF_CHANGED);
                    final String soqlQuery = SOQLBuilder.getInstanceWithFields(settings.getString(soup,"").split(","))//From preference now
                            .from(soupToLoad).limit(LIMIT).build(); //Limit hardcoded but can change.
                    final SyncDownTarget target = new SoqlSyncDownTarget(soqlQuery);
                    final SyncState sync = syncMgr.syncDown(target, options,
                            soup, callback); //entry.getKey() getKey returns soup name in case of all soups
                    syncId = sync.getId();
                } else {
                    syncMgr.reSync(syncId, callback);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException occurred while parsing", e);
            } catch (SyncManager.SmartSyncException e) {
                Log.e(TAG, "SmartSyncException occurred while attempting to sync down", e);
            }
        //}
    }


    public synchronized void syncUp(final String soup, List<String> fieldsToSync, final Activity context) {
        final SyncUpTarget target = new SyncUpTarget();
        final SyncOptions options = SyncOptions.optionsForSyncUp(fieldsToSync,
                SyncState.MergeMode.LEAVE_IF_CHANGED);

        try {
            syncMgr.syncUp(target, options, soup, new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        syncDown(soup,context);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SyncManager.SmartSyncException e) {
            Log.e(TAG, "SmartSyncException occurred while attempting to sync up", e);
        }
    }



    @Override
    public List<JSONObject> loadInBackground() {
        if (!smartStore.hasSoup(soupToLoad)) {
            return null;
        }
        final QuerySpec querySpec = QuerySpec.buildAllQuerySpec(soupToLoad,
                orderPath, QuerySpec.Order.ascending, LIMIT);
        JSONArray results = null;
        List<JSONObject> obj = new ArrayList<JSONObject>();
        try {
            results = smartStore.query(querySpec, 0);
            for (int i = 0; i < results.length(); i++) {
                obj.add(results.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        } catch (SmartSqlHelper.SmartSqlException e) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e);
        }
        return obj;
    }


    private void fireLoadCompleteIntent() {
        final Intent intent = new Intent(LOAD_COMPLETE_INTENT_ACTION);
        SalesforceSDKManager.getInstance().getAppContext().sendBroadcast(intent);
    }
}
