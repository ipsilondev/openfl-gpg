package com.gpgex;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.appstate.AppStateManager.StateResult;

public class GooglePlayGames extends Extension implements GameHelper.GameHelperListener {
	
	private static GooglePlayGames instance=null;
	private static GameHelper mHelper=null;
	public static final String TAG = "OPENFL-GPG";
	private static boolean userRequiresLogin=false;
	private static HaxeObject onDataGetObject=null;
	private static HaxeObject onDataLoginResult=null;
	private static boolean enableCloudStorage=false;

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void init(boolean cloudStorage){
		if(mHelper!=null){
			if(!mHelper.isConnecting()) return;
			mHelper=null;
		}
		enableCloudStorage=cloudStorage;
		mainActivity.runOnUiThread(new Runnable() {
            public void run() { 
				mHelper = new GameHelper(mainActivity, GameHelper.CLIENT_GAMES | (enableCloudStorage?GameHelper.CLIENT_APPSTATE:0));
				mHelper.enableDebugLog(true);
				mHelper.setup(GooglePlayGames.getInstance());
				mHelper.setMaxAutoSignInAttempts(userRequiresLogin?1:0);
				mHelper.onStart(mainActivity);
				userRequiresLogin=false;
				Log.i(TAG, "PlayGames: INIT COMPLETE");
            }
        });
	}	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void login(){
		Log.i(TAG, "PlayGames: Forcing Login");
		userRequiresLogin=true;
		mHelper=null;
		init(enableCloudStorage);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean setScore(String id, int score){
		try {
			Games.Leaderboards.submitScore(mHelper.mGoogleApiClient, id, score);
        } catch (Exception e) {
			Log.i(TAG, "PlayGames: setScore Exception");
			Log.i(TAG, e.toString());
			return false;
		}
    	Log.i(TAG, "PlayGames: setScore complete");
    	return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean displayScoreboard(String id){
		try {
			mainActivity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mHelper.mGoogleApiClient, id), 0);
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: displayScoreboard Exception");
			Log.i(TAG, e.toString());
			login();
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean displayAllScoreboards(){
		try {
			mainActivity.startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mHelper.mGoogleApiClient), 0);
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: displayAllScoreboards Exception");
			Log.i(TAG, e.toString());
			login();
			return false;
		}
		return true;
	}

	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static GooglePlayGames getInstance(){
		if(instance==null) instance=new GooglePlayGames();
		return instance;
	}

	@Override
    public void onSignInFailed() {
		if(onDataLoginResult!=null) onDataLoginResult.call1("loginResultCallback",-1);
        Log.i(TAG, "PlayGames: onSignInFailed");
    }

    @Override
    public void onSignInSucceeded() {
		if(onDataLoginResult!=null) onDataLoginResult.call1("loginResultCallback",1);
        Log.i(TAG, "PlayGames: onSignInSucceeded");
    }
	
	@Override
    public void onSignInStart() {
		if(onDataLoginResult!=null) onDataLoginResult.call1("loginResultCallback",0);
        Log.i(TAG, "PlayGames: onSignInStart");
    }
	

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean cloudSet(int key, String value){
		try {
			AppStateManager.update(mHelper.mGoogleApiClient, key, value.getBytes());
		} catch (Exception e) {
			Log.i(TAG, "PlayGames: cloudSet Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean cloudGet(int key, HaxeObject callbackObject){
		try {
			onDataGetObject=callbackObject;
			AppStateManager.load(mHelper.mGoogleApiClient, key).setResultCallback(new ResultCallback<StateResult>(){
				@Override
				public void onResult(StateResult result){
					try{
						AppStateManager.StateConflictResult conflictResult = result.getConflictResult();
						AppStateManager.StateLoadedResult loadedResult = result.getLoadedResult();
						if (loadedResult != null) {
						    String res=(loadedResult.getStatus().getStatusCode()==0?new String(loadedResult.getLocalData()):null);
						    onDataGetObject.call2("cloudGetCallback", loadedResult.getStateKey(), res);
						} else if (conflictResult != null) {
						    String server=new String(conflictResult.getServerData());
						    String local=new String(conflictResult.getLocalData());
						    AppStateManager.resolve(mHelper.mGoogleApiClient, 			  conflictResult.getStateKey(),
						    						conflictResult.getResolvedVersion(),  conflictResult.getServerData());
						    onDataGetObject.call3("cloudGetConflictCallback", conflictResult.getStateKey(), local, server);
						}
					} catch (Exception e) {
						Log.i(TAG, "PlayGames: cloudGet CRITICAL Exception");
						Log.i(TAG, e.toString());
					}
				}
			});
		} catch (Exception e) {
			Log.i(TAG, "PlayGames: cloudGet Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean unlock(String id){
		try{
			Games.Achievements.unlock(mHelper.mGoogleApiClient, id);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: unlock Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean reveal(String id){
		try{
			Games.Achievements.reveal(mHelper.mGoogleApiClient, id);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: reveal Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean increment(String id, int step){
		try{
			Games.Achievements.increment(mHelper.mGoogleApiClient, id, step);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: increment Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean setSteps(String id, int steps){
		try{
			Games.Achievements.setSteps(mHelper.mGoogleApiClient, id, steps);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: setSteps Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean displayAchievements(){
		try{
			mainActivity.startActivityForResult(Games.Achievements.getAchievementsIntent(mHelper.mGoogleApiClient), 0);
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: displayAchievements Exception");
			Log.i(TAG, e.toString());
			login();
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static boolean setLoginResultCallback(HaxeObject callbackObject){
		onDataLoginResult = callbackObject;
		return true;
	}		
}
