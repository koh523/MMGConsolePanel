package jp.aiaida.panelpreference;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;


 /**
 * @author koh
 *
 * Prefereceデータの変更後直後の summaryフィールドへの反映方法にはいくつかの手法があるようだ。
 *   1. PreferenceScreen上のダイアログが dismissする際発行するイベントを捕らえる。
 *   2. 各種Prererenceオブジェトが 発行する変更イベントをとらえる。
 *   3. Preferenceデータそのものが変更された際発行されるイベントを捕らえる
 *
 *   ここでは、２番目の各オブジェクトが発行する変更イベントに対するリスナを用意し、その処理ルーチンで
 *   summaryフィールドを更新する方法を採用した。
 *
 *    方法1の参照サイトは http://yan-note.blogspot.jp/2010/09/android-preferencescreen.html
 *    ただし、PreferenceScreen自体に変化が起きないとイベント発行しないようで、サンプルに様に
 *    PreferenceSreenが入れ子になっていない、本事例では 使えない手法であった。
 *
 *    方法２は 方法１がうまくいかず模索している段階で Android APIDemo を参照している際に
 *    EditTextPrefereceそのものがイベントを発行することに気づき、独自実装したもの。
 *
 *    方法３の参照サイトは http://techbooster.jpn.org/andriod/ui/1066/
 *    本サービスを利用する前後で SharedPreferenceオブジェクトに対して Listerを登録・非登録する
 *    処理を追加する必要がある。
 *
 *    スマートでジェネリックな方法は 方法３であるが、方法３の参考サイトにたどり付く前に
 *    方法２で独自実装してしまった、かつ、現時点(2012.7.17)現在、問題なく動作しているので
 *    そのまま採用することにする。
 *
 *
 */
public class PanelPreferenceActivity extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Fragment newFragment = FragmentPreference.instantiate(this, PanelPreferenceActivity.FragmentPreference.class.getName(), null);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.fragment_content, newFragment).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_panel_preference, menu);
        return true;
    }

    public static class FragmentPreference extends PreferenceFragment {
    	public static final String KEY_TARGET_IP_ADDRESS = "target_ip_adress";
    	public static final String KEY_TARGET_IP_PORT = "target_ip_port";
    	public static final String KEY_FLAG_ENABLE_NETWORK = "flag_enable_network";
    	public static final String KEY_FLAG_CLIMERMODE_FREERUN = "flag_climermode_freerun";
    	public static final String KEY_SESSION_INTERVAL = "session_interval";
    	public static final String KEY_SESSION_TIMELIMIT = "session_timelimit";
//    	private static final String KEY_PREFERENCE_SCREEN = "preference_screen";

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
           // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference);

            final SharedPreferences myPreferenceFile = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			EditTextPreference prefTargetIpAddress = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_TARGET_IP_ADDRESS);
			prefTargetIpAddress.setSummary(myPreferenceFile.getString(KEY_TARGET_IP_ADDRESS, ""));
			prefTargetIpAddress.setOnPreferenceChangeListener(onPreferenceChangeListener);


			EditTextPreference prefTargetIpPort = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_TARGET_IP_PORT);
			prefTargetIpPort.setSummary(myPreferenceFile.getString(KEY_TARGET_IP_PORT, ""));
			prefTargetIpPort.setOnPreferenceChangeListener(onPreferenceChangeListener);

			CheckBoxPreference prefFlagEnableNetwork = (CheckBoxPreference)this.getPreferenceScreen().findPreference(KEY_FLAG_ENABLE_NETWORK);
			prefFlagEnableNetwork.setChecked(myPreferenceFile.getBoolean(KEY_FLAG_ENABLE_NETWORK, true));

			CheckBoxPreference prefFlagClimerMode = (CheckBoxPreference)this.getPreferenceScreen().findPreference(KEY_FLAG_ENABLE_NETWORK);
			prefFlagClimerMode.setChecked(myPreferenceFile.getBoolean(KEY_FLAG_CLIMERMODE_FREERUN, false));

			EditTextPreference prefSessionInterval = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_SESSION_INTERVAL);
			prefSessionInterval.setSummary(myPreferenceFile.getString(KEY_SESSION_INTERVAL, "200"));
			prefSessionInterval.setOnPreferenceChangeListener(onPreferenceChangeListener);

			EditTextPreference prefSessionTimelimit = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_SESSION_TIMELIMIT);
			prefSessionTimelimit.setSummary(myPreferenceFile.getString(KEY_SESSION_TIMELIMIT, "5000"));
			prefSessionTimelimit.setOnPreferenceChangeListener(onPreferenceChangeListener);

			/**
			 *  PreferenceScreen上での イベントは発生しないので 下記センテンスは 無用
			 *
			PreferenceScreen screen = (PreferenceScreen)this.findPreference(KEY_PREFERENCE_SCREEN);
			screen.setOnPreferenceClickListener(onPreferenceClickListener);

			screen.setOnPreferenceChangeListener(onPreferenceChangeListener);
			*/
		}



		private final OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				EditTextPreference editTextPref = (EditTextPreference)preference;
				editTextPref.setText((String)newValue);
				editTextPref.setSummary((CharSequence)newValue);
				return false;
			}
		};

/*
 *		OnPreferenceClickListenerは preferenceがクリックされた時点でコールされるため、
 *      値は変更前のもの。従って、summaryを更新するには 適していないイベントと判断。
 *
 		private final OnPreferenceClickListener onPreferenceClickListener  = new OnPreferenceClickListener(){

			public boolean onPreferenceClick(Preference preference) {
				//PreferenceScreen  screen = (PreferenceScreen)preference;
				PreferenceScreen screen = (PreferenceScreen)findPreference(KEY_PREFERENCE_SCREEN);
				Dialog dialog = screen.getDialog();
				dialog.setOnDismissListener(dialogDismissListener);

				return false;
			}

		};
*/

/*
 * 		値を変更するために表示されたダイアログが消える段階でイベントが発行することを想定。
 *      ベースのPreferenceScreenに 当該イベントをリンクさせるも、イベントそのものが発生しないため
 *      今回の実装では適さない。
		private final OnDismissListener dialogDismissListener = new OnDismissListener(){

			public void onDismiss(DialogInterface dialog) {
				updateSummary();
			}
		};
*/
/*
		private final void updateSummary(){
            SharedPreferences myPreferenceFile = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			EditTextPreference prefTargetIpAddress = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_TARGET_IP_ADDRESS);
			prefTargetIpAddress.setSummary(myPreferenceFile.getString(KEY_TARGET_IP_ADDRESS, ""));

			EditTextPreference prefTargetIpPort = (EditTextPreference)this.getPreferenceScreen().findPreference(KEY_TARGET_IP_PORT);
			prefTargetIpPort.setSummary(myPreferenceFile.getString(KEY_TARGET_IP_PORT, ""));

			CheckBoxPreference prefFlagEnableNetwork = (CheckBoxPreference)this.getPreferenceScreen().findPreference(KEY_FLAG_ENABLE_NETWORK);
			prefFlagEnableNetwork.setChecked(myPreferenceFile.getBoolean(KEY_FLAG_ENABLE_NETWORK, true));
		}
*/
    }
}