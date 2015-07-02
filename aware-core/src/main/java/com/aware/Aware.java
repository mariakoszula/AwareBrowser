
package com.aware;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;

import com.aware.providers.Aware_Provider;
import com.aware.providers.Aware_Provider.Aware_Device;
import com.aware.providers.Aware_Provider.Aware_Plugins;
import com.aware.providers.Aware_Provider.Aware_Settings;
import com.aware.ui.Plugins_Manager;
import com.aware.ui.Stream_UI;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.DownloadPluginService;
import com.aware.utils.Https;
import com.aware.utils.WearClient;
import com.aware.utils.WebserviceHelper;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Main AWARE framework service. awareContext will start and manage all the services and settings.
 * @author denzil
 *
 */
public class Aware extends Service {
    /**
     * Debug flag (default = false).
     */
    public static boolean DEBUG = false;
    
    /**
     * Debug tag (default = "AWARE").
     */
    public static String TAG = "AWARE";
    
    /**
     * Broadcasted event: awareContext device information is available
     */
    public static final String ACTION_AWARE_DEVICE_INFORMATION = "ACTION_AWARE_DEVICE_INFORMATION";
    
    /**
     * Received broadcast on all modules
     * - Sends the data to the defined webserver
     */
    public static final String ACTION_AWARE_SYNC_DATA = "ACTION_AWARE_SYNC_DATA";
    
    /**
     * Received broadcast on all modules
     * - Cleans the data collected on the device
     */
    public static final String ACTION_AWARE_CLEAR_DATA = "ACTION_AWARE_CLEAR_DATA";
    
    /**
     * Received broadcast: refresh the framework active sensors.
     */
    public static final String ACTION_AWARE_REFRESH = "ACTION_AWARE_REFRESH";
    
    /**
     * Received broadcast: plugins must implement awareContext broadcast receiver to share their current status.
     */
    public static final String ACTION_AWARE_CURRENT_CONTEXT = "ACTION_AWARE_CURRENT_CONTEXT";
    
    /**
     * Stop all plugins
     */
    public static final String ACTION_AWARE_STOP_PLUGINS = "ACTION_AWARE_STOP_PLUGINS";
    
    /**
     * Stop all sensors
     */
    public static final String ACTION_AWARE_STOP_SENSORS = "ACTION_AWARE_STOP_SENSORS";
    
    /**
     * Received broadcast on all modules
     * - Cleans old data from the content providers
     */
    public static final String ACTION_AWARE_SPACE_MAINTENANCE = "ACTION_AWARE_SPACE_MAINTENANCE";
    
    /**
     * Used by Plugin Manager to refresh UI
     */
    public static final String ACTION_AWARE_PLUGIN_MANAGER_REFRESH = "ACTION_AWARE_PLUGIN_MANAGER_REFRESH";

    /**
     * Used when quitting a study. This will reset the device to default settings.
     */
    public static final String ACTION_QUIT_STUDY = "ACTION_QUIT_STUDY";

    /**
     * DownloadManager AWARE update ID, used to prompt user to install the update once finished downloading.
     */
    private static long AWARE_FRAMEWORK_DOWNLOAD_ID = 0;

    /**
     * DownloadManager queue for plugins, in case we have multiple dependencies to install.
     */
    public static final ArrayList<Long> AWARE_PLUGIN_DOWNLOAD_IDS = new ArrayList<>();

    /**
     * DownloadManager queue for plugins, in case we have multiple dependencies to install.
     */
    public static final ArrayList<String> AWARE_PLUGIN_DOWNLOAD_PACKAGES = new ArrayList<>();

    private static AlarmManager alarmManager = null;
    private static PendingIntent repeatingIntent = null;
    private static Context awareContext = null;
    private static PendingIntent webserviceUploadIntent = null;
    
    private static Intent awareStatusMonitor = null;
    private static Intent applicationsSrv = null;
    private static Intent accelerometerSrv = null;
    private static Intent locationsSrv = null;
    private static Intent bluetoothSrv = null;
    private static Intent screenSrv = null;
    private static Intent batterySrv = null;
    private static Intent networkSrv = null;
    private static Intent trafficSrv = null;
    private static Intent communicationSrv = null;
    private static Intent processorSrv = null;
    private static Intent mqttSrv = null;
    private static Intent gyroSrv = null;
    private static Intent wifiSrv = null;
    private static Intent telephonySrv = null;
    private static Intent timeZoneSrv = null;
    private static Intent rotationSrv = null;
    private static Intent lightSrv = null;
    private static Intent proximitySrv = null;
    private static Intent magnetoSrv = null;
    private static Intent barometerSrv = null;
    private static Intent gravitySrv = null;
    private static Intent linear_accelSrv = null;
    private static Intent temperatureSrv = null;
    private static Intent esmSrv = null;
    private static Intent installationsSrv = null;
    private static Intent keyboard = null;
    private static Intent wearClient = null;
    
    private final String PREF_FREQUENCY_WATCHDOG = "frequency_watchdog";
    private final String PREF_LAST_UPDATE = "last_update";
    private final String PREF_LAST_SYNC = "last_sync";
    private final int CONST_FREQUENCY_WATCHDOG = 5 * 60; //5 minutes check
    
    private SharedPreferences aware_preferences;
    
    /**
     * Singleton instance of the framework
     */
    private static Aware awareSrv = Aware.getService();
    
    /**
     * Get the singleton instance to the AWARE framework
     * @return {@link Aware} obj
     */
    public static Aware getService() {
        if( awareSrv == null ) awareSrv = new Aware();
        return awareSrv;
    }

    /**
     * Activity-Service binder
     */
    private final IBinder serviceBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        Aware getService() {
            return Aware.getService();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        awareContext = getApplicationContext();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        awareContext.registerReceiver(storage_BR, filter);
        
        filter = new IntentFilter();
        filter.addAction(Aware.ACTION_AWARE_CLEAR_DATA);
        filter.addAction(Aware.ACTION_AWARE_REFRESH);
        filter.addAction(Aware.ACTION_AWARE_SYNC_DATA);
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(Aware.ACTION_QUIT_STUDY);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        awareContext.registerReceiver(aware_BR, filter);

        Intent synchronise = new Intent(Aware.ACTION_AWARE_SYNC_DATA);
        webserviceUploadIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, synchronise, 0);
        
        if( ! Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
            stopSelf();
            return;
        }

        aware_preferences = getSharedPreferences("aware_core_prefs", MODE_PRIVATE);
        if( aware_preferences.getAll().isEmpty() ) {
            SharedPreferences.Editor editor = aware_preferences.edit();
            editor.putInt(PREF_FREQUENCY_WATCHDOG, CONST_FREQUENCY_WATCHDOG);
            editor.putLong(PREF_LAST_SYNC, 0);
            editor.putLong(PREF_LAST_UPDATE, 0);
            editor.commit();
        }

        //this sets the default settings to all plugins too
        SharedPreferences prefs = getSharedPreferences( getPackageName(), Context.MODE_PRIVATE );
        if( prefs.getAll().isEmpty() && Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0 ) {
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, true);
            prefs.edit().commit(); //commit changes
        } else {
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, false);
        }

        Map<String,?> defaults = prefs.getAll();
        for(Map.Entry<String, ?> entry : defaults.entrySet()) {
            if( Aware.getSetting(getApplicationContext(), entry.getKey()).length() == 0 ) {
                Aware.setSetting(getApplicationContext(), entry.getKey(), entry.getValue());
            }
        }

        if( Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0 ) {
            UUID uuid = UUID.randomUUID();
            Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid.toString());
        }

        DEBUG = Aware.getSetting(awareContext, Aware_Preferences.DEBUG_FLAG).equals("true");
        TAG = Aware.getSetting(awareContext, Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(awareContext,Aware_Preferences.DEBUG_TAG):TAG;

        get_device_info();

        if( Aware.DEBUG ) Log.d(TAG,"AWARE framework is created!");

        //Only the official client will do this.
        if ( getPackageName().equals("com.aware") ) {

            if( DEBUG ) Log.d(TAG, "Starting Android Wear HTTP proxy...");
            wearClient = new Intent(this, WearClient.class);
            startService(wearClient);

            awareStatusMonitor = new Intent(getApplicationContext(), Aware.class);
            repeatingIntent = PendingIntent.getService(getApplicationContext(), 0, awareStatusMonitor, 0);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, aware_preferences.getInt(PREF_FREQUENCY_WATCHDOG, 300) * 1000, repeatingIntent);
            new AsyncPing().execute();
        }
    }
    
    private class AsyncPing extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			//Ping AWARE's server with awareContext device's information for framework's statistics log
	        ArrayList<NameValuePair> device_ping = new ArrayList<NameValuePair>();
	        device_ping.add(new BasicNameValuePair(Aware_Preferences.DEVICE_ID, Aware.getSetting(awareContext, Aware_Preferences.DEVICE_ID)));
	        device_ping.add(new BasicNameValuePair("ping", String.valueOf(System.currentTimeMillis())));
	        new Https(awareContext).dataPOST("https://api.awareframework.com/index.php/awaredev/alive", device_ping, true);
	        return true;
		}
    }
    
    private void get_device_info() {
        Cursor awareContextDevice = awareContext.getContentResolver().query(Aware_Device.CONTENT_URI, null, null, null, null);
        if( awareContextDevice == null || ! awareContextDevice.moveToFirst() ) {
            ContentValues rowData = new ContentValues();
            rowData.put("timestamp", System.currentTimeMillis());
            rowData.put("device_id", Aware.getSetting(awareContext, Aware_Preferences.DEVICE_ID));
            rowData.put("board", Build.BOARD);
            rowData.put("brand", Build.BRAND);
            rowData.put("device",Build.DEVICE);
            rowData.put("build_id", Build.DISPLAY);
            rowData.put("hardware", Build.HARDWARE);
            rowData.put("manufacturer", Build.MANUFACTURER);
            rowData.put("model", Build.MODEL);
            rowData.put("product", Build.PRODUCT);
            rowData.put("serial", Build.SERIAL);
            rowData.put("release", Build.VERSION.RELEASE);
            rowData.put("release_type", Build.TYPE);
            rowData.put("sdk", Build.VERSION.SDK_INT);
            
            try {
                awareContext.getContentResolver().insert(Aware_Device.CONTENT_URI, rowData);
                
                Intent deviceData = new Intent(ACTION_AWARE_DEVICE_INFORMATION);
                sendBroadcast(deviceData);
                
                if( Aware.DEBUG ) Log.d(TAG, "Device information:"+ rowData.toString());
                
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
        }
        if( awareContextDevice != null && ! awareContextDevice.isClosed()) awareContextDevice.close();
    }

    /**
     * Identifies if the device is a watch or a phone.
     * @param c
     * @return boolean
     */
    public static boolean is_watch(Context c) {
        UiModeManager uiManager = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
        if( uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_WATCH ) {
            return true;
        }
        return false;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
            DEBUG = Aware.getSetting(awareContext,Aware_Preferences.DEBUG_FLAG).equals("true");
            TAG = Aware.getSetting(awareContext,Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(awareContext,Aware_Preferences.DEBUG_TAG):TAG;
            
            if( Aware.DEBUG ) Log.d(TAG,"AWARE framework is active...");

            //Plugins need to be able to start services too, as requested in their settings
            startAllServices();

            //The official client takes care of keeping the plugins running and updated
            if( getPackageName().equals("com.aware") ) {
                ArrayList<String> active_plugins = new ArrayList<>();
                Cursor enabled_plugins = getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_STATUS + "=" + Aware_Plugin.STATUS_PLUGIN_ON, null, null);
                if( enabled_plugins != null && enabled_plugins.moveToFirst() ) {
                    do {
                        String package_name = enabled_plugins.getString(enabled_plugins.getColumnIndex(Aware_Plugins.PLUGIN_PACKAGE_NAME));
                        active_plugins.add(package_name);
                    }while(enabled_plugins.moveToNext());
                }
                if( enabled_plugins != null && ! enabled_plugins.isClosed() ) enabled_plugins.close();

                //Check if there are updates on the plugins
                if( active_plugins.size() > 0 ) {
                    for(String package_name : active_plugins ) {
                        startPlugin(getApplicationContext(), package_name);
                    }
                    if( ! Aware.is_watch(this) ) {
                        new CheckPlugins().execute(active_plugins);
                    }
                }

                if( Aware.getSetting(getApplicationContext(), Aware_Preferences.AWARE_AUTO_UPDATE).equals("true") ) {
	            	if( aware_preferences.getLong(PREF_LAST_UPDATE, 0) == 0 || (aware_preferences.getLong(PREF_LAST_UPDATE, 0) > 0 && System.currentTimeMillis()-aware_preferences.getLong(PREF_LAST_UPDATE, 0) > 6*60*60*1000) ) { //check every 6h

                        //Check if there are updates to the client
	            		if( ! Aware.is_watch(this) ) {
                            new Update_Check().execute();
                        }

	            		SharedPreferences.Editor editor = aware_preferences.edit();
	            		editor.putLong(PREF_LAST_UPDATE, System.currentTimeMillis());
	            		editor.commit();
	            	}
	            }
            }
            
            if( Aware.getSetting(getApplicationContext(), Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {    
                int frequency_webservice = Integer.parseInt(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE));
                if( frequency_webservice == 0 ) {
                    if(DEBUG) {
                        Log.d(TAG,"Data sync is disabled.");
                    }
                    alarmManager.cancel(webserviceUploadIntent);
                } else if( frequency_webservice > 0 ) {
                    //Fixed: set alarm only once if not set yet.
                    if( aware_preferences.getLong(PREF_LAST_SYNC, 0) == 0 || (aware_preferences.getLong(PREF_LAST_SYNC, 0) > 0 && System.currentTimeMillis() - aware_preferences.getLong(PREF_LAST_SYNC, 0) > frequency_webservice * 60 * 1000 ) ) {
                    	if( DEBUG ) {
                            Log.d(TAG,"Data sync every " + frequency_webservice + " minute(s)");
                        }
                    	SharedPreferences.Editor editor = aware_preferences.edit();
                    	editor.putLong(PREF_LAST_SYNC, System.currentTimeMillis());
                    	editor.commit();
                    	alarmManager.setInexactRepeating(AlarmManager.RTC, aware_preferences.getLong(PREF_LAST_SYNC, 0), frequency_webservice * 60 * 1000, webserviceUploadIntent);
                    }
                }

                //client checks if study is active
                if( getPackageName().equals("com.aware") ) {
                    new Study_Check().execute();
                }
            }
            
            if( ! Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA).equals("0") ) {
                Intent dataCleaning = new Intent(ACTION_AWARE_SPACE_MAINTENANCE);
                awareContext.sendBroadcast(dataCleaning);
            }
        } else { //Turn off all enabled plugins and services

            stopAllServices();

            Cursor enabled_plugins = getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_STATUS + "=" + Aware_Plugin.STATUS_PLUGIN_ON, null, null);
        	if( enabled_plugins != null && enabled_plugins.moveToFirst() ) {
        		do {
        			stopPlugin(getApplicationContext(), enabled_plugins.getString(enabled_plugins.getColumnIndex(Aware_Plugins.PLUGIN_PACKAGE_NAME)));
        		}while(enabled_plugins.moveToNext());
                if( Aware.DEBUG ) Log.w(TAG,"AWARE plugins disabled...");
        	}
        	if( enabled_plugins != null && ! enabled_plugins.isClosed()) enabled_plugins.close();
        }
        return START_STICKY;
    }
    
    /**
     * Stops a plugin. Expects the package name of the plugin.
     * @param context
     * @param package_name
     */
    public static void stopPlugin(Context context, String package_name ) {
        //Check if plugin is bundled within an application/plugin
        try {
            Class.forName(package_name + ".Plugin");
            Intent bundled = new Intent();
            bundled.setClassName(context.getPackageName(), package_name + ".Plugin");
            boolean result = context.stopService(bundled);
            if( result ) {
                if( Aware.DEBUG ) Log.d(TAG, "Bundled " + package_name + ".Plugin stopped...");

                ContentValues rowData = new ContentValues();
                rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_OFF);
                context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null);

                return;
            }
        } catch (ClassNotFoundException e ) {}

        ArrayList<String> stopping = new ArrayList<>();
        Cursor cached = context.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null, null);
    	if( cached != null && cached.moveToFirst() ) {
    		stopping.add(package_name);
    		ContentValues rowData = new ContentValues();
            rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_OFF);
            context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null);
    	}
    	if( cached != null && ! cached.isClosed() ) cached.close();

        for( String p : stopping ) {
            Intent plugin = new Intent();
            plugin.setClassName( package_name, package_name + ".Plugin");
            context.stopService(plugin);
            if( Aware.DEBUG ) Log.d(TAG, package_name + " stopped...");
        }

        //FIXED: terminate bundled AWARE service within a plugin
        Intent core = new Intent();
        core.setClassName( context.getPackageName(), "com.aware.Aware" );
        context.stopService(core);
        core.setClassName( package_name, "com.aware.Aware" );
    }
    
    /**
     * Starts a plugin. Expects the package name of the plugin.
     * It checks if the plugin does exist on the phone. If it doesn't, it will request the user to install it automatically.
     * @param context
     * @param package_name
     */
    public static void startPlugin(Context context, String package_name ) {

        if( awareContext == null ) awareContext = context;

        //Check if plugin is bundled within an application/plugin
        try {
            Class.forName(package_name + ".Plugin");
            Intent bundled = new Intent();
            bundled.setClassName(context.getPackageName(), package_name + ".Plugin");
            ComponentName result = context.startService(bundled);
            if( result != null ) {
                if( Aware.DEBUG ) Log.d(TAG, "Bundled " + package_name + ".Plugin started...");

                ContentValues rowData = new ContentValues();
                rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_ON);
                context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null);

                return;
            }
        } catch (ClassNotFoundException e ) {}

    	//Check if plugin is cached
    	Cursor cached = context.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null, null);
    	if( cached != null && cached.moveToFirst() ) {
            //Installed on the phone
    		if( isClassAvailable(context, package_name, "Plugin") ) {
    			Intent plugin = new Intent();
        		plugin.setClassName(package_name, package_name + ".Plugin");
        		context.startService(plugin);
        		if( Aware.DEBUG ) Log.d(TAG, package_name + " started...");
        		
        		ContentValues rowData = new ContentValues();
                rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_ON);
                context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + package_name + "'", null);

                cached.close();
                return;
    		}
    	}
        if( cached != null && ! cached.isClosed() ) cached.close();

        if( Aware.DEBUG ) Log.d(Aware.TAG, package_name + " is not installed, attempting to download from the repository...");

        //Ok, not bundled or installed, request the missing plugin from the server
        downloadPlugin(context, package_name, false);
    }

    /**
     * Requests the download of a plugin given the package name from AWARE webservices.
     * @param context
     * @param package_name
     * @param is_update
     */
    public static void downloadPlugin( Context context, String package_name, boolean is_update ) {
    	if( is_queued(package_name) ) return;

        AWARE_PLUGIN_DOWNLOAD_PACKAGES.add(package_name);

        Intent pluginIntent = new Intent(context, DownloadPluginService.class);
    	pluginIntent.putExtra("package_name", package_name);
    	pluginIntent.putExtra("is_update", is_update);
		context.startService(pluginIntent);
    }

    private static boolean is_queued(String package_name) {
        for( String pkg : AWARE_PLUGIN_DOWNLOAD_PACKAGES ) {
            if( pkg.equalsIgnoreCase( package_name ) ) return true;
        }
        return false;
    }
    
    /**
     * Given a plugin's package name, fetch the context card for reuse.
     * @param context: application context
     * @param package_name: plugin's package name
     * @return View for reuse (instance of LinearLayout)
     */
    public static View getContextCard( final Context context, final String package_name ) {
    	
    	if( ! isClassAvailable(context, package_name, "ContextCard") ) {
    		return null;
    	}
    	
    	String ui_class = package_name + ".ContextCard";
    	CardView card = new CardView(context);
    	LayoutParams params = new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );
        params.setMargins( 0,0,0,10 );
        card.setLayoutParams(params);

    	try {
			Context packageContext = context.createPackageContext(package_name, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			
			Class<?> fragment_loader = packageContext.getClassLoader().loadClass(ui_class);
            Object fragment = fragment_loader.newInstance();
            Method[] allMethods = fragment_loader.getDeclaredMethods();
            Method m = null;
            for( Method mItem : allMethods ) {
                String mName = mItem.getName();
                if( mName.contains("getContextCard") ) {
                    mItem.setAccessible(true);
                    m = mItem;
                    break;
                }
            }

            View ui = (View) m.invoke( fragment, packageContext );
			if( ui != null ) {
				//Check if plugin has settings. If it does, tapping the card shows the settings
				if( isClassAvailable(context, package_name, "Settings") ) {
					ui.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent open_settings = new Intent();
							open_settings.setClassName(package_name, package_name + ".Settings");
							open_settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(open_settings);
						}
					});
				}
				
				//Set card look-n-feel
				ui.setBackgroundColor(Color.WHITE);
				ui.setPadding(20, 20, 20, 20);
                card.addView(ui);

				return card;
			} else {
				return null;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
	 * Given a package and class name, check if the class exists or not.
	 * @param package_name
	 * @param class_name
	 * @return true if exists, false otherwise
	 */
	private static boolean isClassAvailable( Context context, String package_name, String class_name ) {
		try{
			Context package_context = context.createPackageContext(package_name, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE); 
			package_context.getClassLoader().loadClass(package_name+"."+class_name);			
		} catch ( ClassNotFoundException e ) {
			return false;
		} catch ( NameNotFoundException e ) {
			return false;
		}
		return true;
	}
    
    /**
     * Retrieve setting value given key.
     * @param key
     * @return value
     */
    public static String getSetting( Context context, String key ) {
        
    	boolean is_restricted_package = true;
    	
    	ArrayList<String> global_settings = new ArrayList<String>();
        global_settings.add(Aware_Preferences.DEBUG_FLAG);
        global_settings.add(Aware_Preferences.DEBUG_TAG);
        global_settings.add("study_id");
        global_settings.add("study_start");
        global_settings.add(Aware_Preferences.DEVICE_ID);
        global_settings.add(Aware_Preferences.STATUS_WEBSERVICE);
        global_settings.add(Aware_Preferences.FREQUENCY_WEBSERVICE);
        global_settings.add(Aware_Preferences.WEBSERVICE_WIFI_ONLY);
        global_settings.add(Aware_Preferences.WEBSERVICE_SERVER);
        global_settings.add(Aware_Preferences.STATUS_APPLICATIONS);
        //allow plugin's to react to MQTT
        global_settings.add(Aware_Preferences.STATUS_MQTT);
        global_settings.add(Aware_Preferences.MQTT_USERNAME);
        global_settings.add(Aware_Preferences.MQTT_PASSWORD);
        global_settings.add(Aware_Preferences.MQTT_SERVER);
        global_settings.add(Aware_Preferences.MQTT_PORT);
        global_settings.add(Aware_Preferences.MQTT_PROTOCOL);
        global_settings.add(Aware_Preferences.MQTT_KEEP_ALIVE);
        global_settings.add(Aware_Preferences.MQTT_QOS);
    	
    	if( global_settings.contains(key) ) {
    		is_restricted_package = false;
    	}
    	
    	String value = "";
        Cursor qry = context.getContentResolver().query(Aware_Settings.CONTENT_URI, null, Aware_Settings.SETTING_KEY + " LIKE '" + key + "'" + ( is_restricted_package ? " AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + context.getPackageName() + "'" : ""), null, null);
        if( qry != null && qry.moveToFirst() ) {
            value = qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE));
        }
        if( qry != null && ! qry.isClosed() ) qry.close();
        return value;
    }

    /**
     * Retrieve setting value given a key of a plugin's settings
     * @param context
     * @param key
     * @param package_name
     * @return value
     */
    public static String getSetting( Context context, String key, String package_name ) {
        if( package_name.equals("com.aware") ) {
            return getSetting(context, key);
        }

        String value = "";
        Cursor qry = context.getContentResolver().query(Aware_Settings.CONTENT_URI, null, Aware_Settings.SETTING_KEY + " LIKE '" + key + "' AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + package_name + "'", null, null);
        if( qry != null && qry.moveToFirst() ) {
            value = qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE));
        }
        if( qry != null && ! qry.isClosed() ) qry.close();
        return value;
    }
    
    /**
     * Insert / Update settings of the framework
     * @param key
     * @param value
     */
    public static void setSetting( Context context, String key, Object value ) {
        
    	boolean is_restricted_package = true;
    	
    	ArrayList<String> global_settings = new ArrayList<String>();
    	global_settings.add(Aware_Preferences.DEBUG_FLAG);
    	global_settings.add(Aware_Preferences.DEBUG_TAG);
    	global_settings.add("study_id");
    	global_settings.add("study_start");
        global_settings.add(Aware_Preferences.DEVICE_ID);
        global_settings.add(Aware_Preferences.STATUS_WEBSERVICE);
        global_settings.add(Aware_Preferences.FREQUENCY_WEBSERVICE);
        global_settings.add(Aware_Preferences.WEBSERVICE_WIFI_ONLY);
        global_settings.add(Aware_Preferences.WEBSERVICE_SERVER);
        global_settings.add(Aware_Preferences.STATUS_APPLICATIONS); //allow plugins to get accessibility events
        //allow plugin's to react to MQTT
        global_settings.add(Aware_Preferences.STATUS_MQTT);
        global_settings.add(Aware_Preferences.MQTT_USERNAME);
        global_settings.add(Aware_Preferences.MQTT_PASSWORD);
        global_settings.add(Aware_Preferences.MQTT_SERVER);
        global_settings.add(Aware_Preferences.MQTT_PORT);
        global_settings.add(Aware_Preferences.MQTT_PROTOCOL);
        global_settings.add(Aware_Preferences.MQTT_KEEP_ALIVE);
        global_settings.add(Aware_Preferences.MQTT_QOS);

    	if( global_settings.contains(key) ) {
    		is_restricted_package = false;
    	}

        //Only the client can set the Device ID
        if( key.equals(Aware_Preferences.DEVICE_ID) && ! context.getPackageName().equals("${applicationId}") ) return;

    	ContentValues setting = new ContentValues();
        setting.put(Aware_Settings.SETTING_KEY, key);
        setting.put(Aware_Settings.SETTING_VALUE, value.toString());
        setting.put(Aware_Settings.SETTING_PACKAGE_NAME, context.getPackageName());

        Cursor qry = context.getContentResolver().query(Aware_Settings.CONTENT_URI, null, Aware_Settings.SETTING_KEY + " LIKE '" + key + "'" + (is_restricted_package ? " AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + context.getPackageName() + "'" : ""), null, null);
        //update
        if( qry != null && qry.moveToFirst() ) {
            try {
                if( ! qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE)).equals(value.toString()) ) {
                    context.getContentResolver().update(Aware_Settings.CONTENT_URI, setting, Aware_Settings.SETTING_ID + "=" + qry.getInt(qry.getColumnIndex(Aware_Settings.SETTING_ID)), null);
                    if( Aware.DEBUG) Log.d(Aware.TAG,"Updated: "+key+"="+value);
                }
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
        //insert
        } else {
            try {
                context.getContentResolver().insert(Aware_Settings.CONTENT_URI, setting);
                if( Aware.DEBUG) Log.d(Aware.TAG,"Added: " + key + "=" + value);
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
        }
        if( qry != null && ! qry.isClosed() ) qry.close();
    }

    /**
     * Insert / Update settings of a plugin
     * @param key
     * @param value
     * @param package_name
     */
    public static void setSetting( Context context, String key, Object value, String package_name ) {

        if( package_name.equals("com.aware") || key.equals(Aware_Preferences.DEVICE_ID) ) {
            setSetting(context, key, value);
            return;
        }

        ContentValues setting = new ContentValues();
        setting.put(Aware_Settings.SETTING_KEY, key);
        setting.put(Aware_Settings.SETTING_VALUE, value.toString());
        setting.put(Aware_Settings.SETTING_PACKAGE_NAME, package_name);

        Cursor qry = context.getContentResolver().query(Aware_Settings.CONTENT_URI, null, Aware_Settings.SETTING_KEY + " LIKE '" + key + "' AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + package_name + "'", null, null);
        //update
        if( qry != null && qry.moveToFirst() ) {
            try {
                if( ! qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE)).equals(value.toString()) ) {
                    context.getContentResolver().update(Aware_Settings.CONTENT_URI, setting, Aware_Settings.SETTING_ID + "=" + qry.getInt(qry.getColumnIndex(Aware_Settings.SETTING_ID)), null);
                    if( Aware.DEBUG) Log.d(Aware.TAG,"Updated: "+key+"="+value + " in " + package_name);
                }
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
            //insert
        } else {
            try {
                context.getContentResolver().insert(Aware_Settings.CONTENT_URI, setting);
                if( Aware.DEBUG) Log.d(Aware.TAG,"Added: " + key + "=" + value + " in " + package_name);
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
        }
        if( qry != null && ! qry.isClosed() ) qry.close();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if( repeatingIntent != null ) alarmManager.cancel(repeatingIntent);
        if( webserviceUploadIntent != null) alarmManager.cancel(webserviceUploadIntent);
        
        if( aware_BR != null ) awareContext.unregisterReceiver(aware_BR);
        if( storage_BR != null ) awareContext.unregisterReceiver(storage_BR);
    }

    /**
     * Client: check if a certain study is still ongoing, resets client otherwise.
     */
    private class Study_Check extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new BasicNameValuePair(Aware_Preferences.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID)));
            HttpResponse response = new Https(getApplicationContext()).dataPOST(Aware.getSetting(getApplicationContext(),Aware_Preferences.WEBSERVICE_SERVER), data, true);
            if( response != null && response.getStatusLine().getStatusCode() == 200) {
                try {
                    String json_str = Https.undoGZIP(response);
                    JSONArray j_array = new JSONArray(json_str);
                    JSONObject io = j_array.getJSONObject(0);
                    if( io.has("message") ) {
                        if( io.getString("message").equals("This study is not ongoing anymore.") ) return true;
                    }
                    return false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean is_closed) {
            super.onPostExecute(is_closed);
            if( is_closed ) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.ic_action_aware_studies);
                mBuilder.setContentTitle("AWARE");
                mBuilder.setContentText("The study has ended! Thanks!");
                mBuilder.setAutoCancel(true);

                NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notManager.notify(new Random(System.currentTimeMillis()).nextInt(), mBuilder.build());

                reset(getApplicationContext());
            }
        }
    }

    public static void reset(Context c) {
        if( ! c.getPackageName().equals("com.aware") ) return;

        String device_id = Aware.getSetting( c, Aware_Preferences.DEVICE_ID );

        //Remove all settings
        c.getContentResolver().delete( Aware_Settings.CONTENT_URI, null, null );

        //Read default client settings
        SharedPreferences prefs = c.getSharedPreferences( c.getPackageName(), Context.MODE_PRIVATE );
        PreferenceManager.setDefaultValues(c, c.getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, true);
        prefs.edit().commit();

        Map<String,?> defaults = prefs.getAll();
        for(Map.Entry<String, ?> entry : defaults.entrySet()) {
            Aware.setSetting(c, entry.getKey(), entry.getValue());
        }

        //Keep previous AWARE Device ID
        Aware.setSetting(c, Aware_Preferences.DEVICE_ID, device_id);

        //Turn off all active plugins
        Cursor active_plugins = c.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_STATUS + "=" + Plugins_Manager.PLUGIN_ACTIVE, null, null);
        if( active_plugins != null && active_plugins.moveToFirst() ) {
            do {
                Aware.stopPlugin(c, active_plugins.getString(active_plugins.getColumnIndex(Aware_Plugins.PLUGIN_PACKAGE_NAME)));
            } while(active_plugins.moveToNext());
        }
        if( active_plugins != null && ! active_plugins.isClosed() ) active_plugins.close();
    }

    private class CheckPlugins extends AsyncTask<ArrayList<String>, Void, Boolean> {
        private ArrayList<String> updated = new ArrayList<>();
        @Override
        protected Boolean doInBackground(ArrayList<String>... params) {
            for( String package_name : params[0] ) {
                HttpResponse http_request = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/plugins/get_plugin/" + package_name, true);
                if( http_request != null && http_request.getStatusLine().getStatusCode() == 200 ) {
                    try {
                        String json_string = Https.undoGZIP(http_request);
                        if( ! json_string.equals("[]") ) {
                            try {
                                JSONObject json_package = new JSONObject(json_string);
                                if( json_package.getInt("version") > Plugins_Manager.getVersion(getApplicationContext(), package_name) ) {
                                    updated.add(package_name);
                                }
                            } catch (JSONException e) {
                            }
                        }
                    } catch (ParseException e) {}
                }
            }
            if( updated.size() > 0 ) return true;
            return false;
        }

        @Override
        protected void onPostExecute(Boolean updates) {
            super.onPostExecute(updates);
            if( updates ) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.ic_stat_aware_plugin_dependency);
                mBuilder.setContentTitle("AWARE Update");
                mBuilder.setContentText("Found " + updated.size() + " updated plugin(s). Install?");
                mBuilder.setAutoCancel(true);

                Intent updateIntent = new Intent(getApplicationContext(), UpdatePlugins.class);
                updateIntent.putExtra("updated", updated);

                PendingIntent clickIntent = PendingIntent.getService(getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(clickIntent);
                NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notManager.notify(updated.size(), mBuilder.build());
            }
        }
    }

    /**
     * Client: check if there is an update to the client.
     */
    private class Update_Check extends AsyncTask<Void, Void, Boolean> {
    	String filename = "", whats_new = "";
    	int version = 0;
    	PackageInfo awarePkg = null;
    	
    	@Override
    	protected Boolean doInBackground(Void... params) {
    		try {
				awarePkg = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
				return false;
			}
			
    		HttpResponse response = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/awaredev/framework_latest", true);
	        if( response != null && response.getStatusLine().getStatusCode() == 200 ) {
	        	try {
					JSONArray data = new JSONArray(Https.undoGZIP(response));
					JSONObject latest_framework = data.getJSONObject(0);
					
					if( Aware.DEBUG ) Log.d(Aware.TAG, "Latest: " + latest_framework.toString());
					
					filename = latest_framework.getString("filename");
					version = latest_framework.getInt("version");
					whats_new = latest_framework.getString("whats_new");
					
					if( version > awarePkg.versionCode ) {
						return true;
					}
					return false;
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
	        } else {
	        	if( Aware.DEBUG ) Log.d(Aware.TAG, "Unable to fetch latest framework from AWARE repository...");
	        }
    		return false;
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean result) {
    		super.onPostExecute(result);
    		if( result ) {
    			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
    			mBuilder.setSmallIcon(R.drawable.ic_stat_aware_update);
    			mBuilder.setContentTitle("AWARE Update");
    			mBuilder.setContentText("Version: " + version + ". Install?");
                mBuilder.setAutoCancel(true);
    			
    			Intent updateIntent = new Intent(getApplicationContext(), UpdateFrameworkService.class);
    			updateIntent.putExtra("filename", filename);
    			
    			PendingIntent clickIntent = PendingIntent.getService(getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    			mBuilder.setContentIntent(clickIntent);
    			NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    			notManager.notify(version, mBuilder.build());
    		}
    	}
    }
    
    /**
     * Client's plugin monitor
     * - Installs a plugin that was just downloaded
     * - Checks if a package is a plugin or not
     * @author denzilferreira
     */
    public static class PluginMonitor extends BroadcastReceiver {
    	private static PackageManager mPkgManager;
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		mPkgManager = context.getPackageManager();
    		
        	Bundle extras = intent.getExtras();
            Uri packageUri = intent.getData();
            if( packageUri == null ) return;
            String packageName = packageUri.getSchemeSpecificPart();
            if( packageName == null ) return;
            
            if( ! packageName.matches("com.aware.plugin.*") ) return;
        	
            if( intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) ) {
                //Updating for new package
                if( extras.getBoolean(Intent.EXTRA_REPLACING) ) {
                    if(Aware.DEBUG) Log.d(TAG, packageName + " is updating!");
                    
                    ContentValues rowData = new ContentValues();
                    rowData.put(Aware_Plugins.PLUGIN_VERSION, Plugins_Manager.getVersion(context, packageName));
                    
                    Cursor current_status = context.getContentResolver().query(Aware_Plugins.CONTENT_URI, new String[]{Aware_Plugins.PLUGIN_STATUS}, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null, null);
                    if( current_status != null && current_status.moveToFirst() ) {
                        if( current_status.getInt(current_status.getColumnIndex(Aware_Plugins.PLUGIN_STATUS)) == Plugins_Manager.PLUGIN_UPDATED ) { //was updated, set to active now
                        	rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_ON);
                        }
                    }
                    if( current_status != null && ! current_status.isClosed() ) current_status.close();
                    
                    context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null);
                    
                    //Refresh plugin manager UI if visible
                    context.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));
                    
                    //Refresh stream UI if visible
                    context.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));

                    //Start plugin
                    Aware.startPlugin(context, packageName);

                    return;
                }
                
                //Installing new
                try {
                    ApplicationInfo appInfo = mPkgManager.getApplicationInfo( packageName, PackageManager.GET_ACTIVITIES );
                    //Check if this is a package for which we have more info from the server
                    new Plugin_Info_Async().execute(appInfo);
                } catch( final NameNotFoundException e ) {
                	e.printStackTrace();
                }
            }
            
            if( intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) ) {
                //Updating
                if( extras.getBoolean(Intent.EXTRA_REPLACING) ) {
                    //this is an update, bail out.
                    return;
                }

                //Deleting
                context.getContentResolver().delete(Aware_Plugins.CONTENT_URI, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null);
                if( Aware.DEBUG ) Log.d(TAG,"AWARE plugin removed:" + packageName);

                //Refresh stream UI if visible
                context.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));
                
                //Refresh Plugin manager UI if visible
                context.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));
            }
    	}
    }
    
    /**
     * Fetches info from webservices on installed plugins.
     * @author denzilferreira
     *
     */
    private static class Plugin_Info_Async extends AsyncTask<ApplicationInfo, Void, JSONObject> {
		private ApplicationInfo app;
        private byte[] icon;
    	
    	@Override
		protected JSONObject doInBackground(ApplicationInfo... params) {
			
    		app = params[0];
    		
    		JSONObject json_package = null;
            HttpResponse http_request = new Https(awareContext).dataGET("https://api.awareframework.com/index.php/plugins/get_plugin/" + app.packageName, true);
            if( http_request != null && http_request.getStatusLine().getStatusCode() == 200 ) {
            	try {
            		String json_string = Https.undoGZIP(http_request);
            		if( ! json_string.trim().equalsIgnoreCase("[]") ) {
            			json_package = new JSONObject(json_string);
                        icon = Plugins_Manager.cacheImage("http://api.awareframework.com" + json_package.getString("iconpath"), awareContext);
            		}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
            return json_package;
		}
    	
		@Override
		protected void onPostExecute(JSONObject json_package) {
			super.onPostExecute(json_package);
			
			//If we already have cached information for this package, just update it
			boolean is_cached = false;
			Cursor plugin_cached = awareContext.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + app.packageName + "'", null, null);
			if( plugin_cached != null && plugin_cached.moveToFirst() ) {
				is_cached = true;
			}
			if( plugin_cached != null && ! plugin_cached.isClosed()) plugin_cached.close();
			
			ContentValues rowData = new ContentValues();
            rowData.put(Aware_Plugins.PLUGIN_PACKAGE_NAME, app.packageName);
            rowData.put(Aware_Plugins.PLUGIN_NAME, app.loadLabel(awareContext.getPackageManager()).toString());
            rowData.put(Aware_Plugins.PLUGIN_VERSION, Plugins_Manager.getVersion(awareContext, app.packageName));
            rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_ON);
            if( json_package != null ) {
            	try {
            		rowData.put(Aware_Plugins.PLUGIN_ICON, icon);
					rowData.put(Aware_Plugins.PLUGIN_AUTHOR, json_package.getString("first_name") + " " + json_package.getString("last_name") + " - " + json_package.getString("email"));
					rowData.put(Aware_Plugins.PLUGIN_DESCRIPTION, json_package.getString("desc"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
            
            if( ! is_cached ) {
            	awareContext.getContentResolver().insert(Aware_Plugins.CONTENT_URI, rowData);
            } else {
            	awareContext.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + app.packageName + "'", null);
            }
            
            if( Aware.DEBUG ) Log.d(TAG,"AWARE plugin added and activated:" + app.packageName);
            
            //Refresh stream UI if visible
            awareContext.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));
            
            //Refresh Plugin Manager UI if visible
            awareContext.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));

            //Start plugin
            Aware.startPlugin(awareContext, app.packageName);
		}
    }

    public static class UpdatePlugins extends IntentService {
        public UpdatePlugins() {super("Update Plugins service");}

        @Override
        protected void onHandleIntent(Intent intent) {
            ArrayList<String> packages = intent.getStringArrayListExtra("updated");
            for( String package_name : packages ) {
                Aware.downloadPlugin(getApplicationContext(), package_name, true);
            }
        }
    }

    /**
     * Background service to download latest version of AWARE
     * @author denzilferreira
     *
     */
    public static class UpdateFrameworkService extends IntentService {
		public UpdateFrameworkService() {
			super("Update Framework service");			
		}
		@Override
		protected void onHandleIntent(Intent intent) {
			String filename = intent.getStringExtra("filename");
			
			//Make sure we have the releases folder
			File releases = new File(Environment.getExternalStorageDirectory()+"/AWARE/releases/");
			releases.mkdirs();
			
			String url = "http://www.awareframework.com/" + filename;
			
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
			request.setDescription("Updating AWARE...");
			request.setTitle("AWARE Update");
			request.setDestinationInExternalPublicDir("/", "AWARE/releases/"+filename);
			DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			AWARE_FRAMEWORK_DOWNLOAD_ID = manager.enqueue(request);
		}
    }
    
    /**
     * BroadcastReceiver that monitors for AWARE framework actions:
     * - ACTION_AWARE_SYNC_DATA = upload data to remote webservice server.
     * - ACTION_AWARE_CLEAR_DATA = clears local device's AWARE modules databases.
     * - ACTION_AWARE_REFRESH - apply changes to the configuration.
     * - {@link DownloadManager#ACTION_DOWNLOAD_COMPLETE} - when AWARE framework update has been downloaded.
     * - {}@link WifiManager#WIFI_STATE_CHANGED_ACTION} - when Wi-Fi is available to sync
     * @author denzil
     *
     */
    public static class Aware_Broadcaster extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //We are only synching the device information, not aware's settings and active plugins.
        	String[] DATABASE_TABLES = Aware_Provider.DATABASE_TABLES;
        	String[] TABLES_FIELDS = Aware_Provider.TABLES_FIELDS;
        	Uri[] CONTEXT_URIS = new Uri[]{ Aware_Device.CONTENT_URI };
        	
        	if( intent.getAction().equals(Aware.ACTION_AWARE_SYNC_DATA) && Aware.getSetting(context, Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {
            	Intent webserviceHelper = new Intent( context, WebserviceHelper.class );
                webserviceHelper.setAction( WebserviceHelper.ACTION_AWARE_WEBSERVICE_SYNC_TABLE );
    			webserviceHelper.putExtra( WebserviceHelper.EXTRA_TABLE, DATABASE_TABLES[0] );
        		webserviceHelper.putExtra( WebserviceHelper.EXTRA_FIELDS, TABLES_FIELDS[0] );
        		webserviceHelper.putExtra( WebserviceHelper.EXTRA_CONTENT_URI, CONTEXT_URIS[0].toString() );
        		context.startService(webserviceHelper);
            }

            //Monitor if the user just connected to Wi-Fi and the client is supposed to sync the data to a study when he does
            if( intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ) {
                int wifi_state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if( wifi_state == WifiManager.WIFI_STATE_ENABLED ) {
                    if( Aware.getSetting(context, Aware_Preferences.WEBSERVICE_WIFI_ONLY).equals("true") && Aware.getSetting(context, Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {
                        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if( activeNetwork != null && activeNetwork.isConnectedOrConnecting() && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ) {
                            if(Aware.DEBUG) Log.d(Aware.TAG, "Internet is available, let's sync!");
                            context.sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
                        }
                    }
                }
            }
        	
            if( intent.getAction().equals(Aware.ACTION_AWARE_CLEAR_DATA) ) {
                context.getContentResolver().delete(Aware_Provider.Aware_Device.CONTENT_URI, null, null);
                if( Aware.DEBUG ) Log.d(TAG,"Cleared " + CONTEXT_URIS[0]);
                
                //Clear remotely if webservices are active
                if( Aware.getSetting(context, Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {
	        		Intent webserviceHelper = new Intent( context, WebserviceHelper.class );
                    webserviceHelper.setAction(WebserviceHelper.ACTION_AWARE_WEBSERVICE_CLEAR_TABLE );
	        		webserviceHelper.putExtra( WebserviceHelper.EXTRA_TABLE, DATABASE_TABLES[0] );
	        		context.startService(webserviceHelper);
                }
            }

            if( intent.getAction().equals(Aware.ACTION_QUIT_STUDY) ) {
                Aware.reset(context);

                if( context.getPackageName().equals("com.aware") ) {
                    Intent preferences = new Intent(context, Aware_Preferences.class);
                    preferences.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(preferences);
                }
            }

            if( intent.getAction().equals(Aware.ACTION_AWARE_REFRESH)) {
                Intent refresh = new Intent(context, Aware.class);
                context.startService(refresh);
            }
            
            if( intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) ) {

            	DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            	long downloaded_id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            	
            	if( downloaded_id == AWARE_FRAMEWORK_DOWNLOAD_ID ) {
            		if( Aware.DEBUG ) Log.d(Aware.TAG, "AWARE framework update received...");
            		Query qry = new Query();
            		qry.setFilterById(AWARE_FRAMEWORK_DOWNLOAD_ID);
            		Cursor data = manager.query(qry);
            		if( data != null && data.moveToFirst() ) {
            			if( data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL ) {
            				String filePath = data.getString(data.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            				File mFile = new File( Uri.parse(filePath).getPath() );
            				Intent promptUpdate = new Intent(Intent.ACTION_VIEW);
            				promptUpdate.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
            				promptUpdate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            				context.startActivity(promptUpdate);
            			}
            		}
            		if( data != null && ! data.isClosed() ) data.close();

                    return;
            	}

            	if( AWARE_PLUGIN_DOWNLOAD_IDS.size() > 0 ) {
            		for( int i = 0; i < AWARE_PLUGIN_DOWNLOAD_IDS.size(); i++ ) {
                	    long queue = AWARE_PLUGIN_DOWNLOAD_IDS.get(i);
                	    if( queue == downloaded_id ) {
                            Cursor cur = manager.query(new Query().setFilterById(queue));
                            if( cur != null && cur.moveToFirst() ) {
                                if( cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL ) {
                                    String filePath = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                                    if( Aware.DEBUG ) Log.d(Aware.TAG, "Plugin to install: " + filePath);

                                    File mFile = new File( Uri.parse(filePath).getPath() );
                                    if( ! Aware.is_watch(context) ) {
                                        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                                        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        promptInstall.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
                                        context.startActivity(promptInstall);
                                    }
                                }
                            }
                            if( cur != null && ! cur.isClosed() ) cur.close();
                            AWARE_PLUGIN_DOWNLOAD_IDS.remove(downloaded_id);//dequeue
                	    }
                	}
            	}
                if( AWARE_PLUGIN_DOWNLOAD_IDS.size() == 0 ) AWARE_PLUGIN_DOWNLOAD_PACKAGES.clear();
            }
        }
    }
    private static final Aware_Broadcaster aware_BR = new Aware_Broadcaster();

    /**
     * Checks if we have access to the storage of the device. Turns off AWARE when we don't, turns it back on when available again.
     */
    public static class Storage_Broadcaster extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ) {
                if( Aware.DEBUG ) Log.d(TAG,"Resuming AWARE data logging...");
            }
            if ( intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED) ) {
                if( Aware.DEBUG ) Log.w(TAG,"Stopping AWARE data logging until the SDCard is available again...");
            }
            Intent aware = new Intent(context, Aware.class);
            context.startService(aware);
        }
    }
    private static final Storage_Broadcaster storage_BR = new Storage_Broadcaster();
    
    /**
     * Start active services
     */
    protected void startAllServices() {
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_APPLICATIONS).equals("true")) {
            startApplications();
        }else stopApplications();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_ACCELEROMETER).equals("true") ) {
            startAccelerometer();
        }else stopAccelerometer();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_INSTALLATIONS).equals("true")) {
            startInstallations();
        }else stopInstallations();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_GPS).equals("true") 
         || Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_NETWORK).equals("true") ) {
            startLocations();
        }else stopLocations();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_BLUETOOTH).equals("true") ) {
            startBluetooth();
        }else stopBluetooth();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_SCREEN).equals("true") ) {
            startScreen();
        }else stopScreen();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_BATTERY).equals("true") ) {
            startBattery();
        }else stopBattery();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_NETWORK_EVENTS).equals("true") ) {
            startNetwork();
        }else stopNetwork();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_NETWORK_TRAFFIC).equals("true") ) {
            startTraffic();
        }else stopTraffic();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_COMMUNICATION_EVENTS).equals("true") 
    	 || Aware.getSetting(awareContext, Aware_Preferences.STATUS_CALLS).equals("true") 
    	 || Aware.getSetting(awareContext, Aware_Preferences.STATUS_MESSAGES).equals("true") ) {
            startCommunication();
        }else stopCommunication();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_PROCESSOR).equals("true") ) {
            startProcessor();
        }else stopProcessor();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_TIMEZONE).equals("true") ) {
            startTimeZone();
        }else stopTimeZone();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_MQTT).equals("true") ) {
            startMQTT();
        }else stopMQTT();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_GYROSCOPE).equals("true") ) {
            startGyroscope();
        }else stopGyroscope();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_WIFI).equals("true") ) {
            startWiFi();
        }else stopWiFi();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_TELEPHONY).equals("true") ) {
            startTelephony();
        }else stopTelephony();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_ROTATION).equals("true") ) {
            startRotation();
        }else stopRotation();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_LIGHT).equals("true") ) {
            startLight();
        }else stopLight();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_PROXIMITY).equals("true") ) {
            startProximity();
        }else stopProximity();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_MAGNETOMETER).equals("true") ) {
            startMagnetometer();
        }else stopMagnetometer();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_BAROMETER).equals("true") ) {
            startBarometer();
        }else stopBarometer();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_GRAVITY).equals("true") ) {
            startGravity();
        }else stopGravity();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_LINEAR_ACCELEROMETER).equals("true") ) {
            startLinearAccelerometer();
        }else stopLinearAccelerometer();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_TEMPERATURE).equals("true") ) {
            startTemperature();
        }else stopTemperature();
        
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_ESM).equals("true") ) {
            startESM();
        }else stopESM();

        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_KEYBOARD).equals("true") ) {
            startKeyboard();
        }else stopKeyboard();

        if( getPackageName().equals("com.aware") ) {
            awareContext.startService(wearClient);
        }
    }
    
    /**
     * Stop all services
     */
    protected void stopAllServices() {
        stopApplications();
        stopAccelerometer();
        stopBattery();
        stopBluetooth();
        stopCommunication();
        stopLocations();
        stopNetwork();
        stopTraffic();
        stopScreen();
        stopProcessor();
        stopMQTT();
        stopGyroscope();
        stopWiFi();
        stopTelephony();
        stopTimeZone();
        stopRotation();
        stopLight();
        stopProximity();
        stopMagnetometer();
        stopBarometer();
        stopGravity();
        stopLinearAccelerometer();
        stopTemperature();
        stopESM();
        stopInstallations();
        stopKeyboard();

        if( getPackageName().equals("com.aware") ) {
            awareContext.stopService(wearClient);
        }
    }

    /**
     * Start keyboard module
     */
    protected void startKeyboard() {
        if( keyboard == null ) keyboard = new Intent(awareContext, Keyboard.class);
        awareContext.startService(keyboard);
    }

    /**
     * Stop keyboard module
     */
    protected void stopKeyboard() {
        if( keyboard != null ) awareContext.stopService(keyboard);
    }

    /**
     * Start Applications module
     */
    protected void startApplications() {
        if( applicationsSrv == null) applicationsSrv = new Intent(awareContext, Applications.class);
        awareContext.startService(applicationsSrv);
    }
    
    /**
     * Stop Applications module
     */
    protected void stopApplications() {
        if( applicationsSrv != null) awareContext.stopService(applicationsSrv);
    }
    
    /**
     * Start Installations module
     */
    protected void startInstallations() {
        if(installationsSrv == null) installationsSrv = new Intent(awareContext, Installations.class);
        awareContext.startService(installationsSrv);
    }
    
    /**
     * Stop Installations module
     */
    protected void stopInstallations() {
        if(installationsSrv != null) awareContext.stopService(installationsSrv);
    }
    
    /**
     * Start ESM module
     */
    protected void startESM() {
        if( esmSrv == null ) esmSrv = new Intent(awareContext, ESM.class);
        awareContext.startService(esmSrv);
    }
    
    /**
     * Stop ESM module
     */
    protected void stopESM() {
        if( esmSrv != null ) awareContext.stopService(esmSrv);
    }
    
    /**
     * Start Temperature module
     */
    protected void startTemperature() {
        if( temperatureSrv == null ) temperatureSrv = new Intent(awareContext, Temperature.class);
        awareContext.startService(temperatureSrv);
    }
    
    /**
     * Stop Temperature module
     */
    protected void stopTemperature() {
        if( temperatureSrv != null ) awareContext.stopService(temperatureSrv);
    }
    
    /**
     * Start Linear Accelerometer module
     */
    protected void startLinearAccelerometer() {
        if( linear_accelSrv == null ) linear_accelSrv = new Intent(awareContext, LinearAccelerometer.class);
        awareContext.startService(linear_accelSrv);
    }
    
    /**
     * Stop Linear Accelerometer module
     */
    protected void stopLinearAccelerometer() {
        if( linear_accelSrv != null ) awareContext.stopService(linear_accelSrv);
    }
    
    /**
     * Start Gravity module
     */
    protected void startGravity() {
        if( gravitySrv == null ) gravitySrv = new Intent(awareContext, Gravity.class);
        awareContext.startService(gravitySrv);
    }
    
    /**
     * Stop Gravity module
     */
    protected void stopGravity() {
        if( gravitySrv != null ) awareContext.stopService(gravitySrv);
    }
    
    /**
     * Start Barometer module
     */
    protected void startBarometer() {
        if( barometerSrv == null ) barometerSrv = new Intent(awareContext, Barometer.class);
        awareContext.startService(barometerSrv);
    }
    
    /**
     * Stop Barometer module
     */
    protected void stopBarometer() {
        if( barometerSrv != null ) awareContext.stopService(barometerSrv);
    }
    
    /**
     * Start Magnetometer module
     */
    protected void startMagnetometer() {
        if( magnetoSrv == null ) magnetoSrv = new Intent(awareContext, Magnetometer.class);
        awareContext.startService(magnetoSrv);
    }
    
    /**
     * Stop Magnetometer module
     */
    protected void stopMagnetometer() {
        if( magnetoSrv != null ) awareContext.stopService(magnetoSrv);
    }
    
    /**
     * Start Proximity module
     */
    protected void startProximity() {
        if( proximitySrv == null ) proximitySrv = new Intent(awareContext, Proximity.class);
        awareContext.startService(proximitySrv);
    }
    
    /**
     * Stop Proximity module
     */
    protected void stopProximity() {
        if( proximitySrv != null ) awareContext.stopService(proximitySrv);
    }
    
    /**
     * Start Light module
     */
    protected void startLight() {
        if( lightSrv == null ) lightSrv = new Intent(awareContext, Light.class);
        awareContext.startService(lightSrv);
    }
    
    /**
     * Stop Light module
     */
    protected void stopLight() {
        if( lightSrv != null ) awareContext.stopService(lightSrv);
    }
    
    /**
     * Start Rotation module
     */
    protected void startRotation() {
        if( rotationSrv == null ) rotationSrv = new Intent(awareContext, Rotation.class);
        awareContext.startService(rotationSrv);
    }
    
    /**
     * Stop Rotation module
     */
    protected void stopRotation() {
        if( rotationSrv != null ) awareContext.stopService(rotationSrv);
    }
    
    /**
     * Start the Telephony module
     */
    protected void startTelephony() {
        if( telephonySrv == null) telephonySrv = new Intent(awareContext, Telephony.class);
        awareContext.startService(telephonySrv);
    }
    
    /**
     * Stop the Telephony module
     */
    protected void stopTelephony() {
        if( telephonySrv != null ) awareContext.stopService(telephonySrv);
    }
    
    /**
     * Start the WiFi module
     */
    protected void startWiFi() {
        if( wifiSrv == null ) wifiSrv = new Intent(awareContext, WiFi.class);
        awareContext.startService(wifiSrv);
    }
    
    protected void stopWiFi() {
        if( wifiSrv != null ) awareContext.stopService(wifiSrv);
    }
    
    /**
     * Start the gyroscope module
     */
    protected void startGyroscope() {
        if( gyroSrv == null ) gyroSrv = new Intent(awareContext, Gyroscope.class);
        awareContext.startService(gyroSrv);
    }
    
    /**
     * Stop the gyroscope module
     */
    protected void stopGyroscope() {
        if( gyroSrv != null ) awareContext.stopService(gyroSrv);
    }
    
    /**
     * Start the accelerometer module
     */
    protected void startAccelerometer() {
        if( accelerometerSrv == null ) accelerometerSrv = new Intent(awareContext, Accelerometer.class);
        awareContext.startService(accelerometerSrv);
    }
    
    /**
     * Stop the accelerometer module
     */
    protected void stopAccelerometer() {
        if( accelerometerSrv != null) awareContext.stopService(accelerometerSrv);
    }
    
    /**
     * Start the Processor module
     */
    protected void startProcessor() {
        if( processorSrv == null) processorSrv = new Intent(awareContext, Processor.class);
        awareContext.startService(processorSrv);
    }
    
    /**
     * Stop the Processor module
     */
    protected void stopProcessor() {
        if( processorSrv != null ) awareContext.stopService(processorSrv);
    }
    
    /**
     * Start the locations module
     */
    protected void startLocations() {
        if( locationsSrv == null) locationsSrv = new Intent(awareContext, Locations.class);
        awareContext.startService(locationsSrv);
    }
    
    /**
     * Stop the locations module
     */
    protected void stopLocations() {
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_GPS).equals("false") 
         && Aware.getSetting(awareContext, Aware_Preferences.STATUS_LOCATION_NETWORK).equals("false") ) {
            if(locationsSrv != null) awareContext.stopService(locationsSrv);
        }
    }
    
    /**
     * Start the bluetooth module
     */
    protected void startBluetooth() {
        if( bluetoothSrv == null) bluetoothSrv = new Intent(awareContext, Bluetooth.class);
        awareContext.startService(bluetoothSrv);
    }
    
    /**
     * Stop the bluetooth module
     */
    protected void stopBluetooth() {
        if(bluetoothSrv != null) awareContext.stopService(bluetoothSrv);
    }
    
    /**
     * Start the screen module
     */
    protected void startScreen() {
        if( screenSrv == null) screenSrv = new Intent(awareContext, Screen.class);
        awareContext.startService(screenSrv);
    }
    
    /**
     * Stop the screen module
     */
    protected void stopScreen() {
        if(screenSrv != null) awareContext.stopService(screenSrv);
    }
    
    /**
     * Start battery module
     */
    protected void startBattery() {
        if( batterySrv == null) batterySrv = new Intent(awareContext, Battery.class);
        awareContext.startService(batterySrv);
    }
    
    /**
     * Stop battery module
     */
    protected void stopBattery() {
        if(batterySrv != null) awareContext.stopService(batterySrv);
    }
    
    /**
     * Start network module
     */
    protected void startNetwork() {
        if( networkSrv == null ) networkSrv = new Intent(awareContext, Network.class);
        awareContext.startService(networkSrv);
    }
    
    /**
     * Stop network module
     */
    protected void stopNetwork() {
        if(networkSrv != null) awareContext.stopService(networkSrv);
    }
    
    /**
     * Start traffic module
     */
    protected void startTraffic() {
        if(trafficSrv == null) trafficSrv = new Intent(awareContext, Traffic.class);
        awareContext.startService(trafficSrv);
    }
    
    /**
     * Stop traffic module
     */
    protected void stopTraffic() {
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_NETWORK_TRAFFIC).equals("false") ) {
            if( trafficSrv != null ) awareContext.stopService(trafficSrv);
        }
    }
    
    /**
     * Start the TimeZone module
     */
    protected void startTimeZone() {
        if(timeZoneSrv == null) timeZoneSrv = new Intent(awareContext, TimeZone.class);
        awareContext.startService(timeZoneSrv);
    }
    
    /**
     * Stop the TimeZone module
     */
    protected void stopTimeZone() {
        if( timeZoneSrv != null ) awareContext.stopService(timeZoneSrv);
    }
    
    /**
     * Start communication module
     */
    protected void startCommunication() {
        if( communicationSrv == null ) communicationSrv = new Intent(awareContext, Communication.class);
        awareContext.startService(communicationSrv);
    }
    
    /**
     * Stop communication module
     */
    protected void stopCommunication() {
        if( Aware.getSetting(awareContext, Aware_Preferences.STATUS_COMMUNICATION_EVENTS).equals("false") 
         && Aware.getSetting(awareContext, Aware_Preferences.STATUS_CALLS).equals("false") 
         && Aware.getSetting(awareContext, Aware_Preferences.STATUS_MESSAGES).equals("false") ) {
            if(communicationSrv != null) awareContext.stopService(communicationSrv);
        }
    }
    
    /**
     * Start MQTT module
     */
    protected void startMQTT() {
        if( mqttSrv == null ) mqttSrv = new Intent(awareContext, Mqtt.class);
        awareContext.startService(mqttSrv);
    }
    
    /**
     * Stop MQTT module
     */
    protected void stopMQTT() {
        if( mqttSrv != null ) awareContext.stopService(mqttSrv);
    }
}
