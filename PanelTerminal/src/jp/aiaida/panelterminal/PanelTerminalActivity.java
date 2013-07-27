package jp.aiaida.panelterminal;

import jp.aiaida.panelpreference.PanelPreferenceActivity.FragmentPreference;
import jp.aiaida.panelterminal.SocketClientTask.SocketClientTaskCallback;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PanelTerminalActivity extends Activity {
	static final String DEFALT_IP_ADDRESS_SERVER = "192.168.111.10";
	static final String DEFALT_IP_PORT_SERVER = "8181";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Fragment newFragment = FragmentTerminal.newInstance();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.fragment_content, newFragment).commit();
	}

	public static class FragmentTerminal extends Fragment implements SocketClientTaskCallback{
		EditText etAddress;
		EditText etPort;
		EditText etSend;
		TextView tvResponse;
		static Handler mHandler;

		public static FragmentTerminal newInstance() {
			FragmentTerminal ff = new FragmentTerminal();
			return ff;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);

	        final SharedPreferences myPreferenceFile = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
	        String ip_address_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_ADDRESS, DEFALT_IP_ADDRESS_SERVER);
	        String ip_port_server = myPreferenceFile.getString(FragmentPreference.KEY_TARGET_IP_PORT, DEFALT_IP_PORT_SERVER);
	        final int timelimit = Integer.valueOf(myPreferenceFile.getString(FragmentPreference.KEY_SESSION_TIMELIMIT, "4000"));

			final View fragView = inflater.inflate(R.layout.panel_terminal,	container, false);

			etAddress = (EditText) fragView.findViewById(R.id.txtIPAdress);
			etPort = (EditText) fragView.findViewById(R.id.txtPort);
			etSend = (EditText) fragView.findViewById(R.id.txtSend);
			tvResponse = (TextView) fragView.findViewById(R.id.txtReceive);

			etAddress.setText(ip_address_server);
			etPort.setText(ip_port_server);

			final FragmentTerminal fragment = this;
			mHandler = new Handler();

			Button btnSend = (Button) fragView.findViewById(R.id.btnSend);
			btnSend.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					String strAddress = etAddress.getText().toString();
					String strPort = etPort.getText().toString();
					String strRequest = etSend.getText().toString();
					SocketClientTask task = new SocketClientTask(strAddress, strPort, timelimit, (SocketClientTaskCallback)fragment);
					task.execute(strRequest);
				}
			});

			Button btnClear = (Button) fragView.findViewById(R.id.btnClear);
			btnClear.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					etSend.getEditableText().clear();
					tvResponse.setText("");
				}

			});

			return fragView;
		}

		public void onSuccess(String args, final String result) {
			//tvResponse.setText(result);
			//tvResponse.setText("just recieved");
			mHandler.post(new Runnable(){
				public void run(){
					tvResponse.setText(result);
				}
			});
		}

		public void onFailed(String args, int code) {

		}

	}

}