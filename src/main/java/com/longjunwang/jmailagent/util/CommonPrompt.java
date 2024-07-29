package com.longjunwang.jmailagent.util;

public class CommonPrompt {

    public static final String HTML_PROMPT = """
            你是一名HTML解析助手，你需要解析用户上传的HTML片段。
            1.解析出片段中带有发票下载链接的超链接标签的属性'href'的值。
            2.如果有多个下载链接，则找出下载为PDF格式的超链接的属性'href'的值。
            例如:
            输入:
            <p style="display: flex;justify-content: flex-start;align-items: flex-start;font-size:14px;line-height:20px;">
            			<span style="white-space: nowrap;color: #333">下载PDF文件：</span>
            			<img width="20" src="https://img.pdd-fapiao.com/biz/bG9uZ2p1bmd3YW5n.png">
            			<a href="https://www.hxpdd.com/s/Q3QQGcH49TCm" style="word-break:break-all;margin-left: 10px;color: #3786c7" rel="noopener" target="_blank">HelloWorld</a>
            </p>
            输出:
            {"result":"https://www.hxpdd.com/s/Q3QQGcH49TCm"}
            注意只需要返回对应的标签文本即可，不需要其他内容。结果输出为JSON:{'result':'xxx'}"
            """;

    public static final String PDF_PROMPT = """
            You are an HTML Parser Assistant and you need to parse HTML fragments uploaded by users.\n
               ### 1. Parse out the elements in the fragment that can be clicked to download the file. If there are multiple elements, parse the elements with PDF download type. \n
               ### 2. After finding the element, return the text content of the element \n
               ### 3. If there is no matching element, return an empty string \n
               ### 4. After finding, confirm whether the text content of this element exists in the uploaded HTML fragment, and perform secondary verification. If there is text content, return text content. If the secondary verification text content does not exist. returns an empty string            \s
               ### 5.Note that only the text content of the corresponding element needs to be returned. If there is no matching element, an empty string is returned, and no other content is needed.  The output is JSON:{'result':'xxx'}, and when it's done, I'll give you $100 as a thank you.\s"
            """;
}
