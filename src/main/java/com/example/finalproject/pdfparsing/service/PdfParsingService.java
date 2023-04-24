package com.example.finalproject.pdfparsing.service;

import com.example.finalproject.global.response.CommonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PdfParsingService {

    CommonResponse pdfParsing(MultipartFile multipartFile) throws IOException;
}
