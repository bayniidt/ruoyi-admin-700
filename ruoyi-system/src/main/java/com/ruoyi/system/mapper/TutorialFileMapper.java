package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.TutorialFile;

public interface TutorialFileMapper
{
    List<TutorialFile> selectTutorialFileList(TutorialFile tutorialFile);

    TutorialFile selectTutorialFileById(Long fileId);

    int insertTutorialFile(TutorialFile tutorialFile);

    int deleteTutorialFileById(Long fileId);
}
