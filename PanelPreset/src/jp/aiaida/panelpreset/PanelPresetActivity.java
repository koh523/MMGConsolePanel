package jp.aiaida.panelpreset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.aiaida.panelpreference.PanelPreferenceActivity.FragmentPreference;
import jp.aiaida.panelterminal.SocketClientTask;
import jp.aiaida.panelterminal.SocketClientTask.SocketClientTaskCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PanelPresetActivity extends Activity {

	int page = 1;

	// 定数定義 将来的には独立クラスを定義して クラスから参照する形にする。 2012.6.10
	static final int NUM_CC_PARAMS = 22;
//	static final String THIS_PACKAGE_NAME = "jp.aiaida.panelpreset";
	public static final String TABLE_SLIDER_PROP = "slider_prop";
	public static final String TABLE_PRESET_SET = "preset_set";

	static final String DEFALT_IP_ADDRESS_SERVER = "192.168.111.10";
	static final String DEFALT_IP_PORT_SERVER = "8181";

	static final int SOCKET_CLIENT_TASK_TIMELIMIT = 1000;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//mContext = this.getApplicationContext();

		//Fragment newFragment = FragmentPreset.newInstance(page);
		Bundle args = new Bundle();
		args.putInt("page", 2);
		Fragment newFragment = FragmentPreset.instantiate(this, PanelPresetActivity.FragmentPreset.class.getName(), args);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.fragment_content, newFragment).commit();
	}

	public static class FragmentPreset extends Fragment implements SocketClientTaskCallback{
		static int m_page;
		private Button btnApply;
		private Button btnCommit;
		private Button btnCancel;
		private View[] arraySeekbars= new View[NUM_CC_PARAMS];
		private View[] arrayDigitviews = new View[NUM_CC_PARAMS];
		private List<SliderProperty> sliderList = new ArrayList<SliderProperty>();
		FragmentPreset fragmentpreset = this;
		private String mThisPackageName;// = this.getActivity().getApplicationContext().getPackageName();

		private DataBaseHelper mDbHelper;
		private SQLiteDatabase mDb;

		private boolean mSocketClientTaskBusy = false;
/*
    	String ip_address_server = DEFALT_IP_ADDRESS_SERVER;
    	String ip_port_server = DEFALT_IP_PORT_SERVER;
 */
        private String ip_address_server;
        private String ip_port_server;
        private int mTimeLimit;
        private int mSessionInterval;
        boolean flagEnableNetwork;

        EditText editTextOnDialog;
        private int mActiveIndex;

    	/*
		public static FragmentPreset newInstance(int page) {
			FragmentPreset fp = new FragmentPreset();
			m_page = page;
			return fp;
		}
		*/

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			mThisPackageName = this.getActivity().getApplicationContext().getPackageName();

			final SharedPreferences myPreferenceFile = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
	        ip_address_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_ADDRESS, DEFALT_IP_ADDRESS_SERVER);
	        ip_port_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_PORT, DEFALT_IP_PORT_SERVER);
	        mTimeLimit = Integer.valueOf(myPreferenceFile.getString(FragmentPreference.KEY_SESSION_TIMELIMIT, "4000"));
	        mSessionInterval = Integer.valueOf(myPreferenceFile.getString(FragmentPreference.KEY_SESSION_INTERVAL, "200"));
	        flagEnableNetwork = myPreferenceFile.getBoolean(FragmentPreference.KEY_FLAG_ENABLE_NETWORK, false);
			m_page = getArguments().getInt("page");
			/* --- DBの初期化 --- */
			setDataBase();

			Cursor cursor = mDb.rawQuery("select * from "+TABLE_SLIDER_PROP, null);
			cursor.moveToFirst();
			while(!cursor.isAfterLast()){
				sliderList.add(SliderProperty.extractSliderProperty(cursor));
				cursor.moveToNext();
			}

			View fragView = inflater.inflate(R.layout.panel_preset, container, false);
			cursor = mDb.rawQuery("select * from " + TABLE_PRESET_SET + " where _id = "+m_page, null);
			cursor.moveToFirst();

			try{
				/* --- Seekbar群の初期化 および 配列への登録 --- */
				for (int i = 0; i < NUM_CC_PARAMS; i++) {
					String element = "element" + i;
					int rid = getResources().getIdentifier(element, "id", mThisPackageName);
					if (rid > 0) {
						LinearLayout layout = (LinearLayout) fragView.findViewById(rid);
						SeekBar seekbar = (SeekBar) layout.findViewById(R.id.seekBar1);
						//seekbar.setOnSeekBarChangeListener(seekbarListener);

						SliderProperty property = sliderList.get(i);
						if (property != null){
							TextView textview = (TextView)layout.findViewById(R.id.txtCaption);
							textview.setText(property.getCaption());

							textview = (TextView)layout.findViewById(R.id.txtLabelMin);
							textview.setText(property.getLabelMin());

							textview = (TextView)layout.findViewById(R.id.txtLabelMax);
							textview.setText(property.getLabelMax());

							textview = (TextView)layout.findViewById(R.id.txtUnits);
							textview.setText(property.getLabelUnit());

							int max = (int)(property.getScaleMax() - property.getScaleMin());

							seekbar.setMax(max);
							seekbar.setOnSeekBarChangeListener(seekbarListener);
							arraySeekbars[i] =seekbar;

							textview = (TextView)layout.findViewById(R.id.txtDigitalDisplay);
							textview.setClickable(true); // textViewでは イベント有効化を明示することが必要。
							textview.setOnClickListener(textviewClickListener);
							textview.setClickable(true);
							//textview.setTextIsSelectable(true);
							arrayDigitviews[i] = textview;

							/* --- lastデータの設定 ---*/
							if (cursor != null){
								int column = cursor.getColumnIndex(property.getName());
								int value = (int)cursor.getLong(column);
								seekbar.setProgress(value);
								textview.setText(FormatedIndicator((long)value, property));
							}
						}
				}
			}

			}catch(IndexOutOfBoundsException e){

			}

			/* --- ボタン用イベントの登録 --- */
			btnApply = (Button) fragView.findViewById(R.id.btn_apply);
			btnCommit = (Button) fragView.findViewById(R.id.btn_commit);
			btnCancel = (Button) fragView.findViewById(R.id.btn_cancel);

			btnApply.setOnClickListener(buttonClickListener);
			btnCommit.setOnClickListener(buttonClickListener);
			btnCancel.setOnClickListener(buttonClickListener);

			return fragView;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			if (savedInstanceState != null){
				for (int i = 0; i < NUM_CC_PARAMS; i++){
					SeekBar bar = (SeekBar)arraySeekbars[i];
					int value = savedInstanceState.getInt("param"+i, 100);
					bar.setProgress(value);
				}
			}else{
				/* ここで refreshDisplay()を実行しても反映されず、初期化時の画面のままとどまっている。
				 * refreshDisplay()の呼び出しポイントは　onResume()に移動したところ、期待した動作と
				 * なった。なぜ反映されないのかは不明のままである。

					//refreshDisplay();
				 */
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			/* refreshDisplya()の呼び出しポイントを onActivityCreated()
			 * から移動させた。 2012.7.13
			 */
			if (mDb == null){
				setDataBase();
			}
			refreshDisplay();
		}

		@Override
		public void onPause() {
			super.onPause();
			// データベースのclose()を忘れずに
			mDbHelper.close();
			mDb.close();
			mDbHelper = null;
			mDb = null;
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);

			for (int i = 0; i < NUM_CC_PARAMS; i++){
				SeekBar seekbar = (SeekBar)arraySeekbars[i];
				outState.putInt("param"+i, seekbar.getProgress());
			}
		}

		private final OnClickListener buttonClickListener = new OnClickListener() {
			public void onClick(View view) {
				//Toast.makeText(getActivity(), "Button pressed", Toast.LENGTH_SHORT).show();
				if (view == btnApply) {
					// SocketClientTaskを開始して ターゲットに設定コマンドを発行する。
					for (int i = 0; i < NUM_CC_PARAMS; i++){
						SliderProperty property = sliderList.get(i);
						TextView textview = (TextView)arrayDigitviews[i];

						// build up command message
						String cmd = "SET_" + property.getName() + " = " +  textview.getText();

/* 第一案
	動作はするが、時間がかかる。
	原因: mSocketClientTaskBusyがfalseになることはなく、タイムアウトによって
	      次に処理に移るため。このルーチンはタイムアウトが発生しても、
	      コマンド処理をスキップしていないためうまく動いている様に見える。

						long startTime = System.currentTimeMillis();
						while( (System.currentTimeMillis() - startTime) < SOCKET_CLIENT_TASK_TIMELIMIT){
							if (mSocketClientTaskBusy == false)	break;
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentpreset);
						task.execute(cmd);
						mSocketClientTaskBusy = true;
*/


/* 第2案
						long startTime = System.currentTimeMillis();
						while( (System.currentTimeMillis() - startTime) < SOCKET_CLIENT_TASK_TIMELIMIT){
						{
							if (mSocketClientTaskBusy == false){
								Log.d("sender", cmd);
								SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, (SocketClientTaskCallback)fragmentpreset);
								task.execute(cmd);
								//mSocketClientTaskBusy = true;
								mSocketClientTaskBusy = true;

							}
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
*/

// 暫定処理
						if (flagEnableNetwork == true){
							SocketClientTask task = new SocketClientTask(ip_address_server, ip_port_server, mTimeLimit, (SocketClientTaskCallback)fragmentpreset);
							Log.d("sender", cmd);
							task.execute(cmd);
							try {
								Thread.sleep(mSessionInterval);
							} catch (InterruptedException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}

						}else{
							Log.d("sender", cmd);
						}
					}
				}
				if (view == btnCommit) {
					ContentValues cv = new ContentValues();

					for (int i = 0; i < NUM_CC_PARAMS; i++){
						SliderProperty property = sliderList.get(i);
						SeekBar bar = (SeekBar)arraySeekbars[i];

						String key = property.getName();
						String value = String.valueOf(bar.getProgress());

						cv.put(key, value);
					}
					mDb.update(TABLE_PRESET_SET, cv, "_id = "+m_page, null);
					Log.d("Preset saved", "on page"+m_page);
				}
				if (view == btnCancel){
					refreshDisplay();
					Log.d("Preset canceled", "on page"+m_page);
				}
			}
		};

		private final OnSeekBarChangeListener seekbarListener = new OnSeekBarChangeListener(){
			private int getActiveIndex(SeekBar arg0){
				int index = 0;
				for (int i =0; i < NUM_CC_PARAMS; i++){
					if (arraySeekbars[i] == arg0){
						index = i;
						break;
					}
				}

				return index;
			}

			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				int index = getActiveIndex(arg0);
				try{
					SeekBar bar = (SeekBar)arraySeekbars[index];
					int reading = bar.getProgress();
					SliderProperty property = sliderList.get(index);
					TextView textview = (TextView)arrayDigitviews[index];
					textview.setText(FormatedIndicator(reading, property));
				}
				catch(IndexOutOfBoundsException e){

				}
			}

			public void onStartTrackingTouch(SeekBar arg0) {
				//int index = getActiveIndex(arg0);
				//Toast.makeText(getActivity(), "Bar@"+index+" Touch Started", Toast.LENGTH_SHORT).show();
			}

			public void onStopTrackingTouch(SeekBar arg0) {
				//int index = getActiveIndex(arg0);
				//Toast.makeText(getActivity(), "Bar@"+index+" Touch Stopped", Toast.LENGTH_SHORT).show();
			}
		};

		private final OnClickListener textviewClickListener = new OnClickListener(){

			public void onClick(View arg0) {
				for (int i = 0; i < NUM_CC_PARAMS; i++){
					if (arrayDigitviews[i] == arg0){
						mActiveIndex = i;
						TextView textView = (TextView)arrayDigitviews[i];
						SliderProperty property = sliderList.get(i);

						//Toast.makeText(getActivity(), "Item"+i+" Clicked", Toast.LENGTH_SHORT).show();
						AlertDialog.Builder dlg = new AlertDialog.Builder(getActivity());
						dlg.setTitle(property.getCaption());
						//dlg.setMessage("hello world");
						//NumberPicker numberPicker = new NumberPicker(getActivity());
						String text = (String) textView.getText();
						//int value = Integer.getInteger("100");
						//numberPicker.setValue(1000);

						editTextOnDialog = new EditText(getActivity());
						editTextOnDialog.setText(text);
						editTextOnDialog.setInputType(InputType.TYPE_CLASS_NUMBER);
						editTextOnDialog.setSelection(text.length()-3);

						dlg.setView(editTextOnDialog);
						dlg.setPositiveButton("OK", dialogClickListener);
						dlg.setNegativeButton("Cancel", dialogClickListener);
						dlg.show();
						break;
					}
				}
			}
		};


		private final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				Log.d("dialogClicked", dialog + ":" + which);
				if (which == DialogInterface.BUTTON_POSITIVE){
					String reading =(String)editTextOnDialog.getText().toString();
					Double value = Double.valueOf(reading);
					SeekBar bar = (SeekBar)arraySeekbars[mActiveIndex];
					SliderProperty property = sliderList.get(mActiveIndex);
					int progress =(int)( value * (1.0/property.getScaleResolution()) - property.getScaleMin());
					bar.setProgress(progress);
				}
			}

		};


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



		public static String FormatedIndicator(long reading, SliderProperty property){
			String formated;
			if (property == null)	return null;

			double resolution = property.getScaleResolution();
			double value = (double)(reading + property.getScaleMin());
			value *= resolution;
			/*
			if (resolution < 1){
				formated = String.format("%.1f", value);
			}else{
				formated = String.format("%d", (int)value);
			}
			*/
			// すべてのパラメータにおいて 少数第二位まで表示する。
			formated = String.format("%.2f", value);
			return formated;
		}

		private void refreshDisplay(){
			Cursor cursor = mDb.rawQuery("select * from " + TABLE_PRESET_SET + " where _id = "+m_page, null);
			if (cursor != null){
				cursor.moveToFirst();

				for (int i = 0; i < NUM_CC_PARAMS; i++) {
					SliderProperty property = sliderList.get(i);
					SeekBar seekbar = (SeekBar)arraySeekbars[i];
					TextView textview = (TextView)arrayDigitviews[i];

					int column = cursor.getColumnIndex(property.getName());
					int value = (int)cursor.getLong(column);
					seekbar.setProgress(value);
					textview.setText(FormatedIndicator((long)value, property));
				}
			}
		}
/*
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO 自動生成されたメソッド・スタブ

		}
*/

		public void onSuccess(String cmd, String result) {
			mSocketClientTaskBusy = false;
			Log.d("sender", "succeeded: "+ cmd);
		}

		public void onFailed(String cmd, int code) {
			mSocketClientTaskBusy = false;
			Log.e("sender", "failed: " + cmd + ", reason: " + code);
		}
	}

	public static class PresetSet {
		static SQLiteDatabase db;
		static DataBaseHelper dbHelper;

		public static HashMap<String, Long> getPresetSet(Activity activity, int page){
			String name;
			int column;
			long value;
			SliderProperty property = null;

			final List<SliderProperty> sliderList = new ArrayList<SliderProperty>();
			HashMap <String, Long>presetSet = new HashMap<String, Long>();

			try {
				setDataBase(activity);

				Cursor cursor = db.rawQuery("select * from "+TABLE_SLIDER_PROP, null);
				if (cursor != null){
					cursor.moveToFirst();
					while(!cursor.isAfterLast()){
						sliderList.add(SliderProperty.extractSliderProperty(cursor));
						cursor.moveToNext();
					}

					cursor = db.rawQuery("select * from " + TABLE_PRESET_SET + " where _id = " + page, null);
					if (cursor != null) {
						cursor.moveToFirst();
						for (int i = 0; i < NUM_CC_PARAMS; i++) {
							property = sliderList.get(i);
							name = property.getName();
							column = cursor.getColumnIndex(name);
							value = cursor.getLong(column);
							Log.d("getPresetSet", name + ": " + value);
							presetSet.put(name, value);
						}
					}
				}
			} catch (IndexOutOfBoundsException e) {

			}finally{
				dbHelper.close();
				db.close();
			}
			return presetSet;
		}

		private final static void setDataBase(Activity activity) {
			dbHelper = new DataBaseHelper(activity.getApplicationContext(), activity.getApplicationContext().getPackageName());
			try{
				dbHelper.createEmptyDataBase();
				db = dbHelper.openDataBase();
			}catch (IOException ioe){
				throw new Error("Unable to create database");
			}
			catch (SQLException sqle){
				throw sqle;
			}
		}
	}
}