package Util;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;


public class StringUtil {
    public KeyPair keypair;

    public StringUtil() throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.keypair = Get_KeyPair();
    }
//-----------------------------------------------------------------------------------------------------------
    // HASH encryption
//----------------------------------------------------------------------------------------------------------
    // RIPEMD 160 需要外掛
    public static String apply_RIPEMD160(String input){
        byte[] r = input.getBytes(StandardCharsets.UTF_8);
        RIPEMD160Digest d = new RIPEMD160Digest();
        d.update (r, 0, r.length);
        byte[] o = new byte[d.getDigestSize()];
        d.doFinal (o, 0);

        String hex="";
        for(byte i:o){
            hex+= Integer.toHexString(0xff & i);
        }

        return hex;
    }
    public static String applyHASH(String input,String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
//-----------------------------------------------------------------------------------------------------------
    // ECDSA encryption
//----------------------------------------------------------------------------------------------------------

    public static KeyPair Get_KeyPair(){
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
            keyPairGenerator.initialize(ecGenParameterSpec,new SecureRandom());
            keyPairGenerator.initialize(256);
            return keyPairGenerator.genKeyPair();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
    public static byte[] Get_PublicKey(KeyPair kp){ return  kp.getPublic().getEncoded();}
    public static String GetPublicKeyStr(KeyPair kp){
        byte[] bytes = kp.getPublic().getEncoded();
        return encodeHex(bytes);
    }
    public static byte[] Get_PrivateKey(KeyPair kp){ return kp.getPrivate().getEncoded();}
    public static String GetPrivateKeyStr(KeyPair kp){
        byte[] bytes = kp.getPrivate().getEncoded();
        return encodeHex(bytes);
    }

    // 数据准16进制编码
    public static String encodeHex(final byte[] data) {
        return encodeHex(data, true);
    }

    // 数据转16进制编码
    public static String encodeHex(final byte[] data, final boolean toLowerCase) {
        final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        final char[] toDigits = toLowerCase ? DIGITS_LOWER : DIGITS_UPPER;
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return new String(out);
    }
/*
    public  String Generate_ECDSA_Private_Key(PrivateKey pvt) throws IllegalAccessException {
        ECPrivateKey ecpvt = (ECPrivateKey) pvt;
        String sepvt  = adjust_To_64(ecpvt.getS().toString(16));
        return sepvt;
    }

    public static String Generate_ECDSA_Public_Key(PublicKey pub) throws IllegalAccessException {

        ECPublicKey epub = (ECPublicKey)pub;
        ECPoint pt = epub.getW();

        String sx = adjust_To_64(pt.getAffineX().toString(16)).toUpperCase();
        String sy = adjust_To_64(pt.getAffineY().toString(16)).toUpperCase();
        String bcPub =  sx + sy;

        return  bcPub;
    }
*/
    public String Generate_Address(String  publickey) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return this.apply_RIPEMD160( this.applyHASH(this.apply_RIPEMD160(publickey),"SHA-256") );
    }




//-----------------------------------------------------------------------------------------------------------
    // ECDSA Signature
//----------------------------------------------------------------------------------------------------------
    public static String Get_Signature(String input,PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, SignatureException {

        Signature ecdSign = Signature.getInstance("SHA256withECDSA");
        ecdSign.initSign(privateKey);
        ecdSign.update(input.getBytes("UTF-8"));

        byte[] signature = ecdSign.sign();
        String sig = Base64.getEncoder().encodeToString(signature);
        return sig;
    }

    public static boolean verify_Signature(JSONObject jsObject) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, UnsupportedEncodingException, SignatureException {

        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        KeyFactory kf = KeyFactory.getInstance("EC");

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(jsObject.getString("publicKey")));

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(jsObject.getString("messages").getBytes("UTF-8"));

        boolean result = ecdsaVerify.verify(Base64.getDecoder().decode(jsObject.getString("signature")));
        return result;
    }
//-----------------------------------------------------------------------------------------------------------
    // BIP39 Data hiding
//----------------------------------------------------------------------------------------------------------
    private static int CL,ENTLength; // 8,256
    public static List<String> Get_Mnemonic(String Privatekey) throws IOException, NoSuchAlgorithmException {

        String ENT = string_to_binary(Privatekey);

        ENTLength = ENT.length();
        CL = ENTLength / 32;
        String HASH = applyHASH(ENT,"SHA-256");
        String CS = string_to_binary(HASH).substring(0,CL);

        String ENT_CS = ENT+CS; // 264-bit
        List<String>GENT_CS = group_string(ENT_CS);
        List<Integer> DENT_CS = binary_to_decimal(GENT_CS);
        List<String>Mnemonic = decimal_to_Mnemonic(DENT_CS);

        return Mnemonic;
    }

    private static String string_to_binary(String privatekey){

        String binary="";

        // privatekey is hex
        char [] chrs = privatekey.toCharArray();
        for(char chr:chrs){
            int i = Integer.parseInt(String.valueOf(chr),16);
            String bin= Integer.toBinaryString(i);
            if(bin.length()<4){
                while(bin.length()<4)
                    bin = "0"+bin;
            }
            else if(bin.length()>4){
                while(bin.length()>4)
                    bin = bin.substring(1,bin.length()-1);
            }
            binary+=bin;
        }
        return binary;

    }
    private static List<String > group_string(String ENTCS){
        List<String> group = new ArrayList<>();
        String tmp="";
        char[] centcs = ENTCS.toCharArray();

        for(int i=0;i<centcs.length;i++){

            if(i%11==0 && i!=0){
                group.add(tmp);
                tmp="";
            }
            tmp+=centcs[i];
        }group.add(tmp);
        return group;
    }
    private static List<Integer> binary_to_decimal(List<String> binary){
        List<Integer> decimal = new ArrayList<>();

        for(String bin:binary){
            int de = Integer.parseInt(bin,2);
            decimal.add(de);
        }
        return decimal;
    }
    private static List<String> decimal_to_Mnemonic(List<Integer> integers) throws IOException {
        List<String> mnemonic = new ArrayList<>();
        for(int t:integers){
            String line = Files.readAllLines(Paths.get("bip-0039/english.txt")).get(t);
            mnemonic.add(line);
        }
        return mnemonic;
    }

    public static String Reverse_Mnemonic(List<String> mnemonic) throws FileNotFoundException {

        List<Integer>DENT_CS = Mnemonic_to_decimal(mnemonic);
        List<String>GENT_CS = decimal_to_binary(DENT_CS);

        String ENT_CS = ungroup_string(GENT_CS);
        String ENT = ENT_CS.substring(0,ENTLength);
        String privateKey = binary_to_string(ENT);
        return privateKey;
    }
    private static List<Integer> Mnemonic_to_decimal(List<String>mnemonic) throws FileNotFoundException {
        List<Integer>DEN_CS=new ArrayList<>();
        File englishtxt = new File("bip-0039/english.txt");
        for(String mn:mnemonic){
            Scanner scanner = new Scanner(englishtxt);
            int i=0;
            while(scanner.hasNextLine()){
                String m = scanner.nextLine();
                if(m.equals(mn))
                    DEN_CS.add(i);
                i++;
            }
        }
        return DEN_CS;
    }
    private static List<String> decimal_to_binary(List<Integer>integers){
        List<String> binary = new ArrayList<>();
        for(int t:integers){
            String bin = Integer.toBinaryString(t);
            if(bin.length()<11){
                while(bin.length()<11)
                    bin = "0"+bin;
            }
            else if(bin.length()>11){
                while(bin.length()>11)
                    bin = bin.substring(1,bin.length()-1);
            }
            binary.add(bin);
        }
        return binary;
    }
    private static String ungroup_string(List<String>GENTCS){
        String entcs = "";
        for(String e:GENTCS){
            entcs+=e;
        }
        return entcs;
    }
    private static String binary_to_string(String ENT){
        String privatekey="";
        char[] chrs = ENT.toCharArray();

        String tmp="";
        for(int i=0;i<chrs.length;i++){
            // 4個4個抓
            if(i%4==0 && i!=0){
                privatekey += Integer.toHexString(Integer.parseInt(tmp,2));
                tmp="";
            }
            tmp+=chrs[i];
        }privatekey+=Integer.toHexString(Integer.parseInt(tmp,2));
        return privatekey;
    }

/*
//-----------------------------------------------------------------------------------------------------------
    // AES encryption
//----------------------------------------------------------------------------------------------------------
    private static byte[] encode(Key key,byte[] srcBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        return cipher.doFinal(srcBytes);
    }

    private static byte[] decode(Key key,byte[] srcBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE,key);
        return  cipher.doFinal(srcBytes);
    }

    private static String encode_Base64(Key key,byte[] srcBytes) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return new String (Base64.encode(encode(key,srcBytes)));
    }

    private static byte[] decode_Base64(Key key, String base64String ) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return decode(key,Base64.decode(base64String));
    }

    public static String Priv_En_apply_AES(PrivateKey private_key, String msg) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        // 私鑰加密
        String base64str = encode_Base64(private_key,msg.getBytes());
        return base64str;
    }
    public static String  Pub_De_apply_AES(PublicKey public_key,String ciphertext) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        // 公鑰解密
        byte[] deBytes = decode_Base64(public_key,ciphertext);
        return new String(deBytes);
    }


    public static String Priv_De_apply_AES(PrivateKey private_key, String ciphertext) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        // 私鑰解密
        byte[] deByte = decode_Base64(private_key,ciphertext);
        return new String(deByte);
    }
    public static String  Pub_En_apply_AES(PublicKey public_key,String msg) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        // 公鑰加密
        String base64str = encode_Base64(public_key,msg.getBytes());
        return new String(base64str);
    }
 */

}
