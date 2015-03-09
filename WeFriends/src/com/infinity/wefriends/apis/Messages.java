package com.infinity.wefriends.apis;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.infinity.utils.HttpRequest;
import com.infinity.wefriends.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Messages {
	protected Users users = null;
	protected Context m_context;
	protected DatabaseHelper database = null;
	
	public Messages(Context context) {
		m_context = context;
		users = new Users(context);
		database = new DatabaseHelper(context,"wefriendsdb");
	}
	
	public List<ContentValues> getAndSaveNewMessages() {
		String accessToken = users.getCachedAccessToken();
		HttpRequest.Response response = new HttpRequest.Response();
		String requestURL = "http://" + m_context.getString(R.string.server_host) + ":" + m_context.getString(R.string.server_web_service_port) + "/messages/getnewmessages?accesstoken=" + accessToken;
		if (HttpRequest.get(requestURL, response) == HttpRequest.HTTP_FAILED)
			return null;
		try {
			JSONObject jsonObj = new JSONObject(response.getString());
			if(jsonObj.getInt("status")!=200) {
				users.broadcastReLoginAction();
				return null;
			}
			if (jsonObj.getInt("count")==0) {
				return null;
			}
			SQLiteDatabase db = database.getWritableDatabase();
			List<ContentValues> messageList = new ArrayList<ContentValues>();
			JSONArray jsonArray = jsonObj.getJSONArray("messages");
			int messageCount = jsonArray.length();
			for (int i=0;i<messageCount;i++) {
				JSONObject messageObj = jsonArray.getJSONObject(i);
				ContentValues message = new ContentValues();
				message.put("sender", URLDecoder.decode(messageObj.getString("sender"),"utf-8"));
				message.put("messagetype",messageObj.getString("messagetype"));
				message.put("chatgroup", URLDecoder.decode(messageObj.getString("chatgroup"),"utf-8"));
				message.put("timestramp", messageObj.getLong("timestramp"));
				message.put("message", URLDecoder.decode(messageObj.getString("message"),"utf-8"));
				message.put("ishandled", 0);
				messageList.add(message);
				db.insert("messagecache", "", message);
			}
			db.close();
			return messageList;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public List<ContentValues> getCachedMessages(String messageType, String sender, String chatGroup, int page) {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query("messagecache", new String[]{"sender", "messagetype", "chatgroup", "timestramp", "message", "ishandled"} , "", new String[]{}, "", "", "timestramp DESC");
		List<ContentValues> resultList = new ArrayList<ContentValues>();
		while (cursor.moveToNext()) {
			ContentValues value = new ContentValues();
			value.put("sender", cursor.getString(cursor.getColumnIndex("sender")));
			value.put("messagetype", cursor.getString(cursor.getColumnIndex("messagetype")));
			value.put("chatgroup", cursor.getString(cursor.getColumnIndex("chatgroup")));
			value.put("timestramp", cursor.getLong(cursor.getColumnIndex("timestramp")));
			value.put("message", cursor.getString(cursor.getColumnIndex("message")));
			value.put("ishandled", cursor.getInt(cursor.getColumnIndex("ishandled")));
			
			if (messageType!=null && (!messageType.equals("")) && messageType.equals(value.getAsString("messagetype")))
				resultList.add(value);
			else if (messageType==null || messageType.equals(""))
				resultList.add(value);
			
			if (sender!=null && (!sender.equals("")) && sender.equals(value.getAsString("sender")))
				resultList.add(value);
			else if (sender==null || sender.equals(""))
				resultList.add(value);
			
			if (chatGroup!=null && (!chatGroup.equals("")) && chatGroup.equals(value.getAsString("chatgroup")))
				resultList.add(value);
			else if (chatGroup==null || chatGroup.equals(""))
				resultList.add(value);
			
		}
		db.close();
		
		if (page==0) {
			return resultList;
		}
		
		int skipCount = (page-1) * 15;
		for (int i=0;i<skipCount;i++) {
			if (resultList.size() > 0)
				resultList.remove(0);
		}
		
		List<ContentValues> newList = new ArrayList<ContentValues>();
		for (int i=0;i<15;i++) {
			if (resultList.size() >= (i+1))
				newList.add(resultList.get(i));
		}
		
		return newList;
	}

}