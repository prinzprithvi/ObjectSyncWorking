package com.application.objectsync.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.application.objectsync.R;
import com.application.objectsync.rest_service.ConstantsSync;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditActivity extends SalesforceActivity {

    JSONObject detailObj;
    SharedPreferences settings;
    List<String> colApiNames;
    String curObj;
    private static final String TAG="EditActivity";
    //private static final String ID="Id";
    //dynamic component variables
    LinearLayout rootLinearLayout;
    private Map<String,Integer> allViewIds=new HashMap<String,Integer>();
    private static final String TXT_VIEW_ID_APPEND="txt";
    private static final String EDT_VIEW_ID_APPEND="edt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        getActionBar().setHomeButtonEnabled(true);
        //getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setIcon(R.drawable.ic_action_back);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        //load all headers
        final Intent launchIntent = getIntent();
        if (launchIntent != null) {
            try{
                if(!launchIntent.getStringExtra(ActivityDetail.PASS_OBJECT_KEY).equals(""))
                    detailObj = new JSONObject(launchIntent.getStringExtra(ActivityDetail.PASS_OBJECT_KEY));
                curObj=launchIntent.getStringExtra(ConstantsSync.PASS_DETAIL_INTENT_KEY);
                colApiNames= Arrays.asList(settings.getString(curObj,"").split(","));
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        configScreen();

    }


    @Override
    public void onResume(RestClient client) {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(false);
        final MenuItem logoutItem = menu.findItem(R.id.action_logout);
        logoutItem.setVisible(false);
        final MenuItem addItem = menu.findItem(R.id.action_add);
        addItem.setVisible(false);
        final MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        refreshItem.setIcon(R.drawable.ic_action_save);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                //overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
                return true;
            case R.id.action_refresh:
                save();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void configScreen()
    {
        rootLinearLayout=(LinearLayout) findViewById(R.id.mainLayout);
        if(detailObj!=null)
        {
            for(String title : colApiNames)
            {
                addTextView(title);
                try{
                    if(!detailObj.isNull(title))
                        addEditText(title,detailObj.getString(title));
                    else
                        addEditText(title,"");
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        else
        {
            for(String title : colApiNames)
            {
                addTextView(title);
                try{
                    addEditText(title,"");
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    }

    private void addTextView(String text)
    {
        allViewIds.put(text+TXT_VIEW_ID_APPEND,View.generateViewId());
        TextView textView  = new TextView(EditActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        lp.weight=1;
        lp.topMargin=5;
        textView.setTextSize(20);
        textView.setLayoutParams(lp);
        textView.setId(allViewIds.get(text+TXT_VIEW_ID_APPEND));
        textView.setText(text);
        textView.setTextColor(Color.parseColor("#000000"));
        rootLinearLayout.addView(textView);
    }
    private void addEditText(String api,String text)
    {
        allViewIds.put(api+EDT_VIEW_ID_APPEND,View.generateViewId());
        EditText editText  = new EditText(EditActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        lp.weight=1;
        editText.setLayoutParams(lp);
        editText.setId(allViewIds.get(api+EDT_VIEW_ID_APPEND));
        editText.setText(text);
        editText.setTextColor(Color.parseColor("#000000"));
        rootLinearLayout.addView(editText);

    }


    //save to localDB
    private void save() {

        for(String api : colApiNames)
        {
            final String apiText = ((EditText) findViewById(allViewIds.get(api+EDT_VIEW_ID_APPEND))).getText().toString();
            if (TextUtils.isEmpty(apiText)) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final SmartStore smartStore = SmartSyncSDKManager.getInstance().getSmartStore();
        JSONObject object;
        try {
            boolean isCreate;
            if(detailObj!=null)
                isCreate = TextUtils.isEmpty(detailObj.getString(Constants.ID));
            else
                isCreate=true;
            if (!isCreate) {
                object = smartStore.retrieve(curObj,
                        smartStore.lookupSoupEntryId(curObj,
                                Constants.ID, detailObj.getString(Constants.ID))).getJSONObject(0);
            } else {
                object = new JSONObject();
                object.put(Constants.ID, "local_" + System.currentTimeMillis()
                        + Constants.EMPTY_STRING);
                final JSONObject attributes = new JSONObject();
                attributes.put(Constants.TYPE.toLowerCase(), curObj);
                object.put(Constants.ATTRIBUTES, attributes);
            }
            object.put(SyncManager.LOCAL, true);
            object.put(SyncManager.LOCALLY_UPDATED, !isCreate);
            object.put(SyncManager.LOCALLY_CREATED, isCreate);
            object.put(SyncManager.LOCALLY_DELETED, false);
            for(String api: colApiNames)
            {
                object.put(api,((EditText) findViewById(allViewIds.get(api+EDT_VIEW_ID_APPEND))).getText().toString());
            }

            if (isCreate) {
                smartStore.create(curObj, object);
            } else {
                smartStore.upsert(curObj, object);
            }
            Toast.makeText(this, "Save successful!", Toast.LENGTH_LONG).show();
            finish();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException occurred while parsing", e);
        }
    }
}
