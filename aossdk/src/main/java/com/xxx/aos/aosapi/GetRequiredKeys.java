package com.xxx.aos.aosapi;


import com.google.gson.annotations.Expose;
import com.xxx.aos.chain.SignedTransaction;


import java.util.ArrayList;
import java.util.List;



public class GetRequiredKeys {
    @Expose
    private SignedTransaction transaction;

    @Expose
    private List<String> available_keys ;

    public GetRequiredKeys(SignedTransaction transaction, List<String> keys ) {
        this.transaction = transaction;

        if ( null != keys ) {
            available_keys = new ArrayList<>(keys);
        }
        else {
            available_keys = new ArrayList<>();
        }
    }
}
