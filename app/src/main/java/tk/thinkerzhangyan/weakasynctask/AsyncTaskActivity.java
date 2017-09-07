package tk.thinkerzhangyan.weakasynctask;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;


public class AsyncTaskActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AsyncTaskActivity";

    private Button mButtonKaiShiA;
    private Button mButtonKaiShiB;
    private Button mButtonKaiShiC;
    private Button mButtonStop;
    private Button mButtonExit;
    private TextView mTextView;

    private MyAsyncTaskA mMyAsyncTaskA;

    private MyAsyncTaskB mMyAsyncTaskB;

    private MyAsyncTaskC mMyAsyncTaskC;

    enum Which{
        ASYNCTASKA, ASYNCTASKB, ASYNCTASKC,
    }

    Which mWhich;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_async_task);


        findViews();

        setOnClicks();

        initAsyncTasks();

    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.buttonKaishiA:
                mWhich = Which.ASYNCTASKA;
                mMyAsyncTaskA.execute();
                disableButton();
                break;
            case R.id.buttonKaishiB:
                mWhich = Which.ASYNCTASKB;
                mMyAsyncTaskB.execute();
                disableButton();
                break;
            case R.id.buttonKaishiC:
                mWhich = Which.ASYNCTASKC;
                mMyAsyncTaskC.execute();
                disableButton();
                break;
            case R.id.buttonStop:
                stopAsyncTask(mWhich);
                enableButton();
                break;
            case R.id.buttonExit:
                finish();
                break;
            default:
                break;
        }

    }

    private void findViews(){
        mButtonKaiShiA = (Button) findViewById(R.id.buttonKaishiA);
        mButtonKaiShiB = (Button) findViewById(R.id.buttonKaishiB);
        mButtonKaiShiC = (Button) findViewById(R.id.buttonKaishiC);

        mButtonStop = (Button) findViewById(R.id.buttonStop);

        mButtonExit = (Button) findViewById(R.id.buttonExit);

        mTextView = (TextView) findViewById(R.id.textView);
    }

    private void setOnClicks(){
        mButtonKaiShiA.setOnClickListener(this);
        mButtonKaiShiB.setOnClickListener(this);
        mButtonKaiShiC.setOnClickListener(this);
        mButtonExit.setOnClickListener(this);

        mButtonStop.setOnClickListener(this);
    }

    private void initAsyncTasks(){

        mMyAsyncTaskA = new MyAsyncTaskA();
        mMyAsyncTaskB = new MyAsyncTaskB(this);
        mMyAsyncTaskC = new MyAsyncTaskC(this);

    }

    private void disableButton(){
        mButtonKaiShiA.setEnabled(false);
        mButtonKaiShiB.setEnabled(false);
        mButtonKaiShiC.setEnabled(false);
        mButtonStop.setEnabled(true);
        mButtonExit.setEnabled(true);
    }

    private void enableButton(){
        mButtonKaiShiA.setEnabled(true);
        mButtonKaiShiB.setEnabled(true);
        mButtonKaiShiC.setEnabled(true);
        mButtonStop.setEnabled(false);
        mButtonExit.setEnabled(false);
    }


    private void stopAsyncTask(Which which){
        if(which==Which.ASYNCTASKA){
            mMyAsyncTaskA.cancel(true);
            mMyAsyncTaskA = new MyAsyncTaskA();
        }
        else if(which==Which.ASYNCTASKB){
            mMyAsyncTaskB.cancel(true);
            mMyAsyncTaskB = new MyAsyncTaskB(this);
        }
        else if(which==Which.ASYNCTASKC){
            mMyAsyncTaskC.cancel(true);
            mMyAsyncTaskC = new MyAsyncTaskC(this);
        }
    }



    /****************************会引起内存泄漏的AsyncTask********************************************/
    class MyAsyncTaskA extends AsyncTask<Void,Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            int i=0;
            try {
                while (true) {
                    if (Thread.currentThread().isInterrupted())
                        break;
                    Thread.currentThread().sleep(1000);
                    publishProgress(++i);
                    Log.d(TAG,"A-i:"+i);
                }
            } catch (Exception e) {
                return "异常";
            }
            return "被终止了";
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
            // 在这里更新下载进度
            mTextView.setText("postA:"+params[0]);
        }

        @Override
        protected void onCancelled(String result) {
            super.onCancelled(result);
            mTextView.setText("onCancelledA:"+result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(String result) {
            mTextView.setText("onPostExecuteA:"+result);
        }

    }

    /****************************会引起内存泄漏的AsyncTask********************************************/
    static class MyAsyncTaskB extends WeakAsyncTaskB<Void,Integer,String,AsyncTaskActivity>{

        public MyAsyncTaskB(AsyncTaskActivity context) {
            super(context);
        }

        @Override
        protected void onPreExecute(AsyncTaskActivity weaktarget) {
        }

        //doInBackground 方法一直持有对Activity的引用，导致内存泄露。
        @Override
        protected String doInBackground(AsyncTaskActivity weaktarget, Void... voids) {
            int i=0;
            try {
                while (true) {
                    if (Thread.currentThread().isInterrupted())
                        break;
                    Thread.currentThread().sleep(1000);
                    publishProgress(++i);
                    Log.d(TAG,"B-i:"+i);
                }
            } catch (Exception e) {
                return "异常";
            }

            return "被终止了";
        }

        @Override
        protected void onProgressUpdate(AsyncTaskActivity weaktarget, Integer... values) {
            weaktarget.mTextView.setText("postB:"+values[0]);
        }

        @Override
        protected void onPostExecute(AsyncTaskActivity weaktarget, String s) {
            weaktarget.mTextView.setText("onPostExecuteB:"+s);
        }

        @Override
        protected void onCancelled(AsyncTaskActivity weaktarget, String s) {
            weaktarget.mTextView.setText("onCancelledB:"+s);
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

    }

    static abstract class WeakAsyncTaskB<Param,Progress,Result,WeakTarget> extends AsyncTask<Param,Progress,Result> {

        protected WeakReference<WeakTarget> mTarget;

        public WeakAsyncTaskB(WeakTarget weakTarget) {
            mTarget = new WeakReference<>(weakTarget);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final WeakTarget target = mTarget.get();
            if(target!=null){
                onPreExecute(target);
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            final WeakTarget target = mTarget.get();
            if(target !=null){
                onPostExecute(target ,result);
            }
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            super.onProgressUpdate(values);
            final WeakTarget target = mTarget.get();
            if (target !=null){
                onProgressUpdate(target ,values);
            }
        }

        @Override
        protected Result doInBackground(Param... params) {
            Log.d(TAG, "doInBackground: "+params.getClass());
            //   final WeakTarget target =null;
            // return doInBackground(target ,params);
            //当doInBackground(target ,params)存在无限循环的时候，doInBackground(target ,params)方法会一直持有对Activity的引用，这会导致泄漏。
            final WeakTarget target = mTarget.get();
            if (target !=null){
                return doInBackground(target ,params);
            }else {
                return null;
            }
        }

        @Override
        protected void onCancelled(Result result) {
            super.onCancelled(result);
            final WeakTarget target = mTarget.get();
            if (target !=null){
                onCancelled(target ,result);
            }
        }

        protected abstract void onPreExecute(WeakTarget weaktarget);
        protected abstract Result doInBackground(WeakTarget weaktarget,Param... params);
        protected abstract void onProgressUpdate(WeakTarget weaktarget,Progress... values) ;
        protected abstract void  onPostExecute(WeakTarget weaktarget,Result result);
        protected abstract void  onCancelled(WeakTarget weaktarget,Result result);

    }

    /****************************不会引起内存泄漏的AsyncTask********************************************/
    static class MyAsyncTaskC extends WeakAsyncTaskC<Void, Integer, String, AsyncTaskActivity> {


        public MyAsyncTaskC(AsyncTaskActivity asyncTaskActivity) {
            super(asyncTaskActivity);
        }

        @Override
        protected void onPreExecute(WeakReference<AsyncTaskActivity> weaktarget) {

        }

        @Override
        protected String doInBackground(WeakReference<AsyncTaskActivity> weaktarget, Void... voids) {
            try {
                int i = 0;
                while (true) {
                    if (Thread.currentThread().isInterrupted())
                        break;
                    Thread.currentThread().sleep(1000);
                    publishProgress(++i);
                    Log.d(TAG,"C-i:"+i);
                }
            } catch (Exception e) {
                return "异常";
            }

            return "被终止了";
        }

        @Override
        protected void onProgressUpdate(WeakReference<AsyncTaskActivity> weaktarget, Integer... values) {
            AsyncTaskActivity activity = weaktarget.get();
            if (activity != null)
                activity.mTextView.setText("postC:" + values[0]);
        }

        @Override
        protected void onPostExecute(WeakReference<AsyncTaskActivity> weaktarget, String s) {
            AsyncTaskActivity activity = weaktarget.get();
            if (activity != null)
                activity.mTextView.setText("postC:" + s);
        }

        @Override
        protected void onCancelled(WeakReference<AsyncTaskActivity> weaktarget, String s) {
            AsyncTaskActivity activity = weaktarget.get();
            if (activity != null)
                activity.mTextView.setText("cancelC:" + s);
        }


    }

    static abstract class WeakAsyncTaskC<Param, Progress, Result, WeakTarget> extends AsyncTask<Param, Progress, Result> {

        protected WeakReference<WeakTarget> mTarget;

        public WeakAsyncTaskC(WeakTarget weakTarget) {
            mTarget = new WeakReference<>(weakTarget);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            onPreExecute(mTarget);

        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);

            onPostExecute(mTarget, result);

        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            super.onProgressUpdate(values);

            onProgressUpdate(mTarget, values);

        }

        @Override
        protected Result doInBackground(Param... params) {
            Log.d(TAG, "doInBackground: " + params.getClass());

            return doInBackground(mTarget, params);

        }

        @Override
        protected void onCancelled(Result result) {
            super.onCancelled(result);

            onCancelled(mTarget, result);

        }

        protected abstract void onPreExecute(WeakReference<WeakTarget> weaktarget);

        protected abstract Result doInBackground(WeakReference<WeakTarget> weaktarget, Param... params);

        protected abstract void onProgressUpdate(WeakReference<WeakTarget> weaktarget, Progress... values);

        protected abstract void onPostExecute(WeakReference<WeakTarget> weaktarget, Result result);

        protected abstract void onCancelled(WeakReference<WeakTarget> weaktarget, Result result);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
       // stopAsyncTask(mWhich);
    }
}
