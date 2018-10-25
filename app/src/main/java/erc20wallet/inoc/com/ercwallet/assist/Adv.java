package erc20wallet.inoc.com.ercwallet.assist;

import android.util.Log;

public class Adv {

    private static final String TAG = "inoc_wallet";
    private static final int WANT_TO_KNOW_LEVEL[] = {2 };
    private static final LOG_INFO WANT_TO_KNOW_INFOS[] = {LOG_INFO.CLASS , LOG_INFO.METHOD , LOG_INFO.LINE_NUMBER};
    enum LOG_INFO { FILENAME, CLASS, METHOD , LINE_NUMBER }


    private static String _posOfSource(){
        Exception ex = new Exception();
        StackTraceElement[] ele = ex.getStackTrace();

        StringBuilder _sourceLocation = new StringBuilder("");
        for( int i=0 ; i<WANT_TO_KNOW_LEVEL.length ; i++){
            for (LOG_INFO type : WANT_TO_KNOW_INFOS ){
                switch (type ){
                    case FILENAME:
                        _sourceLocation.append("F"+ele[WANT_TO_KNOW_LEVEL[i]].getFileName());
                        break;
                    case CLASS:
                        _sourceLocation.append("#"+ele[WANT_TO_KNOW_LEVEL[i]].getClassName());
                        break;
                    case METHOD:
                        _sourceLocation.append("-"+ele[WANT_TO_KNOW_LEVEL[i]].getMethodName());
                        break;
                    case LINE_NUMBER:
                        _sourceLocation.append(":"+ele[WANT_TO_KNOW_LEVEL[i]].getLineNumber());
                        break;
                }
            }
            if( i+1 == WANT_TO_KNOW_LEVEL.length ){

            }else{
                _sourceLocation.append("\n");
            }
        }
        return _sourceLocation.toString();
    }

    public static void e(String log){

        Log.e(TAG , _posOfSource() + "\n [[ERR]] " + log);
    }
    public static void i(String log){
        _posOfSource();
        Log.i(TAG , _posOfSource() + "\n [[Info]] " + log);
    }
    public static void v(String log){

        Log.v(TAG , _posOfSource() + "\n [[Verbose]] " + log);
    }
    public static void w(String log){

        Log.w(TAG , _posOfSource() + "\n [[warn]] " + log);
    }


}
