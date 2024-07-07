package com.longjunwang.jmailagent.mapper;

import com.longjunwang.jmailagent.entity.InvoiceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvoiceMapper {

    int insertBySelective(InvoiceInfo invoiceInfo);

    InvoiceInfo selectByNumberId(@Param("id") String id);

    List<InvoiceInfo> selectAll();

    List<InvoiceInfo> selectByFileNames(@Param("list") List<String> list);

    int softDelete(@Param("id") String id);

    int hardDelete(@Param("id") String id);
}
