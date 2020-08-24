package BlockChain;


import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Blockchain {

    public static Logger logger = Logger.getLogger(Blockchain.class.getName());

    public static int difficulty=3;

    public static long block_time=3; //10 secs
    public static long adjustment_block_size =5; // how often adjustment difficulty
    public static ArrayList<Block> blockchain;

    public Blockchain(){
        blockchain = new ArrayList<Block>();
    }

    // Add Genesis BlockChain.Block
    public boolean Add_First_Block(Block first_block){

            this.blockchain.add(first_block);
            logger.info("The genesis BlockChain.Block has been added in the blockchain");
            return true;
    }
    // Add new BlockChain.Block
    public boolean Add_Block_to_Chain(Block new_block) throws UnsupportedEncodingException, NoSuchAlgorithmException {
            if(new_block.MerkletreeRoot==""){
                logger.warning("The  BlockChain.Block has no Util.MerkleTree Root");
                return false;
            }


            this.blockchain.add(new_block);

            logger.info("BlockChain.Block "+(blockchain.size()-1)+" has been added in the blockchain");

            return true;

    }
    public Boolean Is_Chain_valid() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Block previous_block;
        Block current_block;
        for(int i=1;i<this.blockchain.size();i++){
            current_block = this.blockchain.get(i);
            previous_block = this.blockchain.get(i-1);

            if(!current_block.previous_hash.equals(previous_block.hash)){
                logger.info("BlockChain.Block " + i +" is invalid");
                return false;
            }
            /*if(!current_block.hash.equals(current_block.calculateHash(current_block))) {
                logger.info("Bock " + i + " block hash is different");
                return false;
            }*/

        }
        logger.info("BlockChain.Blockchain has been checked, It's all valid ");
        return true;
    }
    // Use to check that is the  newest block current
    public  Boolean Is_Block_current(Block new_block){
        String previous_hash = this.blockchain.get(blockchain.size()-1).hash;
        if(new_block.previous_hash.equals(previous_hash))
            return true;
        else
            return false;
    }

    public void Run_through_Blockchain(){
        for(int i=0;i<this.blockchain.size();i++){
            System.out.println("BlockChain.Block "+i+": "+this.blockchain.get(i).hash );
        }
    }

    // Function to adjustment difficulty
    public  static int Adjustment_Difficulty(double time){
        //則難度按比例增加，但最大不能超過4倍。
        //
        //依據的公式如下：
        //
        //下一周期的難度係數=當前周期的難度係數 * (20160分鐘÷當前周期2016個區塊的實際出塊時間)
        double t = difficulty * Math.round ((block_time/time));
      /*  System.out.println("t "+t);
        System.out.println("blockchain size: "+this.blockchain.size());
        System.out.println("time: "+time);*/
        if (time >= block_time){
            System.out.println("difficulty down");
            difficulty-=1;
        }
        else{
            System.out.println("difficulty up");
            difficulty+=1;
        }

        return difficulty;
    }


    public String get_All_Blocks_JSON() throws IllegalAccessException {

        JSONObject Jblocks = new JSONObject();

        for(int i=0;i<blockchain.size() ;i++){
            Block block = blockchain.get(i);

            JSONObject Jblock = new JSONObject();
            Jblock.put("previous hash",block.previous_hash);
            Jblock.put("No",block.No);
            Jblock.put("previous hash",block.previous_hash);
            Jblock.put("hash",block.hash);
            Jblock.put("difficulty",block.difiifulty);
            Jblock.put("miner",block.minerAddr);
            Jblock.put("MerkleTree",block.MerkletreeRoot);



            JSONObject txn = new JSONObject();
            for(int j=0;j<block.transactions.size();j++) {
                txn.put("txn-" + j, block.transactions.get(j).Transaction_to_JSON());
            }
            Jblock.put("signature",block.signature);
            Jblock.put("Transactions",txn);
            Jblocks.put("Block-"+i,Jblock);
        }


        JSONObject bc = new JSONObject();
        bc.put("Blockchain",Jblocks);
        return bc.toString();
    }
}
