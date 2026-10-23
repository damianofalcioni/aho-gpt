package aho.gpt.android.auth;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONException;
import org.json.JSONObject;

/** One encrypted atomic record, never backed up or exposed to WebView. */
public final class CredentialStore {
    private static final String ALIAS = "openai.credentials.v1";
    private final AtomicFile file;

    public CredentialStore(Context context) {
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "openai.enc"));
    }

    public synchronized JSONObject load() throws IOException, GeneralSecurityException, JSONException {
        if (!file.getBaseFile().exists()) { return new JSONObject(); }
        JSONObject envelope = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, decode(envelope.getString("iv"))));
        return new JSONObject(new String(cipher.doFinal(decode(envelope.getString("data"))), StandardCharsets.UTF_8));
    }

    public synchronized void save(JSONObject record) throws GeneralSecurityException, JSONException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        JSONObject envelope = new JSONObject().put("iv", encode(cipher.getIV()))
                .put("data", encode(cipher.doFinal(record.toString().getBytes(StandardCharsets.UTF_8))));
        FileOutputStream stream = file.startWrite();
        try {
            stream.write(envelope.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
        } catch (IOException exception) { file.failWrite(stream); throw exception; }
    }

    private SecretKey key() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) { return (SecretKey) store.getKey(ALIAS, null); }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    private static byte[] decode(String value) { return Base64.decode(value, Base64.NO_WRAP); }
    private static String encode(byte[] value) { return Base64.encodeToString(value, Base64.NO_WRAP); }
}
