package Users.Wallet;

import BlockChain.Miner;
import BlockChain.Transaction;

import Users.SocketAction;
import Users.UserFunctions;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WalletUser {

    static final int EXIT_CODE=-15;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, SignatureException, IllegalBlockSizeException, InterruptedException {

        Scanner scanner = new Scanner(System.in);

        String remoteHost="";

        Miner user=null;



        List<Transaction> transactions = new ArrayList<>();

        // input ip
        if("".equals(remoteHost)){
            System.out.println("輸入節點:");
            System.out.print("\tip:\t");
            remoteHost = scanner.nextLine().strip();


            if(!SocketAction.TestConnection(remoteHost))
                System.exit(EXIT_CODE);
        }


        String command="";

        do{
            System.out.print("[*]\t");
            command= scanner.nextLine().strip();

            if("import".equals(command)){
                user = UserFunctions.loadKey();

                if(user == null ) {
                    System.out.println("no wallet import");
                    continue;
                }else{
                    System.out.println("**** wallet import ****");
                    System.out.println("Address:\t"+user.address);
                    System.out.println("***********************\n");
                    user.balance = SocketAction.getBalance(remoteHost,user.address);
                }
            }
            else if("Maketransaction".equals(command)){
                if(user == null ) {
                    System.out.println("no wallet import");
                    continue;
                }
                if(user.balance==-1){
                    System.out.println("尚未取得餘額");
                    continue;
                }

                transactions.add(UserFunctions.makeTransaction(user));

                UserFunctions.List_Transaction(transactions);
            }
            else if("commit".equals(command)){
                if(user.balance==-1 || user ==null){
                    System.out.println("尚未取得餘額 or 無錢包印入");
                    continue;
                }

                // commit transaction
                String response = SocketAction.commitTransaction(remoteHost,transactions.get(0));

                if("exceed length".equals(response)){
                    System.out.println("該區塊交易已滿");
                }else{
                    System.out.println("交易成功提交");
                    transactions.remove(0);
                }
            }
            else if(command.equals("balance")){
                if(user ==null){
                    System.out.println("無錢包印入");
                    continue;
                }
                user.balance = SocketAction.getBalance(remoteHost,user.address);
            }
        }while (true);

    }
}
