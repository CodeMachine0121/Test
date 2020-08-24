package Util;

import Util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {


    // transaction List
    List<String> txList;
    // Merkle Root
    public String root;

    /**
     * constructor
     * @param txList transaction List
     * */
    public MerkleTree(List<String> txList){
        this.txList = txList;
        root="";
    }

    /**
     * initialize merkle tree
     * */

    public void init_Merkletree() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<String> tempTxList = new ArrayList<String>();

        for(int i=0;i<this.txList.size();i++){
            tempTxList.add(this.txList.get(i));
        }

        List<String> newTxList = getNewTxList(tempTxList);

        while(newTxList.size()!=1){
            newTxList = getNewTxList(newTxList);
        }
        this.root = newTxList.get(0);

    }
    /**
     * return Node Hash List.
     * @param tempTxList
     * @return
     *
     *
     *
     *  0 1 2 3 4 5 6 7 8
     *  => h(0,1), h(2,3), h(4,5), h(6,7) ,h(8,"")
     *  => h2( h1(0,1), h1(2,3) ), h2( h1(4,5), h1(6,7) ), h1( h(6,7) ,h1(8,""))
     *  => h3( h2( h1(0,1), h1(2,3) ), h2( h1(4,5), h1(6,7) ) ) , h3( h2( h1(6,7) ,h1(8,"")), "" )
     *  => h4( h3( h2( h1(0,1), h1(2,3) ), h2( h1(4,5), h1(6,7) ) ) , h3( h2( h1(6,7) ,h1(8,"")), "" ) )
     */

    private List<String> getNewTxList(List<String> tempTxList) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        List<String > newTxList = new ArrayList<>();
        int index=0;
        while(index<tempTxList.size()){
            // left
            String left = tempTxList.get(index);
            index++;

            // right
            String right = "";
            if(index!=tempTxList.size()){
                right=tempTxList.get(index);
            }

            String shaHaxValue = StringUtil.applyHASH(left+right,"SHA-256");
            newTxList.add(shaHaxValue);
            index++;
        }
        return newTxList;
    }

}
