package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        ajax.put("isAdmin", SecurityUtils.isAdmin(user.getUserId()));
        ajax.put("promoBaseLink", SecurityUtils.isAdmin(user.getUserId())
                ? configService.selectConfigByKey("agent.subid.base-link") : "");
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (user.getPartnerStackKey() != null)
        {
            String partnerStackKey = StringUtils.trim(user.getPartnerStackKey());
            if (partnerStackKey.length() > 100)
            {
                return error("PartnerStack Key长度不能超过100个字符");
            }
            currentUser.setPartnerStackKey(partnerStackKey);
        }
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 绑定 PartnerStack Key
     */
    @Log(title = "绑定 PartnerStack Key", businessType = BusinessType.UPDATE)
    @PutMapping("/partnerStackKey")
    public AjaxResult updatePartnerStackKey(@RequestBody Map<String, String> params)
    {
        String partnerStackKey = StringUtils.trim(params.get("partnerStackKey"));
        if (StringUtils.isEmpty(partnerStackKey))
        {
            return error("PartnerStack Key不能为空");
        }
        if (partnerStackKey.length() > 100)
        {
            return error("PartnerStack Key长度不能超过100个字符");
        }
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setPartnerStackKey(partnerStackKey);
        currentUser.setUpdateBy(loginUser.getUsername());
        if (userService.updateUserProfile(currentUser) > 0)
        {
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("绑定 PartnerStack Key失败，请联系管理员");
    }

    @Log(title = "设置推广链接", businessType = BusinessType.UPDATE)
    @PutMapping("/promoBaseLink")
    public AjaxResult updatePromoBaseLink(@RequestBody Map<String, String> params)
    {
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            return error("只有超级管理员才能设置推广链接");
        }
        String promoBaseLink = StringUtils.trim(params.get("promoBaseLink"));
        if (StringUtils.isEmpty(promoBaseLink))
        {
            return error("推广链接不能为空");
        }
        if (!(promoBaseLink.startsWith("http://") || promoBaseLink.startsWith("https://")))
        {
            return error("推广链接格式不正确");
        }
        if (promoBaseLink.length() > 500)
        {
            return error("推广链接长度不能超过500个字符");
        }
        com.ruoyi.system.domain.SysConfig config = new com.ruoyi.system.domain.SysConfig();
        config.setConfigKey("agent.subid.base-link");
        config.setConfigName("代理SubId推广基础链接");
        config.setConfigValue(promoBaseLink);
        config.setConfigType("N");
        config.setUpdateBy(getUsername());
        java.util.List<com.ruoyi.system.domain.SysConfig> configs = configService.selectConfigList(config);
        if (configs.isEmpty())
        {
            config.setCreateBy(getUsername());
            configService.insertConfig(config);
        }
        else
        {
            config.setConfigId(configs.get(0).getConfigId());
            config.setCreateBy(configs.get(0).getCreateBy());
            configService.updateConfig(config);
        }
        return success("推广链接设置成功");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUserId();
        SysUser user = userService.selectUserById(userId);
        String password = user.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
            if (userService.updateUserAvatar(loginUser.getUserId(), avatar))
            {
                String oldAvatar = loginUser.getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
