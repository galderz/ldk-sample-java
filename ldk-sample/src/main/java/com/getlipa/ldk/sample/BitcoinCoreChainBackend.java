package com.getlipa.ldk.sample;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class BitcoinCoreChainBackend implements ChainBackend {

    private final String URL = Env.get("BTC_CORE_URL");
    private final String BTC_USER = Env.get("BTC_CORE_USER");
    private final String BTC_PASS = Env.get("BTC_CORE_PASS");

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    private final NetworkParameters networkParameters;

    public BitcoinCoreChainBackend(final NetworkParameters networkParameters) {
        this.networkParameters = networkParameters;
    }

    @Override
    public boolean isSynced() {
        return true;
    }

    @Override
    public int blockHeight() {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getblockcount\", \"params\": []}";
        final String credentials = Credentials.basic(BTC_USER, BTC_PASS);
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, body))
                .addHeader("Authorization", credentials)
                .build();
        try {
            final Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            
            final var json = new JSONObject(response.body().string());
            return json.getInt("result");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int blockHeight(byte[] blockHash) {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getblockheader\", \"params\": [\"%s\", true]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(body, Hex.toHexString(blockHash))))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            final var json = new JSONObject(response.body().string());
            return json.getJSONObject("result").getInt("height");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] blockHash(int height) {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getblockhash\", \"params\": [%s]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(body, height)))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            final var json = new JSONObject(response.body().string());
            return Hex.decode(json.getString("result"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] blockHeader(final byte[] hash) {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getblockheader\", \"params\": [\"%s\"]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(body, Hex.toHexString(hash))))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final var response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            System.out.println(response);
            final var json = new JSONObject(response.body().string()).getJSONObject("result");
            final var buffer = ByteBuffer.wrap(new byte[80]);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(json.getInt("version"));
            buffer.put(reverse(Hex.decode(json.getString("previousblockhash"))), 0, 32);
            buffer.put(reverse(Hex.decode(json.getString("merkleroot"))), 0, 32);
            buffer.putInt(json.getInt("time"));
            buffer.put(reverse(Hex.decode(json.getString("bits"))), 0, 4);
            buffer.putInt(json.getInt("nonce"));
            return (buffer.array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void publish(final byte[] transaction) {
        final var jtx = new Transaction(networkParameters, transaction);
        final var tx = Hex.toHexString(jtx.bitcoinSerialize());
        System.out.printf("Broadcasting tx: %s%n", tx);
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"sendrawtransaction\", \"params\": [\"0%s\"]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(body, tx)))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final var response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            final var json = new JSONObject(response.body().string());
            System.out.println(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isConfirmed(final byte[] txid) {
        return determineBlockInfo(txid) != null;
    }

    @Override
    public TxBlockInfo determineBlockInfo(final byte[] txid) {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getrawtransaction\", \"params\": [\"%s\", true]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(body, Hex.toHexString(reverse(txid)))))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final var response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            final String message = response.body().string();
            System.out.println(message);
            if (!new JSONObject(message).has("result")) {
                return null;
            }
            final var json = new JSONObject(message).getJSONObject("result");

            return new TxBlockInfo(
                    Hex.decode(json.getString("blockhash")),
                    0,
                    0,
                    Hex.decode(json.getString("hex")),
                    txid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long determineBlockIndex(final byte[] blockHash, final byte[] txid) {
        final var body = "{\"jsonrpc\": \"1.0\", \"id\": \"curltest\", \"method\": \"getblock\", \"params\": [\"%s\"]}";
        final var request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(JSON, String.format(
                        body,
                        Hex.toHexString(blockHash)
                )))
                .addHeader("Authorization", Credentials.basic(BTC_USER, BTC_PASS))
                .build();
        try {
            final var response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            final var json = new JSONObject(response.body().string()).getJSONObject("result");
            System.out.println(json);
            if (!json.has("tx")) {
                return -1;
            }
            final var tx = json.getJSONArray("tx");
            for (var index = 0; index < tx.length(); index++) {
                if (Arrays.equals(Hex.decode(tx.getString(index)), reverse(txid))) {
                    return index;
                }
            }
            return -1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static byte[] reverse(final byte[] input) {
        final var output = new byte[input.length];
        for (var i = 0; i < input.length; i++) {
            output[input.length - i -1] = input[i];
        }
        return output;
    }
}
