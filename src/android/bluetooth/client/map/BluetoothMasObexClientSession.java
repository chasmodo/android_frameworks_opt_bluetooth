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

import android.os.Handler;
import android.os.Process;
import android.util.Log;

import java.io.IOException;

import javax.btobex.ClientSession;
import javax.btobex.HeaderSet;
import javax.btobex.ObexTransport;
import javax.btobex.ObexHelper;
import javax.btobex.ResponseCodes;

class BluetoothMasObexClientSession {
    private static final String TAG = "BluetoothMasObexClientSession";

    private static final byte[] MAS_TARGET = new byte[] {
            (byte) 0xbb, 0x58, 0x2b, 0x40, 0x42, 0x0c, 0x11, (byte) 0xdb, (byte) 0xb0, (byte) 0xde,
            0x08, 0x00, 0x20, 0x0c, (byte) 0x9a, 0x66
    };

    static final int MSG_OBEX_CONNECTED = 100;
    static final int MSG_OBEX_DISCONNECTED = 101;
    static final int MSG_REQUEST_COMPLETED = 102;

    private final ObexTransport mTransport;

    private final Handler mSessionHandler;

    private ClientThread mClientThread;

    private volatile boolean mInterrupted;

    private class ClientThread extends Thread {
        private final ObexTransport mTransport;

        private ClientSession mSession;

        private BluetoothMasRequest mRequest;

        private boolean mConnected;

        public ClientThread(ObexTransport transport) {
            super("MAS ClientThread");

            mTransport = transport;
            mConnected = false;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            connect();

            if (mConnected) {
                mSessionHandler.obtainMessage(MSG_OBEX_CONNECTED).sendToTarget();
            } else {
                mSessionHandler.obtainMessage(MSG_OBEX_DISCONNECTED).sendToTarget();
                return;
            }

            while (!mInterrupted) {
                synchronized (this) {
                    if (mRequest == null) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            mInterrupted = true;
                        }
                    }
                }

                if (!mInterrupted && mRequest != null) {
                    try {
                        mRequest.execute(mSession);
                    } catch (IOException e) {
                        // this will "disconnect" to cleanup
                        mInterrupted = true;
                    }

                    BluetoothMasRequest oldReq = mRequest;
                    mRequest = null;

                    mSessionHandler.obtainMessage(MSG_REQUEST_COMPLETED, oldReq).sendToTarget();
                }
            }

            disconnect();

            mSessionHandler.obtainMessage(MSG_OBEX_DISCONNECTED).sendToTarget();
        }

        private void connect() {
            try {
                int mps = ObexHelper.MAX_CLIENT_PACKET_SIZE;

                Log.w(TAG, "connect: MPS: " + mps);
                mSession = new ClientSession(mTransport);
                mSession.setMaxPacketSize(mps);
                HeaderSet headerset = new HeaderSet();
                headerset.setHeader(HeaderSet.TARGET, MAS_TARGET);

                headerset = mSession.connect(headerset);

                if (headerset.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
                    mConnected = true;
                    // Turn on/off SRM based on transport capability based on OBEX-over-L2CAP capable
                    mSession.mSrmClient.setLocalSrmCapability(
                            ((BluetoothMapTransport)mTransport).isSrmCapable());
                } else {
                    disconnect();
                }
            } catch (IOException e) {
                Log.w(TAG, "handled connect exception: ", e);
            }
        }

        private void disconnect() {
            Log.w(TAG, "disconnect: ");
            try {
                mSession.disconnect(null);
            } catch (IOException e) {
            }

            try {
                mSession.close();
            } catch (IOException e) {
                Log.w(TAG, "handled disconnect exception:", e);
            }

            mConnected = false;
        }

        public synchronized boolean schedule(BluetoothMasRequest request) {
            if (mRequest != null) {
                return false;
            }

            mRequest = request;
            notify();

            return true;
        }
    }

    public BluetoothMasObexClientSession(ObexTransport transport, Handler handler) {
        mTransport = transport;
        mSessionHandler = handler;
    }

    public void start() {
        if (mClientThread == null) {
            mClientThread = new ClientThread(mTransport);
            mClientThread.start();
        }

    }

    public void stop() {
        if (mClientThread != null) {
            mClientThread.interrupt();

            (new Thread() {
                @Override
                public void run() {
                    try {
                        mClientThread.join();
                        mClientThread = null;
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Interrupted while waiting for thread to join");
                    }
                }
            }).run();
        }
    }

    public boolean makeRequest(BluetoothMasRequest request) {
        if (mClientThread == null) {
            return false;
        }

        if (mClientThread.mSession.mSrmClient.getLocalSrmCapability() == ObexHelper.SRM_CAPABLE) {
            Log.d(TAG, "Client is srm capable");
            if (request instanceof BluetoothMasRequestGetFolderListing ||
                request instanceof BluetoothMasRequestGetFolderListingSize ||
                request instanceof BluetoothMasRequestGetMessagesListing ||
                request instanceof BluetoothMasRequestGetMessage ||
                request instanceof BluetoothMasRequestPushMessage ||
                request instanceof BluetoothMasRequestGetMessagesListingSize) {
                    mClientThread.mSession.mSrmClient.setLocalSrmStatus(
                            ObexHelper.LOCAL_SRM_ENABLED);
                    request.mHeaderSet.setHeader(HeaderSet.SINGLE_RESPONSE_MODE,
                            ObexHelper.OBEX_SRM_ENABLED);
                    mClientThread.mSession.mSrmClient.setLocalSrmpWait(false);
            }
        } else {
                Log.d(TAG, "Client is not srm capable");
        }

        return mClientThread.schedule(request);
    }

    public int readTransportType() {
        return ((BluetoothMapTransport)mTransport).mType;
    }
}
