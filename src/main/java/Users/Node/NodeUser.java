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
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;

import static Users.SocketAction.SocketRead;

public class NodeUser {

    static String host=null;
    static int port = 8000;

    static InetAddress Remotehost=null;

    public static Blockchain blockchain;
    public static ArrayList<Block> bufferChain = new ArrayList<>();


    static InetAddress master=null;
    static Timer timer=new Timer();


    public static void main(String[] args) throws IOException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalAccessException, BadPaddingException, SignatureException, InvalidAlgorithmParameterException, IllegalBlockSizeException, InterruptedException {
        blockchain = new Blockchain();

        Scanner scanner = new Scanner(System.in);

        System.out.print("創建區塊鏈 or 繼承區塊練(create / load):\t");
        String option = scanner.nextLine();
        if(option.equals("load")){
            // setting remote node
            System.out.println("須輸入遠端節點:");

            System.out.print("\tip:\t");
            String remotehost = scanner.nextLine();
            Remotehost = InetAddress.getByName(remotehost);
            // 測試連縣
            if(SocketAction.TestConnection(remotehost)){
                // 連線到遠端節點要取新區塊鏈
                Connection_to_Node(Remotehost,port);
            }else
                System.exit(-15);
        }

        System.out.println("輸入節點:");
        System.out.print("\tip:\t");
        host = scanner.nextLine();

        if(option.equals("create")){
            Block block = MakeEmptyBlock("0",blockchain.blockchain.size());
            bufferChain.add(block);
        }


        TurnOn_Node_Server(host,port);

    }

    private static void TurnOn_Node_Server(String host,int port) throws IOException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalAccessException, BadPaddingException, SignatureException, InvalidAlgorithmParameterException, IllegalBlockSizeException, InterruptedException {

        System.out.println("開啟節點伺服器.....");
        InetAddress addr = InetAddress.getByName(host);
        ServerSocket socket =new ServerSocket(port,50,addr);
        System.out.println("節點伺服器開啟完畢");

        timer = null;


        if(Remotehost!=null)
            timer=SetTimer(Remotehost);

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
                    int BlockNo=blockchain.blockchain.size();

                    System.out.println("接收新連線: " + clientSocket.getInetAddress());
                    String cmd = null;
                    try {
                        // get command
                        cmd = SocketRead(clientSocket);

                        // 要先有transaction 才能傳
                        if (cmd.equals("ask-block")) {

                            System.out.print("\t要求節點=>\t");
                            if(bufferChain.get(0).transactions.size()==0){
                                SocketAction.SocketWrite("No transaction", clientSocket);
                                System.out.println("No transaction in block");
                                throw new IOException();
                            }else
                                System.out.println(bufferChain.get(0).get_Block_to_Json(BlockNo));

                            // send block no
                            SocketAction.SocketWrite(String.valueOf(BlockNo), clientSocket);

                            // send block
                            Thread.sleep(100);
                            SocketAction.SocketWrite(bufferChain.get(0).get_Block_to_Json(BlockNo), clientSocket);
                        }
                        else if (cmd.equals("mine")) { // 提交區塊
                            SocketAction.SocketWrite(String.valueOf(BlockNo), clientSocket);

                            // receive new block
                            String jblock = SocketRead(clientSocket);
                            if (jblock.equals("no"))
                                throw new IOException();

                            Block new_block = UserFunctions.Convert2Block(jblock, BlockNo);
                            System.out.println("新區塊: " );
                            UserFunctions.printOutBlock(jblock,BlockNo);


                            // recieve difficulty
                            String res_difficulty = SocketRead(clientSocket);
                            // set difficulty
                            Blockchain.difficulty = Integer.parseInt(res_difficulty);


                            Boolean result=false;
                            // check if new block valid, Genesis不需要檢查
                            if(BlockNo==0)
                                result=true;
                            else
                                result = blockchain.Is_Block_current(new_block);

                            if (result) {
                                blockchain.Add_Block_to_Chain(new_block);

                                bufferChain.remove(0);
                                Block newBufferBlock = new Block(new_block.hash,Blockchain.difficulty);
                                bufferChain.add(newBufferBlock);

                                SocketAction.SocketWrite(blockchain.get_All_Blocks_JSON(), clientSocket);
                            } else {
                                SocketAction.SocketWrite("no", clientSocket);
                            }

                        }
                        else if(cmd.equals("commit")){

                            // get transaction String
                            String Stransaction = SocketRead(clientSocket);
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
                        else if(cmd.equals("balance")){
                            System.out.print("\t要求餘額=>\t");

                            // get Address
                            String Address= SocketRead(clientSocket);

                            // send balance
                            double balance = CalculateBalance(Address);
                            SocketAction.SocketWrite(String.valueOf(balance), clientSocket);

                            System.out.println(balance);
                        }
                        else if(cmd.equals("ask-blockchain")){
                            Thread.sleep(100);
                            System.out.print("\t要求區塊鏈=>\n\t\t");

                            if(blockchain.blockchain.size()==0){
                                SocketAction.SocketWrite("No chain in this node", clientSocket);
                                throw new IOException();
                            }else{
                                SocketAction.SocketWrite("I have chain", clientSocket);
                            }

                            // 確定 自己的 blockchain size 大於 目前的 number再傳送
                            // Get blockchain size
                            int bno = Integer.parseInt(SocketRead(clientSocket));
                            int localsize = blockchain.blockchain.size();

                            System.out.println("client size: "+bno);
                            System.out.println("\t\tlocal size:"+blockchain.blockchain.size());

                            // send response
                            if(bno>localsize){ // client longer
                                SocketAction.SocketWrite("Ur chain longer", clientSocket);
                                // 須與該節點要求區塊練

                                // get new blockchain
                                String sblockchain = SocketRead(clientSocket);
                                System.out.println("get new chain\n\t"+sblockchain);

                                // get new blockchain size
                                int blocksize = Integer.parseInt(SocketRead(clientSocket));
                                System.out.println("new size: "+blocksize);

                                // 設定排程 固定跟client要求區塊鏈
                                System.out.println("變更主節點: "+clientSocket.getInetAddress());

                                // 之後必須統一port
                                timer = SetTimer(clientSocket.getInetAddress());

                                // 確定此時有無 主節點存在
                                /*
                                 *  有: 取消自身timer 回傳給主節點要求更換
                                 *  無: 掠過直接設定主節點
                                 * */
                                if(master!=null){
                                    CancelTimer(timer);
                                    Socket mastersocket = new Socket(master,port);
                                    Thread.sleep(100);
                                    SocketAction.SocketWrite("changeMaster", mastersocket);
                                    mastersocket.close();
                                }

                                // 設定master
                                master = clientSocket.getInetAddress();

                                blockchain.blockchain=UserFunctions.Convert2Blockchain(sblockchain,blocksize);
                                // 更改 buffer block previous hash
                                bufferChain.get(0).previous_hash=blockchain.blockchain.get(blockchain.blockchain.size()-1).hash;

                                throw new IOException();
                            }
                            else if(bno==localsize){
                                SocketAction.SocketWrite("same length", clientSocket);
                                throw new IOException();
                            }
                            else{
                                SocketAction.SocketWrite("longer", clientSocket);
                            }


                            // Send blockchain
                            Thread.sleep(100);
                            String sblockchain = blockchain.get_All_Blocks_JSON();
                            SocketAction.SocketWrite(sblockchain, clientSocket);

                            Thread.sleep(100);
                            // 如果自己的鏈比較長=> send blockchain size
                            SocketAction.SocketWrite(String.valueOf(localsize), clientSocket);


                        }
                        else if(cmd.equals("changeMaster")){
                            System.out.println("更換主節點");
                            master = clientSocket.getInetAddress();
                            timer = SetTimer(master);
                        }

                        else if(cmd.equals("get-blockchain")){
                            System.out.println("\t下載區塊鏈\t");
                            SocketAction.SocketWrite(blockchain.get_All_Blocks_JSON(), clientSocket);
                            Thread.sleep(100);
                            SocketAction.SocketWrite(String.valueOf(BlockNo),clientSocket);
                        }
                        else if(cmd.equals("test")){
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



    private static Timer SetTimer(InetAddress remotehost) throws UnknownHostException {
        Remotehost=remotehost;
        timer =new Timer();

        System.out.println("*********設定排程********");
        TimerTask askChain = new TimerTask() {
            @Override
            public void run() {
                try {
                    Connection_to_Node(Remotehost,port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(askChain, 10000, 30000);// 1秒後開始，之後每過30秒再執行
        return timer;
    }

    private static void CancelTimer(Timer timer){
        System.out.println("*********取消排程********");
        timer.cancel();
    }

    // 連線其他節點取得區塊練
    private static void Connection_to_Node(InetAddress host,int port) throws IOException {
        String debug="";
        while(!host.equals("no")){
            Socket socket = new Socket(host, port);;
            try{
                // send command
                SocketAction.SocketWrite("ask-blockchain", socket);
                Thread.sleep(100);

                // get response
                String res = SocketRead(socket);
                if(!res.equals("I have chain")){
                    System.out.println("Remote Node have no Chain");
                    socket.close();
                    continue;
                }

                int oldSize = blockchain.blockchain.size();

                // send blockchain size
                SocketAction.SocketWrite(String.valueOf(oldSize), socket);
                Thread.sleep(100);
                // get response
                res = SocketRead(socket);

                if(res.equals("same length")){ // 一樣長度
                    System.out.println("Remote Node have same length chain");
                    socket.close();
                    break;
                }
                else if(res.equals("Ur chain longer")){
                    // client比較長
                    // send own blockchain to the node
                    System.out.println("Your blockchain is longer");
                    int localsize = blockchain.blockchain.size();

                    // Send blockchain
                    Thread.sleep(100);
                    String sblockchain = blockchain.get_All_Blocks_JSON();
                    SocketAction.SocketWrite(sblockchain, socket);

                    Thread.sleep(100);
                    // 如果自己的鏈比較長=> send blockchain size
                    SocketAction.SocketWrite(String.valueOf(localsize), socket);

                    // 更新 master
                    master = null;
                    // 取消排程
                    CancelTimer(timer);

                    socket.close();
                    break;
                }


                /* 取得區塊鏈 */



                // get new blockchain
                String sblockchain = SocketRead(socket);

                // get new blockchain size
                int blocksize = Integer.parseInt(SocketRead(socket));


                System.out.println("get new chain\n");
                UserFunctions.printOutBlockchain(sblockchain,blocksize);
                System.out.println("new size: "+blocksize);

                blockchain.blockchain = UserFunctions.Convert2Blockchain(sblockchain,blocksize);
                // 更改 buffer block previous hash
                bufferChain.get(0).previous_hash=blockchain.blockchain.get(blockchain.blockchain.size()-1).hash;




                // 設定 master
                master=socket.getInetAddress();
                // 設定 新排成
                timer = SetTimer(master);

                if(blockchain.blockchain.size() > oldSize){
                    socket.close();
                    break;
                }

            }catch (Exception e){
                System.out.println("與節點連線有誤(再試一次), maybe host dose not  exist");
                //e.printStackTrace();
                socket.close();
                continue;
            }
        }
        return;
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




}