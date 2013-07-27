package jp.aiaida.panelterminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


public class SocketClientTask implements Runnable{
	private String mIpAddress;
	private int mIpPort;
	private String mCommand;
	private StringBuilder sbReceiveMsg;
	private Thread thread;
	private boolean runState = true;
	private int reasonCode;
	private int mTimeLimit;
	SocketClientTaskCallback mCallback;

	private static final long TIME_LIMIT= 4000;
	private static final String MY_SIGNETURE = "SokcetClientTask";

	private static final int ERR_SOCKETCLIENTTASK_LOSTCONNECTION = -1;
	private static final int ERR_SOCKETCLIENTTASK_TIMEOVER = -2;
	private static final int ERR_SOCKETCLIENTTASK_FORCESTOP = -4;
	private static final int ERR_SCOKETCLIENTTASK_HASEXCEPTION = -8;

	public interface SocketClientTaskCallback{
		void onSuccess(String cmdmsg, String response);
		void onFailed(String cmdmsg, int reason);
	}

	public SocketClientTask(String address, String port, int limit, SocketClientTaskCallback callback){
		mIpAddress = address;
		mIpPort = Integer.parseInt(port);
		sbReceiveMsg = new StringBuilder();
		mCallback = callback;
		mTimeLimit = limit;
	}

	/*
	public void execute(final String cmd){
 		(new Thread(){public void run(){
 			try{
				String response = connect(cmd);
//	            Log.i("Response", response);
				if (response == null){
					mCallback.onFailed(cmd, -1);
				}else {
					Log.i("execute", "just callig callbaak func");
					mCallback.onSuccess(cmd, response);
				}

			}catch(Exception e){
			}
		}}).start();
	}
	*/

	public void execute(final String cmd){
		mCommand = cmd;
		thread = new Thread(this);
		thread.start();
	}

	String connect(){
		return connect(mCommand);
	}

	synchronized String connect(String cmdmsg){
		Socket socket = null;
		InputStream is = null;
		PrintWriter pw = null;
		BufferedReader br = null;
		long startTime;

		runState = true;

        try {
            socket = new Socket(mIpAddress, mIpPort);
            // 出力処理
            pw = new PrintWriter(socket.getOutputStream(),true);
            pw.println(cmdmsg);
            Log.d(MY_SIGNETURE, cmdmsg);

            // 入力処理
            is = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            int prompt = 0;	// prompt">"の数を メッセージエンドと認識させるため。
	        startTime = System.currentTimeMillis();

            while(runState){
            	if (is.available() > 0){
                	char[] readline = new char[is.available()];
                	br.read(readline);
                	if (String.valueOf(readline).toString().indexOf("> ") >= 0){
                		prompt++;
                		Log.i("find prompt", Integer.toString(prompt));
                	}
                	sbReceiveMsg.append(readline);
                	if (prompt == 2){
                		/* 正常exit */
                		break;
                	}
            	}
            	if (socket.isConnected() == false){
            		runState = false;
            		reasonCode = ERR_SOCKETCLIENTTASK_LOSTCONNECTION;
            		break;
            	}
        		long erapsedTime  =  System.currentTimeMillis() - startTime;
        		if ((erapsedTime - TIME_LIMIT) > 0){
        			runState = false;
        			reasonCode = ERR_SOCKETCLIENTTASK_TIMEOVER;
        			break;
        		}
            }
            pw.close();
            br.close();
            is.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            runState = false;
            reasonCode = ERR_SCOKETCLIENTTASK_HASEXCEPTION;
        } catch (IOException e) {
            e.printStackTrace();
            runState = false;
            reasonCode = ERR_SCOKETCLIENTTASK_HASEXCEPTION;
        } finally{
        	try {
                pw.close();
                br.close();
                is.close();
                socket.close();
                socket = null;
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
        String stResponse = null;
        if (runState == true){
        	stResponse = sbReceiveMsg.toString();
            sbReceiveMsg = null;
        }
		return stResponse;
 	}

	public void stop()
	{
		runState = false;
		reasonCode = ERR_SOCKETCLIENTTASK_FORCESTOP;
		//thread = null;
	}

	public void run() {
		try{
			String response = connect();
//            Log.i("Response", response);
			if (response == null){
				mCallback.onFailed(mCommand, reasonCode);
			}else {
				Log.i("execute", "just callig callbaak func");
				mCallback.onSuccess(mCommand, response);
			}

		}catch(Exception e){
		}
		thread = null;
	}

}
