package com.longjunwang.jmailagent.service;

import cn.dev33.satoken.util.SaResult;
import com.longjunwang.jmailagent.entity.InvoiceInfo;
import com.longjunwang.jmailagent.mapper.InvoiceMapper;
import com.longjunwang.jmailagent.util.OssUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InvoiceService {

    @Resource
    InvoiceMapper invoiceMapper;

    public InvoiceInfo selectById(String id){
        return invoiceMapper.selectByNumberId(id);
    }

    public InvoiceInfo insert(InvoiceInfo invoiceInfo){
        log.info("invoice: {}", invoiceInfo);
        invoiceMapper.insertBySelective(invoiceInfo);
        return invoiceInfo;
    }

    public String tempUrl(String id) {
        InvoiceInfo invoiceInfo = invoiceMapper.selectByNumberId(id);
        return OssUtil.generateTempUrl(invoiceInfo.getFileName());
    }

    public List<InvoiceInfo> selectAll() {
        List<String> fileNames = OssUtil.listObject();
        if (fileNames.isEmpty()){
            return new ArrayList<>();
        }
        return invoiceMapper.selectByFileNames(fileNames);
    }

    public SaResult softDelete(String id) {
        InvoiceInfo invoiceInfo = invoiceMapper.selectByNumberId(id);
        invoiceMapper.softDelete(invoiceInfo.getNumber());
        return SaResult.ok("移入回收站");
    }

    public SaResult hardDelete(String id) {
        InvoiceInfo invoiceInfo = invoiceMapper.selectByNumberId(id);
        OssUtil.delete(invoiceInfo.getFileName());
        invoiceMapper.hardDelete(invoiceInfo.getNumber());
        return SaResult.ok("删除成功");
    }

    public List<InvoiceInfo> getRecycleList() {
        return invoiceMapper.selectAll().stream().filter(item -> "1".equals(item.getIsDelete()))
                .collect(Collectors.toList());
    }
}
