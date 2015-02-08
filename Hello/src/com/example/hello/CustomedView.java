package com.example.hello;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomedView extends LinearLayout{
	
	protected TextView textView = null;
	protected String showText = "";
	Context m_context;
	MainActivity m_parent = null;
	
	public CustomedView(Context context)
	{
		super(context);
		m_context = context;
		
		CreateView();
	}
	
	public CustomedView(Context context, AttributeSet attrs)
	{
		super(context,attrs);
		m_context = context;
		
		CreateView();
	}
	
	protected void CreateView()
	{
		LayoutInflater.from(m_context).inflate(R.layout.customedview, this, true);
		
		textView = (TextView)findViewById(R.id.customedtextview);
		
		textView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(m_context, showText, Toast.LENGTH_SHORT).show();
				m_parent.callback();
			}
		});
		textView.setTextColor(Color.rgb(0, 0, 0));
	}
	
	public void Init(String text, MainActivity parent)
	{
		showText = text;
		m_parent = parent;
	}

}