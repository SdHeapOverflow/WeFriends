package com.infinity.wefriends.apis;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.infinity.utils.*;
import com.infinity.wefriends.MainActivity;
import com.infinity.wefriends.R;
import com.infinity.wefriends.apis.DataBaseHelper;

public class Users {
	protected DataBaseHelper database = null;
	protected Context m_context = null;
	
	public static final int TOKEN_VALID = 200;
	public static final int TOKEN_INVALID = 403;
	public static final int CONNECTION_ERROR = -1;
	
	public static final int LOGIN_OK = 200;
	public static final int LOGIN_FAILED = 403;
	
	public Users(Context context) {
		m_context = context;
		database = new DataBaseHelper(context,"wefriendsdb");
	}
	
	public int authenticateCachedAccessToken() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query("usercache", new String[]{"accesstoken"}, "", new String[]{}, "", "", "", "1");
		if (!cursor.moveToNext()) {
			db.close();
			return TOKEN_INVALID;
		}
		String accessToken = cursor.getString(cursor.getColumnIndex("accesstoken"));
		db.close();
		return authenticateAccessToken(accessToken);
	}
	
	public int authenticateAccessToken(String accessToken) {
		HttpRequest.Response response = new HttpRequest.Response();
		String requestURL = "http://" + m_context.getString(R.string.server_host) + ":" + m_context.getString(R.string.server_web_service_port) + "/users/getuserinfobytoken?accesstoken=" + accessToken;
		if(HttpRequest.get(requestURL,response) == HttpRequest.HTTP_FAILED)
			return CONNECTION_ERROR;
		try {
			JSONObject jsonObj = new JSONObject(response.getString());
			if (jsonObj.getInt("status")==200) {
				return TOKEN_VALID;
			} else {
				return TOKEN_INVALID;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CONNECTION_ERROR;
	}
	
	public int login(String phone, String password) {
		HttpRequest.Response response = new HttpRequest.Response();
		String requestURL = "http://" + m_context.getString(R.string.server_host) + ":" + m_context.getString(R.string.server_web_service_port) + "/users/login";
		List<NameValuePair> postFields = new ArrayList<NameValuePair>();
		postFields.add(new BasicNameValuePair("phone",phone));
		postFields.add(new BasicNameValuePair("password",password));
		if (HttpRequest.post(requestURL, postFields, response) == HttpRequest.HTTP_FAILED)
			return CONNECTION_ERROR;
		try {
			JSONObject jsonObj = new JSONObject(response.getString());
			if (jsonObj.getInt("status")==200) {
				SQLiteDatabase db = database.getWritableDatabase();
				db.execSQL("DELETE FROM usercache");
				ContentValues values = new ContentValues();
				values.put("accesstoken", jsonObj.getString("accesstoken"));
				values.put("wefriendsid", "");
				values.put("nickname", "");
				values.put("avatar", "");
				values.put("whatsup", "");
				values.put("friends","[]");
				values.put("phone", phone);
				values.put("email", "");
				values.put("intro", "");
				values.put("gender", 0);
				values.put("region", "");
				values.put("collegeid", "");
				db.insert("usercache", "", values);
				db.close();
				return LOGIN_OK;
			} else {
				return LOGIN_FAILED;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return CONNECTION_ERROR;
	}
	
	public ContentValues getCachedUserInfo() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query("usercache", new String[]{"wefriendsid","nickname","avatar","phone","email","intro","gender","region","collegeid"}, "", new String[]{}, "", "", "","1");
		if (!cursor.moveToNext()) {
			db.close();
			return null;
		}
		ContentValues values = new ContentValues();
		values.put("wefriendsid", cursor.getString(cursor.getColumnIndex("wefriendsid")));
		values.put("nickname", cursor.getString(cursor.getColumnIndex("nickname")));
		values.put("avatar", cursor.getString(cursor.getColumnIndex("avatar")));
		values.put("phone", cursor.getString(cursor.getColumnIndex("phone")));
		values.put("email", cursor.getString(cursor.getColumnIndex("email")));
		values.put("intro", cursor.getString(cursor.getColumnIndex("intro")));
		values.put("gender", cursor.getString(cursor.getColumnIndex("gender")));
		values.put("region", cursor.getString(cursor.getColumnIndex("region")));
		values.put("collegeid", cursor.getString(cursor.getColumnIndex("collegeid")));
		return values;
	}
	
	public ContentValues getAndSaveUserInfo() {
		HttpRequest.Response response = new HttpRequest.Response();
		String accessToken = getCachedAccessToken();
		String requestURL = "http://" + m_context.getString(R.string.server_host) + ":" + m_context.getString(R.string.server_web_service_port) + "/users/getuserinfobytoken?accesstoken=" + accessToken;
		if (HttpRequest.get(requestURL, response)==HttpRequest.HTTP_FAILED) {
			return null;
		}
		try {
			JSONObject jsonObj = new JSONObject(response.getString());
			if (jsonObj.getInt("status")!=200) {
				broadcastReLoginAction();
				return null;
			}
			JSONObject userInfo = jsonObj.getJSONObject("userinfo");
			SQLiteDatabase db = database.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("wefriendsid", URLDecoder.decode(userInfo.getString("wefriendsid"),"utf-8"));
			values.put("nickname", URLDecoder.decode(userInfo.getString("nickname"),"utf-8"));
			values.put("avatar", URLDecoder.decode(userInfo.getString("avatar"),"utf-8"));
			values.put("phone", URLDecoder.decode(userInfo.getString("phone"),"utf-8"));
			values.put("email", URLDecoder.decode(userInfo.getString("email"),"utf-8"));
			values.put("intro", URLDecoder.decode(userInfo.getString("intro"),"utf-8"));
			values.put("gender", userInfo.getInt("gender"));
			values.put("region", URLDecoder.decode(userInfo.getString("region"),"utf-8"));
			values.put("collegeid", URLDecoder.decode(userInfo.getString("collegeid"),"utf-8"));
			db.update("usercache", values, "", new String[]{});
			db.close();
			return values;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String getCachedWhatsUp() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query("usercache", new String[]{"whatsup"}, "", new String[]{}, "", "", "");
		if (!cursor.moveToNext())
			return null;
		String whatsUp = cursor.getString(cursor.getColumnIndex("whatsup"));
		db.close();
		return whatsUp;
	}
	
	public String getAndSaveWhatsUp() {
		String accessToken = getCachedAccessToken();
		HttpRequest.Response response = new HttpRequest.Response();
		String requestURL = "http://" + m_context.getString(R.string.server_host) + ":" + m_context.getString(R.string.server_web_service_port) + "/users/getwhatsup?accesstoken=" + accessToken;
		if (HttpRequest.get(requestURL, response)==HttpRequest.HTTP_FAILED)
			return null;
		try {
			JSONObject jsonObj = new JSONObject(response.getString());
			if (jsonObj.getInt("status")!=200) {
				broadcastReLoginAction();
				return null;
			}
			String whatsUp = URLDecoder.decode(jsonObj.getString("whatsup"),"utf-8");
			SQLiteDatabase db = database.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("whatsup", whatsUp);
			db.update("usercache", values, "", new String[]{});
			db.close();
			return whatsUp;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String getCachedAccessToken() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query("usercache", new String[]{"accesstoken"}, "", new String[]{}, "", "", "", "1");
		if (!cursor.moveToNext()) {
			db.close();
			broadcastReLoginAction();
			return null;
		}
		String accessToken = cursor.getString(cursor.getColumnIndex("accesstoken"));
		db.close();
		return accessToken;
	}
	
	public void broadcastReLoginAction() {
		Intent intent = new Intent();
		intent.setAction("WEFRIENDS_RELOGIN");
		m_context.sendBroadcast(intent);
	}
	
}
