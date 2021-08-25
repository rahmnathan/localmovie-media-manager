package com.github.rahmnathan.localmovie.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Service
public class FileSenderHeaderService {

    public long parseStartByte(HttpServletRequest request) {
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null) {
            return Long.parseLong(rangeHeader.split("-")[0].substring(6));
        }

        return 0L;
    }

    public void setResponseHeaders(long totalBytes, long startByte, HttpServletResponse response){
        if(startByte != 0L) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startByte + "-" + (totalBytes - 1) + "/" + totalBytes);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(totalBytes - startByte));
    }


}
