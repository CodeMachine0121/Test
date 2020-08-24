package BlockChain;

import Util.StringUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.Array;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Miner {

    public String address;
    public String publicKey;
    public String privateKey;
    public double balance;
    private StringUtil stringUtil;
    private Logger logger = Logger.getLogger(Miner.class.getName());

    public List<String> mnemonic;
    private KeyPair kp;
    public Miner() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalAccessException, NoSuchPaddingException, IOException {
        stringUtil = new StringUtil();
        this.kp = stringUtil.keypair;
        this.publicKey = stringUtil.GetPublicKeyStr(kp);
        this.privateKey = stringUtil.GetPrivateKeyStr(kp);

        this.address = stringUtil.Generate_Address(this.publicKey);
        this.balance = -1;

        //OutputKeys(this.publicKey,this.privateKey);
        this.mnemonic = stringUtil.Get_Mnemonic(this.privateKey);
    }
//-----------------------------------------------------------------------------------------------------------
    // transaction function
//----------------------------------------------------------------------------------------------------------
    public Transaction Make_Transaction(String sender, String receiver, double amount, double fee, String messages) throws NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, IllegalBlockSizeException, InvalidKeyException, SignatureException, BadPaddingException, IllegalAccessException {
        String signature = stringUtil.Get_Signature(messages,kp.getPrivate());
        Transaction t =  new Transaction(sender,receiver,amount,fee,messages,stringUtil.GetPublicKeyStr(kp),signature);
        return t;
    }

    public String MakeBlockSignature(String data) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return StringUtil.Get_Signature(data,kp.getPrivate());
    }
//-----------------------------------------------------------------------------------------------------------
    // mining function
//----------------------------------------------------------------------------------------------------------

    public double Mining_Mode(Block block) throws UnsupportedEncodingException, NoSuchAlgorithmException, IllegalAccessException, NoSuchPaddingException, SignatureException, InvalidKeyException {
       double t = block.calculateHash(this);
       block.set_MerkelTree_Root(block.transactions);
       return t;
    }


//-----------------------------------------------------------------------------------------------------------
    // restoring function
//----------------------------------------------------------------------------------------------------------

    public  static  Boolean Save_Mnemonic (String path, List<String>Mnemonic,String publicKey) throws IOException {
        String m="";
        for(String mn : Mnemonic){
            m+=mn+" ";
        }

        File file = new File(path+"/mnemonic.txt");
        if(file.createNewFile()){
            System.out.println("Create file....");
            FileWriter writer = new FileWriter(path+"/mnemonic.txt");
            writer.write(m+"\n");
            writer.write(publicKey);
            writer.close();
            return true;
        }else{
            System.out.println("There's already an file in the folder");
            return false;
        }

    }

    public static Miner  Load_Mnemonic(String path) throws IOException, IllegalAccessException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException {
        File file = new File(path+"/mnemonic.txt");
        String data="",data2="";
        try{
            Scanner scanner = new Scanner(file);
            data = scanner.nextLine();
            data2 = scanner.nextLine();
        }catch (Exception e){
            System.out.println("No file found");
            return null;
        }
        List<String> mnemonic = Arrays.asList(data.split(" "));
        Miner user = new Miner();
        StringUtil stringUtil = new StringUtil();
        user.privateKey = stringUtil.Reverse_Mnemonic(mnemonic);
        user.publicKey = data2;
        user.address = stringUtil.Generate_Address(user.publicKey);
        System.out.println("Loading user successfully");
        return user;
    }


}
