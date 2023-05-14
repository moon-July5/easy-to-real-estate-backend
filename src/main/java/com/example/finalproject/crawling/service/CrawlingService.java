package com.example.finalproject.crawling.service;

import com.example.finalproject.pdfparsing.dto.PdfParsingResDTO;

public interface CrawlingService {

    public void crawling(String complexesNumber, PdfParsingResDTO pdfParsingResDTO);
    public String getComplexesNumber(String address) throws Exception;
}
