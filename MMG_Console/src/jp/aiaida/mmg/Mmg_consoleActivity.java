package jp.aiaida.mmg;

import jp.aiaida.panelfront.PanelFrontActivity.FragmentFront;
import jp.aiaida.panelpreference.PanelPreferenceActivity;
//import jp.aiaida.panelpreference.R;
import jp.aiaida.panelpreference.PanelPreferenceActivity.FragmentPreference;
import jp.aiaida.panelpreset.PanelPresetActivity.FragmentPreset;
import jp.aiaida.panelterminal.PanelTerminalActivity.FragmentTerminal;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class Mmg_consoleActivity extends Activity {
    private ProgressDialog mProgressDialog;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContenView()より前に ActionBar表示を設定
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.main);
        // Action Barにタブを追加する。

        final ActionBar bar = getActionBar();
        //final int tabCount = bar.getTabCount();
        final int tabCount = 1;
        String text;
        Fragment fragment;
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        Bundle args;

        text  = "FRONT";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentFront>(this, FragmentFront.class, text)));

        args = new Bundle();
        args.putInt("page", 1);
        text = "PRESET1";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreset>(this, FragmentPreset.class, text, args)));

        args = new Bundle();
        args.putInt("page", 2);
        text = "PRESET2";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreset>(this, FragmentPreset.class, text, args)));

        args = new Bundle();
        args.putInt("page", 3);
        text = "PRESET3";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreset>(this, FragmentPreset.class, text, args)));

        args = new Bundle();
        args.putInt("page", 4);
        text = "PRESET4";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreset>(this, FragmentPreset.class, text, args)));

        args = new Bundle();
        args.putInt("page", 5);
        text = "PRESET5";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreset>(this, FragmentPreset.class, text, args)));

        text = "TERMINAL";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentTerminal>(this, FragmentTerminal.class, text)));

        text = "CONFIG";
        bar.addTab(bar.newTab()
                .setText(text)
                .setTabListener(new TabListener<FragmentPreference>(this, FragmentPreference.class, text)));

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }


    }

    /*
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add("Configuration");
    	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();

        Fragment newFragment = FragmentPreference.instantiate(this, FragmentPreference.class.getName(), null);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.fragment_content, newFragment).commit();

		return super.onOptionsItemSelected(item);
	}
	*/

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO 自動生成されたメソッド・スタブ
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
	}
    void showDialog(){
    	//myFragment.show(fragmentTransaction, "PRESET");	// この方法は有効ではない。
    	mProgressDialog = new ProgressDialog(this);
    	mProgressDialog.setTitle("now loading...");
    	mProgressDialog.show();
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
    }

    void dismissDialog(){
    	mProgressDialog.dismiss();
    }

	private class TabListener<T extends Fragment> implements ActionBar.TabListener {
    	private Activity mActivity;
        private Fragment mFragment;
        private final Class<T> mClass;
        private Fragment mInstance;
        private final String mTag;
        private final Bundle mArgs;


        public TabListener(Activity activity, Class<T>clz, String tag ){
        	this(activity, clz, tag, null);
        }


        public TabListener(Activity activity, Class<T>clz, String tag, Bundle args ){
        	mActivity = activity;
        	mClass = clz;
        	mTag = tag;
        	mArgs = args;

        	mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
        	if (mFragment != null && !mFragment.isDetached()){
        		FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        		ft.detach(mFragment);
        		ft.commit();
        	}
        }


        public void onTabSelected(Tab tab, FragmentTransaction ft) {
        	if (mFragment == null){
        		//mFragment = mInstance;
        		mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(R.id.fragment_content, mFragment, mTag); // framgent_contentは main.xmlで定義されている
        	}else{
        		ft.attach(mFragment);
        	}
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        	if (mFragment != null){
        		ft.detach(mFragment);
        	}
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            //Toast.makeText(Mmg_consoleActivity.this, "Reselected!", Toast.LENGTH_SHORT).show();
        }

    }

}