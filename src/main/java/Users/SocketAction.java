package Users;

import BlockChain.Block;
import BlockChain.Blockchain;
import BlockChain.Miner;
import BlockChain.Transaction;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Timer;


public class SocketAction {

    public static final int SERVER_PORT=8000;
    public static final long TIME_DELAY=100;
    public int getSERVER_PORT() {
        return SERVER_PORT;
    }

    public static void SocketWrite(String msg, Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(msg);
    }
    public static String SocketRead(Socket socket) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String greeting = in.readLine();
        return greeting;
    }

    public static boolean TestConnection(String remoteHost) throws IOException {
        System.out.println("測試連線....");
        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByName(remoteHost),SERVER_PORT);
            SocketWrite("test",socket);
            Thread.sleep(TIME_DELAY);
            System.out.println("連線成功");
            socket.close();
            return true;
        }catch (UnknownHostException e){
            System.out.println("無法辨識之主機");
            socket.close();
            return false;
        } catch (IOException | InterruptedException e) {
            System.out.println("無連線至節點");
            socket.close();
            return false;
        }
    }

//  For Wallet user
    public static String commitTransaction(String remoteHost, Transaction transaction) throws IOException, InterruptedException, IllegalAccessException {
        Socket socket = new Socket(remoteHost, SERVER_PORT);
        Thread.sleep(TIME_DELAY);

        String command = "commit";

        // send command
        SocketWrite(command,socket);

        // send Transaction
        SocketWrite(transaction.Transaction_to_JSON().toString(),socket);

        // get response
        String response = SocketRead(socket);

        socket.close();
        return response;
    }
    public static double getBalance(String remoteHost,String address) throws IOException, InterruptedException {

        Socket socket = new Socket(remoteHost, SERVER_PORT);
        Thread.sleep(TIME_DELAY);

        String command = "balance";

        // send Command
        SocketWrite(command,socket);
        Thread.sleep(TIME_DELAY);
        // send Address
        SocketWrite(address,socket);

        // get balance
        double Balance = Double.parseDouble(SocketRead(socket));
        System.out.println("餘額:\t"+Balance);

        socket.close();
        return Balance;
    }

//  For Miner user
    public static Block getBlock(String remoteHost) throws IOException, InterruptedException {
        Socket socket;
        String command="ask-block";
        String strNumber;
        String strBlock;

        Block block = null;
        while(true){
            socket=new Socket(remoteHost,SERVER_PORT);

            // send command
            //out.println(command);
            SocketWrite(command, socket);
            Thread.sleep(TIME_DELAY);
            // Get Block number
            strNumber = SocketRead(socket);

            if(strNumber.equals("No transaction")){
                System.out.println("目前區塊目前沒交易");
                socket.close();
                break;
            }

            if (!"no".equals(strNumber)) {
                // Get new block
                strBlock = SocketRead(socket);

                if(strBlock==null || strNumber ==null){
                    System.out.println("收到資料有誤");
                    continue;
                }

                int blockNumber = Integer.parseInt(strNumber);

                try{
                    // convert to block obj
                    block = UserFunctions.Convert2Block(strBlock, blockNumber);
                    // print out block
                    UserFunctions.printOutBlock(strBlock,blockNumber);
                }catch (Exception e){
                   e.printStackTrace();
                }
            }
            socket.close();
            break;
        }
        return block;
    }
    public static void getBlockchain(String remoteHost) throws IOException, InterruptedException {
        Socket socket = new Socket(remoteHost,SERVER_PORT);
        String command = "get-blockchain";

        SocketWrite(command,socket);
        String chain = SocketRead(socket);
        Thread.sleep(TIME_DELAY);

        // get chain size
        int chainSize = Integer.parseInt(SocketRead(socket));

        System.out.println("\t目前區塊鏈\n\t\t");
        UserFunctions.printOutBlockchain(chain,chainSize);
        socket.close();
    }
    public static void mineBlock(String remoteHost, Block block, Miner miner) throws IOException, InterruptedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalAccessException {

        Socket socket = new Socket(remoteHost, SERVER_PORT);
        Thread.sleep(TIME_DELAY);

        String command = "mine";

        // send command
        SocketWrite(command, socket);
        Thread.sleep(TIME_DELAY);

        // get current block number
        int current_Block_Number = Integer.parseInt(SocketRead(socket));
        if (block.No != current_Block_Number) {
            System.out.println("\t區塊過時 請在跟節點要求新區塊");
            block = null;
            SocketWrite("no", socket);
            Thread.sleep(TIME_DELAY);
            socket.close();
            return;
        }

        // 開始挖礦
        System.out.println("\t挖礦中.....");
        double spendTime = miner.Mining_Mode(block);
        int blockNumber=block.No;


        // send block
        SocketWrite(block.get_Block_to_Json(blockNumber), socket);
        Thread.sleep(TIME_DELAY);
        System.out.println("新區塊: ");

        UserFunctions.printOutBlock(block.get_Block_to_Json(blockNumber),blockNumber);


        // calculate and send the difficulty
        int newDifficulty = Blockchain.Adjustment_Difficulty(spendTime);
        SocketWrite(String.valueOf(newDifficulty), socket);
        Thread.sleep(TIME_DELAY);

        // Get response
        String result = SocketRead(socket);
        socket.close();


        System.out.println("目前區塊鏈: ");

        getBlockchain(remoteHost);


        if ("no".equals(result)){
            System.out.println("區塊提交錯誤, 請重新與節點要求區塊在進行計算");
        }else if ("yes".equals(result)){
            System.out.println("成功發布");
        }

    }




}
