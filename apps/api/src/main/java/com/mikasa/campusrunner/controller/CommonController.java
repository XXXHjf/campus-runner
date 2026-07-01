package com.mikasa.campusrunner.controller;

import com.aliyuncs.utils.StringUtils;
import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.exception.UploadException;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.common.utils.AliOSSUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * author  Edith
 * created  2024/4/23 14:39
 */
@RestController
@RequestMapping("/api/upload")
@Slf4j
@Tag(name = "其他接口")
public class CommonController {

    @Autowired
    private AliOSSUtil aliOSSUtil;

    @PostMapping
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile img, HttpServletRequest request){
        String dirName = request.getParameter("dirName");
        if (dirName == null || dirName.isEmpty()){
            dirName = "other/swiper";
        }
        log.info("File upload: {}, dir: {}", img, dirName);
        String extand = img.getOriginalFilename().substring(img.getOriginalFilename().lastIndexOf('.'));
        UUID uuid = UUID.randomUUID();
        String objName = uuid + extand;
        log.info("Filename: {}", objName);
        try {
            String url = aliOSSUtil.upload(objName, img.getBytes(), dirName);
            return Result.success(url);
        } catch (IOException e) {
            throw new UploadException(MessageConstant.FILE_UPLOAD_FAILED);
        }
    }

}
