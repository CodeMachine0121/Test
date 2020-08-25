package Users;

import BlockChain.Block;
import BlockChain.Miner;
import BlockChain.Transaction;

import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static BlockChain.Miner.Load_Mnemonic;

public class UserFunctions {
    static Scanner scanner = new Scanner(System.in);

    public static Miner loadKey() throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalAccessException, InvalidKeySpecException {

        System.out.print("#Create or Load: ");
        String option = scanner.nextLine().strip().toLowerCase();

        String path="";
        Miner newMiner;

        if("create".equals(option)) {

            newMiner = new Miner();
            System.out.print("Enter the path to save mnemonic: ");
            if (newMiner.Save_Mnemonic(scanner.nextLine(), newMiner.mnemonic, newMiner.publicKey))
                return newMiner;
            else
                return null;
        } else if("load".equals(option)){
            System.out.print("enter the path, saved the mnemonic\n\t:");
            path = scanner.nextLine().strip();
            return Load_Mnemonic(path);
        }else{
            System.out.println("unKnown command");
        }

        return null;
    }

    // For wallet users
    public static Transaction makeTransaction(Miner user) throws NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, UnsupportedEncodingException, SignatureException, IllegalAccessException, InvalidKeyException {

        // input information of transaction
        String sender = user.address;
        System.out.print("\tReceiver:\t");
        String receiver = scanner.next().strip();
        System.out.print("\tMessage:\t");
        String messages = scanner.next().strip();
        System.out.print("\tAmount:\t");
        double amount=scanner.nextDouble();
        System.out.print("\tFee:\t");
        double fee=scanner.nextDouble();

        return user.Make_Transaction(sender,receiver,amount,fee,messages);
    }

    public static void List_Transaction(List<Transaction> transactions){

        System.out.println("**** ALL Transactions ****");
        int e=0;
        for (Transaction t:transactions) {
            System.out.println("No\t"+(e++));
            System.out.println("Receiver:\t"+t.receiver);
            System.out.println("Message:\t"+t.Get_messages());
            System.out.println("Amount:\t"+t.Get_amount());
            System.out.println("Fee:\t"+t.fee);
            System.out.println("***********************\n");
        }

    }




    // All user used

    public static ArrayList<Block> Convert2Blockchain(String strBlockchain, int chainSize) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        JSONObject jsonBlockchain = new JSONObject(strBlockchain).getJSONObject("Blockchain");
        ArrayList<Block> newchain = new ArrayList<>();

        for(int i=0;i<chainSize;i++){
            try{
                Block block = Convert2Block(jsonBlockchain.toString(),i);
                newchain.add(block);
            }catch (Exception e){
                break;
            }
        }
        return newchain;
    }
    public static Block Convert2Block(String strBlock, int blockNumber) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        JSONObject jsonBlock = new JSONObject(strBlock).getJSONObject("Block-"+blockNumber);

        Block block = new Block(jsonBlock.getString("previous hash"),jsonBlock.getInt("difficulty"));

        JSONObject jsonTransactions = jsonBlock.getJSONObject("Transactions");
        JSONObject jsonSingleTransaction ;

        for(int i = 0; i< Block.block_limitation; i++){
            try {
                jsonSingleTransaction= jsonTransactions.getJSONObject("txn-" + i);
                block.Add_Transaction( Convert2Transaction( jsonSingleTransaction.toString() ) );
            } catch (Exception e){
                break;
            }
        }
        block.No = jsonBlock.getInt("No");
        block.hash = jsonBlock.getString("hash");
        block.signature = jsonBlock.getString("signature");
        block.MerkletreeRoot = jsonBlock.getString("MerkleTree");
        block.minerAddr = jsonBlock.getString("miner");
        return  block;
    }
    public static Transaction Convert2Transaction(String Strasaction) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        JSONObject jtransaction = new JSONObject(Strasaction);

        String sender = jtransaction.getString("sender");
        String receiver = jtransaction.getString("receiver");
        String message = jtransaction.getString("messages");
        double amount = jtransaction.getDouble("amount");
        double fee = jtransaction.getDouble("fee");
        String pulickey = jtransaction.getString("publicKey");
        String txnsign = jtransaction.getString("txnsign");

        return new Transaction(sender,receiver,amount,fee,message,pulickey,txnsign);
    }

    public static void printOutBlockchain(String strBlockchain,int chainSize){

        JSONObject jsonBlockchain = new JSONObject(strBlockchain).getJSONObject("Blockchain");

        for(int i=0;i<chainSize;i++){
            printOutBlock(jsonBlockchain.toString(),i);

        }
        System.out.println();

    }
    public  static void printOutBlock(String strBlock,int blockNumber){

        JSONObject jsonBlock = new JSONObject(strBlock).getJSONObject("Block-"+blockNumber);

        System.out.println("\t{Block-"+blockNumber);

        System.out.println("\t\tHash:"+jsonBlock.getString("hash"));
        System.out.println("\t\tPrevious Hash:"+jsonBlock.getString("previous hash"));
        System.out.println("\t\tMiner: "+jsonBlock.getString("miner"));
        System.out.println("\t\tSignature:"+jsonBlock.getString("signature"));
        System.out.println("\t\tMerkleTree:"+jsonBlock.getString("MerkleTree"));

        System.out.println("\t\tTransactions:{");
        JSONObject jsonTransactions = jsonBlock.getJSONObject("Transactions");
        JSONObject jsonSingleTransaction;
        for(int i=0;i<Block.block_limitation;i++){
            try{
                jsonSingleTransaction = jsonTransactions.getJSONObject("txn-"+i);
                System.out.println("\t\t\ttxn-"+i+": {");
                System.out.println("\t\t\t\tSender: "+jsonSingleTransaction.getString("sender"));
                System.out.println("\t\t\t\tReceiver: "+jsonSingleTransaction.getString("receiver"));
                System.out.println("\t\t\t\tMessage: "+jsonSingleTransaction.getString("messages"));
                System.out.println("\t\t\t\tAmount: "+jsonSingleTransaction.getDouble("amount"));
                System.out.println("\t\t\t\tFee: "+jsonSingleTransaction.getDouble("fee"));
                System.out.println("\t\t\t\tPublicKey: "+jsonSingleTransaction.getString("publicKey"));
                System.out.println("\t\t\t\ttxnSignature: "+jsonSingleTransaction.getString("txnsign"));
                System.out.println("\t\t\t}");
            }catch (Exception e){
                break;
            }
        }

        System.out.println("\t\t}");
        System.out.println("\t}");
    }

}
