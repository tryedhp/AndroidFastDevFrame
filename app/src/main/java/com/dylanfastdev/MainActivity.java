package com.dylanfastdev;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.orhanobut.logger.Logger;

import org.xutils.http.RequestParams;
import org.xutils.image.ImageOptions;
import org.xutils.x;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.bluemobi.dylan.fastdev.base.BasePhotoActivity;
import cn.bluemobi.dylan.fastdev.config.Config;
import cn.bluemobi.dylan.fastdev.utils.CommonAdapter;
import cn.bluemobi.dylan.fastdev.utils.CommonViewHolder;
import cn.bluemobi.dylan.fastdev.view.CircleImageView;
import cn.bluemobi.dylan.fastdev.view.CycleViewPager;
import cn.bluemobi.dylan.fastdev.view.RatingBar;

public class MainActivity extends BasePhotoActivity {
    private WebView webView;
    private CycleViewPager cycle_view_pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMyRatingBar();
        showCircleImage();
        showCycleViewPager();

    }

    private void showCycleViewPager() {
        cycle_view_pager = (CycleViewPager) findViewById(R.id.cv);
        //设置选中和未选中时的图片
        assert cycle_view_pager != null;
        cycle_view_pager.setIndicators(R.mipmap.dot_focus, R.mipmap.dot_normal);
        List<String> urls = new ArrayList<>();
        urls.add("http://pic.58pic.com/58pic/15/37/32/458PICq58PICPd4_1024.jpg");
        urls.add("http://pic8.nipic.com/20100627/1614097_004848992811_2.jpg");
        urls.add("http://pic.58pic.com/58pic/15/35/53/14t58PICUJv_1024.jpg");
        cycle_view_pager.setData(urls, new CycleViewPager.ImageCycleViewListener() {
            @Override
            public void onImageClick(String url, int position, View imageView) {

            }
        });
    }

    private void showCircleImage() {
        String url = "http://img.blog.csdn.net/20161016171244996";
        CircleImageView circleImageView = (CircleImageView) findViewById(R.id.ci);
        x.image().bind(circleImageView, url, new ImageOptions.Builder().setImageScaleType(ImageView.ScaleType.CENTER_CROP).build());
    }

    private void setMyRatingBar() {
        setContentView(R.layout.ac_ratingbar);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rb);
        ratingBar.setClickable(true);//设置可否点击
        ratingBar.setStar(2.5f);//设置显示的星星个数
        ratingBar.setStepSize(RatingBar.StepSize.Half);//设置每次点击增加一颗星还是半颗星
        ratingBar.setOnRatingChangeListener(new RatingBar.OnRatingChangeListener() {
            @Override
            public void onRatingChange(float ratingCount) {//点击星星变化后选中的个数
                Log.d("RatingBar", "RatingBar-Count=" + ratingCount);
            }
        });
    }

    /**
     * 普通适配器的方法
     */
    private void myAdapterTest() {
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.listview);
        List<String> datas = new ArrayList<>();
        datas.add("普通适配器测试1");
        datas.add("普通适配器测试2");
        datas.add("普通适配器测试3");
        datas.add("普通适配器测试4");
        listView.setAdapter(new MyAdapter(context, datas));
    }

    /**
     * 万能适配器的方法
     */
    private void commonAdapterTest() {
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.listview);
        List<String> datas = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            datas.add("万能适配器测试" + i);
        }
        listView.setAdapter(new CommonAdapter<String>(context, datas, R.layout.item) {

            @Override
            protected void convertView(View item, String s) {
                TextView textView = CommonViewHolder.get(item, R.id.textView);
                textView.setText(s);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showdialog();
            }
        });
    }

    @Override
    public void photoPath(String path) {
        super.photoPath(path);
        RequestParams requestParams = new RequestParams("http://10.58.178.120:8080/intco/mobi/member/uploadImage");
        requestParams.addBodyParameter("mId", "1");
        requestParams.addBodyParameter("imgFile", new File(path));
        ajax(requestParams);
    }

    /**
     * 网页测试
     */
    private void webViewTest() {
        webView = new WebView(this);
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
//        webView.getSettings().setDomStorageEnabled(true);
//
//
//        WebSettings ws = webView.getSettings();
//        /**
//         *
//         * setAllowFileAccess 启用或禁止WebView访问文件数据 setBlockNetworkImage 是否显示网络图像
//         * setBuiltInZoomControls 设置是否支持缩放 setCacheMode 设置缓冲的模式
//         * setDefaultFontSize 设置默认的字体大小 setDefaultTextEncodingName 设置在解码时使用的默认编码
//         * setFixedFontFamily 设置固定使用的字体 setJavaSciptEnabled 设置是否支持Javascript
//         * setLayoutAlgorithm 设置布局方式 setLightTouchEnabled 设置用鼠标激活被选项
//         * setSupportZoom 设置是否支持变焦
//         * */
//        ws.setBuiltInZoomControls(true);// 隐藏缩放按钮
//        ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);// 排版适应屏幕
//        ws.setUseWideViewPort(true);// 可任意比例缩放
//        ws.setLoadWithOverviewMode(true);// setUseWideViewPort方法设置webview推荐使用的窗口。setLoadWithOverviewMode方法是设置webview加载的页面的模式。
//        ws.setSavePassword(true);
//        ws.setSaveFormData(true);// 保存表单数据
//        ws.setJavaScriptEnabled(true);
//        ws.setGeolocationEnabled(true);// 启用地理定位
//        ws.setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");// 设置定位的数据库路径
//        ws.setDomStorageEnabled(true);
//        ws.setPluginState(WebSettings.PluginState.ON); //设置webview支持插件
//        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
//        webView.loadUrl("http://www.wzjgswj.gov.cn/wap/NewCont.aspx?ColumnID=34&ID=4107");
//        loadUrl(webView,"http://116.236.217.38:7679/V4.0_WebApp/Area/WebApp/Hospital/OnlineConsultation/Main.html?deviceType=3&machineCode=5c101e60-ca96-424e-8328-b9fa8b3daab8&womanId=42493&requestStartTime=636116568969480000&os=Android5.1&network=WIFI");
//        loadUrl(webView, "http://54.222.212.189/v_graph/v_graph.html?from_portal=1&_v=1462845840614");
    }

    @Override
    protected void onResume() {
        super.onResume();
//        try {
//            webView.getClass().getMethod("onResume").invoke(webView, (Object[]) null);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        try {
//            webView.getClass().getMethod("onPause").invoke(webView, (Object[]) null);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void initTitleBar() {
    }

    @Override
    public void initViews() {

//        setContentView(R.layout.activity_main);

    }

    @Override
    public void initData() {
        RequestParams requestParams = new RequestParams("http://10.58.187.40:8080/WebDemo/login?userName=zhangsan&passWord=45678");
        ajax(requestParams);
    }

    @Override
    public void netOnSuccess(Map<String, Object> data) {
        super.netOnSuccess(data);
    }

    @Override
    public void addListener() {

    }

}