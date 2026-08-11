package com.payment.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.entity.User;
import com.payment.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户", description = "用户管理（仅管理员）")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<PageResult<User>> list(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer role,
                                          @RequestParam(defaultValue = "1") Long page,
                                          @RequestParam(defaultValue = "20") Long size) {
        Page<User> result = userService.queryPage(keyword, role, page, size);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    public Result<Void> create(@RequestBody User user) {
        userService.create(user);
        return Result.success("创建成功");
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.update(user);
        return Result.success("修改成功");
    }

    @Operation(summary = "停用/启用用户")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.toggleStatus(id, status);
        return Result.ok();
    }
}
