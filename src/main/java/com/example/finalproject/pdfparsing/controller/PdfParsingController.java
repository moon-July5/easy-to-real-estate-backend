package com.example.finalproject.pdfparsing.controller;



import com.example.finalproject.global.response.CommonResponse;
import com.example.finalproject.pdfparsing.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequiredArgsConstructor

public class PdfParsingController {

    private final PdfParsingService pdfParsingService;


    @PostMapping("/api/pdfupload")
    public CommonResponse pdfParsing(
            @RequestPart("multipartFile")
            @RequestParam(value = "multipartFile", required = false) MultipartFile multipartFile) throws IOException {
        return pdfParsingService.pdfParsing(multipartFile);

    }
}

