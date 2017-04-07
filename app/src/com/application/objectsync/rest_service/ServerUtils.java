package com.application.objectsync.rest_service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.application.objectsync.MainActivity;
import com.application.objectsync.soup_operations.SoupOperations;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.SOQLBuilder;
import com.salesforce.androidsdk.smartsync.util.SoqlSyncDownTarget;
import com.salesforce.androidsdk.smartsync.util.SyncDownTarget;
import com.salesforce.androidsdk.smartsync.util.SyncOptions;
import com.salesforce.androidsdk.smartsync.util.SyncState;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Map;

/**
 * Created by Admin on 29-03-2017.
 */

public class ServerUtils {

    private static String TAG="SERVER_UTILS_LOGTAG";
    public static final String LOAD_COMPLETE_INTENT_ACTION = "com.salesforce.samples.smartsyncexplorer.loaders.LIST_LOAD_COMPLETE";
    public void fetchData(final Activity context, RestClient client, RestRequest restRequest, final IResposeObject resp, final String type){


        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            resp.Response(result.asJSONObject(),type);

                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,
                                context.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }


    /*public synchronized void syncDown(Activity context,SyncManager syncMgr) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> allEntries = settings.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());


            long syncId=-1;
            final SyncManager.SyncUpdateCallback callback = new SyncManager.SyncUpdateCallback() {

                @Override
                public void onUpdate(SyncState sync) {
                    if (SyncState.Status.DONE.equals(sync.getStatus())) {
                        //fireLoadCompleteIntent();
                        final Intent intent = new Intent(LOAD_COMPLETE_INTENT_ACTION);
                        SalesforceSDKManager.getInstance().getAppContext().sendBroadcast(intent);
                        Log.e(TAG,"Done");
                    }
                }
            };
            try {
                if (syncId == -1) {
                    final SyncOptions options = SyncOptions.optionsForSyncDown(SyncState.MergeMode.LEAVE_IF_CHANGED);
                    final String soqlQuery = SOQLBuilder.getInstanceWithFields(entry.getValue().toString().split(","))
                            .from(Constants.CONTACT).limit(100).build(); //Limit hardcoded but can change.
                    final SyncDownTarget target = new SoqlSyncDownTarget(soqlQuery);
                    final SyncState sync = syncMgr.syncDown(target, options,
                            entry.getKey(), callback); //getKey returns soup name
                    syncId = sync.getId();
                } else {
                    syncMgr.reSync(syncId, callback);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException occurred while parsing", e);
            } catch (SyncManager.SmartSyncException e) {
                Log.e(TAG, "SmartSyncException occurred while attempting to sync down", e);
            }
        }



    }*/
}
