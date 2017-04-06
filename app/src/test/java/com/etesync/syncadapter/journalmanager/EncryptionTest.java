/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.etesync.syncadapter.journalmanager;

import com.etesync.syncadapter.utils.Base64;

import org.apache.commons.codec.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EncryptionTest {
    private static final String USER = "test@localhost";
    private static final String PASSWORD = "SomePassword";
    private static final String keyBase64 = "Gpn6j6WJ/9JJbVkWhmEfZjlqSps5rwEOzjUOO0rqufvb4vtT4UfRgx0uMivuGwjF7/8Y1z1glIASX7Oz/4l2jucgf+lAzg2oTZFodWkXRZCDmFa7c9a8/04xIs7koFmUH34Rl9XXW6V2/GDVigQhQU8uWnrGo795tupoNQMbtB8RgMX5GyuxR55FvcybHpYBbwrDIsKvXcBxWFEscdNU8zyeq3yjvDo/W/y24dApW3mnNo7vswoL2rpkZj3dqw==";

    @Before
    public void setUp() throws IOException {
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void testDerivePassword() {
        String key = Crypto.deriveKey(USER, PASSWORD);
        assertEquals(key, keyBase64);
    }

    @Test
    public void testCryptoV1() throws Exceptions.IntegrityException, Exceptions.GenericCryptoException {
        Crypto.CryptoManager cryptoManager = new Crypto.CryptoManager(1, keyBase64, "TestSaltShouldBeJournalId");

        String clearText = "This Is Some Test Cleartext.";
        byte[] cipher = cryptoManager.encrypt(clearText.getBytes(Charsets.UTF_8));
        assertEquals(clearText, new String(cryptoManager.decrypt(cipher), Charsets.UTF_8));

        String expected = "Lz+HUFzh1HdjxuGdQrBwBG1IzHT0ug6mO8fwePSbXtc=";
        assertEquals(expected, Base64.encodeToString(cryptoManager.hmac("Some test data".getBytes(Charsets.UTF_8)), Base64.NO_WRAP));
    }

    @Test
    public void testCryptoV2() throws Exceptions.IntegrityException, Exceptions.GenericCryptoException {
        Crypto.CryptoManager cryptoManager = new Crypto.CryptoManager(2, keyBase64, "TestSaltShouldBeJournalId");

        String clearText = "This Is Some Test Cleartext.";
        byte[] cipher = cryptoManager.encrypt(clearText.getBytes(Charsets.UTF_8));
        assertEquals(clearText, new String(cryptoManager.decrypt(cipher), Charsets.UTF_8));

        String expected = "XQ/A0gentOaE98R9wzf3zEIAHj4OH1GF8J4C6JiJupo=";
        assertEquals(expected, Base64.encodeToString(cryptoManager.hmac("Some test data".getBytes(Charsets.UTF_8)), Base64.NO_WRAP));
    }

    @Test(expected=Exceptions.VersionTooNewException.class)
    public void testCryptoVersionTooNew() throws Exceptions.IntegrityException, Exceptions.VersionTooNewException {
        new Crypto.CryptoManager(120, keyBase64, "TestSaltShouldBeJournalId");
    }

    @Test(expected=Exceptions.IntegrityException.class)
    public void testCryptoVersionOutOfRange() throws Exceptions.IntegrityException, Exceptions.VersionTooNewException {
        new Crypto.CryptoManager(999, keyBase64, "TestSaltShouldBeJournalId");
    }
}
