package erc20wallet.inoc.com.ercwallet;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static erc20wallet.inoc.com.ercwallet.assist.Adv.e;
import static erc20wallet.inoc.com.ercwallet.assist.Adv.i;
import static erc20wallet.inoc.com.ercwallet.assist.Adv.v;

public class MainActivity extends AppCompatActivity {

    private File DEFAULT_DIR ;
    private final String DEFAULT_PASSWD = "DEFAULT_PASSWD";
    final String AssetDir ="/asset";

    ArrayList<String> items = null;
    ArrayAdapter adapter  = null;

    ArrayList<String> items_recv = null;
    ArrayAdapter adapter_recv  = null;

    List<Credentials> mCredentials = new ArrayList<>();
    List<Credentials> mCredentials_recv = new ArrayList<>();
    int mSelectedCredential = -1;
    int mSelectedCredential_recv = -1;

    private boolean mPermission_granded = false;
    private final int PERMISSION_CODE = 111;
    View mView ;

    String mHashLink = "";
    private Web3j web3j;
    private void test(){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ( requestCode == PERMISSION_CODE){
            for( int i=0 ; i< permissions.length ; i++){

                v(String.format(" %s's permission %s" , permissions[i] , grantResults[i] == PERMISSION_GRANTED ? "granted." : "denied."));
                if( grantResults[i] == PERMISSION_DENIED ){
                    mPermission_granded = false;
                    return;
                }
            }
        }
        mPermission_granded = true;

        if(!DEFAULT_DIR.exists()){
            DEFAULT_DIR.mkdir();
        }
        copyAssetFile();
        updateWalletList();  // update wallet list.
    }

    private final int RESOURCES[] ={
            R.id.btn_test,

            R.id.btn_createwallet,
            R.id.btn_connectNet,
            R.id.btn_transaction,
            R.id.btn_gotoLink

    };
    private View.OnClickListener mClickEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_test :
                    test();
                    break;
                case R.id.btn_createwallet:
                    createWallet(DEFAULT_PASSWD);
                    break;
                case R.id.btn_connectNet:
                    connectNet();
                    break;
                case R.id.btn_transaction:
                    transaction();
                    break;
                case R.id.btn_gotoLink:
                    goToLink();
                    break;
            }
        }
    };


    private ListView.OnItemClickListener mListEvent = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            if( adapterView.getId() == R.id.lv_wallets){
                appendHistory("Selected wallet's index : " + i );
                final String address = mCredentials.get(i).getAddress();
                final String balance = getAccountBalance(mCredentials.get(i));
                v(address);
                
                //v(mCredentials.get(i).getEcKeyPair().toString());
                mSelectedCredential = i;

                /*make receiver list.*/
                if( !items_recv .isEmpty()){
                    items_recv .clear();
                }

                for( int idx =0 ; idx < mCredentials.size(); idx ++){
                    if( idx == mSelectedCredential ){
                        continue;
                    }
                    Credentials _cred = mCredentials.get(idx);
                    mCredentials_recv.add(_cred);
                    items_recv.add(_cred.getAddress());
                }
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter_recv.notifyDataSetChanged();
                        ((TextView)findViewById(R.id.tv_adress)).setText(address);
                        ((TextView)findViewById(R.id.tv_balance)).setText(balance);
                        
                    }
                });
            }else if ( adapterView.getId() == R.id.lv_wallets_recv){
                mSelectedCredential_recv = i;
                final String address = mCredentials_recv.get(i).getAddress();
                final String balance = getAccountBalance(mCredentials_recv.get(i));
                v(address);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter_recv.notifyDataSetChanged();
                        ((TextView)findViewById(R.id.tv_adress2)).setText(address);
                        ((TextView)findViewById(R.id.tv_balance2)).setText(balance);
                    }
                });
            }

        }
    };
    private final void appendHistory(final String _log){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.et_log)).append("\n" + _log);
            }
        });
    }

    public String getAccountBalance(Credentials _cred ){

        try {
            if( web3j == null){
                Snackbar.make(mView, "Connect testnet first." , Snackbar.LENGTH_SHORT).show();
                return "Connect testnet first.";
            }
            EthGetBalance _balance = web3j.ethGetBalance(_cred.getAddress() , DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
            BigInteger wei = _balance.getBalance();
            v(" wei : " + wei.toString() );
            return wei.toString() + " wei";
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return "invalid balance";
    }

    public boolean transaction(){

        Snackbar.make(mView, "Start transaction.. Please wait..", Snackbar.LENGTH_SHORT).show();
        appendHistory("Start transaction. Please wait...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                v("send  1wei");
                try {
                    TransactionReceipt _transferReceipt = Transfer.sendFunds(
                            web3j, mCredentials.get(mSelectedCredential),
                            mCredentials_recv.get(mSelectedCredential_recv).getAddress(),
                            BigDecimal.ONE , Convert.Unit.WEI).send();

                    appendHistory("Done transaction, You can check detail log through DETAIL button.");
                    i("Transaction done ." + _transferReceipt.getBlockNumber() + " / " + _transferReceipt.getTransactionHash());
                    final String _transactionHash = _transferReceipt.getTransactionHash();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.tv_transactionhash)).setText("Transaction hash : " + _transactionHash );
                        }
                    });
                    mHashLink = "https://ropsten.etherscan.io/tx/"+ _transactionHash;
                } catch (Exception e) {
                    e(e.getMessage());
                    e.printStackTrace();
                    appendHistory("Unknown error Occurred. please connect administrator.");
                }
            }
        }).start();

        return true;
    }
    public boolean goToLink(){
        if( mHashLink == null || mHashLink.equals("")){
            appendHistory("Have to transaction first.");
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse(mHashLink));
        startActivity(intent);
        return true;
    }
    public boolean connectNet(){
        final String _testnetURL  = "https://ropsten.infura.io/v3/aed4570c599e4438a46a982092eee546";
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*if( mSelectedCredential == -1 || mCredentials == null ){
                    return ;
                }*/
                try {
                    web3j = Web3jFactory.build(new HttpService(_testnetURL));
                    v("connected client version : " + web3j.web3ClientVersion().send().getWeb3ClientVersion());
                    appendHistory("Connected client : "+_testnetURL );
                    appendHistory("client version : " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

    public boolean updateWalletList(){
        if( !mPermission_granded ){
            e("permission denied.");
            return false;
        }

        if( !items.isEmpty()) {
            items.clear();
        }
        ArrayList<File> fList = new ArrayList<File>();
        File []_files_asset = new File (DEFAULT_DIR.getAbsolutePath()+AssetDir).listFiles();

        for( int i =0 ; i< _files_asset.length; i++){
            if(_files_asset[i].isDirectory())
                continue;
            fList.add(_files_asset[i]);
        }
        File []_files_download = DEFAULT_DIR.listFiles();
        for( int i =0 ; i< _files_download.length; i++){
            if(_files_download[i].isDirectory())
                continue;
            fList.add(_files_download[i]);
        }
        File[] _files = new File[fList.size()] ;
        _files = fList.toArray(_files);

        if( _files!= null ){
            ((TextView)findViewById(R.id.tv_walletList)).setText("Wallet List [SENDER] (Selectable count : "+ _files.length+")");

            if( !mCredentials.isEmpty()){
                mCredentials.clear();
            }
            for( int i=0 ; i< _files.length; i++){
                try {
                    i("wallet["+i+"] : " + _files[i].getAbsolutePath());
                    Credentials _cred = WalletUtils.loadCredentials(DEFAULT_PASSWD , _files[i].getAbsolutePath());
                    mCredentials.add(_cred);
                    items.add(/*f.getName() + "\n" + */_cred.getAddress());
                    i("Account : "+ _cred.getAddress());
                } catch (IOException | CipherException e) {
                    e.printStackTrace();
                    e(e.getMessage());
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
        return true;
    }


    public boolean createWallet (final String passwd){
        v("input passwd : " + passwd);
        View _v = findViewById(R.id.btn_test);
        if( DEFAULT_DIR != null){
            try {
                String fName = WalletUtils.generateLightNewWalletFile(passwd , DEFAULT_DIR);
                i("create file name : " + fName);
                Credentials _cred = WalletUtils.loadCredentials(passwd , DEFAULT_DIR + "/" + fName);
                String _addr =_cred.getAddress();
                i("wallet addres :" + _addr);
                Snackbar.make(_v ,"new Wallet created - \n" + _addr , Snackbar.LENGTH_SHORT ).show();
                appendHistory("new Wallet created - \n" + _addr);
            } catch (NoSuchAlgorithmException |NoSuchProviderException |InvalidAlgorithmParameterException |CipherException |IOException e) {
                e.printStackTrace();
                e(e.getMessage());
            }
        }
        updateWalletList();
        return true;
    }



    public void copyAssetFile(){
        File assetDir = new File(DEFAULT_DIR.getAbsolutePath()+AssetDir);
        if(!assetDir.exists()){
            assetDir.mkdir();
        }
        byte[] tmpbuf = new byte[1024];
        String[] assetList = {
                "UTC--2018-09-23T18-58-46.739--e399d4d877bbedaa5f1a9e2f4fe8106c45972976.json",
                "UTC--2018-09-23T19-35-03.873--1b44c0d3bb828f574ca36fddaf939b1d5f00507a.json",
                "UTC--2018-09-23T20-57-20.744--444d59442d6dad320926cdad738e28a805ce0585.json"
        };
        AssetManager manager = getResources().getAssets();

        for( String _str : assetList) {
            File f = new File(assetDir.getAbsolutePath() + '/'+ _str);
            if (!f.exists()) {
                try {
                    InputStream is = manager.open(_str);
                    FileOutputStream fos = new FileOutputStream(f);
                    while (is.read(tmpbuf) > 0) {
                        fos.write(tmpbuf);
                    }
                    fos.close();
                    is.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    public void preparing(){
        DEFAULT_DIR = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getAbsolutePath()+ '/' + "wallets");
        items = new ArrayList<String>();
        adapter  =new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice ,items);
        ((ListView)findViewById(R.id.lv_wallets)).setAdapter(adapter);

        items_recv = new ArrayList<String>();
        adapter_recv  =new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice ,items_recv);
        ((ListView)findViewById(R.id.lv_wallets_recv)).setAdapter(adapter_recv);
        items_recv.add("Please select SENDER first.");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mView = findViewById(R.id.btn_createwallet);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ((ListView)findViewById(R.id.lv_wallets)).setOnItemClickListener(mListEvent);
        ((ListView)findViewById(R.id.lv_wallets_recv)).setOnItemClickListener(mListEvent);
        for ( int _res : RESOURCES){
            findViewById(_res).setOnClickListener(mClickEvent);
        }// register event.

        // default setting
        preparing();


        int permissionCheck_write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck_read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck_internet = ContextCompat.checkSelfPermission(this, INTERNET);
        if(permissionCheck_write != PERMISSION_GRANTED || permissionCheck_read != PERMISSION_GRANTED || permissionCheck_internet != PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this ,  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.READ_EXTERNAL_STORAGE , Manifest.permission.INTERNET} , PERMISSION_CODE);
        }else{
            mPermission_granded = true;
        }

    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWalletList();  // update wallet list.
            }
        }).start();

        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
