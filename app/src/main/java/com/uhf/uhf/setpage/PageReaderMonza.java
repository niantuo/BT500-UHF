package com.uhf.uhf.setpage;


import com.uhf.uhf.LogList;
import com.uhf.uhf.R;
import com.uhf.uhf.UHFApplication;
import com.ui.base.BaseActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.TextView;

import cn.tonyandmoney.tina.uhf_lib.base.CMD;
import cn.tonyandmoney.tina.uhf_lib.base.ERROR;
import cn.tonyandmoney.tina.uhf_lib.base.ReaderBase;
import cn.tonyandmoney.tina.uhf_lib.helper.ISO180006BOperateTagBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.InventoryBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.OperateTagBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.ReaderHelper;
import cn.tonyandmoney.tina.uhf_lib.helper.ReaderSetting;

public class PageReaderMonza extends BaseActivity {
	private LogList mLogList;
	
	private TextView mGet, mSet;

	private RadioGroup mGroupMonzaStatus, mGroupMonzaStore;

	private ReaderHelper mReaderHelper;
	private ReaderBase mReader;
	
	private static ReaderSetting m_curReaderSetting;
    private static InventoryBuffer m_curInventoryBuffer;
    private static OperateTagBuffer m_curOperateTagBuffer;
    private static ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;
    
    private LocalBroadcastManager lbm;
    
    @Override
    protected void onResume() {
    	if (mReader != null) {
    		if (!mReader.IsAlive())
    			mReader.StartWait();
    	}
    	super.onResume();
    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.page_reader_monza);
		((UHFApplication) getApplication()).addActivity(this);
		
		try {
			mReaderHelper = ReaderHelper.getDefaultHelper();
			mReader = mReaderHelper.getReader();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		m_curReaderSetting = mReaderHelper.getCurReaderSetting();
		m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
		m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
		m_curOperateTagISO18000Buffer = mReaderHelper.getCurOperateTagISO18000Buffer();

		mLogList = (LogList) findViewById(R.id.log_list);
		mGet = (TextView) findViewById(R.id.get);
		mSet = (TextView) findViewById(R.id.set);
		mSet.setOnClickListener(setMonzaClickListener);
		mGet.setOnClickListener(setMonzaClickListener);

		mGroupMonzaStatus = (RadioGroup) findViewById(R.id.group_monza_status);
		mGroupMonzaStore = (RadioGroup) findViewById(R.id.group_monza_store);

		lbm  = LocalBroadcastManager.getInstance(this);
		
		IntentFilter itent = new IntentFilter();
		itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
		itent.addAction(ReaderHelper.BROADCAST_REFRESH_READER_SETTING);
		lbm.registerReceiver(mRecv, itent);
		
		updateView();
	}
	
	private void updateView() {
		if (m_curReaderSetting.btMonzaStatus == 0x00) {
			mGroupMonzaStatus.check(R.id.set_monza_close);
		} else if ((m_curReaderSetting.btMonzaStatus & 0xFF) == 0x8D) {
			mGroupMonzaStatus.check(R.id.set_monza_open);
		}
		
		if (m_curReaderSetting.blnMonzaStore) {
			mGroupMonzaStore.check(R.id.set_monza_save);
		} else {
			mGroupMonzaStore.check(R.id.set_monza_unsave);
		}
	}
	
	
	private OnClickListener setMonzaClickListener = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			switch(arg0.getId()) {
			case R.id.get:
				mReader.getImpinjFastTid(m_curReaderSetting.btReadId);
				break;
			case R.id.set:
				boolean blnOpen = (mGroupMonzaStatus.getCheckedRadioButtonId() == R.id.set_monza_open);
				boolean blnSave = (mGroupMonzaStore.getCheckedRadioButtonId() == R.id.set_monza_save);
				
				mReader.setImpinjFastTid(m_curReaderSetting.btReadId, blnOpen, blnSave);
				m_curReaderSetting.btMonzaStatus = (byte) (blnOpen ? 0x8D : 0x00);
				m_curReaderSetting.blnMonzaStore = blnSave;
				break;
			}
		}
	};
	
	private final BroadcastReceiver mRecv = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_READER_SETTING)) {
				byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);
				
				if (btCmd == CMD.SET_AND_SAVE_IMPINJ_FAST_TID || btCmd == CMD.GET_IMPINJ_FAST_TID) {
					updateView();
				}
				
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {
            	mLogList.writeLog((String)intent.getStringExtra("log"), intent.getIntExtra("type", ERROR.SUCCESS));
            }
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mLogList.tryClose()) return true;
		}

		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if (lbm != null)
			lbm.unregisterReceiver(mRecv);
	}
}

