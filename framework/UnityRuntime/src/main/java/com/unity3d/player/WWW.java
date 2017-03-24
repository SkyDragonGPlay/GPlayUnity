package com.unity3d.player;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class WWW extends Thread {
    private int mRedirectCount;
    private int mRequestCode;
    private String mRequestUrl;
    private byte[] mSendData;
    private Map<String,String> mRequestProperties;

    private static final String KEY_URL_FILE_APK = "!/";

    private static boolean isGPlayRuntime = false;
    private static String sResourceRootPath;

    public WWW(int requestCode, String url, byte[] data, Map<String,String> map) {
        this.mRequestCode = requestCode;
        this.mRequestUrl = preprocessUrl(url);
        this.mSendData = data;
        this.mRequestProperties = map;
        this.mRedirectCount = 0;
        this.start();
    }

    String preprocessUrl(String requestUrl) {
        if(TextUtils.isEmpty(requestUrl)) return "";
        if(!isGPlayRuntime) return requestUrl;
        URL url;
        try {
            url = new URL(requestUrl);
            if (url.getProtocol().equalsIgnoreCase("file") || url.getProtocol().equalsIgnoreCase("jar")) {
                int index = requestUrl.indexOf(KEY_URL_FILE_APK);
                if(index != -1) {
                    String resFile =  sResourceRootPath + requestUrl.substring(index + KEY_URL_FILE_APK.length() );
                    File f = new File(resFile);
                    if(f.exists()) {
                        return "file://" + sResourceRootPath + requestUrl.substring(index + KEY_URL_FILE_APK.length());
                    }
                }
            }
            return requestUrl;
        }
        catch (MalformedURLException var3_3) {
            WWW.errorCallback(this.mRequestCode, var3_3.toString());
            return requestUrl;
        }
    }

    public static void setResourceRootPath(String rootPath) {
        sResourceRootPath = rootPath;
    }

    public static void setIsGPlayRuntime(boolean b) {
        isGPlayRuntime = b;
    }

    @Override
    public void run() {
        System.out.println("zjf@ WWW running!!!");
        URLConnection urlConnection;
        URL url;
        if (++this.mRedirectCount > 5) {
            WWW.errorCallback(this.mRequestCode, "Too many redirects");
            return;
        }
        try {
            url = new URL(this.mRequestUrl);
            urlConnection = url.openConnection();
        }
        catch (MalformedURLException var3_3) {
            WWW.errorCallback(this.mRequestCode, var3_3.toString());
            return;
        }
        catch (IOException var3_4) {
            WWW.errorCallback(this.mRequestCode, var3_4.toString());
            return;
        }
        if (url.getProtocol().equalsIgnoreCase("file") && url.getHost() != null && url.getHost().length() != 0) {
            WWW.errorCallback(this.mRequestCode, url.getHost() + url.getFile() + " is not an absolute path!");
            return;
        }
        if (this.mRequestProperties != null) {
            for (Map.Entry entry : this.mRequestProperties.entrySet()) {
                urlConnection.addRequestProperty((String)entry.getKey(), (String)entry.getValue());
            }
        }
        if (this.mSendData != null) {
            urlConnection.setDoOutput(true);
            try {
                OutputStream os = urlConnection.getOutputStream();
                int writedDataLength = 0;
                while (writedDataLength < this.mSendData.length) {
                    int minWriteSize = Math.min(1428, this.mSendData.length - writedDataLength);
                    os.write(this.mSendData, writedDataLength, minWriteSize);
                    writedDataLength += minWriteSize;
                    this.progressCallback(writedDataLength, this.mSendData.length, 0, 0, 0, 0);
                }
            }
            catch (Exception var5_15) {
                WWW.errorCallback(this.mRequestCode, var5_15.toString());
                return;
            }
        }
        int responseCode;
        if (urlConnection instanceof HttpURLConnection) {
            HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
            try {
                responseCode = httpURLConnection.getResponseCode();
            }
            catch (IOException var6_17) {
                WWW.errorCallback(this.mRequestCode, var6_17.toString());
                return;
            }
            Map<String, List<String>> map = httpURLConnection.getHeaderFields();
            List<String> listHeaderValues = map.get("location");
            if (!(map == null || responseCode != 301 && responseCode != 302 || listHeaderValues == null || listHeaderValues.isEmpty())) {
                httpURLConnection.disconnect();
                this.mRequestUrl = listHeaderValues.get(0);
                this.run();
                return;
            }
        }
        Map<String, List<String>> map = urlConnection.getHeaderFields();
        int errorCode = this.headerCallback(map) ? 1 : 0;
        if (!(map != null && map.containsKey("content-length") || urlConnection.getContentLength() == -1)) {
            errorCode = errorCode != 0 || this.headerCallback("content-length", String.valueOf(urlConnection.getContentLength())) ? 1 : 0;
        }
        if (!(map != null && map.containsKey("content-type") || urlConnection.getContentType() == null)) {
            errorCode = errorCode != 0 || this.headerCallback("content-type", urlConnection.getContentType()) ? 1 : 0;
        }
        if (errorCode != 0) {
            WWW.errorCallback(this.mRequestCode, this.mRequestUrl + " aborted");
            return;
        }
        int bufferSize = 1428;
        int contentLength = urlConnection.getContentLength() > 0 ? urlConnection.getContentLength() : 0;
        if (url.getProtocol().equalsIgnoreCase("file") || url.getProtocol().equalsIgnoreCase("jar")) {
            bufferSize = contentLength == 0 ? 32768 : Math.min(contentLength, 32768);
        }
        responseCode = 0;
        InputStream is = null;
        try {
            long startTimes = System.currentTimeMillis();
            byte[] buffer = new byte[bufferSize];
            bufferSize = 1;
            String string = "";
            is = null;
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                is = httpURLConnection.getErrorStream();
                string = "" + httpURLConnection.getResponseCode() + ": " + httpURLConnection.getResponseMessage();
            }
            if (is == null) {
                is = urlConnection.getInputStream();
                bufferSize = 0;
            }
            int readedLength = 0;
            while (readedLength != -1) {
                if (this.readCallback(buffer, readedLength)) {
                    WWW.errorCallback(this.mRequestCode, this.mRequestUrl + " aborted");
                    return;
                }
                if (bufferSize == 0) {
                    this.progressCallback(0, 0, responseCode += readedLength, contentLength, System.currentTimeMillis(), startTimes);
                }
                readedLength = is.read(buffer);
            }
            if (bufferSize != 0) {
                WWW.errorCallback(this.mRequestCode, string);
            }
        }
        catch (Exception var6_20) {
            WWW.errorCallback(this.mRequestCode, var6_20.toString());
            return;
        } finally {
            if(null != is) {
                try{
                    is.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.progressCallback(0, 0, responseCode, responseCode, 0, 0);
        WWW.doneCallback(this.mRequestCode);
    }

    public boolean headerCallback(Map<String,List<String>> httpHeaders) {
        if (httpHeaders == null || httpHeaders.size() == 0) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            for (String headerValue : entry.getValue()) {
                stringBuilder.append(entry.getKey());
                stringBuilder.append(": ");
                stringBuilder.append(headerValue);
                stringBuilder.append("\r\n");
            }
            if (entry.getKey() != null) continue;
            for (String headerValue : entry.getValue()) {
                stringBuilder.append("Status: ");
                stringBuilder.append(headerValue);
                stringBuilder.append("\r\n");
            }
        }
        return WWW.headerCallback(this.mRequestCode, stringBuilder.toString());
    }

    public boolean headerCallback(String httpHeader, String httpHeaderValues) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(httpHeader);
        stringBuilder.append(": ");
        stringBuilder.append(httpHeaderValues);
        stringBuilder.append("\n\r");
        return WWW.headerCallback(this.mRequestCode, stringBuilder.toString());
    }

    public  boolean readCallback(byte[] arrby, int n2) {
        return WWW.readCallback(this.mRequestCode, arrby, n2);
    }

    public void progressCallback(int writedLength, int totalLength, int contentWritedLength, int contentLength, long currentTimes, long startTimes) {
        float progress1;
        double totalTimes;
        float progress2;
        if (contentLength > 0) {
            double d3;
            progress1 = (float)contentWritedLength / (float)contentLength;
            progress2 = 1.0f;
            totalLength = Math.max(contentLength - contentWritedLength, 0);
            totalTimes = (double)totalLength / (d3 = 1000.0 * (double)contentWritedLength / Math.max((double)(currentTimes - startTimes), 0.1));
            if (Double.isInfinite(totalTimes) || Double.isNaN(totalTimes)) {
                totalTimes = 0.0;
            }
        } else if (totalLength > 0) {
            progress1 = 0.0f;
            progress2 = writedLength / totalLength;
            totalTimes = 0.0;
        } else {
            return;
        }
        WWW.progressCallback(this.mRequestCode, progress2, progress1, totalTimes, contentLength);
    }

    private static native boolean headerCallback(int requestCode, String headers);

    private static native boolean readCallback(int requestCode, byte[] buffer, int bufferLength);

    private static native void progressCallback(int requestCode, float progress1, float progress2, double totalTimes, int contentLength);

    private static native void errorCallback(int requestCode, String errorMessage);

    private static native void doneCallback(int requestCode);
}

