/*
 * Copyright (c) 2012-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.application.objectsync;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.application.objectsync.activities.ActivityDetail;
import com.application.objectsync.recievers.ConnectivityChangeReceiver;
import com.application.objectsync.rest_service.ConstantsSync;
import com.application.objectsync.rest_service.IResposeObject;
import com.application.objectsync.rest_service.ServerUtils;
import com.application.objectsync.soup_operations.SoupOperations;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartstore.ui.SmartStoreInspectorActivity;
import com.salesforce.androidsdk.smartsync.app.SmartSyncSDKManager;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {

    private RestClient client;
    private ArrayAdapter<String> listAdapter;
	private ServerUtils makeReqObj;
	SmartStore smartStore;
	private SoupOperations soupOperations;
	private SyncManager syncMgr;
	private ListView objects;
	private LogoutDialogFragment logoutConfirmationDialog;
	List<String> allSoups;
    private SwipeRefreshLayout swipeContainer;

    private static boolean FIRST_RUN=true;
	SharedPreferences settings;
	Map<String, ?> allEntries;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addListener();
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);
		settings = PreferenceManager.getDefaultSharedPreferences(this);


		makeReqObj=new ServerUtils();
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		objects=(ListView) findViewById(R.id.contacts_list);
		objects.setAdapter(listAdapter);
		objects.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				Intent detailIntent=new Intent(MainActivity.this,ActivityDetail.class);
				detailIntent.putExtra(ConstantsSync.PASS_DETAIL_INTENT_KEY,allSoups.get(i));
				startActivity(detailIntent);
			}
		});


        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                callCustomApi();
                swipeContainer.setRefreshing(false);

            }
        });
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


    }
	

	
	@Override
	public void onResume(RestClient client) {
        // Keeping reference to rest client
		// only for first time
		if(FIRST_RUN)
		{
			logoutConfirmationDialog = new LogoutDialogFragment();
			smartStore = SmartSyncSDKManager.getInstance().getSmartStore();
			syncMgr = SyncManager.getInstance();
			soupOperations=new SoupOperations();
			this.client = client;
			findViewById(R.id.root).setVisibility(View.VISIBLE);
			allEntries = settings.getAll();
			if(allEntries!=null)
			{
				allSoups = new ArrayList<String>();
				allSoups.addAll(allEntries.keySet());
				listAdapter.addAll(allSoups);
			}
			else
				callCustomApi();
			FIRST_RUN=false;

		}
	}

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}
	


//menu items
@Override
public boolean onCreateOptionsMenu(Menu menu) {
	final MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main_activity, menu);
	final MenuItem searchItem = menu.findItem(R.id.action_search);
	searchItem.setVisible(false);
	final MenuItem refreshItem = menu.findItem(R.id.action_refresh);
	refreshItem.setVisible(false);
	final MenuItem addItem = menu.findItem(R.id.action_add);
	addItem.setVisible(false);
	return super.onCreateOptionsMenu(menu);
}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.action_logout:
				logoutConfirmationDialog.show(getFragmentManager(), "LogoutDialog");
				return true;
			case R.id.action_switch_user:
				launchAccountSwitcherActivity();
				return true;
			case R.id.action_inspect_db:
				launchSmartStoreInspectorActivity();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void launchAccountSwitcherActivity() {
		final Intent i = new Intent(this, SalesforceSDKManager.getInstance().getAccountSwitcherActivityClass());
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(i);
	}


	private void launchSmartStoreInspectorActivity() {
		this.startActivity(SmartStoreInspectorActivity.getIntent(this, false, null));
	}
	
	/*private void sendRequest(String soql) throws UnsupportedEncodingException {
		RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, final RestResponse result) {
				result.consumeQuietly(); // consume before going back to main thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							listAdapter.clear();
							JSONArray records = result.asJSONObject().getJSONArray("records");
							for (int i = 0; i < records.length(); i++) {
								listAdapter.add(records.getJSONObject(i).getString("Name"));
							}
						} catch (Exception e) {
							onError(e);
						}
					}
				});
			}
			
			@Override
			public void onError(final Exception exception) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this,
								MainActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
								Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}*/


	private void callCustomApi(){
		RestRequest mobileConfObj=new RestRequest(RestRequest.RestMethod.GET,ConstantsSync.MOBILE_CONFIGURATION_URL,null);
		makeReqObj.fetchData(MainActivity.this,client,mobileConfObj,response,"CONFIG");
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	IResposeObject response=new IResposeObject() {
		@Override
		public void Response(JSONObject resp,String type) {
			if(type.equalsIgnoreCase("CONFIG"))
			{
				try{
					//[{"objectNames":"Contact","fieldValues":"LastName,Account,MobilePhone"}]
					allSoups=new ArrayList<String>();
					JSONArray configArray = resp.getJSONArray("settings");
					for(int i=0;i<configArray.length();i++)
					{
						JSONObject soupObject=configArray.getJSONObject(i);
						String soupName=soupObject.getString("objectNames");
						allSoups.add(soupName);
						String[] soupFields=soupObject.getString("fieldValues").split(",");
						soupOperations.registerConfigSoup(MainActivity.this,soupName,soupObject.getString("fieldValues"));
						soupOperations.registerSoup(soupName,soupFields,smartStore);
					}
					//add data to listView
                    listAdapter.clear();
					listAdapter.addAll(allSoups);
					//makeReqObj.syncDown(MainActivity.this,syncMgr);

					Log.v("--->",configArray.toString());
				}catch (Exception e)
				{
					Log.v("Error",e.getMessage());
				}
			}

		}
	};
	private void addListener()
	{
		registerReceiver(
				new ConnectivityChangeReceiver(),
				new IntentFilter(
						ConnectivityManager.CONNECTIVITY_ACTION));
	}

}
