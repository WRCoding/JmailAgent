package com.longjunwang.jmailagent.entity;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class InvoiceInfo {

    private Integer id;
    private String fileName;
    @Alias("Number")
    private String number;
    @Alias("Date")
    private String date;
    @Alias("Total")
    private String total;
    @Alias("Buyer")
    private String buyer;
    @Alias("BuyerTaxID")
    private String buyerTaxId;
    @Alias("Issuer")
    private String issuer;
    @Alias("Reviewer")
    private String reviewer;
    @Alias("Receiptor")
    private String receiptor;
    private String name;
    private String createTime;
    private String updateTime;
    private String isDelete;

}
