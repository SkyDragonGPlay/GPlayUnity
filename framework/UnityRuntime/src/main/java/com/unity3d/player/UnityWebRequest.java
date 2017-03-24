package com.unity3d.player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

class UnityWebRequest implements Runnable {
    private long nativeObjectId;
    private String requestUrl;
    private String method;
    private Map<String,String> requestProperties;
    private static final String[] protocols = new String[]{"TLSv1.2", "TLSv1.1"};
    private static volatile SSLSocketFactory socketFactory;

    private static SSLSocketFactory getSSLSocketFactory() {
        if (UnityConstants.IS_GE_SDK_VERSION_21) {
            return null;
        }
        if (socketFactory != null) {
            return socketFactory;
        }
        String[] arrstring = protocols;
        synchronized (arrstring) {
            for (String string : protocols) {
                try {
                    SSLContext sSLContext = SSLContext.getInstance(string);
                    sSLContext.init(null, null, null);
                    socketFactory = sSLContext.getSocketFactory();
                    return socketFactory;
                }
                catch (Exception var5_6) {
                    UnityLog.Log(5, "UnityWebRequest: No support for " + string + " (" + var5_6.getMessage() + ")");
                    continue;
                }
            }
        }
        return null;
    }

    public UnityWebRequest(long nObjectId, String method, Map httpProperties, String url) {
        this.nativeObjectId = nObjectId;
        this.requestUrl = url;
        this.method = method;
        this.requestProperties = httpProperties;
    }

    @Override
    public void run() {
        URLConnection urlConnection;
        URL url;
        try {
            url = new URL(this.requestUrl);
            urlConnection = url.openConnection();
            if(urlConnection instanceof  HttpURLConnection) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection)urlConnection;
                httpsURLConnection.setSSLSocketFactory(getSSLSocketFactory());
            }
        }
        catch (MalformedURLException malformedURLException) {
            this.malformattedUrlCallback(malformedURLException.toString());
            return;
        }
        catch (IOException ioException) {
            this.errorCallback(ioException.toString());
            return;
        }
        if (url.getProtocol().equalsIgnoreCase("file") && !url.getHost().isEmpty()) {
            this.malformattedUrlCallback("file:// must use an absolute path");
            return;
        }
        if (urlConnection instanceof JarURLConnection) {
            this.badProtocolCallback("A URL Connection to camera Java ARchive (JAR) file or an entry in nativeObjectId JAR file is not supported");
            return;
        }
        if (urlConnection instanceof HttpURLConnection) {
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                httpURLConnection.setRequestMethod(this.method);
                httpURLConnection.setInstanceFollowRedirects(false);
            }
            catch (ProtocolException protocolException) {
                this.badProtocolCallback(protocolException.toString());
                return;
            }
        }

        if (this.requestProperties != null) {
            for (Map.Entry entry : this.requestProperties.entrySet()) {
                urlConnection.addRequestProperty((String)entry.getKey(), (String)entry.getValue());
            }
        }
        int available = 1428;
        int size = this.uploadCallback(null);
        if (size > 0) {
            urlConnection.setDoOutput(true);
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Math.min(size, available));
                OutputStream outputStream = urlConnection.getOutputStream();
                size = this.uploadCallback(byteBuffer);
                while (size > 0) {
                    outputStream.write(byteBuffer.array(), byteBuffer.arrayOffset(), size);
                    size = this.uploadCallback(byteBuffer);
                }
            }
            catch (Exception exception) {
                this.errorCallback(exception.toString());
                return;
            }
        }
        if (urlConnection instanceof HttpURLConnection) {
            HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
            try {
                int code = httpURLConnection.getResponseCode();
                this.responseCodeCallback(code);
            }
            catch (UnknownHostException unknowHostException) {
                this.unknownHostCallback(unknowHostException.toString());
            }
            catch (IOException ioException) {
                this.errorCallback(ioException.toString());
                return;
            }
        }
        Map<String, List<String>> map = urlConnection.getHeaderFields();
        this.headerCallback(map);
        if (!(map != null && map.containsKey("content-length") || urlConnection.getContentLength() == -1)) {
            this.headerCallback("content-length", String.valueOf(urlConnection.getContentLength()));
        }
        if (!(map != null && map.containsKey("content-type") || urlConnection.getContentType() == null)) {
            this.headerCallback("content-type", urlConnection.getContentType());
        }
        int contentLength;
        if ((contentLength = urlConnection.getContentLength()) > 0) {
            this.contentLengthCallback(contentLength);
        }
        int length = -1;
        if (url.getProtocol().equalsIgnoreCase("file")) {
            length = contentLength == 0 ? 32768 : Math.min(contentLength, 32768);
        }
        try {
            InputStream inputStream = null;
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                this.responseCodeCallback(httpURLConnection.getResponseCode());
                inputStream = httpURLConnection.getErrorStream();
            }
            if (inputStream == null) {
                inputStream = urlConnection.getInputStream();
            }
            Channel channel = Channels.newChannel(inputStream);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
            int readSize = ((ReadableByteChannel)channel).read(byteBuffer);
            while (readSize != -1 && this.downloadCallback(byteBuffer, readSize)) {
                byteBuffer.clear();
                readSize = ((ReadableByteChannel)channel).read(byteBuffer);
            }
            channel.close();
            return;
        }
        catch (UnknownHostException unknownHostException) {
            this.unknownHostCallback(unknownHostException.toString());
            return;
        }
        catch (SSLHandshakeException sslHandshakeException) {
            this.sslCannotConnectCallback(sslHandshakeException.toString());
            return;
        }
        catch (Exception exception) {
            this.errorCallback(exception.toString());
            return;
        }
    }

    public void headerCallback(String httpPropKey, String httpPropValue) {
        headerCallback(this.nativeObjectId, httpPropKey, httpPropValue);
    }

    public void headerCallback(Map<String,List<String>> httpProperties) {
        if (httpProperties == null || httpProperties.size() == 0) {
            return;
        }
        Iterator iter = httpProperties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            String attributeName = (String)entry.getKey();
            if (attributeName == null) {
                attributeName = "Status";
            }
            for (String prop : (List<String>)entry.getValue()) {
                this.headerCallback(attributeName, prop);
            }
        }
    }

    public int uploadCallback(ByteBuffer byteBuffer) {
        return uploadCallback(this.nativeObjectId, byteBuffer);
    }


    public void contentLengthCallback(int contentLength) {
        contentLengthCallback(this.nativeObjectId, contentLength);
    }

    public boolean downloadCallback(ByteBuffer byteBuffer, int size) {
        return downloadCallback(this.nativeObjectId, byteBuffer, size);
    }

    public void responseCodeCallback(int code) {
        responseCodeCallback(this.nativeObjectId, code);
    }

    public void unknownHostCallback(String msg) {
        errorCallback(this.nativeObjectId, 7, msg);
    }

    public void badProtocolCallback(String msg) {
        errorCallback(this.nativeObjectId, 4, msg);
    }

    public void malformattedUrlCallback(String msg) {
        errorCallback(this.nativeObjectId, 5, msg);
    }

    public void sslCannotConnectCallback(String msg) {
        errorCallback(this.nativeObjectId, 16, msg);
    }

    public void errorCallback(String msg) {
        errorCallback(this.nativeObjectId, 2, msg);
    }

    private static native int uploadCallback(long nId, ByteBuffer buffer);

    private static native void contentLengthCallback(long nId, int contentLength);

    private static native boolean downloadCallback(long nId, ByteBuffer buffer, int size);

    private static native void responseCodeCallback(long nId, int code);

    private static native void headerCallback(long nId, String httpPropKey, String httpPropValue);

    private static native void errorCallback(long nId, int code, String msg);
}

