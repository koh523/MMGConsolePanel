package jp.aiaida.panelfront;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import jp.aiaida.panelpreference.PanelPreferenceActivity.FragmentPreference;
import jp.aiaida.panelpreset.DataBaseHelper;
import jp.aiaida.panelpreset.PanelPresetActivity;
import jp.aiaida.panelpreset.SliderProperty;
import jp.aiaida.panelpreset.PanelPresetActivity.FragmentPreset;
import jp.aiaida.panelpreset.PanelPresetActivity.PresetSet;
import jp.aiaida.panelterminal.SocketClientTask;
import jp.aiaida.panelterminal.SocketClientTask.SocketClientTaskCallback;

public class PanelFrontActivity extends Activity {

	static final String DEFALT_IP_ADDRESS_SERVER = "192.168.111.10";
	static final String DEFALT_IP_PORT_SERVER = "8181";

	static final String TARGET_PARAM_S1 = "S1";
	static final String TARGET_PARAM_S2 = "S2";
	static final String TARGET_PARAM_S3 = "S3";
	static final String TARGET_PARAM_S4 = "S4";
	static final String TARGET_PARAM_R1 = "R1";
	static final String TARGET_PARAM_R2 = "R2";
	static final String TARGET_PARAM_B1 = "B1";
	static final String TARGET_PARAM_B2 = "B2";
	static final String TARGET_PARAM_B3 = "B3";
	static final String TARGET_PARAM_B4 = "B4";
	static final String TARGET_PARAM_B5 = "B5";
	static final String TARGET_PARAM_D1 = "D1";
	static final String TARGET_PARAM_D2 = "D2";
	static final String TARGET_PARAM_D3 = "D3";
	static final String TARGET_PARAM_D4 = "D4";
	static final String TARGET_PARAM_ST = "ST";
	static final String TARGET_PARAM_TS = "TS";
	static final String TARGET_PARAM_T1 = "T1";
	static final String TARGET_PARAM_T2 = "T2";
	static final String TARGET_PARAM_T3 = "T3";
	static final String TARGET_PARAM_JTT = "E1";
	static final String TARGET_PARAM_STT = "E2";
	static final String TARGET_PARAM_ERRCHK = "SW";

    static final String TARGET_STATUS_AMP = "AMP";
    static final String TARGET_STATUS_BRK = "BRK";
    static final String TARGET_STATUS_REVP = "REV_P";
    static final String TARGET_STATUS_REVD = "REV_D";
    static final String TARGET_STATUS_REVE = "REV_E";
    static final String TARGET_STATUS_OMGP = "OMG_P";
    static final String TARGET_STATUS_OMGD = "OMG_D";
    static final String TARGET_STATUS_VOLTS = "VOLT_S";
    static final String TARGET_STATUS_VOLTD = "VOLT_D";
    static final String TARGET_STATUS_CURRD = "CURR_D";
    static final String TARGET_STATUS_POWD = "POW_D";


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Fragment newFragment = FragmentFront.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_content, newFragment).commit();
    }

    public static class FragmentFront extends Fragment implements Runnable, SocketClientTaskCallback{
		Button btnStart;
    	Button btnStop;
    	Button btnReload;
    	FragmentFront fragmentfront = this;
       	private static HashMap<String, View> viewMap = new HashMap<String, View>();
       	HashMap<String, String> valueMap = new HashMap<String, String>();
       	HashMap<String, SliderProperty> sliderPropMap = new HashMap<String, SliderProperty>();
       	String latestResponse = null;

        String ip_address_server;
        String ip_port_server;
        boolean flagEnableNetwork;
        boolean flagClimerModeFreerun;

		private DataBaseHelper mDbHelper;
		private SQLiteDatabase mDb;

		private String mThisPackageName;
		private Handler mHandler;


        public static FragmentFront newInstance(){
    		FragmentFront ff = new FragmentFront();
    		return ff;
    	}

        @Override
		public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
			mThisPackageName = this.getActivity().getApplicationContext().getPackageName();
        	setDataBase();
			Cursor cursor = mDb.rawQuery("select * from " + PanelPresetActivity.TABLE_SLIDER_PROP, null);
			cursor.moveToFirst();
			while(!cursor.isAfterLast()){
				SliderProperty property = SliderProperty.extractSliderProperty(cursor);
				sliderPropMap.put(property.getName(), property);
				cursor.moveToNext();
			}
			mDbHelper.close();
			mDb.close();

			mHandler = new Handler();
        }

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			LinearLayout group, unit;
			TextView txtCaption;
			EditText txtField;
			ProgressBar bar;

			super.onCreateView(inflater, container, savedInstanceState);
	        final SharedPreferences myPreferenceFile = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
	        ip_address_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_ADDRESS, DEFALT_IP_ADDRESS_SERVER);
	        ip_port_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_PORT, DEFALT_IP_PORT_SERVER);
	        flagEnableNetwork = myPreferenceFile.getBoolean(FragmentPreference.KEY_FLAG_ENABLE_NETWORK, false);
	        flagClimerModeFreerun = myPreferenceFile.getBoolean(FragmentPreference.KEY_FLAG_CLIMERMODE_FREERUN, false);
			View fragView = inflater.inflate(R.layout.panel_front, container, false);

/*-----       Layout for current STAUTS displays      ----*/

			/*---  setup for 1st group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status1);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_amp);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_AMP, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_brk);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_BRK, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);

			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);

			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

			/*--- setup for 2nd group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status2);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_revp);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_REVP, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_revd);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_REVD, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_reve);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_REVE, txtField);

			/*--- setup for 3rd group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status3);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_omgp);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_OMGP, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_omgd);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_OMGD, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);

			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);

			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

			/*--- setup for 4th group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status4);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_volts);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_VOLTS, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_voltd);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_VOLTD, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);

			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);

			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

			/*--- setup for 5th group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status5);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_currd);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_CURRD, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_powd);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			viewMap.put(TARGET_STATUS_POWD, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption  = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);

			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);

			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

			/*--- setup for 6th group on STATUS region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_status6);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);
			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);


			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);
			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtGauge);
			txtField.setVisibility(View.INVISIBLE);
			bar = (ProgressBar)unit.findViewById(R.id.bar);
			bar.setVisibility(View.INVISIBLE);

/*---       Layout for Current SETTIMG displays       ---*/

			/*--- setup for 1st group on SETTING region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_setting1);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_d1);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_D1, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_d2);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_D2, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_d3);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_D3, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit4);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_d4);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_D4, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit5);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			unit = (LinearLayout)group.findViewById(R.id.unit6);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			/*--- setup for 2nd group on SETTING region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_setting2);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_s1);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_S1, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_s2);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_S2, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_s3);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_S3, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit4);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_s4);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_S4, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit5);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_r1);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_R1, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit6);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_r2);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_R2, txtField);

			/*--- setup for 3rd group on SETTING region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_setting3);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_b1);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_B1, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_b2);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_B2, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_b3);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_B3, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit4);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_b4);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_B4, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit5);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_b5);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_B5, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit6);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			/*--- setup for 4th group on SETTING region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_setting4);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_st);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_ST, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_ts);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_TS, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_t1);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_T1, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit4);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_t2);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_T2, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit5);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_t3);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_T3, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit6);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			/*--- setup for 5th group on SETTING region ---*/
			group = (LinearLayout)fragView.findViewById(R.id.group_setting5);

			unit = (LinearLayout)group.findViewById(R.id.unit1);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_jtt);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_JTT, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit2);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_stt);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_STT, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit3);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			unit = (LinearLayout)group.findViewById(R.id.unit4);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setText(R.string.caption_errchk);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			viewMap.put(TARGET_PARAM_ERRCHK, txtField);

			unit = (LinearLayout)group.findViewById(R.id.unit5);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

			unit = (LinearLayout)group.findViewById(R.id.unit6);
			txtCaption = (TextView)unit.findViewById(R.id.txtCaption);
			txtCaption.setVisibility(View.INVISIBLE);
			txtField = (EditText)unit.findViewById(R.id.txtContent);
			txtField.setVisibility(View.INVISIBLE);

/*---   Preset Radio Buttons Group ---*/
			RadioGroup radioGroup = (RadioGroup)fragView.findViewById(R.id.radiogroup_preset);
			radioGroup.setOnCheckedChangeListener(rgCheckedChangeLister);

/*---   Bottom buttons  ---*/
			btnStart = (Button)fragView.findViewById(R.id.btnStart);
			btnStart.setOnClickListener(buttnClickListener);

			btnStop = (Button)fragView.findViewById(R.id.btnStop);
			btnStop.setOnClickListener(buttnClickListener);

			btnReload = (Button)fragView.findViewById(R.id.btnReload);
			btnReload.setOnClickListener(buttnClickListener);

/*---   Issue Threadingtask ---*/
			Thread thread = new Thread(this);
			thread.start();

			return fragView;
		}


	    @Override
		public void onResume() {
			super.onResume();
	    }

	    private final OnCheckedChangeListener rgCheckedChangeLister = new OnCheckedChangeListener(){
	    	public void onCheckedChanged(RadioGroup group, int resid) {
				int page = 0;

		        if (-1 == resid) {
		            Toast.makeText((Context)fragmentfront.getActivity(),
		                    "クリアされました",
		                    Toast.LENGTH_SHORT).show();
		        } else {
		        	if (resid == R.id.radiobutton_preset1){
		        		page = 1;
		        	}
		        	else if (resid == R.id.radiobutton_preset2){
		        		page = 2;
		        	}
		        	else if (resid == R.id.radiobutton_preset3){
		        		page = 3;
		        	}
		        	else{
		        		page = 1;
		        	}
		        }

		        // DBから 指定のpresetセットの読み出し
		        HashMap<String, Long> presetSet = PresetSet.getPresetSet(getActivity(), page);
		    	for (Iterator<Entry<String, Long>> it = presetSet.entrySet().iterator(); it.hasNext(); ){
		    		Entry<String, Long> entry = it.next();

		    		String key = (String)entry.getKey();
		    		Long value = (Long)entry.getValue();
		    		String arg = null;
		    		if (key == "SW"){
		    			arg = flagClimerModeFreerun == true? "ON":"OFF";
		    		}else{
		    			SliderProperty property = sliderPropMap.get(key);
		    			arg = FragmentPreset.FormatedIndicator(value, property);
		    		}
		        	String cmd = "SET_" + key + " = " + arg;
		        	Log.d("preset loading", cmd);
			        if (true == flagEnableNetwork){
				        SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
			        	task.execute(cmd);
			        	try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
   					}
		        }
	    		String arg = flagClimerModeFreerun == true? "ON":"OFF";
	    		String cmd = "SET_SW = " + arg;
	        	Log.d("preset loading", cmd);

		    	if (true == flagEnableNetwork){
		    		SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
		    		task.execute(cmd);
		    		try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}

		    		task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
		    		task.execute("lsdat");
		    	}
			}
		};

		private final OnClickListener buttnClickListener = new OnClickListener(){
			public void onClick(View v) {
				String strRequest = null;
				if (v == btnStart){
					// STARTボタン クリックイベント
					strRequest = "AUTO";
				}
				if (v == btnStop){
					// STOPボタン クリックイベント
					strRequest = "STOP";
				}
				if (v == btnReload){
					// Reloadボタン クリックイベント
					strRequest = "lsdat";
				}
				if (true == flagEnableNetwork){
					SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
					task.execute(strRequest);
				}
			}
	    };

		public void run() {
//			while(true){	// しばし定期自動更新は行わない。
			{
				try {
					Thread.sleep(1000);
				}catch (InterruptedException e){

				}
				/*
				 * Threadのなかで さらにThreadを生成することはできない模様。
				SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
				task.execute("lsdat");
				*/

				handler.sendEmptyMessage(0);
			}
		}

		final Handler handler = new Handler(){
	    	public void handleMessage(Message msg){
				Log.d("hander", "called");
				if (true == flagEnableNetwork){
					try{
						SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentfront);
						task.execute("lsdat");
					}catch(RejectedExecutionException e){
						// AsyncTaskを多数発行し、内部キューサイズを超えるとこの例外が発生する。
						// http://techracho.bpsinc.jp/baba/2010_07_01/1974
					}
				}
	    	}
    	};


	    public void refreshDisplay(String response){
	    	// parse response message. and get into hash table.
	    	String array[] = response.split("\n", 0);

	    	//String array[] = sample.split("\n", 0);
	    	int size = array.length;
	    	for (int i = 0; i < size; i++){
	    		String params[] = array[i].split(":", 2);
	    		if (params.length == 2){
	    			String values[] = params[1].split("\\[", 2);	// escapeシーケンスが無いと PatternSyntacExceptionが発生する。
	    			Log.d("params", params[0] + ":" + values[0]);
	    			valueMap.put(params[0], values[0]);
	    		}else{
	    			Log.d("param", params[0]);
	    		}
	    	}

	    	for (Iterator<Entry<String, String>> it = valueMap.entrySet().iterator(); it.hasNext(); ){
	    		Entry<String, String> entry = it.next();

	    		String key = (String)entry.getKey();
	    		String value = (String)entry.getValue();

	    		Log.d("key", key+":"+value);

		    	// コンポーネントMapより TAGによりVIEWを取得
	    		EditText view = (EditText)viewMap.get(key);
		    	// VIEWがもつテキストプロパティに、値を設定。
	    		view.setText(value);
	    	}
	    }


		public void onSuccess(String cmd, final String result) {
			if (cmd == "lsdat"){
				Log.i("preset loader", "now uptodate");
				//Toast.makeText((Context)fragmentfront.getActivity(), "update lsdat", Toast.LENGTH_SHORT).show();
				mHandler.post(new Runnable(){
					public void run(){
						refreshDisplay(result);
					}
				});

				latestResponse = result;
			}else{
				Log.d("preset loader", "succeeded: "+ cmd);
			}

		}


		public void onFailed(String cmd, int code) {
			//Toast.makeText((Context)fragmentfront.getActivity(), "falied socket taslt", Toast.LENGTH_SHORT).show();
			Log.d("preset loader", "failed: " + cmd + ", reason: " + code);
		}

		private void setDataBase() {
			mDbHelper = new DataBaseHelper(this.getActivity().getApplicationContext(), mThisPackageName );
			try{
				mDbHelper.createEmptyDataBase();
				mDb = mDbHelper.openDataBase();
			}catch (IOException ioe){
				throw new Error("Unable to create database");
			}
			catch (SQLException sqle){
				throw sqle;
			}
		}


		String sample =
				"> --- SETTING ---\n" +
				"S1:00.00[m/sec]\n" +
				"S2:00.00[m/sec]\n" +
				"S3:00.00[m/sec]\n" +
				"S4:00.00[m/sec]\n" +
				"R1:00.00[m/sec]\n" +
				"R2:00.00[m/sec]\n" +
				"B1:00000\n" +
				"B2:00000\n" +
				"B3:11500\n" +
				"B4:11500\n" +
				"B5:11500\n" +
				"D1:0000.00[m]\n" +
				"D2:0000.00[m]\n" +
				"D3:0000.00[m]\n" +
				"D4:0000.00[m]\n" +
				"ST:00.000[sec]\n" +
				"TS:00.000[sec]\n" +
				"T1:00.000[sec]\n" +
				"T2:00.000[sec]\n" +
				"T3:00.000[sec]\n" +
				"E1:00[%]\n" +
				"E2:00[%]\n" +
				"SW:ON\n" +
				"--- STATUS ---\n" +
				"AMP:0512\n" +
				"BRK:00000\n" +
				"REV_P:00000.00[m]\n" +
				"REV_D:00000.00[m]\n" +
				"REV_E:00000.00[m]\n" +
				"OMG_P:00.00[m/sec]\n" +
				"OMG_D:00.00[m/sec]\n" +
				"VOLT_S:00.00[V]\n" +
				"VOLT_D:00.00[V]\n" +
				"CURR_D:000.00[A]\n" +
				"POW_D:0000.0[W]\n" +
				"\n" +
				">\n";

    }
}