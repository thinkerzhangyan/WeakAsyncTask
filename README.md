# WeakAsyncTask


AsyncTask在使用过程中可能会出现内存溢出的情况，下面就这种情况就行介绍

AsyncTask的用法很简单，那么我们看下面这段代码

```
private TextView mTv;

new AsyncTask<...> {

    @Override

    protected void onPostExecute(Objecto) {

        mTv.setText("text");

    }

}.execute();
```
乍一看好像没什么问题，但这段代码会导致内存泄露，线程有可能会超出当前Activity的生命周期之后仍然在run，因为这个时候线程已经不受控制了。Activity生命周期已经结束，需要被系统回收掉，但是AsyncTask还在持有TextView的引用，这样就导致了内存泄露。

修改代码
  
```
private TextView mTv;

new AsyncTask<...> {

    @Override

    protected void onPostExecute(Objecto) {

        //mTv.setText("text");

}

}.execute();
```
直接注释掉，不做UI操作了，虽然表面看不会存在问题了，但是实际上还存在着问题，因为这里AsyncTask是个内部类，由于Java内部类的特点，AsyncTask内部类会持有外部类的隐式引用。即使从代码上看我在AsyncTask里没有持有外部的任何引用，但是写在Activity里，对context仍然会有个强引用，这样如果线程超过Activity生命周期，Activity还是无法回收造成内存泄露。

那问题怎么解决呢，有两种办法：
> * 第一，在Activity生命周期结束前，去cancel AsyncTask，因为Activity都要销毁了，这个时候再跑线程，绘UI显然已经没什么意义了。不过关于AsyncTask的cancel的用法，存在误区，请看[AsyncTask的cancel方法解读][1]
> * 第二，如果一定要写成内部类的形式，写成静态内部类，同时对context采用WeakRefrence,在使用之前判断是否为空。


示例代码如下：
```
static abstract class WeakAsyncTask<Param, Progress, Result, WeakTarget> extends AsyncTask<Param, Progress, Result> {

        protected WeakReference<WeakTarget> mTarget;

        public WeakAsyncTaskB(WeakTarget weakTarget) {
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
    
 static class MyAsyncTask extends WeakAsyncTask<Void, Integer, String, AsyncTaskActivity> {
 
        public MyAsyncTask(AsyncTaskActivity asyncTaskActivity) {
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
                activity.mTextView.setText("post:" + values[0]);
        }

        @Override
        protected void onPostExecute(WeakReference<AsyncTaskActivity> weaktarget, String s) {
            AsyncTaskActivity activity = weaktarget.get();
            if (activity != null)
                activity.mTextView.setText("post:" + s);
        }

        @Override
        protected void onCancelled(WeakReference<AsyncTaskActivity> weaktarget, String s) {
            AsyncTaskActivity activity = weaktarget.get();
            if (activity != null)
                activity.mTextView.setText("cancel:" + s);
        }
    }
```

参考链接：
[内存泄露之Thread][2]
[Android性能优化之常见的内存泄漏][3]
