package com.bitcryptopay.cryptopay;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.CMSTypedData;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.MQVPrivateKeySpec;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private RequestQueue queue;
    MainActivity me = this;


    public void onTestOneClick(View v) {
        String url = "https://api.blockcypher.com/v1/beth/test/addrs/";
        JsonObjectRequest r = new JsonObjectRequest(Request.Method.GET, url + address, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    ((TextView)findViewById(R.id.balancetext)).setText(response.getString("balance"));
                } catch (JSONException e) {
                    ((TextView)findViewById(R.id.balancetext)).setText("failed!");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ((TextView)findViewById(R.id.balancetext)).setText("error response");
            }
        });
        queue.add(r);
    }

    private String privatekey, publickey, address;
    private String outaddress = "b26a0e6e593f33c7e177a55b57072b48f8b0c560";

    public void onTestTwoClick(View v) {
        String url = "https://api.blockcypher.com/v1/beth/test/addrs?token=e7b00d26dbca419ba5b9499f4dea638a";
        JsonObjectRequest r = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    privatekey = response.getString("private");
                    publickey = response.getString("public");
                    address = response.getString("address");
                    ((TextView)findViewById(R.id.addresstext)).setText(address);
                } catch (JSONException e) {
                    ((TextView)findViewById(R.id.addresstext)).setText("failed!");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ((TextView)findViewById(R.id.addresstext)).setText("error response");
            }
        });
        queue.add(r);
    }

    public void onTestThreeClick(View v) {
        String url = "https://api.blockcypher.com/v1/beth/test/faucet?token=e7b00d26dbca419ba5b9499f4dea638a";
        JSONObject request = new JSONObject();
        try {
            request.put("address", address);
            request.put("amount", 1000000000000000000L);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest r = new JsonObjectRequest(Request.Method.POST, url, request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    ((TextView)findViewById(R.id.getconfirmtext)).setText(response.getString("tx_ref"));
                } catch (JSONException e) {
                    ((TextView)findViewById(R.id.getconfirmtext)).setText("failed!");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ((TextView)findViewById(R.id.getconfirmtext)).setText("error response");
            }
        });
        queue.add(r);
    }

    public void onTestFourClick(View v) {
        String url = "https://api.blockcypher.com/v1/beth/test/addrs/";
        JsonObjectRequest r = new JsonObjectRequest(Request.Method.GET, url + outaddress, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    ((TextView)findViewById(R.id.theirbalancetext)).setText(response.getString("balance"));
                } catch (JSONException e) {
                    ((TextView)findViewById(R.id.theirbalancetext)).setText("failed!");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ((TextView)findViewById(R.id.getconfirmtext)).setText("error response");
            }
        });
        queue.add(r);
    }

    public void onTestFiveClick(View v) {
        String url = "https://api.blockcypher.com/v1/beth/test/txs/new?token=e7b00d26dbca419ba5b9499f4dea638a";
        JSONObject request = new JSONObject();
        JSONObject inputs = new JSONObject();
        JSONObject outputs = new JSONObject();
        try {
            inputs.put("addresses", new JSONArray(new Object[] {address}));
            outputs.put("addresses", new JSONArray(new Object[] {outaddress}));
            outputs.put("value", 100000000000L);
            request.put("inputs", new JSONArray(new Object[] {inputs}));
            request.put("outputs", new JSONArray(new Object[] {outputs}));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest r = new JsonObjectRequest(Request.Method.POST, url, request, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    //sign string
                    String tosign = response.getJSONArray("tosign").getString(0);
                    byte[] tosignbytes = Hex.decode(tosign);
                    byte[] pkb = Hex.decode(privatekey);
                    Security.addProvider(new BouncyCastleProvider());
                    ECPrivateKeySpec ks = new ECPrivateKeySpec(new BigInteger(pkb), (ECParameterSpec)ECNamedCurveTable.getParameterSpec("secp256k1"));
                    KeyFactory kf = KeyFactory.getInstance("EC");
                    PrivateKey pk = kf.generatePrivate(ks);
                    Signature sig = Signature.getInstance("NONEwithECDSA");
                    sig.initSign(pk);
                    sig.update(tosignbytes);
                    tosign = new String(Hex.encode(sig.sign()));
                    response.put("signatures", new JSONArray(new Object[] {tosign}));

                    String url = "https://api.blockcypher.com/v1/beth/test/txs/send?token=e7b00d26dbca419ba5b9499f4dea638a";
                    JsonObjectRequest v = new JsonObjectRequest(Request.Method.POST, url, response, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String text = response.getJSONObject("tx").getString("hash");
                                ((TextView)findViewById(R.id.sendconfirmtext)).setText(text);
                            } catch (Exception e) {
                                ((TextView)findViewById(R.id.sendconfirmtext)).setText("at the end!");
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            ((TextView)findViewById(R.id.sendconfirmtext)).setText("error second response");

                        }
                    });
                    queue.add(v);
                } catch (Exception e) {
                    ((TextView)findViewById(R.id.sendconfirmtext)).setText("exception during sign!");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ((TextView)findViewById(R.id.sendconfirmtext)).setText("error first response");
            }
        });
        queue.add(r);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new IntentIntegrator(me).initiateScan();
            }});
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
