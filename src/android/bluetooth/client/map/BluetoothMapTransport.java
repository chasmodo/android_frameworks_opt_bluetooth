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

import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.btobex.ObexTransport;
import javax.btobex.ObexHelper;

class BluetoothMapTransport implements ObexTransport {
    private static final String TAG = "BluetoothMapTransport";
    public static final int TYPE_RFCOMM = 0;
    public static final int TYPE_L2CAP = 1;

    private final BluetoothSocket mSocket;
    public final int mType;

    public BluetoothMapTransport(BluetoothSocket socket, int type) {
        super();
        this.mSocket = socket;
        this.mType = type;
    }

    @Override
    public void create() throws IOException {
    }

    @Override
    public void listen() throws IOException {
    }

    @Override
    public void close() throws IOException {
        mSocket.close();
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void disconnect() throws IOException {
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return mSocket.getInputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return mSocket.getOutputStream();
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public boolean isSrmCapable() {
        return mType == TYPE_L2CAP;
    }

    public int getMaxPacketSize() {
        return ObexHelper.MAX_PACKET_SIZE_INT;
    }

}
