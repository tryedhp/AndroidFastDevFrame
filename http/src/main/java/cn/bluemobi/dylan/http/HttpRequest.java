package cn.bluemobi.dylan.http;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map;

import cn.bluemobi.dylan.http.dialog.LoadingDialog;
import cn.bluemobi.dylan.http.download.ProgressResponseBody;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * http请求解析类
 * Created by yuandl on 2017-03-31.
 */

public class HttpRequest {

    /**
     * 要显示的对话框的上下文
     */
    private WeakReference<Context> context;

    public HttpRequest setShowLoadingDialog(boolean showLoadingDialog) {
        isShowLoadingDialog = showLoadingDialog;
        return this;
    }

    /**
     * 默认显示加载进度对话框
     */
    private boolean isShowLoadingDialog = true;

    /**
     * 接口监听者
     */
    private Observable<? extends Response<ResponseBody>> observable;
    private LoadingDialog loadingDialog = null;


    /**
     * 【第一步】设置上下文
     *
     * @param context 上下文
     * @return 本类对象
     */
    public HttpRequest(Context context) {
        this.context = new WeakReference<Context>(context);
    }

    /**
     * 【第二步】设置隐藏加载对话框（可忽略此步骤，默认显示）
     *
     * @return 本类对象
     */
    public HttpRequest hideLoadingDialog() {
        isShowLoadingDialog = false;
        return this;
    }

    /**
     * 【第三步】设置访问的几口接口
     *
     * @param observable 接口对象
     * @return 本类对象
     */
    public HttpRequest setObservable(Observable<? extends Response<ResponseBody>> observable) {
        this.observable = observable;
        return this;
    }

    private boolean isShowSuccessMessage = true;

    public HttpRequest hideSuccessMessage() {
        isShowSuccessMessage = false;
        return this;
    }


    private boolean isShowOtherStatusMessage = true;

    public HttpRequest hideOtherStatusMessage() {
        isShowOtherStatusMessage = false;
        return this;
    }

    private boolean isShowFailMessage = true;

    public HttpRequest hideFailMessage() {
        isShowFailMessage = false;
        return this;
    }

    private boolean canCancel = true;

    public HttpRequest setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
        return this;
    }

    private ResponseInterceptor responseInterceptor;

    public HttpRequest setResponseInterceptor(ResponseInterceptor responseInterceptor) {
        this.responseInterceptor = responseInterceptor;
        return this;
    }

    /**
     * 【第四步】设置访问接口的返回监听
     *
     * @param httpResponse 请求相应监听
     * @return 本类对象
     */
    public Subscription setDataListener(final HttpCallBack httpResponse) {
        String network_unusual = MessageManager.getMessageManager().isUseEnglishLanguage() ? "Network  unusual" : "网络不可用";
        final String network_error = MessageManager.getMessageManager().isUseEnglishLanguage() ? "Network  error" : MessageManager.getMessageManager().getErrorMessage();

        if (!NetworkUtil.isNetworkAvailable(context.get())) {
            Toast.makeText(context.get(), network_unusual, Toast.LENGTH_SHORT).show();
            if (httpResponse != null) {
                httpResponse.netOnFailure(new Exception(network_unusual));
                httpResponse.netOnFinish();
            }
            return null;
        }

        if (isShowLoadingDialog) {
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(context.get());
            }
        } else {
            loadingDialog = null;
        }
        Subscription subscribe = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<ResponseBody>>() {

                    @Override
                    public void onStart() {
                        if (loadingDialog != null) {
                            loadingDialog.show();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnStart();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnFinish();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (Http.getHttp().isDebugMode()) {
                            e.printStackTrace();
                        }
                        if (isShowFailMessage) {
                            Toast.makeText(context.get(), network_error, Toast.LENGTH_SHORT).show();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnFailure(e);
                        }
                        onCompleted();

                    }

                    @Override
                    public void onNext(Response<ResponseBody> responseBodyResponse) {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        String responseString = "";
                        boolean isSuccessful = responseBodyResponse.isSuccessful();
                        try {
                            if (!isSuccessful) {
                                responseString = responseBodyResponse.errorBody().string();
                                if (responseInterceptor != null) {
                                    Map<String, Object> requestParameter = getRequestParement(responseBodyResponse.raw().request());
                                    boolean isInterceptor = responseInterceptor.onResponseStart(context.get(), responseBodyResponse.raw().request().url().url().toString(), requestParameter, responseString, responseBodyResponse.raw().code());
                                    if (isInterceptor) {
                                        return;
                                    }
                                }
                            } else {

                                responseString = responseBodyResponse.body().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//
//                        if (result instanceof ProgressResponseBody) {
//
//                        }
                        ArrayMap<String, Object> jsonBean;
                        try {
                            jsonBean = JsonParse.getJsonParse().jsonParse(responseString);
                            String msg = JsonParse.getString(jsonBean, JsonParse.getJsonParse().getMsg());
                            int code = Integer.parseInt(JsonParse.getString(jsonBean, JsonParse.getJsonParse().getCode()));
                            Map<String, Object> data = (Map<String, Object>) jsonBean.get(JsonParse.getJsonParse().getData());
                            if (responseInterceptor != null) {
                                boolean isInterceptor = responseInterceptor.onResponse(context.get(), code, msg, data, responseBodyResponse.raw().request().url().url().toString());
                                if (isInterceptor) {
                                    return;
                                }
                            }

                            if (code == JsonParse.getJsonParse().getSuccessCode()) {
                                if (MessageManager.getMessageManager().getShowMessageModel() == MessageManager.MessageModel.All && isShowSuccessMessage) {
                                    if (msg != null && !msg.isEmpty() && !"null".equalsIgnoreCase(msg)) {
                                        Toast.makeText(context.get(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if (httpResponse != null) {
                                    httpResponse.netOnSuccess(data);
                                    httpResponse.netOnSuccess(data, msg);
                                }
                            } else {
                                if (MessageManager.getMessageManager().getShowMessageModel() != MessageManager.MessageModel.NO && isShowOtherStatusMessage) {
                                    if (msg != null && !msg.isEmpty() && !"null".equalsIgnoreCase(msg)) {
                                        Toast.makeText(context.get(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if (httpResponse != null) {
                                    httpResponse.netOnOtherStatus(code, msg);
                                    httpResponse.netOnOtherStatus(code, msg, data);
                                }
                            }
                        } catch (Exception e) {
                            if (MessageManager.getMessageManager().getShowMessageModel() != MessageManager.MessageModel.NO && isShowFailMessage) {
                                Toast.makeText(context.get(), network_error, Toast.LENGTH_SHORT).show();
                            }
                            if (httpResponse != null) {
                                httpResponse.netOnFailure(e);
                            }
                            if (Http.getHttp().isDebugMode()) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        if (loadingDialog != null && subscribe != null) {
            loadingDialog.setOnKeyListener(new DialogOnKeyListener(loadingDialog, subscribe, canCancel));
        }
        return subscribe;
    }
    /**
     * 【第四步】设置访问接口的返回监听
     *
     * @param httpResponse 请求相应监听
     * @return 本类对象
     */
    public Subscription setDataListener(final OriginalHttpResponse httpResponse) {
        String network_unusual = MessageManager.getMessageManager().isUseEnglishLanguage() ? "Network  unusual" : "网络不可用";
        final String network_error = MessageManager.getMessageManager().isUseEnglishLanguage() ? "Network  error" : MessageManager.getMessageManager().getErrorMessage();

        if (!NetworkUtil.isNetworkAvailable(context.get())) {
            Toast.makeText(context.get(), network_unusual, Toast.LENGTH_SHORT).show();
            if (httpResponse != null) {
                httpResponse.netOnFailure(new Exception(network_unusual));
                httpResponse.netOnFinish();
            }
            return null;
        }

        if (isShowLoadingDialog) {
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(context.get());
            }
        } else {
            loadingDialog = null;
        }
        Subscription subscribe = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<ResponseBody>>() {

                    @Override
                    public void onStart() {
                        if (loadingDialog != null) {
                            loadingDialog.show();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnStart();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnFinish();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (Http.getHttp().isDebugMode()) {
                            e.printStackTrace();
                        }
                        if (isShowFailMessage) {
                            Toast.makeText(context.get(), network_error, Toast.LENGTH_SHORT).show();
                        }
                        if (httpResponse != null) {
                            httpResponse.netOnFailure(e);
                        }
                        onCompleted();

                    }

                    @Override
                    public void onNext(Response<ResponseBody> responseBodyResponse) {
                        if (loadingDialog != null) {
                            loadingDialog.dismiss();
                        }
                        String responseString = "";
                        boolean isSuccessful = responseBodyResponse.isSuccessful();
                        try {
                            if (!isSuccessful) {
                                responseString = responseBodyResponse.errorBody().string();
                                if (responseInterceptor != null) {
                                    Map<String, Object> requestParameter = getRequestParement(responseBodyResponse.raw().request());
                                    boolean isInterceptor = responseInterceptor.onResponseStart(context.get(), responseBodyResponse.raw().request().url().url().toString(), requestParameter, responseString, responseBodyResponse.raw().code());
                                    if (isInterceptor) {
                                        return;
                                    }
                                }
                            } else {

                                responseString = responseBodyResponse.body().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (httpResponse != null) {
                                httpResponse.netOnSuccess(responseString);
                            }
                        } catch (Exception e) {
                            if (MessageManager.getMessageManager().getShowMessageModel() != MessageManager.MessageModel.NO && isShowFailMessage) {
                                Toast.makeText(context.get(), network_error, Toast.LENGTH_SHORT).show();
                            }
                            if (httpResponse != null) {
                                httpResponse.netOnFailure(e);
                            }
                            if (Http.getHttp().isDebugMode()) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        if (loadingDialog != null && subscribe != null) {
            loadingDialog.setOnKeyListener(new DialogOnKeyListener(loadingDialog, subscribe, canCancel));
        }
        return subscribe;
    }


    private Map<String, Object> getRequestParement(Request original) {
        Map<String, Object> map = new ArrayMap<>();
        //请求体定制：统一添加sign参数
        if (original.body() instanceof FormBody) {
            FormBody oidFormBody = (FormBody) original.body();
            for (int i = 0; i < oidFormBody.size(); i++) {
                String name = oidFormBody.name(i);
                String value = oidFormBody.value(i);
                map.put(name, value);
            }
        } else if (original.body() instanceof MultipartBody) {
            MultipartBody multipartBody = (MultipartBody) original.body();
            for (MultipartBody.Part part : multipartBody.parts()) {
                String name = getPartHeaders(part);
                String value = null;
                try {
                    value = URLDecoder.decode(getRequestBody(part).replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                map.put(name, value);
            }
        }
        return map;
    }

    private String getPartHeaders(MultipartBody.Part part) {
        Class<?> personType = part.getClass();

        //访问私有属性
        Field field = null;
        try {
            field = personType.getDeclaredField("headers");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        field.setAccessible(true);
        try {
            Headers headers = (Headers) field.get(part);
            String s = headers.get("content-disposition");
            return s.split(";")[1].split("=")[1].replace("\"", "");

        } catch (IllegalAccessException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();

        }
        return "";
    }

    private String getRequestBody(MultipartBody.Part part) {

        Class<?> personType = part.getClass();

        //访问私有属性
        Field field = null;
        try {
            field = personType.getDeclaredField("body");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        field.setAccessible(true);
        try {
            RequestBody requestBody = (RequestBody) field.get(part);
            MediaType contentType = requestBody.contentType();
            if (contentType.type().equals("multipart")) {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                Charset UTF8 = Charset.forName("UTF-8");
                Charset charset = contentType.charset(UTF8);
                return buffer.readString(charset);
            } else if (contentType.type().equals("image")) {
                return convertFileSize(requestBody.contentLength());
//                Class<?> requestBodyClass = requestBody.getClass();
//
//                //访问私有属性
//                Field fileRequestBodyClass = null;
//                try {
//                    fileRequestBodyClass = requestBodyClass.getDeclaredField("file");
//
//
//                    fileRequestBodyClass.setAccessible(true);
//                    File file = (File) fileRequestBodyClass.get(requestBody);
//                    Logger.d("file=" + file.getPath());
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }


        } catch (IllegalAccessException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else {
            return String.format("%d B", size);
        }
    }

}
