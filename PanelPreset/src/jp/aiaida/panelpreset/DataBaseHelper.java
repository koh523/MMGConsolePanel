package jp.aiaida.panelpreset;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import au.com.bytecode.opencsv.CSVReader;



public class DataBaseHelper extends SQLiteOpenHelper {

    //The Android のデフォルトでのデータベースパス
//    private static String DB_PATH = "/data/data/jp.aiaida.panelpreset/databases/";
    private static String DB_ROOT = "/data/data/";
    private static String DB_NAME = "mmg_cc";
    private static String DB_NAME_ASSET = "external_sqlitedb.db";
    /*
     * CVSフィアルを使ったDB初期化は見送り。 2012.06.23
     *
    private static String CVS_NAME_ASSET = "slider_prop.csv";
    */
    private SQLiteDatabase mDataBase;
    private final Context mContext;
    private final String mDbPath;

	public DataBaseHelper(Context context, String packageName) {
		super(context, DB_NAME, null, 1);
        this.mContext = context;
        this.mDbPath = DB_ROOT + packageName +"/databases/";
	}

    /**
     * asset に格納したデータベースをコピーするための空のデータベースを作成する
     *
     **/
    public void createEmptyDataBase() throws IOException{
        boolean dbExist = checkDataBaseExists();

        if(dbExist){
            // すでにデータベースは作成されている
        }else{
            // このメソッドを呼ぶことで、空のデータベースが
            // アプリのデフォルトシステムパスに作られる
            this.getReadableDatabase();

            try {
                // asset に格納したデータベースをコピーする
                copyDataBaseFromAsset();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * 再コピーを防止するために、すでにデータベースがあるかどうか判定する
     *
     * @return 存在している場合 {@code true}
     */
    private boolean checkDataBaseExists() {
        SQLiteDatabase checkDb = null;

        try{
            String dbPath = mDbPath + DB_NAME;
            checkDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(SQLiteException e){
            // データベースはまだ存在していない
        }

        if(checkDb != null){
            checkDb.close();
        }
        return checkDb != null ? true : false;
    }

    /**
     * asset に格納したデーだベースをデフォルトの
     * データベースパスに作成したからのデータベースにコピーする
     * */
    private void copyDataBaseFromAsset() throws IOException{

        // asset 内のデータベースファイルにアクセス
        InputStream mInput = mContext.getAssets().open(DB_NAME_ASSET);

        // デフォルトのデータベースパスに作成した空のDB
        String outFileName = mDbPath + DB_NAME;

        OutputStream mOutput = new FileOutputStream(outFileName);

        // コピー
        byte[] buffer = new byte[1024];
        int size;
        while ((size = mInput.read(buffer)) > 0){
            mOutput.write(buffer, 0, size);
        }

        //Close the streams
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    /**
     * assetに格納されているCSVファイルをデータベースの初期値として
     * データベースの初期化時に読み込む
     * @return void
     * @throws IOException
     */
/*
 *  CVSファイルを使ったDBの初期化手法の採用は見送りとする。 2012.06.23
 *
     private void initDataBaseWithCVSinAsset() throws IOException{
    	// 新規に空のDBを作成
        SQLiteDatabase db = openDataBase();

        // asset 内のデータベースファイルにアクセス
        InputStream input = mContext.getAssets().open(CVS_NAME_ASSET);

        CSVReader csv = new CSVReader(new InputStreamReader(input, "UTF-8"));

        String[] column;



        DatabaseUtils.InsertHelper ih = new DatabaseUtils.InsertHelper(db, TABLE_NAME);
        int col_id = ih.getColumnIndex("_id");								// #0
        int col_name = ih.getColumnIndex("name");							// #1
        int col_caption = ih.getColumnIndex("caption");						// #2
        int col_rangemin = ih.getColumnIndex("range_min");					// #3
        int col_rangemax = ih.getColumnIndex("range_max");					// #4
        int col_scalemin = ih.getColumnIndex("scale_min");					// #5
        int col_scalemax = ih.getColumnIndex("scale_max");					// #6
        int col_scaleresolution = ih.getColumnIndex("scale_resolution");	// #7
        int col_labelmin = ih.getColumnIndex("label_min");					// #8
        int col_labelmax = ih.getColumnIndex("label_max");					// #9
        int col_labelunit = ih.getColumnIndex("label_unit");				// #10

        while ((column = csv.readNext()) != null) {
            ih.prepareForInsert();
            ih.bind(col_id, column[0]);
            ih.bind(col_name, column[1]);
            ih.bind(col_caption, column[2]);
            ih.bind(col_rangemin, column[3]);
            ih.bind(col_rangemax, column[4]);
            ih.bind(col_scalemin, column[5]);
            ih.bind(col_scalemax, column[6]);
            ih.bind(col_scaleresolution, column[7]);
            ih.bind(col_labelmin, column[8]);
            ih.bind(col_labelmax, column[9]);
            ih.bind(col_labelunit, column[10]);
            ih.execute();
        }
    }
*/

    public SQLiteDatabase openDataBase() throws SQLException{
        //Open the database
        String myPath = mDbPath + DB_NAME;
        mDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
        return mDataBase;
    }

    @Override
	public void onCreate(SQLiteDatabase db) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public synchronized void close() {
		// TODO 自動生成されたメソッド・スタブ
		if (mDataBase != null){
			mDataBase.close();
		}
		super.close();
	}


}