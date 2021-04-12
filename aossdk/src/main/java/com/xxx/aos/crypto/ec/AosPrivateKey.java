package com.xxx.aos.crypto.ec;


import com.xxx.aos.crypto.digest.Sha256;
import com.xxx.aos.crypto.util.Base58;

import java.math.BigInteger;
import java.security.SecureRandom;



public class AosPrivateKey {

    private static final String PREFIX = "PVT";

    private final BigInteger mPrivateKey;
    private final AosPublicKey mPublicKey;

    private final CurveParam mCurveParam;

    private static final SecureRandom mSecRandom;

    static {
        mSecRandom = new SecureRandom();
    }

    public static SecureRandom getSecuRandom(){
        return mSecRandom;
    }

    public AosPrivateKey(){
        this( CurveParam.SECP256_K1);
    }

    public AosPrivateKey(int curveParamType){
        mCurveParam = EcTools.getCurveParam(curveParamType);

        mPrivateKey = getOrCreatePrivKeyBigInteger( null );
        mPublicKey = new AosPublicKey(findPubKey( mPrivateKey ), mCurveParam);
    }

    public AosPrivateKey(String base58Str ) {

        String[] split = AosEcUtil.safeSplitEosCryptoString( base58Str );
        byte[] keyBytes;

        if ( split.length == 1 ){
            mCurveParam = EcTools.getCurveParam( CurveParam.SECP256_K1);
            keyBytes = AosEcUtil.getBytesIfMatchedSha256( base58Str, null);
        }
        else {
            if ( split.length < 3 ) {
                throw new IllegalArgumentException("Invalid private key format: " + base58Str);
            }

            mCurveParam = AosEcUtil.getCurveParamFrom( split[1]);
            keyBytes = AosEcUtil.getBytesIfMatchedRipemd160( split[2], split[1], null);
        }


        if ( ( null == keyBytes) || (keyBytes.length < 5 )) {
            throw new IllegalArgumentException("Invalid private key length");
        }

        mPrivateKey = getOrCreatePrivKeyBigInteger( keyBytes );
        mPublicKey = new AosPublicKey(findPubKey( mPrivateKey ), mCurveParam);
    }

    public void clear(){
        mPrivateKey.multiply( BigInteger.ZERO );
    }

    private byte[] findPubKey(BigInteger bnum) {
        EcPoint Q = EcTools.multiply( mCurveParam.G(), bnum );// Secp256k1Param.G, bnum);

        // Q를 curve 상에서, compressed point 로 변환하자. ( 압축을 위해 )

        Q = new EcPoint(Q.getCurve(), Q.getX(), Q.getY(), true);

        return Q.getEncoded();
    }

    public AosPublicKey getPublicKey() {
        return mPublicKey;
    }

    public String toWif() {
        byte[] rawPrivKey = getBytes();
        byte[] resultWIFBytes = new byte[ 1 + 32 + 4 ];

        resultWIFBytes[0] = (byte)0x80;
        System.arraycopy( rawPrivKey, rawPrivKey.length > 32 ? 1 : 0, resultWIFBytes, 1 , 32);

        Sha256 hash = Sha256.doubleHash( resultWIFBytes, 0, 33 );

        System.arraycopy( hash.getBytes(), 0, resultWIFBytes, 33, 4 );

        return Base58.encode( resultWIFBytes );
    }

    public CurveParam getCurveParam(){
        return mCurveParam;
    }

    public EcSignature sign(Sha256 digest ) {
        return EcDsa.sign( digest, this);
    }

    @Override
    public String toString() {
        if ( mCurveParam.isType( CurveParam.SECP256_K1 ) ){
            return toWif();
        }

        return AosEcUtil.encodeEosCrypto( PREFIX, mCurveParam , getBytes());
    }

    public BigInteger getAsBigInteger() {
        return mPrivateKey;
    }


    public byte[] getBytes() {
        byte[] result = new byte[32];
        byte[] bytes = mPrivateKey.toByteArray();
        if (bytes.length <= result.length) {
            System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
        } else {
            assert bytes.length == 33 && bytes[0] == 0;
            System.arraycopy(bytes, 1, result, 0, bytes.length - 1);
        }
        return result;
    }

    public byte[] getBytes(BigInteger value) {
        byte[] result = new byte[32];
        byte[] bytes = value.toByteArray();
        if (bytes.length <= result.length) {
            System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
        } else {
            // This happens if the most significant bit is set and we have an
            // extra leading zero to avoid a negative BigInteger
            assert bytes.length == 33 && bytes[0] == 0;
            System.arraycopy(bytes, 1, result, 0, bytes.length - 1);
        }
        return result;
    }

    private BigInteger toUnsignedBigInteger(BigInteger value ) {
        if ( value.signum() < 0 ) {
            return new BigInteger( 1, value.toByteArray());
        }

        return value;
    }

    private BigInteger toUnsignedBigInteger(byte[] value ) {
        if ( (( value[0]) & 0x80) != 0 ) {
            return new BigInteger( 1, value);
        }

        return new BigInteger(value);
    }

    private BigInteger getOrCreatePrivKeyBigInteger(byte[] value ) {
        if ( null != value ) {
            if (((value[0]) & 0x80) != 0) {
                return new BigInteger(1, value);
            }

            return new BigInteger(value);
        }


        int nBitLength = mCurveParam.n().bitLength();// Secp256k1Param.n.bitLength();
        BigInteger d;
        do {
            // Make a BigInteger from bytes to ensure that Android and 'classic'
            // java make the same BigIntegers from the same random source with the
            // same seed. Using BigInteger(nBitLength, random)
            // produces different results on Android compared to 'classic' java.
            byte[] bytes = new byte[nBitLength / 8];
            mSecRandom.nextBytes(bytes);
            bytes[0] = (byte) (bytes[0] & 0x7F); // ensure positive number
            d = new BigInteger(bytes);
        }
        while (d.equals(BigInteger.ZERO) || (d.compareTo(mCurveParam.n()) >= 0));// Secp256k1Param.n) >= 0));

        return d;
    }
}