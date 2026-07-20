package com.ruoyi.system.service;

import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.TutorialFile;
import com.ruoyi.system.mapper.TutorialFileMapper;

@Service
public class TutorialFileService
{
    private final TutorialFileMapper tutorialFileMapper;

    public TutorialFileService(TutorialFileMapper tutorialFileMapper)
    {
        this.tutorialFileMapper = tutorialFileMapper;
    }

    public List<TutorialFile> selectTutorialFileList(TutorialFile tutorialFile)
    {
        return tutorialFileMapper.selectTutorialFileList(tutorialFile);
    }

    public TutorialFile selectTutorialFileById(Long fileId)
    {
        return tutorialFileMapper.selectTutorialFileById(fileId);
    }

    public TutorialFile upload(MultipartFile file, String createBy) throws IOException
    {
        String filePath = FileUploadUtils.upload(RuoYiConfig.getProfile() + "/tutorial", file);
        TutorialFile tutorialFile = new TutorialFile();
        tutorialFile.setFileName(file.getOriginalFilename());
        tutorialFile.setFilePath(filePath);
        tutorialFile.setFileType(FilenameUtils.getExtension(file.getOriginalFilename()));
        tutorialFile.setFileSize(file.getSize());
        tutorialFile.setCreateBy(createBy);
        tutorialFile.setCreateTime(DateUtils.getNowDate());
        tutorialFileMapper.insertTutorialFile(tutorialFile);
        return tutorialFile;
    }

    public int deleteTutorialFileById(Long fileId)
    {
        TutorialFile tutorialFile = tutorialFileMapper.selectTutorialFileById(fileId);
        int rows = tutorialFileMapper.deleteTutorialFileById(fileId);
        if (rows > 0 && tutorialFile != null)
        {
            FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(tutorialFile.getFilePath()));
        }
        return rows;
    }
}
