package Util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.spec.ECFieldFp;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

public class ECDSA_Example{


    static EllipticCurve E;
    static BigInteger a = BigInteger.ZERO;
    static  BigInteger b = BigInteger.valueOf(7);
    static ECFieldFp Fp;
    //p=2**256 - 2**32 - 977
    static BigInteger p=new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",16);


    final static BigInteger ZERO=BigInteger.ZERO;
   // final static BigInteger ONE = BigInteger.ONE;
    final static BigInteger TWO = BigInteger.TWO;
    final static BigInteger THREE = BigInteger.valueOf(3);
    final static BigInteger FOUR = BigInteger.valueOf(4);
    final static BigInteger Int27 = BigInteger.valueOf(27);

    final static ECPoint Inf = ECPoint.POINT_INFINITY; // 無限

    static BigInteger x(ECPoint P){return P.getAffineX();}
    static BigInteger y(ECPoint P){return P.getAffineY();}

    public static ECPoint add(ECPoint P, ECPoint Q){
        BigInteger m; // 斜率
        if(P.equals(Inf))
            return Q;
        else if (Q.equals(Inf))
            return P;
        else if(P.equals(Q) && y(P).equals(ZERO))
            return Inf;
        else if(x(P).equals(x(Q)) && !y(P).equals(y(Q)))
            return Inf;
        else if(P.equals(Q))
            // m = (3*x(P)^2 + a)*(2*y(P))^-1%p
            m = THREE.multiply(x(P).modPow(TWO,p)).add(a).
                    multiply(TWO.multiply(y(P))).mod(p);
        else
            // m = y(Q)-y(P) * x(Q)-x(P)^-1 %p
            m = y(Q).subtract(y(P)).
                    multiply( (x(Q).subtract(x(P))).modInverse(p)).mod(p);
        // calculate R
        // x(R)=m^2-x(P)-x(Q)%p
        BigInteger xR = m.modPow(TWO,p).subtract(x(P)).subtract(x(Q)).mod(p);
        // y(R)=m*(x(p)-x(R))-y(P)
        BigInteger yR = m.multiply(x(P).subtract(xR)).subtract(y(P)).mod(p);
        return new ECPoint(xR,yR);
    }

    public static ECPoint minus(ECPoint P){
        //[-1]P
        BigInteger yP = y(P).negate().mod(p);
        return new ECPoint(x(P),yP);
    }

    public static ECPoint multiply(BigInteger n, ECPoint P){
        // 使用 LSBF二元法算[n]P
        // 判斷 n 是否為負
        if(n.signum()<0){
            // n<0
            n = n.negate();
            P = minus(P);
            // [n]P = [-n](-P)
        }
        ECPoint R=Inf;
        while(!n.equals(ZERO)){
                if(n.testBit(0)) // n%2==1
                    R = add(R,P);
                n = n.shiftRight(1); // n=n>>1
                P=add(P,P);
        }
        return R;
    }

    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        System.out.println(p.bitLength() + "-bit 質數 p=\n"+p);
        Fp = new ECFieldFp(p); // 有限領域

        // 找基點 G
        BigInteger xP=new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81799",16);
        BigInteger yP=new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",16);
        ECPoint P = new ECPoint(xP,yP);

        E = new EllipticCurve(Fp,a,b);
        System.out.println("橢圓曲線 E/Fp: ");
        System.out.println("a= "+a);
        System.out.println("b= "+b);

        System.out.println("基點G座標:");
        System.out.println("x(P): "+xP);
        System.out.println("y(P): "+yP);

        BigInteger n;
        ECPoint Q,Q2;
        long start,end,t0,t1;
        double ratio;
        for(int i=0;i<5;i++){
            n = new BigInteger(254,random);
            System.out.println("=====================================================");
            System.out.println("n= "+n);
            start = System.nanoTime();
            Q = multiply(n,P);
            end = System.nanoTime();

            System.out.println("Q=[n]P座標: ");
            if(Q.equals(Inf))
                System.out.println("Inf");
            System.out.println("x(Q): "+x(Q));
            System.out.println("y(Q): "+y(Q));

            t0=end-start;
            System.out.printf("multiply() 計算時間: %12d nanosec. \n",t0);
        }

    }

}
