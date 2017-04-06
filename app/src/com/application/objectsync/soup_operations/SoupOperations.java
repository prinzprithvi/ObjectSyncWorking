package com.application.objectsync.soup_operations;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admin on 29-03-2017.
 */

public class SoupOperations {


    private static String TAG="SoupOperations";
    public void registerSoup(String soupName,String[] fields,SmartStore smartStore)
    {

        List<IndexSpec> indexSpecList = new ArrayList<IndexSpec>();
        indexSpecList.add(new IndexSpec("Id", SmartStore.Type.string));
        indexSpecList.add(new IndexSpec(SyncManager.LOCALLY_CREATED, SmartStore.Type.string));
        indexSpecList.add(new IndexSpec(SyncManager.LOCALLY_UPDATED, SmartStore.Type.string));
        indexSpecList.add(new IndexSpec(SyncManager.LOCALLY_DELETED, SmartStore.Type.string));
        indexSpecList.add(new IndexSpec(SyncManager.LOCAL, SmartStore.Type.string));
        for(String field : fields)
        {
            indexSpecList.add(new IndexSpec(field, SmartStore.Type.string));
        }
        smartStore.registerSoup(soupName, indexSpecList.toArray(new IndexSpec[indexSpecList.size()]));
        Log.v(TAG,"registered successfully!");
    }


    public void registerConfigSoup(final Activity context,String soupName, String fields)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(soupName,fields);
        editor.commit();
    }



}
