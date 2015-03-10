/*
 * Copyright (C) 2015 Frank Morgner
 *
 * This file is part of ACardEmulator.
 *
 * ACardEmulator is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * ACardEmulator is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ACardEmulator.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsmartcard.acardemulator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.licel.jcardsim.samples.HelloWorldApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;

import net.pwendland.javacard.pki.isoapplet.IsoApplet;

import openpgpcard.OpenPGPApplet;
import pkgYkneoOath.YkneoOath;

public class SimulatorService extends HostApduService {

    public static final String TAG = "com.vsmartcard.acardemulator.SimulatorService";
    public static final String EXTRA_CAPDU = "MSG_CAPDU";
    public static final String EXTRA_RAPDU = "MSG_RAPDU";
    public static final String EXTRA_ERROR = "MSG_ERROR";
    public static final String EXTRA_DESELECT = "MSG_DESELECT";
    public static final String EXTRA_INSTALL = "MSG_INSTALL";

    private static CardSimulator simulator = null;

    private void createSimulator() {
        String aid, name, extra_install = "", extra_error = "";
        simulator = new CardSimulator();

        name = getResources().getString(R.string.applet_helloworld);
        aid = getResources().getString(R.string.aid_helloworld);
        try {
            simulator.installApplet(AIDUtil.create(aid), HelloWorldApplet.class);
            extra_install += "\n" + name + " (AID: " + aid + ")";
        } catch (Exception e) {
            e.printStackTrace();
            extra_error += "\n" + "Could not install " + name + " (AID: " + aid + ")";
        }

        name = getResources().getString(R.string.applet_openpgp);
        aid = getResources().getString(R.string.aid_openpgp);
        try {
            byte[] aid_bytes = Util.hexStringToByteArray(aid);
            byte[] inst_params = new byte[aid.length()+1];
            inst_params[0] = (byte) aid_bytes.length;
            System.arraycopy(aid_bytes, 0, inst_params, 1, aid_bytes.length);
            simulator.installApplet(AIDUtil.create(aid), OpenPGPApplet.class, inst_params, (short) 0, (byte) inst_params.length);
            extra_install += "\n" + name + " (AID: " + aid + ")";
        } catch (Exception e) {
            e.printStackTrace();
            extra_error += "\n" + "Could not install " + name + " (AID: " + aid + ")";
        }

        name = getResources().getString(R.string.applet_oath);
        aid = getResources().getString(R.string.aid_oath);
        try {
            byte[] aid_bytes = Util.hexStringToByteArray(aid);
            byte[] inst_params = new byte[aid.length()+1];
            inst_params[0] = (byte) aid_bytes.length;
            System.arraycopy(aid_bytes, 0, inst_params, 1, aid_bytes.length);
            simulator.installApplet(AIDUtil.create(aid), YkneoOath.class, inst_params, (short) 0, (byte) inst_params.length);
            extra_install += "\n" + name + " (AID: " + aid + ")";
        } catch (Exception e) {
            e.printStackTrace();
            extra_error += "\n" + "Could not install " + name + " (AID: " + aid + ")";
        }

        name = getResources().getString(R.string.applet_isoapplet);
        aid = getResources().getString(R.string.aid_isoapplet);
        try {
            simulator.installApplet(AIDUtil.create(aid), IsoApplet.class);
            extra_install += "\n" + name + " (AID: " + aid + ")";
        } catch (Exception e) {
            e.printStackTrace();
            extra_error += "\n" + "Could not install " + name + " (AID: " + aid + ")";
        }

        Intent i = new Intent(TAG);
        if (!extra_error.isEmpty())
            i.putExtra(EXTRA_ERROR, extra_error);
        if (!extra_install.isEmpty())
            i.putExtra(EXTRA_INSTALL, extra_install);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    @Override
    public void onCreate () {
        super.onCreate();

        if (simulator == null)
            createSimulator();
    }

    @Override
    public byte[] processCommandApdu(byte[] capdu, Bundle extras) {
        Intent i = new Intent(TAG);
        String extra_error = "";
        byte[] rapdu = null;

        i.putExtra(EXTRA_CAPDU, Util.byteArrayToHexString(capdu));
        try {
            rapdu = simulator.transmitCommand(capdu);
            i.putExtra(EXTRA_RAPDU, Util.byteArrayToHexString(rapdu));
        } catch (Exception e) {
            e.printStackTrace();
            extra_error += "Internal error";
        }

        if (!extra_error.isEmpty())
            i.putExtra(EXTRA_ERROR, extra_error);

        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        return rapdu;
    }

    @Override
    public void onDeactivated(int reason) {
        Intent i = new Intent(TAG);

        switch (reason) {
            case DEACTIVATION_LINK_LOSS:
                i.putExtra(EXTRA_DESELECT, "link lost");
                break;
            case DEACTIVATION_DESELECTED:
                i.putExtra(EXTRA_DESELECT, "deactivated");
                break;
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

}