package Users.Node;

import BlockChain.Block;
import BlockChain.Blockchain;

import BlockChain.Transaction;
import Users.SocketAction;
import Users.UserFunctions;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.Time;
import java.util.*;



public class NodeUser {


    static String remoteHost="";

    public static Blockchain blockchain = new Blockchain();
    public static ArrayList<Block> bufferChain = new ArrayList<>();


    static InetAddress master;
    static Timer timer=new Timer();


    public static void main(String[] args) throws IOException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalAccessException, BadPaddingException, SignatureException, InvalidAlgorithmParameterException, IllegalBlockSizeException, InterruptedException {
        String host;
        Scanner scanner = new Scanner(System.in);


        System.out.print("創建區塊鏈 or 繼承區塊練(create / load):\t");
        String option = scanner.nextLine().strip();
        if("load".equals(option)){

            // setting remote node
            System.out.println("須輸入遠端節點:");
            System.out.print("\tip:\t");
            remoteHost = scanner.nextLine().strip() ;
            // 測試連縣
            if(SocketAction.TestConnection(remoteHost)){
                while(true){

                        // 連線到遠端節點要取新區塊鏈

                        ArrayList<Block> newChain = SocketAction.Connection_to_Node(remoteHost,SocketAction.SERVER_PORT,blockchain.get_All_Blocks_JSON(),blockchain.blockchain.size());
                        if( newChain ==null) {
                            timer = Update_Timer_Blockchain(blockchain.get_All_Blocks_JSON(),timer,0);
                            break;
                        }
                        else{
                            System.out.println("成功讀取區塊鏈");

                            blockchain.blockchain = newChain;
                            // 更改 buffer block previous hash
                            bufferChain.add(MakeEmptyBlock(blockchain.blockchain.get(blockchain.blockchain.size()-1).hash,newChain.size()));
                            // 設定 master
                            master=InetAddress.getByName(remoteHost);
                            // 設定 新排成
                            timer = UserFunctions.SetTimer(remoteHost,timer,newChain.toString(),newChain.size());

                            break;
                        }
                }
              //  UserFunctions.CancelTimer(timer);
            }
            else
                System.exit(-15);

        }

        System.out.println("輸入節點:");
        System.out.print("\tip:\t");
        host = scanner.nextLine().strip();

        // initialize procedure
        if(option.equals("create")){
            Block block = MakeEmptyBlock("0",blockchain.blockchain.size());
            bufferChain.add(block);
            master=null;
        }


        TurnOn_Node_Server(host,SocketAction.SERVER_PORT);

    }

    private static void TurnOn_Node_Server(String host,int port) throws IOException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalAccessException, BadPaddingException, SignatureException, InvalidAlgorithmParameterException, IllegalBlockSizeException, InterruptedException {

        System.out.println("開啟節點伺服器.....");
        InetAddress addr = InetAddress.getByName(host);
        ServerSocket socket =new ServerSocket(port,50,addr);
        System.out.println("節點伺服器開啟完畢");


        while(true) {
            final Socket clientSocket=socket.accept();

            if(bufferChain.size()==0){
                Block block;
                block = MakeEmptyBlock(blockchain.blockchain.get(blockchain.blockchain.size()-1).hash,blockchain.blockchain.size()+1);
                bufferChain.add(block);
            }


            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Get block number
                    int blockNumber=blockchain.blockchain.size();

                    System.out.println("接收新連線: " + clientSocket.getInetAddress());
                    String command = "";

                    try {
                        // get command
                        command = SocketAction.SocketRead(clientSocket);

                        // 要先有transaction 才能傳
                        if ("ask-block".equals(command)) {
                            System.out.print("\t要求節點=>\t");
                            if(bufferChain.get(0).transactions.size()==0){
                                SocketAction.SocketWrite("No transaction", clientSocket);
                                System.out.println("No transaction in block");
                                throw new IOException();
                            }else
                                System.out.println(bufferChain.get(0).get_Block_to_Json(blockNumber));

                            // send block no
                            SocketAction.SocketWrite(String.valueOf(blockNumber), clientSocket);

                            // send block
                            Thread.sleep(SocketAction.TIME_DELAY);
                            SocketAction.SocketWrite(bufferChain.get(0).get_Block_to_Json(blockNumber), clientSocket);
                        }
                        else if ("mine".equals(command)) { // 提交區塊
                            SocketAction.SocketWrite(String.valueOf(blockNumber), clientSocket);

                            // receive new block
                            String jblock = SocketAction.SocketRead(clientSocket);
                            if (jblock.equals("no"))
                                throw new IOException();

                            Block new_block = UserFunctions.Convert2Block(jblock, blockNumber);
                            System.out.println("新區塊: " + new_block.get_Block_to_Json(blockNumber));

                            // recieve difficulty
                            String res_difficulty = SocketAction.SocketRead(clientSocket);
                            // set difficulty
                            Blockchain.difficulty = Integer.parseInt(res_difficulty);


                            Boolean result=false;
                            // check if new block valid, Genesis不需要檢查
                            if(blockNumber==0)
                                result=true;
                            else
                                result = blockchain.Is_Block_current(new_block);

                            if (result) {
                                blockchain.Add_Block_to_Chain(new_block);

                                bufferChain.remove(0);
                                Block newBufferBlock = new Block(new_block.hash,Blockchain.difficulty);
                                bufferChain.add(newBufferBlock);

                                SocketAction.SocketWrite("yes", clientSocket);
                            } else {
                                SocketAction.SocketWrite("no", clientSocket);
                            }

                        }
                        else if("commit".equals(command)){

                            // get transaction String
                            String Stransaction = SocketAction.SocketRead(clientSocket);
                            Transaction t = UserFunctions.Convert2Transaction(Stransaction);

                            String result="";
                            if(bufferChain.get(0).transactions.size() < Block.block_limitation)
                                // Add transaction to block
                                bufferChain.get(0).Add_Transaction(t);
                                // It mean block is full, needs to be mine
                            else
                                result="exceed length";
                            // send result
                            SocketAction.SocketWrite(result, clientSocket);
                        }
                        else if("balance".equals(command)){
                            System.out.print("\t要求餘額=>\t");

                            // get Address
                            String Address=SocketAction.SocketRead(clientSocket);

                            // send balance
                            double balance = CalculateBalance(Address);
                            SocketAction.SocketWrite(String.valueOf(balance), clientSocket);

                            System.out.println(balance);
                        }
                        else if("ask-blockchain".equals(command)){
                            System.out.print("\t要求區塊鏈=>\n\t\t");

                            if(blockchain.blockchain.size()==0){
                                SocketAction.SocketWrite("No chain in this node", clientSocket);
                                throw new IOException();
                            }else{
                                SocketAction.SocketWrite("I have chain", clientSocket);
                            }

                            // 確定 自己的 blockchain size 大於 目前的 number再傳送
                            // Get blockchain size
                            int clientBlockchain_Size = Integer.parseInt(SocketAction.SocketRead(clientSocket));
                            int localsize = blockchain.blockchain.size();

                            System.out.println("client size: "+clientBlockchain_Size);
                            System.out.println("\t\tlocal size:"+blockchain.blockchain.size());

                            // send response
                            if(clientBlockchain_Size>localsize){
                                // client longer
                                SocketAction.SocketWrite("Ur chain longer", clientSocket);
                                // 須與該節點要求區塊練

                                // get new blockchain
                                String strBlockchain = SocketAction.SocketRead(clientSocket);
                                System.out.println("get new chain\n\t"+strBlockchain);

                                // get new blockchain size
                                int client_blockSize = Integer.parseInt(SocketAction.SocketRead(clientSocket));
                                System.out.println("new size: "+client_blockSize);

                                // 設定排程 固定跟client要求區塊鏈
                                System.out.println("變更主節點: "+clientSocket.getInetAddress());
                                timer = Update_Timer_Blockchain(strBlockchain,timer,clientBlockchain_Size);


                                // 確定此時有無 主節點存在
                                /*
                                 *  有: 取消自身timer 回傳給主節點要求更換
                                 *  無: 掠過直接設定主節點
                                 * */
                                if(master!=null){
                                    UserFunctions.CancelTimer(timer);
                                    Socket mastersocket = new Socket(master,port);
                                    Thread.sleep(SocketAction.TIME_DELAY);
                                    SocketAction.SocketWrite("changeMaster", mastersocket);
                                    mastersocket.close();
                                }

                                // 設定master
                                master = clientSocket.getInetAddress();

                                // 設定鏈
                                blockchain.blockchain=UserFunctions.Convert2Blockchain(strBlockchain,client_blockSize);

                                // 更改 buffer block previous hash
                                bufferChain.get(0).previous_hash=blockchain.blockchain.get(client_blockSize-1).hash;
                                throw new IOException();
                            }
                            else if(clientBlockchain_Size==localsize){
                                SocketAction.SocketWrite("same length", clientSocket);
                                throw new IOException();
                            }
                            else {
                                SocketAction.SocketWrite("longer", clientSocket);

                                // Send blockchain
                                Thread.sleep(100);
                                String strBlockchain = blockchain.get_All_Blocks_JSON();
                                SocketAction.SocketWrite(strBlockchain, clientSocket);

                                Thread.sleep(100);
                                // 如果自己的鏈比較長=> send blockchain size
                                SocketAction.SocketWrite(String.valueOf(localsize), clientSocket);
                            }

                        }
                        else if("changeMaster".equals(command)){
                            System.out.println("更換主節點");
                            master = clientSocket.getInetAddress();
                            timer = UserFunctions.SetTimer(remoteHost,timer,blockchain.get_All_Blocks_JSON(),blockNumber);
                        }

                        else if("get-blockchain".equals(command)){
                            System.out.println("\t要求區塊鏈\t");
                            SocketAction.SocketWrite(blockchain.get_All_Blocks_JSON(), clientSocket);
                            Thread.sleep(100);
                            SocketAction.SocketWrite(String.valueOf(blockchain.blockchain.size()),clientSocket);
                        }
                        else if("test".equals(command)){
                            System.out.println("\t測試連線\t");
                        }
                    }
                    catch (IOException | NoSuchAlgorithmException | InvalidKeyException | IllegalAccessException | SignatureException | InterruptedException e) {
                        try {
                            clientSocket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                    finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
            Thread.sleep(1000);
        }
    }


    private static Block MakeEmptyBlock(String previouhash,int No) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalAccessException, InvalidKeyException, BadPaddingException, SignatureException, IllegalBlockSizeException {
        Block block = new Block(previouhash,Blockchain.difficulty);
        block.No =  No;
        return block;
    }

    private static double CalculateBalance(String address) throws IllegalAccessException {
        double balance = 0;
        int BlockchainSize = blockchain.blockchain.size();

        JSONObject Jblockchain = new JSONObject(blockchain.get_All_Blocks_JSON()).getJSONObject("Blockchain");



        for (int i=0;i<BlockchainSize;i++) { // through blockchain
            JSONObject Jblock = Jblockchain.getJSONObject("Block-"+i);
            String miner = Jblock.getString("miner");

            if(miner.equals(address))
                balance+=Block.miner_reward;

            JSONObject Jtransaction = Jblock.getJSONObject("Transactions");
            for(int j=0;j<Block.block_limitation;j++){ // through inside block
                try{
                    JSONObject Jtxn = Jtransaction.getJSONObject("txn-"+j);
                    String receiver = Jtxn.getString("receiver");
                    String sender = Jtxn.getString("sender");
                    double amount = Jtxn.getDouble("amount");
                    double fee = Jtxn.getDouble("fee");

                    if(receiver.equals(address)){
                        balance+=fee*Math.pow(10,-5)+amount ; // 1 fee = 10^-5 btc
                    }
                    if(sender.equals(address)){
                        balance-=fee*Math.pow(10,-5)+amount;
                    }

                }catch (Exception e){
                    break;
                }
            }
        }
        return balance;
    }

    private static Timer Update_Timer_Blockchain(String strBlockchain, Timer timer, int chainSize){
        return UserFunctions.SetTimer(remoteHost,timer,strBlockchain,chainSize);
    }
}
