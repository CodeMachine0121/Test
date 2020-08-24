package BlockChain;

import Util.MerkleTree;
import Util.StringUtil;

import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class Block {
        public static Logger logger = Logger.getLogger(Block.class.getName());

        public  String hash;
        public  String previous_hash;
        public String minerAddr;
        public  int nonce;
        public  int difiifulty;
        public  long timestamp;
        public Miner miner;
        public static  int miner_reward=30;
        public  String data;
        public static int block_limitation=2;
        public  ArrayList<Transaction> transactions = new ArrayList<Transaction>();
        public String MerkletreeRoot;
        public String signature;
        public int No;

        protected double fee=0;

        public Block(String previous_hash,int difficulty) throws UnsupportedEncodingException, NoSuchAlgorithmException {

                this.previous_hash = previous_hash;
                this.hash="";
                this.nonce=0;
                this.timestamp = new Date().getTime();
                this.data = "";
                this.difiifulty = difficulty;
                this.MerkletreeRoot = "";
                this.fee=0;
                this.minerAddr = "";
                this.signature="";
        }
// mine the block
        public double calculateHash(Miner miner) throws UnsupportedEncodingException, NoSuchAlgorithmException, IllegalAccessException, NoSuchPaddingException, SignatureException, InvalidKeyException {

                String calculatedhash;


                this.data = Transactions_to_String();
                this.miner = miner;

                long start = new Date().getTime();
                double cost_time;

                for (Transaction t:this.transactions) {
                        this.fee+= t.fee;
                }
                System.out.println("calculate..");
                while(true){
                        calculatedhash = StringUtil.applyHASH(
                                this.previous_hash+ timestamp +data + nonce,"SHA-256"
                        );
                        // check hash
                        char[] chrs = calculatedhash.toCharArray();
                        boolean flag=true;
                        for(int i=0;i<difiifulty;i++)
                                if(chrs[i]!='0')
                                        flag=false;
                        if(flag){
                                this.hash = calculatedhash;
                                this.minerAddr=miner.address;
                                cost_time = Math.round((new Date().getTime() - start) / 1000);
                                this.signature = miner.MakeBlockSignature(this.hash);
                                logger.info("Hash found: "+this.hash+" @ difficulty: "+this.difiifulty+",  time cost: " + cost_time );
                                break;
                        }
                        else
                                nonce++;
                }
                System.out.println("Finish mining");
                return cost_time;
        }

        // Add single transaction to transactions
        public void Add_Transaction(Transaction transaction){
                transactions.add(transaction);
               // System.out.println("Now "+(transactions.size()-1)+" transactions");
                logger.info("Add one transaction to BlockChain.Block ");
        }

        // Function to collect all transactions' hash
        private  List<String> get_Transaction_hash () throws IllegalAccessException {
                List<String> list =new  ArrayList<String>();

                for(int i =0;i<this.transactions.size();i++){
                        // BlockChain.Transaction object
                        list.add(this.transactions.get(i).transaction_hash);
                }
                return list;
        }

        public String Transactions_to_String() throws IllegalAccessException {
                List<String> list = get_Transaction_hash();
                String hashes="";
                for(int i=0;i<list.size();i++)
                        hashes+=list.get(i);
                return hashes;
        }
        // Function to check is transaction valid
        public Boolean Is_transactions_valid() throws InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, IllegalAccessException {

                Transaction transaction = this.transactions.get(transactions.size()-1);

                JSONObject  jsonObject = transaction.Transaction_to_JSON();

                return StringUtil.verify_Signature(jsonObject);
        }

        public void set_MerkelTree_Root(List<Transaction> transactionList) throws NoSuchAlgorithmException, IllegalAccessException, NoSuchPaddingException, UnsupportedEncodingException {
                // 打包前使用
                List<String> strtrans = get_Transaction_hash();
                MerkleTree merkleTree = new MerkleTree(strtrans);
                merkleTree.init_Merkletree();
                this.MerkletreeRoot = merkleTree.root;
        }



        public String get_Block_to_Json(int blockno) throws IllegalAccessException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, UnsupportedEncodingException {

                JSONObject json = new JSONObject();
                json.put("No",blockno);
                json.put("previous hash",previous_hash);
                json.put("hash",hash);
                json.put("difficulty",difiifulty);
                json.put("miner",minerAddr);
                json.put("MerkleTree",this.MerkletreeRoot);

                JSONObject jtxn = new JSONObject();
                for(int i=0;i<transactions.size();i++){
                        jtxn.put("txn-"+i,transactions.get(i).Transaction_to_JSON());
                }
                json.put("Transactions",jtxn);
                json.put("signature",this.signature);
                JSONObject j = new JSONObject();
                if(blockno>=0)
                        j.put("Block-"+blockno,json);
                else
                        j.put("Block"+blockno,json);

                return j.toString();
        }


}
