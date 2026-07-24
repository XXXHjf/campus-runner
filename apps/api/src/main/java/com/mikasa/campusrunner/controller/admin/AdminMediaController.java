package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.vo.MediaUploadVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/api/media")
@Tag(name = "管理端媒体资源")
@RequiredArgsConstructor
public class AdminMediaController {
    private final MediaAssetService mediaAssetService;

    @PostMapping("/images")
    @Operation(summary = "上传临时图片")
    public Result<MediaUploadVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose) {
        return Result.success(mediaAssetService.uploadImage(
                file,
                purpose,
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId()));
    }

    @DeleteMapping("/images/{id}")
    @Operation(summary = "释放未绑定的临时图片")
    public Result<Void> releaseImage(@org.springframework.web.bind.annotation.PathVariable Long id) {
        mediaAssetService.releaseTemporary(
                id,
                MediaAssetConstant.OWNER_ADMIN,
                BaseContext.getCurrentId());
        return Result.success();
    }
}
