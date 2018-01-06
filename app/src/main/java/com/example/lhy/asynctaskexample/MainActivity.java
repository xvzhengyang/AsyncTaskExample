package com.example.lhy.asynctaskexample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * AsyncTask的使用的例子--另外加上了几个
 */
public class MainActivity extends AppCompatActivity {

    TextView textView;
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.e("handler", "handleMessage: "+msg.what);
            return true;
        }
    }){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e("非callback", "handleMessage: "+msg.what);

        }
    };
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e("mHandler", "handleMessage: "+msg.what);

        }
    };
    private DownloadTask downloadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.testAsyncTask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //要下载的文件地址
                String[] urls = {
                        "http://blog.csdn.net/xvzhengyang/article/details/78873025",
                        "http://blog.csdn.net/xvzhengyang/article/details/78666920",
                        "http://blog.csdn.net/xvzhengyang/article/details/78501200",
                        "http://blog.csdn.net/xvzhengyang/article/details/78490236",
                        "http://blog.csdn.net/xvzhengyang/article/details/78488035"
                };

                downloadTask = new DownloadTask();
                // 线程开启使用execute方法；另外一个Task实例只能调用一次execute方法，执行第二次就会出现异常
                downloadTask.execute(urls);
              /*  handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        downloadTask.cancel(true);
                    }
                }, 200);*/
            }
        });
        textView = findViewById(R.id.textView);
        handler.post(new Runnable() {
            @Override
            public void run() {

                Log.e("mHandler", "handleMessage: "+"message的callback");

            }
        });

        // 判断下子线程中new一个handler对象发送消息，是子线程还是主线程来处理。
     new Thread(){
         @Override
         public void run() {
             while (true){
                 Message message = Message.obtain();
                 message.what = 1;
                // mHandler.sendMessage(message);
                 try {
                     sleep(5000);
                     handler.sendMessage(message);
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }

             }
         }
     }.start();

    }

    // Params : 参数类型 Progress为后台任务执行进度的类型 Result为返回结果的类型
    private class DownloadTask extends AsyncTask<String, Object, Long> {

        /**
         * 主线程--预先操作
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textView.setText("开始下载...");
        }

        @Override
        protected Long doInBackground(String... params) {
            //totalByte表示所有下载的文件的总字节数
            long totalByte = 0;
            //params是一个String数组
            for (String url : params) {
                //遍历Url数组，依次下载对应的文件
                Object[] result = downloadSingleFile(url);
                int byteCount = (int) result[0];
                totalByte += byteCount;
                //在下载完一个文件之后，我们就把阶段性的处理结果发布出去---不调用这个方法那么就会导致onProgressupdate（）无法使用
                publishProgress(result);
                //如果AsyncTask被调用了cancel()方法，那么任务取消，跳出for循环
                if (isCancelled()) {
                    break;
                }
            }
            //将总共下载的字节数作为结果返回
            return totalByte;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int byteCount = (int) values[0];
            String blogName = (String) values[1];
            String text = textView.getText().toString();
            text += "\n博客《" + blogName + "》下载完成，共" + byteCount + "字节";
            textView.setText(text);
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            String text = textView.getText().toString();
            text += "\n全部下载完成，总共下载了" + aLong + "个字节";
            textView.setText(text);
        }


        @Override
        protected void onCancelled(Long aLong) {
            super.onCancelled(aLong);
            textView.setText("取消下载");
        }

        //下载文件后返回一个Object数组：下载文件的字节数以及下载的博客的名字
        private Object[] downloadSingleFile(String str) {
            Object[] result = new Object[2];
            int byteCount = 0;
            String blogName = "";
            HttpURLConnection conn = null;
            try {
                URL url = new URL(str);
                conn = (HttpURLConnection) url.openConnection();
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int length = -1;
                while ((length = is.read(buf)) != -1) {
                    baos.write(buf, 0, length);
                    byteCount += length;
                }
                String respone = new String(baos.toByteArray(), "utf-8");
                int startIndex = respone.indexOf("<title>");
                if (startIndex > 0) {
                    startIndex += 7;
                    int endIndex = respone.indexOf("</title>");
                    if (endIndex > startIndex) {
                        //解析出博客中的标题
                        blogName = respone.substring(startIndex, endIndex);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            result[0] = byteCount;
            result[1] = blogName;
            return result;
        }
    }
}
