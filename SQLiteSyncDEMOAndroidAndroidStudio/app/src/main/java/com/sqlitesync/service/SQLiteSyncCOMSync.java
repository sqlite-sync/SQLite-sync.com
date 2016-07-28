package com.sqlitesync.service;

import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by konradgardocki on 13.07.2016.
 */
public class SQLiteSyncCOMSync {
    private String _serviceUrl;

    public SQLiteSyncCOMSync(String serviceUrl){
        _serviceUrl = serviceUrl;
    }

    public void GetFullDBSchema(String subscriberId, Callback callback){
        new WebServiceCaller(_serviceUrl).execute(new SQLiteSyncCOMSyncTaskParam("GetFullDBSchema",String.format("<subscriber>\"%1$s\"</subscriber>",subscriberId),callback));
    }

    public void GetDataForSync(String subscriberId, String table, Callback callback){
        new WebServiceCaller(_serviceUrl).execute(new SQLiteSyncCOMSyncTaskParam("GetDataForSync",String.format("<subscriber>\"%1$s\"</subscriber><table>\"%2$s\"</table>",subscriberId, table),callback));
    }

    public void SyncCompleted(int syncId, Callback callback){
        new WebServiceCaller(_serviceUrl).execute(new SQLiteSyncCOMSyncTaskParam("SyncCompleted",String.format("<syncId>%1$d</syncId>",syncId),callback));
    }

    public void ReceiveData(String subscriberId, String data, Callback callback){
        new WebServiceCaller(_serviceUrl).execute(new SQLiteSyncCOMSyncTaskParam("ReceiveData",String.format("<subscriber>%1$s</subscriber><data>%2$s</data>",subscriberId, xmlSimpleEscape(data)),callback));
    }

    public interface Callback {
        void onFinished(Object outputObject, Exception exception);
    }

    private String xmlSimpleEscape(String value){
        return value
                .replaceAll("&","&amp;")
                .replaceAll("\"","&quot;")
                .replaceAll("'","&#x27;")
                .replaceAll(">","&gt;")
                .replaceAll("<","&lt;");
    }

    private class WebServiceCaller extends AsyncTask<SQLiteSyncCOMSyncTaskParam,Integer,SQLiteSyncCOMSyncTaskParam>{
        private String _serviceUrl;

        public WebServiceCaller(String serviceUrl){
            _serviceUrl = serviceUrl;
        }

        @Override
        protected SQLiteSyncCOMSyncTaskParam doInBackground(SQLiteSyncCOMSyncTaskParam... taskParams) {
            if(taskParams.length < 1) return null;

            HttpURLConnection connection = null;
            String resultString = null;
            InputStream resultStream = null;
            SQLiteSyncCOMSyncTaskParam taskParam = taskParams[0];
            Object resultObject = null;

            try {
                connection = (HttpURLConnection) new URL(_serviceUrl).openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("POST");

                String request = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "<soap:Body>\n"
                        + "<%1$s xmlns=\"http://sqlite-sync.com/\">\n"
                        + "%2$s\n"
                        + "</%1$s>\n"
                        + "</soap:Body>\n"
                        + "</soap:Envelope>",taskParam.SOAPAction,taskParam.SOAPBody);

                byte[] requestBytes = request.getBytes("UTF-8");
                connection.setRequestProperty("SOAPAction", String.format("http://sqlite-sync.com/%1$s",taskParam.SOAPAction));
                connection.setRequestProperty("Content-Type", "text/xml");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(requestBytes);
                wr.flush();
                wr.close();

                resultStream = connection.getInputStream();
                taskParam.ResultString = IOUtils.toString(resultStream);
            } catch (Exception exc) {
                taskParam.Exception = exc;
            } finally {
                if (resultStream != null) {
                    try {
                        resultStream.close();
                    } catch (IOException exc) {
                        exc.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return taskParam;
        }

        protected void onPostExecute(SQLiteSyncCOMSyncTaskParam taskParam) {
            if(taskParam.Callback == null) return;

            Object resultObject = null;
            if(taskParam.Exception == null){
                try{
                    JSONObject resultJson = XML.toJSONObject(taskParam.ResultString);
            resultObject = resultJson
                    .getJSONObject("soap:Envelope")
                    .getJSONObject("soap:Body")
                    .getJSONObject(taskParam.SOAPAction+"Response")
                    .get(taskParam.SOAPAction+"Result");
        } catch (Exception exc){
        }
    }
            taskParam.Callback.onFinished(resultObject,taskParam.Exception);
        }
    }

    private class SQLiteSyncCOMSyncTaskParam {
        String SOAPAction;
        String SOAPBody;
        SQLiteSyncCOMSync.Callback Callback;
        String ResultString;
        Exception Exception;

        SQLiteSyncCOMSyncTaskParam(String SOAPAction, String SOAPBody, SQLiteSyncCOMSync.Callback Callback) {
            this.SOAPAction = SOAPAction;
            this.SOAPBody = SOAPBody;
            this.Callback = Callback;
        }
    }
}
