/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth.client.map;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * Object representation of event report received by MNS
 * <p>
 * This object will be received in {@link BluetoothMasClient#EVENT_EVENT_REPORT}
 * callback message.
 */
public class BluetoothMapEventReport {

    private final static String TAG = "BluetoothMapEventReport";
    public final static String EXTENDED_EVENT_REPORT_1_1 = "1.1";

    public enum Type {
        NEW_MESSAGE("NewMessage"), DELIVERY_SUCCESS("DeliverySuccess"),
        SENDING_SUCCESS("SendingSuccess"), DELIVERY_FAILURE("DeliveryFailure"),
        SENDING_FAILURE("SendingFailure"), MEMORY_FULL("MemoryFull"),
        MEMORY_AVAILABLE("MemoryAvailable"), MESSAGE_DELETED("MessageDeleted"),
        MESSAGE_SHIFT("MessageShift"), READ_STATUS_CHANGED("ReadStatusChanged");

        private final String mSpecName;

        private Type(String specName) {
            mSpecName = specName;
        }

        @Override
        public String toString() {
            return mSpecName;
        }
    }

    private final String mVersion;

    private final Type mType;

    private final String mHandle;

    private final String mFolder;

    private final String mOldFolder;

    private final BluetoothMapBmessage.Type mMsgType;

    private final String mSubject;

    private final String mDatetime;

    private final String mSenderName;

    private final String mPriority;

    private BluetoothMapEventReport(HashMap<String, String> attrs) throws IllegalArgumentException {

        mVersion = attrs.get("version");

        mType = parseType(attrs.get("type"));

        if (mType != Type.MEMORY_FULL && mType != Type.MEMORY_AVAILABLE) {
            String handle = attrs.get("handle");
            try {
                /* just to validate */
                new BigInteger(attrs.get("handle"), 16);

                mHandle = attrs.get("handle");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for handle:" + handle);
            }
        } else {
            mHandle = null;
        }

        mFolder = attrs.get("folder");

        mOldFolder = attrs.get("old_folder");

        if (mType != Type.MEMORY_FULL && mType != Type.MEMORY_AVAILABLE) {
            String s = attrs.get("msg_type");

            if ("".equals(s)) {
                // Some phones (e.g. SGS3 for MessageDeleted) send empty
                // msg_type, in such case leave it as null rather than throw
                // parse exception
                mMsgType = null;
            } else {
                mMsgType = parseMsgType(s);
            }
        } else {
            mMsgType = null;
        }

        if (mType == Type.NEW_MESSAGE && mVersion.equals(EXTENDED_EVENT_REPORT_1_1)) {
            mSubject = attrs.get("subject");
            mDatetime = attrs.get("datetime");
            mSenderName = attrs.get("sender_name");
            mPriority = attrs.get("priority");

            Log.d(TAG, "received extended event report 1.1 for new message" +
                " Subject: " + mSubject + " Datetime: " + mDatetime +
                " sender_name: " + mSenderName + " Priority: " + mPriority);
        } else {
            mSubject = null;
            mDatetime = null;
            mSenderName = null;
            mPriority = null;
        }
    }

    private Type parseType(String type) throws IllegalArgumentException {
        for (Type t : Type.values()) {
            if (t.toString().equals(type)) {
                return t;
            }
        }

        throw new IllegalArgumentException("Invalid value for type: " + type);
    }

    private BluetoothMapBmessage.Type parseMsgType(String msgType) throws IllegalArgumentException {
        for (BluetoothMapBmessage.Type t : BluetoothMapBmessage.Type.values()) {
            if (t.name().equals(msgType)) {
                return t;
            }
        }

        throw new IllegalArgumentException("Invalid value for msg_type: " + msgType);
    }

    /**
     * @return value corresponding to <code>version</code> parameter in MAP
     *         specification
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * @return {@link BluetoothMapEventReport.Type} object corresponding to
     *         <code>type</code> application parameter in MAP specification
     */
    public Type getType() {
        return mType;
    }

    /**
     * @return value corresponding to <code>handle</code> parameter in MAP
     *         specification
     */
    public String getHandle() {
        return mHandle;
    }

    /**
     * @return value corresponding to <code>folder</code> parameter in MAP
     *         specification
     */
    public String getFolder() {
        return mFolder;
    }

    /**
     * @return value corresponding to <code>old_folder</code> parameter in MAP
     *         specification
     */
    public String getOldFolder() {
        return mOldFolder;
    }

    /**
     * @return {@link BluetoothMapBmessage.Type} object corresponding to
     *         <code>msg_type</code> application parameter in MAP specification
     */
    public BluetoothMapBmessage.Type getMsgType() {
        return mMsgType;
    }

    /**
     * @return value corresponding to <code>subject</code> parameter in MAP
     *         specification
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * @return value corresponding to <code>datetime"</code> parameter in MAP
     *         specification
     */
    public String getDatetime() {
        return mDatetime;
    }

   /**
     * @return value corresponding to <code>sender_name"</code> parameter in MAP
     *         specification
     */
    public String getSenderName() {
        return mSenderName;
    }

   /**
     * @return value corresponding to <code>priority"</code> parameter in MAP
     *         specification
     */
    public String getPriority() {
        return mPriority;
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();

        try {
            json.put("version", mVersion);
            json.put("type", mType);
            json.put("handle", mHandle);
            json.put("folder", mFolder);
            json.put("old_folder", mOldFolder);
            json.put("msg_type", mMsgType);
            json.put("subject", mSubject);
            json.put("datetime", mDatetime);
            json.put("sender_name", mSenderName);
            json.put("priority", mPriority);
        } catch (JSONException e) {
            // do nothing
        }

        return json.toString();
    }

    static BluetoothMapEventReport fromStream(DataInputStream in) {
        BluetoothMapEventReport ev = null;
        HashMap<String, String> attrs = new HashMap<String, String>();

        try {
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(in, "utf-8");

            int event = xpp.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equals("MAP-event-report")) {
                            Log.d(TAG, "MAP-event-report version: " + xpp.getAttributeValue(0));
                            attrs.put(xpp.getAttributeName(0), xpp.getAttributeValue(0));
                        }

                        if (xpp.getName().equals("event")) {
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                attrs.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
                            }

                            ev = new BluetoothMapEventReport(attrs);

                            // return immediately, only one event should be here
                            return ev;
                        }
                        break;
                }

                event = xpp.next();
            }

        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML parser error when parsing XML", e);
        } catch (IOException e) {
            Log.e(TAG, "I/O error when parsing XML", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid event received", e);
        }

        return ev;
    }
}
