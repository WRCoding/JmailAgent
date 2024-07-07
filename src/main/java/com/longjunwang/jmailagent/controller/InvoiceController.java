package com.longjunwang.jmailagent.controller;

import cn.dev33.satoken.util.SaResult;
import com.longjunwang.jmailagent.browser.BrowserService;
import com.longjunwang.jmailagent.entity.InvoiceInfo;
import com.longjunwang.jmailagent.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BrowserService browserService;

    @GetMapping("/list")
    public List<InvoiceInfo> selectAll(){
        return invoiceService.selectAll();
    }

    @GetMapping("/invoice/{id}")
    public InvoiceInfo selectById(@PathVariable("id") String id){
        return invoiceService.selectById(id);
    }

    @GetMapping("/tempUrl")
    public String tempUrl(@RequestParam("id") String id){
        return invoiceService.tempUrl(id);
    }

    @GetMapping("/softDelete")
    public SaResult softDelete(@RequestParam("id") String id){
        return invoiceService.softDelete(id);
    }

    @GetMapping("/hardDelete")
    public SaResult hardDelete(@RequestParam("id") String id){
        return invoiceService.hardDelete(id);
    }

    @GetMapping("/recycle")
    public List<InvoiceInfo> getRecycleList(){
        return invoiceService.getRecycleList();
    }
}
