package BlockChain;

import Util.StringUtil;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class Transaction {

    private String sender;
    public String receiver;
    private double amount;
    public double fee;
    private String messages;

    public String transaction_hash;
    public String publickey;
    public String signature;
    public Transaction(String sender, String receiver, double amount, double fee, String messages, String publickey, String signature ) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.fee = fee;
        this.messages = messages;
        // HASH = RIPEMD160( SHA256( transaction ) )
        this.transaction_hash = StringUtil.apply_RIPEMD160(StringUtil.applyHASH(this.sender+this.receiver+this.amount+this.fee+this.messages,"SHA-256"));

        this.publickey = publickey;
        this.signature = signature;
    }


    public JSONObject Transaction_to_JSON() throws IllegalAccessException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("sender",this.sender);
        jsonObject.put("receiver",this.receiver);
        jsonObject.put("amount",this.amount);
        jsonObject.put("fee",this.fee);
        jsonObject.put("messages",this.messages);
        //jsonObject.put("publicKey", StringUtil.Generate_ECDSA_Public_Key(this.publickey));
        jsonObject.put("publicKey",this.publickey);
        jsonObject.put("txnsign",this.signature);
        return jsonObject;
    }
/*
    public String Transaction_to_String() throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalAccessException {
        return this.sender+this.receiver+this.amount+this.fee+this.messages+Util.StringUtil.Generate_ECDSA_Public_Key(this.publickey);
    }*/

    public double Get_amount(){return this.amount;}
    public  String Get_messages(){return this.messages;}

}
