package Users.Miner;

import BlockChain.Block;

import BlockChain.Miner;


import Users.SocketAction;
import Users.UserFunctions;


import javax.crypto.NoSuchPaddingException;
import java.io.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

public class MinerUser {


    static final int EXIT_CODE=-15;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeySpecException, SignatureException, InvalidKeyException, InterruptedException {

        Scanner scanner = new Scanner(System.in);
        Miner miner = UserFunctions.loadKey();
        String remoteHost ="";

        if(miner==null){
            return;
        }

        Block block = null;
        String jno="";

        // input ip
        if("".equals(remoteHost)){
            System.out.println("輸入節點:");
            System.out.print("\tip:\t");
            remoteHost = scanner.nextLine().strip();

            if(!SocketAction.TestConnection(remoteHost)) {
                System.exit(EXIT_CODE);
            }
        }


        String command="";
        do {
            System.out.print("[*] ");
            command = scanner.nextLine().strip();

            if ("ask-block".equals(command)) {
                block = SocketAction.getBlock(remoteHost);
            }
            else if ("mine".equals(command)) {
                SocketAction.mineBlock(remoteHost, block, miner);
                block=null;
            }
            else if(command.equals("get-blockchain")){
                SocketAction.getBlockchain(remoteHost);
            }
        } while (true);

    }

}
