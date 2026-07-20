package com.ruoyi.web.controller.tutorial;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.TutorialFile;
import com.ruoyi.system.service.TutorialFileService;

@RestController
@RequestMapping("/tutorial/file")
public class TutorialFileController extends BaseController
{
    private final TutorialFileService tutorialFileService;

    public TutorialFileController(TutorialFileService tutorialFileService)
    {
        this.tutorialFileService = tutorialFileService;
    }

    @GetMapping("/list")
    public TableDataInfo list(TutorialFile tutorialFile)
    {
        startPage();
        List<TutorialFile> list = tutorialFileService.selectTutorialFileList(tutorialFile);
        return getDataTable(list);
    }

    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile file) throws Exception
    {
        if (!SecurityUtils.isAdmin())
        {
            return AjaxResult.error("只有 admin 可以上传教学文件");
        }
        return AjaxResult.success(tutorialFileService.upload(file, getUsername()));
    }

    @DeleteMapping("/{fileId}")
    public AjaxResult remove(@PathVariable Long fileId)
    {
        if (!SecurityUtils.isAdmin())
        {
            return AjaxResult.error("只有 admin 可以删除教学文件");
        }
        return toAjax(tutorialFileService.deleteTutorialFileById(fileId));
    }

    @GetMapping("/preview/{fileId}")
    public void preview(@PathVariable Long fileId, HttpServletResponse response) throws Exception
    {
        TutorialFile tutorialFile = tutorialFileService.selectTutorialFileById(fileId);
        if (tutorialFile == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!FileUtils.checkAllowDownload(tutorialFile.getFilePath()))
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        File file = new File(RuoYiConfig.getProfile() + FileUtils.stripPrefix(tutorialFile.getFilePath()));
        String contentType = Files.probeContentType(file.toPath());
        response.setContentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
        String fileName = URLEncoder.encode(tutorialFile.getFileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + fileName);
        FileUtils.writeBytes(file.getAbsolutePath(), response.getOutputStream());
    }
}
