package com.uhf.uhf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nativec.tools.ModuleManager;
import com.uhf.uhf.PopupMenu.MENUITEM;
import com.uhf.uhf.PopupMenu.OnItemClickListener;
import com.uhf.uhf.spiner.DialogCustomed;
import com.uhf.uhf.tagpage.PageInventoryReal;
import com.uhf.uhf.tagpage.PageInventoryReal6B;
import com.uhf.uhf.tagpage.PageTag6BAccess;
import com.uhf.uhf.tagpage.PageTagAccess;
import com.ui.base.BaseActivity;
import com.ui.base.PreferenceUtil;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.tonyandmoney.tina.uhf_lib.base.Converter;
import cn.tonyandmoney.tina.uhf_lib.base.ERROR;
import cn.tonyandmoney.tina.uhf_lib.base.MessageTran;
import cn.tonyandmoney.tina.uhf_lib.base.ReaderBase;
import cn.tonyandmoney.tina.uhf_lib.base.StringTool;
import cn.tonyandmoney.tina.uhf_lib.helper.ISO180006BOperateTagBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.InventoryBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.OperateTagBuffer;
import cn.tonyandmoney.tina.uhf_lib.helper.ReaderHelper;
import cn.tonyandmoney.tina.uhf_lib.helper.ReaderSetting;
import cn.tonyandmoney.tina.uhf_lib.tools.Beeper;
import cn.tonyandmoney.tina.uhf_lib.tools.ExcelUtils;


public class MainActivity extends BaseActivity {
    private ViewPager mPager;
    private List<View> listViews;
    private TextView title[] = new TextView[3];
    private int currIndex = 0;

    private TextView mRefreshButton;

    private ReaderBase mReader;
    private ReaderHelper mReaderHelper;

    //test
    public static Activity activity;

    public static boolean mIsMonitorOpen = false;

    private static ReaderSetting m_curReaderSetting;
    private static InventoryBuffer m_curInventoryBuffer;
    private static OperateTagBuffer m_curOperateTagBuffer;
    private static ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;

    public static int mSaveType = 0;

    //private Setting mViewSetting;
    //private Tag	mTag;
    //private Monitor mMonitor;

    //private LogList mLogList;

    private ImageView iv_menu;
    private PopupMenu popupMenu;

    private LocalBroadcastManager lbm;

    private MENUITEM cur_item = MENUITEM.ITEM1;

    @Override
    protected void onResume() {
        if (mReader != null) {
            if (!mReader.IsAlive())
                mReader.StartWait();
        }
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((UHFApplication) getApplication()).addActivity(this);

        activity = this;

        // Storage Permissions
        ExcelUtils.verifyStoragePermissions(this);

        mRefreshButton = (TextView) findViewById(R.id.refresh);

        mRefreshButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cur_item == MENUITEM.ITEM1) {
                    if (0 == currIndex) {
                        ((PageInventoryReal) findViewById(R.id.view_PageInventoryReal)).refresh();
                    } else {
                        ((PageTagAccess) findViewById(R.id.view_PageTagAccess)).refresh();
                    }
                } else if (cur_item == MENUITEM.ITEM2) {
                    if (0 == currIndex) {
                        ((PageInventoryReal6B) findViewById(R.id.view_PageInventoryReal6B)).refresh();
                    } else {
                        ((PageTag6BAccess) findViewById(R.id.view_PageTag6BAccess)).refresh();
                    }
                }

                if (currIndex == 2) refreshMonitor();

            }
        });

        popupMenu = new PopupMenu(this);

        iv_menu = (ImageView) findViewById(R.id.iv_menu);
        iv_menu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popupMenu.showLocation(R.id.iv_menu);
                popupMenu.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onClick(MENUITEM item, String str) {
                        if (item == MENUITEM.ITEM1) {
                            InitViewPager(MENUITEM.ITEM1);
                        } else if (item == MENUITEM.ITEM2) {
                            InitViewPager(MENUITEM.ITEM2);
                        } else if (item == MENUITEM.ITEM3) {
                            Intent intent;
                            intent = new Intent().setClass(MainActivity.this, Setting.class);
                            startActivity(intent);
                        } else if (item == MENUITEM.ITEM4) {
                            askForOut();
                        } else if (item == MENUITEM.ITEM5) {
                            if (str.equals("English")) {
                                PreferenceUtil.commitString("language", "en");
                            } else if (str.equals("中文")) {
                                PreferenceUtil.commitString("language", "zh");
                            }
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            MainActivity.this.startActivity(intent);

                        } else if (item == MENUITEM.ITEM_add1) {
                            saveExcel();
                        }
                    }
                });
            }
        });

        InitTextView();
        InitViewPager(MENUITEM.ITEM1);

        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            return;
        }

        m_curReaderSetting = mReaderHelper.getCurReaderSetting();
        m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
        m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
        m_curOperateTagISO18000Buffer = mReaderHelper.getCurOperateTagISO18000Buffer();

        //mLogList = (LogList) findViewById(R.id.log_list);

        //mViewSetting = (Setting) listViews.get(0).findViewById(R.id.view_setting);
        //mTag = (Tag) listViews.get(1).findViewById(R.id.view_tag);

        //mViewSetting.setLogList(mLogList);

        lbm = LocalBroadcastManager.getInstance(this);

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_FAST_SWITCH);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_INVENTORY);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_ISO18000_6B);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_READER_SETTING);
        itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
        itent.addAction(ReaderHelper.BROADCAST_WRITE_DATA);
        itent.addAction(ReaderHelper.BROADCAST_REC_DATA);

        lbm.registerReceiver(mRecv, itent);
    }

    private final BroadcastReceiver mRecv = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_FAST_SWITCH)) {

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_INVENTORY)) {

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL)) {

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_ISO18000_6B)) {

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG)) {

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_READER_SETTING)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);
                //mViewSetting.refreshReaderSetting(btCmd);
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {
                //mLogList.writeLog((String)intent.getStringExtra("log"), intent.getIntExtra("type", ERROR.SUCCESS));
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_DATA) && PreferenceUtil.getBoolean(Monitor.mIsChecked, false) && !mIsMonitorOpen) {
                ((UHFApplication) getApplication()).writeMonitor((String) intent.getStringExtra("log"), intent.getIntExtra("type", ERROR.SUCCESS));
                mMonitorListAdapter.notifyDataSetChanged();
                //fixed by lei.li avoid observer serial port when the switch on
            }

            if (intent.getAction().equals(ReaderHelper.BROADCAST_REC_DATA)) {
                //Log.e("lisn3188","Rec: = " + (String)intent.getStringExtra("log") );
                if (currIndex == 2) {
                    if (hexen.isChecked()) {
                        show_data((String) intent.getStringExtra("log"));
                    } else {
                        byte[] rec = getStringhex((String) intent.getStringExtra("log"));
                        /*
                        try{
                            String temp = new String(rec, 0, rec.length,"GBK");
                            show_data(temp);
                        }catch	(UnsupportedEncodingException e){
                            throw   new   RuntimeException("Unsupported   encoding   type.");
                        }
                        */
                        decode_RFID((String) intent.getStringExtra("log"));
                    }
                }
            }
        }
    };

    byte recbuf[] = new byte[1024];
    int wrindex = 0;
    String RFID;
    boolean headen = false;

    private void decode_RFID(String recstr) {
        // dfe
        int i;
        byte dat;
        byte[] rec = getStringhex(recstr);
        if (rec == null || rec.length < 1) return;
        for (i = 0; i < rec.length; i++) {
            dat = rec[i];
            if ((dat == 0x0d)) {
                //   recbuf[wrindex++] = dat;
                //   headen = true;continue;
                //}else if((dat == 0x0a)&&(headen = true)){
                if (wrindex < 2) {
                    headen = false;
                    wrindex = 0;
                    return;
                }
                if (wrindex > 400) {
                    headen = false;
                    wrindex = 0;
                    return;
                }
                RFID = null;
                try {
                    RFID = new String(recbuf, 0, wrindex, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Unsupported   encoding   type.");
                }
                wrindex = 0;
                if (RFID == null) return;
                Date now = new Date();
                SimpleDateFormat temp = new SimpleDateFormat("kk:mm:ss");
                String sTime = temp.format(now);
                //((TextView)findViewById(R.id.rfidshow)).setText("Time "+sTime +" RFID = "+ RFID);
                ((TextView) listViews.get(2).findViewById(R.id.rfidshow2)).setText("Time " + sTime + " RFID = " + RFID);
                show_data(RFID + "\n");
            } else if (dat == 0x0a && wrindex == 0) {
                wrindex = 0;
            } else {
                recbuf[wrindex++] = dat;
            }
            headen = false;
            if (wrindex > 1000) wrindex = 1000;
        }

    }

    public byte[] getStringhex(String ST) {
        ST = ST.replaceAll(" ", "");
        //Log.v("getStringhex",ST);
        char[] buffer = ST.toCharArray();
        byte[] Byte = new byte[buffer.length / 2];
        int index = 0;
        int bit_st = 0;
        for (int i = 0; i < buffer.length; i++) {
            int v = (int) (buffer[i] & 0xFF);
            if (((v > 47) && (v < 58)) || ((v > 64) && (v < 71)) || ((v > 96) && (v < 103))) {
                if (bit_st == 0) {
                    Byte[index] |= (getASCvalue(buffer[i]) * 16);
                    bit_st = 1;
                } else {
                    Byte[index] |= (getASCvalue(buffer[i]));
                    bit_st = 0;
                    index++;
                }
            } else if (v == 32) {
                if (bit_st == 0) ;
                else {
                    index++;
                    bit_st = 0;
                }
            } else continue;
        }
        bit_st = 0;
        return Byte;
    }

    public static byte getASCvalue(char in) {
        byte out = 0;
        switch (in) {
            case '0':
                out = 0;
                break;
            case '1':
                out = 1;
                break;
            case '2':
                out = 2;
                break;
            case '3':
                out = 3;
                break;
            case '4':
                out = 4;
                break;
            case '5':
                out = 5;
                break;
            case '6':
                out = 6;
                break;
            case '7':
                out = 7;
                break;
            case '8':
                out = 8;
                break;
            case '9':
                out = 9;
                break;
            case 'a':
                out = 10;
                break;
            case 'b':
                out = 11;
                break;
            case 'c':
                out = 12;
                break;
            case 'd':
                out = 13;
                break;
            case 'e':
                out = 14;
                break;
            case 'f':
                out = 15;
                break;
            case 'A':
                out = 10;
                break;
            case 'B':
                out = 11;
                break;
            case 'C':
                out = 12;
                break;
            case 'D':
                out = 13;
                break;
            case 'E':
                out = 14;
                break;
            case 'F':
                out = 15;
                break;
        }
        return out;
    }

    private void InitTextView() {
        title[0] = (TextView) findViewById(R.id.tab_index1);
        title[1] = (TextView) findViewById(R.id.tab_index2);
        //title[2] = (TextView) findViewById(R.id.tab_index3);

        title[0].setOnClickListener(new MyOnClickListener(0));
        title[1].setOnClickListener(new MyOnClickListener(1));
        //title[2].setOnClickListener(new MyOnClickListener(2));
    }

    LinearLayout mLayout;
    TextView msg_Text;
    ScrollView mScrollView;
    private final Handler tHandler = new Handler();
    private Runnable mScrollToBottom = new Runnable() {
        @Override
        public void run() {
            int off = mLayout.getMeasuredHeight() - mScrollView.getHeight();
            if (off > 0) {
                mScrollView.scrollTo(0, off);
            }
        }
    };

    public void show_data(String t) {
        if (msg_Text.getText().toString().length() > 2000) {
            String nstr = msg_Text.getText().toString();
            nstr = nstr.substring(t.length(), nstr.length());
            msg_Text.setText(nstr);
        }
        //msg_Text.append(Html.fromHtml(t));
        msg_Text.append(t);
        tHandler.post(mScrollToBottom);//更新Scroll
    }

    private ListView mMonitorList;
    private ArrayAdapter<CharSequence> mMonitorListAdapter;
    private UHFApplication app;
    private TextView mDataSendButton;
    private HexEditTextBox mDataText, mDataText2;
    CheckBox hexen;
    private HexEditTextBox mDataCheck;
    Context mcontext;

    private void InitViewPager(MENUITEM item) {
        cur_item = item;
        mPager = (ViewPager) findViewById(R.id.vPager);
        listViews = new ArrayList<View>();
        LayoutInflater mInflater = getLayoutInflater();
        if (item == MENUITEM.ITEM1) {
            listViews.add(mInflater.inflate(R.layout.lay1, null));
            listViews.add(mInflater.inflate(R.layout.lay2, null));
            listViews.add(mInflater.inflate(R.layout.monitor2, null));
        } else if (item == MENUITEM.ITEM2) {
            listViews.add(mInflater.inflate(R.layout.lay3, null));
            listViews.add(mInflater.inflate(R.layout.lay4, null));
            listViews.add(mInflater.inflate(R.layout.monitor2, null));
        }
        app = (UHFApplication) getApplication();
        mMonitorList = (ListView) listViews.get(2).findViewById(R.id.monitor_list_view);
        mMonitorListAdapter = new ArrayAdapter<CharSequence>(this, R.layout.monitor_list_item, app.mMonitorListItem);
        mMonitorList.setAdapter(mMonitorListAdapter);

        mPager.setAdapter(new MyPagerAdapter(listViews));
        mPager.setCurrentItem(0);
        mPager.setOnPageChangeListener(new MyOnPageChangeListener());
        currIndex = 0;
        title[1].setBackgroundResource(R.drawable.btn_select_background_select_mid_down);
        title[0].setBackgroundResource(R.drawable.btn_select_background_select_right);
        //title[2].setBackgroundResource(R.drawable.btn_select_background_select_left_down);
        //title[2].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
        title[1].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
        title[0].setTextColor(Color.rgb(0xFF, 0xFF, 0xFF));
        listViews.get(2).findViewById(R.id.refresh).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshMonitor();
            }
        });

        msg_Text = (TextView) listViews.get(2).findViewById(R.id.Text1);
        mcontext = this;
        mLayout = (LinearLayout) listViews.get(2).findViewById(R.id.layout);
        mScrollView = (ScrollView) listViews.get(2).findViewById(R.id.sv);
        mDataText = (HexEditTextBox) listViews.get(2).findViewById(R.id.data_send_text);
        mDataText2 = (HexEditTextBox) listViews.get(2).findViewById(R.id.data_send_t);
        hexen = (CheckBox) listViews.get(2).findViewById(R.id.check_hex);
        mDataText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
                String[] result = StringTool.stringToStringArray(mDataText.getText().toString().toUpperCase(), 2);
                try {
                    byte[] buf = StringTool.stringArrayToByteArray(result, result.length);
                    MessageTran msgTran = new MessageTran();
                    byte check = msgTran.checkSum(buf, 0, buf.length);
                    mDataCheck.setText("" + Converter.byteToHex((int) (check & 0xFF) / 16) + Converter.byteToHex((check & 0xFF) % 16));
                } catch (Exception e) {
                }
                ;
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub

            }
        });

        mDataSendButton = (TextView) listViews.get(2).findViewById(R.id.sendd);
        mDataSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
               /*
                String cmd = mDataText.getText().toString().toUpperCase() + mDataCheck.getText().toString().toUpperCase();
                String[] result = StringTool.stringToStringArray(cmd, 2);
                if (result != null && result.length > 0) {
                    mReader.sendBuffer(StringTool.stringArrayToByteArray(result, result.length));
                } else {
                    //Toast.makeText(, "Command not allow empty", Toast.LENGTH_SHORT).show();
                }
                */
                String cmd = mDataText2.getText().toString();
                if (hexen.isChecked()) {
                    String[] result = StringTool.stringToStringArray(cmd, 2);
                    if (result != null && result.length > 0) {
                        // mReader.sendBuffer(StringTool.stringArrayToByteArray(result, result.length));
                        mReader.sendBuffer(getStringhex(cmd));
                    }
                } else {
                    if (cmd != null) {
                        try {
                            byte[] send = cmd.getBytes("GBK");
                            mReader.sendBuffer(send);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        mDataCheck = (HexEditTextBox) listViews.get(2).findViewById(R.id.data_send_check);
    }

    public final void refreshMonitor() {
        //mMonitorListAdapter.clear();
        msg_Text.setText("");
        mDataText.setText("");
        app.mMonitorListItem.clear();
        mMonitorListAdapter.notifyDataSetChanged();
    }

    public class MyPagerAdapter extends PagerAdapter {
        public List<View> mListViews;

        public MyPagerAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            ((ViewPager) arg0).removeView(mListViews.get(arg1));
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public Object instantiateItem(View arg0, int arg1) {
            ((ViewPager) arg0).addView(mListViews.get(arg1), 0);
            return mListViews.get(arg1);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == (arg1);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }
    }

    public class MyOnClickListener implements OnClickListener {
        private int index = 0;

        public MyOnClickListener(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {
            mPager.setCurrentItem(index);
        }
    }

    ;

    public class MyOnPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageSelected(int arg0) {
            currIndex = arg0;
            if (0 == currIndex) {
                title[1].setBackgroundResource(R.drawable.btn_select_background_select_mid_down);
                title[0].setBackgroundResource(R.drawable.btn_select_background_select_right);
                //title[2].setBackgroundResource(R.drawable.btn_select_background_select_left_down);
                mSaveType = 0;
            } else if (1 == currIndex) {
                title[1].setBackgroundResource(R.drawable.btn_select_background_select_mid);
                title[0].setBackgroundResource(R.drawable.btn_select_background_select_right_down);
                // title[2].setBackgroundResource(R.drawable.btn_select_background_select_left_down);
                mSaveType = 1;
            } else {
                title[1].setBackgroundResource(R.drawable.btn_select_background_select_mid_down);
                title[0].setBackgroundResource(R.drawable.btn_select_background_select_right_down);
                //title[2].setBackgroundResource(R.drawable.btn_select_background_select_left);
            }

            title[0].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
            title[1].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
            // title[2].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
            title[currIndex].setTextColor(Color.rgb(0xFF, 0xFF, 0xFF));
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            askForOut();

            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void askForOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.alert_diag_title)).
                setMessage(getString(R.string.are_you_sure_to_exit)).
                setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //close the module
                                ModuleManager.newInstance().setUHFStatus(false);
                                getApplication().onTerminate();
                            }
                        }).setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setCancelable(false).show();
    }

    /**
     * Save the tags as excel file;
     */
    private void saveExcel() {
        DialogCustomed dialogCustomed = new DialogCustomed(this, R.layout.excel_save_dialog);
        dialogCustomed.setTags(m_curInventoryBuffer.lsTagList);
        dialogCustomed.setOperationTags(m_curOperateTagBuffer.lsTagList);
        dialogCustomed.getDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (lbm != null)
            lbm.unregisterReceiver(mRecv);
        Beeper.release();
    }
}
