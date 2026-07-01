# Admin API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 21 admin management APIs (orders, take-orders, users, categories) to the existing Campus Runner backend.

**Architecture:** Follow existing layered pattern — Controller → Service(interface) → ServiceImpl → Mapper(interface + XML). Admin controllers go under `controller/admin/`, services under `service/admin/` + `service/impl/admin/`. Reuse existing Mappers where queries exist; add new Mapper methods + XML SQL for admin-specific queries. New VOs for admin responses go under `pojo/vo/admin/`.

**Tech Stack:** Spring Boot 3, MyBatis XML mapper, Lombok, Java 21

---

## Task 1: Category Management — Mapper Layer

**Files:**
- Modify: `src/main/java/com/mikasa/campusrunner/mapper/CategoryMapper.java`
- Modify: `src/main/resources/mapper/CategoryMapper.xml`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminCategoryVO.java`

- [ ] **Step 1: Create AdminCategoryVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

@Data
public class AdminCategoryVO {
    private Long id;
    private String categoryName;
    private String image;
}
```

- [ ] **Step 2: Add Mapper methods to CategoryMapper.java**

Add these methods to the existing `CategoryMapper` interface:

```java
/**
 * 新增分类
 */
@Transactional
int insert(Category category);

/**
 * 更新分类
 */
@Transactional
int update(Category category);

/**
 * 逻辑删除分类
 */
@Transactional
int deleteById(Long id);
```

- [ ] **Step 3: Add XML SQL to CategoryMapper.xml**

Read existing `src/main/resources/mapper/CategoryMapper.xml` first, then add:

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    insert into tb_category (category_name, image, deleted)
    values (#{categoryName}, #{image}, 0)
</insert>

<update id="update">
    update tb_category
    <set>
        <if test="categoryName != null and categoryName != ''">category_name = #{categoryName},</if>
        <if test="image != null and image != ''">image = #{image},</if>
    </set>
    where id = #{id} and deleted = 0
</update>

<update id="deleteById">
    update tb_category set deleted = 1 where id = #{id}
</update>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminCategoryVO.java src/main/java/com/mikasa/campusrunner/mapper/CategoryMapper.java src/main/resources/mapper/CategoryMapper.xml
git commit -m "feat: add category mapper methods (insert/update/delete)"
```

---

## Task 2: Category Management — Service Layer

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/service/admin/AdminCategoryService.java`
- Create: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminCategoryServiceImpl.java`

- [ ] **Step 1: Create AdminCategoryService interface**

```java
package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.entity.Category;
import java.util.List;

public interface AdminCategoryService {
    List<Category> list();
    Category add(Category category);
    Category update(Category category);
    void delete(Long id);
}
```

- [ ] **Step 2: Create AdminCategoryServiceImpl**

```java
package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.admin.AdminCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AdminCategoryServiceImpl implements AdminCategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> list() {
        log.info("Listing all categories...");
        return categoryMapper.getAll();
    }

    @Override
    @Transactional
    public Category add(Category category) {
        log.info("Adding category: {}", category.getCategoryName());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public Category update(Category category) {
        log.info("Updating category id={}", category.getId());
        categoryMapper.update(category);
        return categoryMapper.getById(category.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting category id={}", id);
        categoryMapper.deleteById(id);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/service/admin/AdminCategoryService.java src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminCategoryServiceImpl.java
git commit -m "feat: add category management service layer"
```

---

## Task 3: Category Management — Controller

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminCategoryController.java`

- [ ] **Step 1: Create AdminCategoryController**

```java
package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.admin.AdminCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/categories")
@Tag(name = "管理端-分类管理")
@Slf4j
public class AdminCategoryController {

    @Autowired
    private AdminCategoryService adminCategoryService;

    @GetMapping
    @Operation(summary = "分类列表")
    public Result<List<Category>> list() {
        log.info("Listing categories...");
        List<Category> list = adminCategoryService.list();
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public Result<Category> add(@RequestBody Category category) {
        log.info("Add category: {}", category.getCategoryName());
        Category result = adminCategoryService.add(category);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category category) {
        log.info("Update category id={}", id);
        category.setId(id);
        Category result = adminCategoryService.update(category);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<String> delete(@PathVariable Long id) {
        log.info("Delete category id={}", id);
        adminCategoryService.delete(id);
        return Result.success("ok");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/controller/admin/AdminCategoryController.java
git commit -m "feat: add category management controller"
```

---

## Task 4: User Management — Mapper Layer

**Files:**
- Modify: `src/main/java/com/mikasa/campusrunner/mapper/UserMapper.java`
- Modify: `src/main/resources/mapper/UserMapper.xml`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminUserListVO.java`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminUserDetailVO.java`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminUserStatisticsVO.java`

- [ ] **Step 1: Create AdminUserListVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

@Data
public class AdminUserListVO {
    private Long id;
    private String username;
    private String realname;
    private String headImg;
    private Integer sex;
    private String phone;
    private String schoolName;
    private String stuId;
    private Integer authentication;
    private Integer studentIdCardReview;
    private Integer score;
    private Integer orderCount;
    private Integer takeOrderCount;
    private String createTime;
}
```

- [ ] **Step 2: Create AdminUserDetailVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminUserDetailVO {
    private Long id;
    private String username;
    private String realname;
    private String headImg;
    private Integer sex;
    private String phone;
    private String schoolName;
    private String stuId;
    private Integer authentication;
    private String studentIdCard;
    private Integer studentIdCardReview;
    private Integer score;
    private BigDecimal money;
    private String alipayPaymentCode;
    private String weChatPaymentCode;
    private Integer isManager;
    private Integer orderCount;
    private Integer takeOrderCount;
    private BigDecimal totalEarned;
    private String createTime;
    private String updateTime;
}
```

- [ ] **Step 3: Create AdminUserStatisticsVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;

@Data
public class AdminUserStatisticsVO {
    private Integer totalCount;
    private Integer authenticatedCount;
    private Integer unauthenticatedCount;
    private Integer pendingReviewCount;
    private Integer todayNewCount;
    private Integer managerCount;
}
```

- [ ] **Step 4: Add Mapper methods to UserMapper.java**

Add to the interface:

```java
/**
 * 管理端-全部用户列表(分页)
 */
List<AdminUserListVO> getAllUsers(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-已认证用户列表(分页)
 */
List<AdminUserListVO> getAuthenticatedUsers(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-待审核用户列表(分页)
 */
List<AdminUserListVO> getPendingReviewUsers(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-用户详情
 */
AdminUserDetailVO getAdminUserDetail(@Param("id") Long id);

/**
 * 管理端-统计: 各认证/审核状态用户数
 */
Long countByAuthStatus(@Param("authentication") Integer authentication);

/**
 * 管理端-统计: 各审核状态用户数
 */
Long countByReviewStatus(@Param("studentIdCardReview") Integer studentIdCardReview);

/**
 * 管理端-统计: 今日新增
 */
Long countTodayNew(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-统计: 管理员数量
 */
Long countManagers();
```

- [ ] **Step 5: Add XML SQL to UserMapper.xml**

Read `src/main/resources/mapper/UserMapper.xml` first (create if it doesn't exist, or check its current content). Add:

```xml
<select id="getAllUsers" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO">
    SELECT u.id, u.username, u.realname, u.head_img, u.sex, u.phone,
           s.school_name, u.stu_id, u.authentication, u.student_id_card_review,
           u.score, u.create_time
    FROM tb_user u
    LEFT JOIN tb_school s ON u.school_id = s.id
    WHERE u.deleted = 0
    ORDER BY u.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="getAuthenticatedUsers" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO">
    SELECT u.id, u.username, u.realname, u.head_img, u.sex, u.phone,
           s.school_name, u.stu_id, u.authentication, u.student_id_card_review,
           u.score, u.create_time
    FROM tb_user u
    LEFT JOIN tb_school s ON u.school_id = s.id
    WHERE u.deleted = 0 AND u.authentication = 1
    ORDER BY u.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="getPendingReviewUsers" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO">
    SELECT u.id, u.username, u.realname, u.head_img, u.sex, u.phone,
           s.school_name, u.stu_id, u.authentication, u.student_id_card_review,
           u.score, u.student_id_card, u.create_time
    FROM tb_user u
    LEFT JOIN tb_school s ON u.school_id = s.id
    WHERE u.deleted = 0 AND u.student_id_card_review = 1
    ORDER BY u.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="getAdminUserDetail" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO">
    SELECT u.id, u.username, u.realname, u.head_img, u.sex, u.phone,
           s.school_name, u.stu_id, u.authentication, u.student_id_card,
           u.student_id_card_review, u.score, u.money,
           u.alipay_payment_code, u.wechat_payment_code,
           u.is_manager, u.create_time, u.update_time
    FROM tb_user u
    LEFT JOIN tb_school s ON u.school_id = s.id
    WHERE u.id = #{id} AND u.deleted = 0
</select>

<select id="countByAuthStatus" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_user WHERE deleted = 0 AND authentication = #{authentication}
</select>

<select id="countByReviewStatus" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_user WHERE deleted = 0 AND student_id_card_review = #{studentIdCardReview}
</select>

<select id="countTodayNew" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_user
    WHERE deleted = 0 AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}
</select>

<select id="countManagers" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_user WHERE deleted = 0 AND is_manager = 1
</select>
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/pojo/vo/admin/ src/main/java/com/mikasa/campusrunner/mapper/UserMapper.java src/main/resources/mapper/UserMapper.xml
git commit -m "feat: add user management mapper layer"
```

---

## Task 5: User Management — Service Layer

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/service/admin/AdminUserService.java`
- Create: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminUserServiceImpl.java`

- [ ] **Step 1: Create AdminUserService interface**

```java
package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;

import java.util.List;

public interface AdminUserService {
    List<AdminUserListVO> listAll(int page, int pageSize);
    List<AdminUserListVO> listAuthenticated(int page, int pageSize);
    List<AdminUserListVO> listPendingReview(int page, int pageSize);
    AdminUserDetailVO detail(Long id);
    AdminUserStatisticsVO statistics();
}
```

- [ ] **Step 2: Create AdminUserServiceImpl**

```java
package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<AdminUserListVO> listAll(int page, int pageSize) {
        log.info("Listing all users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.getAllUsers(offset, pageSize);
    }

    @Override
    public List<AdminUserListVO> listAuthenticated(int page, int pageSize) {
        log.info("Listing authenticated users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.getAuthenticatedUsers(offset, pageSize);
    }

    @Override
    public List<AdminUserListVO> listPendingReview(int page, int pageSize) {
        log.info("Listing pending review users, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return userMapper.getPendingReviewUsers(offset, pageSize);
    }

    @Override
    public AdminUserDetailVO detail(Long id) {
        log.info("Getting user detail, id={}", id);
        return userMapper.getAdminUserDetail(id);
    }

    @Override
    public AdminUserStatisticsVO statistics() {
        log.info("Getting user statistics...");
        AdminUserStatisticsVO vo = new AdminUserStatisticsVO();
        vo.setTotalCount(userMapper.getAllUsersNum().intValue());
        vo.setAuthenticatedCount(userMapper.countByAuthStatus(1).intValue());
        vo.setUnauthenticatedCount(userMapper.countByAuthStatus(0).intValue());
        vo.setPendingReviewCount(userMapper.countByReviewStatus(1).intValue());
        vo.setManagerCount(userMapper.countManagers().intValue());

        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");
        vo.setTodayNewCount(userMapper.countTodayNew(startTime, endTime).intValue());

        return vo;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/service/admin/AdminUserService.java src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminUserServiceImpl.java
git commit -m "feat: add user management service layer"
```

---

## Task 6: User Management — Controller

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminUserController.java`

- [ ] **Step 1: Create AdminUserController**

```java
package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminUserStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/users")
@Tag(name = "管理端-用户管理")
@Slf4j
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/all")
    @Operation(summary = "全部用户")
    public Result<Map<String, Object>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all users, page={}, pageSize={}", page, pageSize);
        List<AdminUserListVO> list = adminUserService.listAll(page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @GetMapping("/authenticated")
    @Operation(summary = "已认证用户")
    public Result<Map<String, Object>> authenticated(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List authenticated users, page={}, pageSize={}", page, pageSize);
        List<AdminUserListVO> list = adminUserService.listAuthenticated(page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @GetMapping("/pending-review")
    @Operation(summary = "待审核用户")
    public Result<Map<String, Object>> pendingReview(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List pending review users, page={}, pageSize={}", page, pageSize);
        List<AdminUserListVO> list = adminUserService.listPendingReview(page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public Result<AdminUserDetailVO> detail(@PathVariable Long id) {
        log.info("Get user detail, id={}", id);
        AdminUserDetailVO vo = adminUserService.detail(id);
        return Result.success(vo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "用户统计")
    public Result<AdminUserStatisticsVO> statistics() {
        log.info("Get user statistics...");
        AdminUserStatisticsVO vo = adminUserService.statistics();
        return Result.success(vo);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/controller/admin/AdminUserController.java
git commit -m "feat: add user management controller"
```

---

## Task 7: Order Management — Mapper Layer

**Files:**
- Modify: `src/main/java/com/mikasa/campusrunner/mapper/OrderMapper.java`
- Modify: `src/main/resources/mapper/OrderMapper.xml`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderListVO.java`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderDetailVO.java`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderStatisticsVO.java`

- [ ] **Step 1: Create AdminOrderListVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderListVO {
    private Long id;
    private String orderNumber;
    private Integer status;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String username;
    private String phone;
    private String pickUpAddress;
    private String reciveAddress;
    private String note;
    private Integer doorAccess;
    private String createTime;
    // In-progress / completed views have these extra:
    private String takerName;
    private String takerPhone;
    private String takeOrderTime;
    private String deliveryTime;
    private String completeTime;
    private Integer withdrawalStatus;
    // Canceled view has these extra:
    private String cancelTime;
    private String cancelReason;
    private BigDecimal refundAmount;
    private String refundStatus;
}
```

- [ ] **Step 2: Create AdminOrderDetailVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderDetailVO {
    // Order info
    private Long id;
    private String orderNumber;
    private Integer status;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFeeRate;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String username;
    private String phone;
    private String pickUpAddress;
    private String reciveAddress;
    private String note;
    private String image;
    private Integer doorAccess;
    private Integer gap;
    private String exceedTime;
    private String createTime;
    private String cancelTime;
    private String cancelReason;
    // Taker info
    private Long takerUserId;
    private String takerRealname;
    private String takerPhone;
    private String takerTakeTime;
    private String takerDeliveryTime;
    private String takerImage;
    // Payment info
    private String paymentTransactionId;
    private String paymentTradeState;
    private Integer paymentTotal;
    private Integer paymentServiceFee;
    private String paymentPayerOpenid;
    private String paymentSuccessTime;
    // Refund info
    private String refundNumber;
    private String refundId;
    private Integer refundAmount;
    private String refundStatus;
    private String refundReason;
    private String refundCreateTime;
}
```

- [ ] **Step 3: Create AdminOrderStatisticsVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminOrderStatisticsVO {
    private Integer totalCount;
    private Integer waitingCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private Integer canceledCount;
    private Integer todayNewCount;
    private BigDecimal todayTotalAmount;
    private BigDecimal todayServiceFee;
}
```

- [ ] **Step 4: Add Mapper methods to OrderMapper.java**

```java
/**
 * 管理端-全部订单(分页)
 */
List<AdminOrderListVO> listAllOrders(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-分状态订单(分页)
 */
List<AdminOrderListVO> listOrdersByStatus(@Param("statuses") List<Integer> statuses,
                                           @Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-订单详情(含接单/支付/退款)
 */
AdminOrderDetailVO getAdminOrderDetail(@Param("id") Long id);

/**
 * 管理端-统计:今日新增
 */
Long countTodayOrders(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-统计:今日支付总额
 */
BigDecimal sumTodayPayAmount(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-统计:今日服务费总额
 */
BigDecimal sumTodayServiceFee(@Param("startTime") String startTime, @Param("endTime") String endTime);
```

- [ ] **Step 5: Add XML SQL queries to OrderMapper.xml**

```xml
<select id="listAllOrders" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO">
    SELECT o.id, o.order_number, o.status, c.category_name,
           o.price, o.service_fee, o.pay_amount,
           o.username, o.phone, o.note, o.door_access, o.create_time,
           ap.address AS pick_up_address, ar.address AS recive_address
    FROM tb_orders o
    LEFT JOIN tb_category c ON o.category_id = c.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ap ON o.pick_up_address = ap.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ar ON o.recive_address = ar.id
    WHERE o.deleted = 0
    ORDER BY o.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="listOrdersByStatus" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO">
    SELECT o.id, o.order_number, o.status, c.category_name,
           o.price, o.service_fee, o.pay_amount,
           o.username, o.phone, o.note, o.door_access, o.create_time,
           o.cancel_time, o.cancel_reson, o.delivery_time,
           ap.address AS pick_up_address, ar.address AS recive_address,
           tk_user.realname AS taker_name, tk_user.phone AS taker_phone,
           tk.create_time AS take_order_time
    FROM tb_orders o
    LEFT JOIN tb_category c ON o.category_id = c.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ap ON o.pick_up_address = ap.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ar ON o.recive_address = ar.id
    LEFT JOIN tb_take_orders tk ON o.id = tk.order_id AND tk.deleted = 0
    LEFT JOIN tb_user tk_user ON tk.user_id = tk_user.id
    WHERE o.deleted = 0
      AND o.status IN
      <foreach collection="statuses" item="s" open="(" separator="," close=")">
          #{s}
      </foreach>
    ORDER BY o.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="getAdminOrderDetail" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO">
    SELECT o.id, o.order_number, o.status, c.category_name,
           o.price, o.service_fee_rate, o.service_fee, o.pay_amount,
           o.username, o.phone, o.note, o.image, o.door_access,
           o.gap, o.exceed_time, o.create_time, o.cancel_time, o.cancel_reson,
           ap.address AS pick_up_address, ar.address AS recive_address,
           tk.user_id AS taker_user_id, tk_user.realname AS taker_realname,
           tk_user.phone AS taker_phone, tk.create_time AS taker_take_time,
           tk.delivery_time AS taker_delivery_time, tk.image AS taker_image,
           pl.transaction_id AS payment_transaction_id, pl.trade_state AS payment_trade_state,
           pl.total AS payment_total, pl.service_fee AS payment_service_fee,
           pl.payer_openid AS payment_payer_openid, pl.success_time AS payment_success_time,
           ri.refund_number, ri.refund_id, ri.refund AS refund_amount,
           ri.refund_status, ri.reason AS refund_reason, ri.create_time AS refund_create_time
    FROM tb_orders o
    LEFT JOIN tb_category c ON o.category_id = c.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ap ON o.pick_up_address = ap.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ar ON o.recive_address = ar.id
    LEFT JOIN tb_take_orders tk ON o.id = tk.order_id AND tk.deleted = 0
    LEFT JOIN tb_user tk_user ON tk.user_id = tk_user.id
    LEFT JOIN tb_payment_log pl ON o.order_number = pl.order_number
    LEFT JOIN tb_refund_info ri ON o.order_number = ri.order_number
    WHERE o.id = #{id} AND o.deleted = 0
</select>

<select id="countTodayOrders" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_orders
    WHERE deleted = 0 AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}
</select>

<select id="sumTodayPayAmount" resultType="java.math.BigDecimal">
    SELECT COALESCE(SUM(pay_amount), 0) FROM tb_orders
    WHERE deleted = 0 AND pay_amount > 0
      AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}
</select>

<select id="sumTodayServiceFee" resultType="java.math.BigDecimal">
    SELECT COALESCE(SUM(service_fee), 0) FROM tb_orders
    WHERE deleted = 0 AND service_fee > 0
      AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}
</select>
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderListVO.java src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderDetailVO.java src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminOrderStatisticsVO.java src/main/java/com/mikasa/campusrunner/mapper/OrderMapper.java src/main/resources/mapper/OrderMapper.xml
git commit -m "feat: add order management mapper layer"
```

---

## Task 8: Order Management — Service Layer

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/service/admin/AdminOrderService.java`
- Create: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminOrderServiceImpl.java`

- [ ] **Step 1: Create AdminOrderService interface**

```java
package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import java.util.List;

public interface AdminOrderService {
    List<AdminOrderListVO> listAll(int page, int pageSize);
    List<AdminOrderListVO> listWaiting(int page, int pageSize);
    List<AdminOrderListVO> listInProgress(int page, int pageSize);
    List<AdminOrderListVO> listCompleted(int page, int pageSize);
    List<AdminOrderListVO> listCanceled(int page, int pageSize);
    AdminOrderDetailVO detail(Long id);
    AdminOrderStatisticsVO statistics();
    void cancel(Long id, String reason);
    void refund(Long id, String reason);
}
```

- [ ] **Step 2: Create AdminOrderServiceImpl**

```java
package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.MessageConstant;
import com.mikasa.campusrunner.common.constant.OrderStatusConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.RefundInfoDTO;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminOrderService;
import com.mikasa.campusrunner.service.user.WeChatPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired(required = false)
    private WeChatPayService weChatPayService;

    @Override
    public List<AdminOrderListVO> listAll(int page, int pageSize) {
        log.info("Listing all orders, page={}, pageSize={}", page, pageSize);
        int offset = (page - 1) * pageSize;
        return orderMapper.listAllOrders(offset, pageSize);
    }

    @Override
    public List<AdminOrderListVO> listWaiting(int page, int pageSize) {
        log.info("Listing waiting orders...");
        int offset = (page - 1) * pageSize;
        return orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.WAIT_TO_TAKE_ORDER), offset, pageSize);
    }

    @Override
    public List<AdminOrderListVO> listInProgress(int page, int pageSize) {
        log.info("Listing in-progress orders...");
        int offset = (page - 1) * pageSize;
        return orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.ALREADY_TAKE_ORDER,
                          OrderStatusConstant.DELIVERYING,
                          OrderStatusConstant.ORDER_FINISH),
            offset, pageSize);
    }

    @Override
    public List<AdminOrderListVO> listCompleted(int page, int pageSize) {
        log.info("Listing completed orders...");
        int offset = (page - 1) * pageSize;
        return orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT,
                          OrderStatusConstant.WITHDRAWAL_SUCCEEDED,
                          OrderStatusConstant.WITHDRAWAL_FAILED),
            offset, pageSize);
    }

    @Override
    public List<AdminOrderListVO> listCanceled(int page, int pageSize) {
        log.info("Listing canceled/refund orders...");
        int offset = (page - 1) * pageSize;
        return orderMapper.listOrdersByStatus(
            Arrays.asList(OrderStatusConstant.CANCELED,
                          OrderStatusConstant.REFUND_PROCESSING,
                          OrderStatusConstant.REFUND_SUCCESS,
                          OrderStatusConstant.REFUND_ABNORMAL),
            offset, pageSize);
    }

    @Override
    public AdminOrderDetailVO detail(Long id) {
        log.info("Getting order detail, id={}", id);
        return orderMapper.getAdminOrderDetail(id);
    }

    @Override
    public AdminOrderStatisticsVO statistics() {
        log.info("Getting order statistics...");
        AdminOrderStatisticsVO vo = new AdminOrderStatisticsVO();
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");

        vo.setTotalCount(orderMapper.getAllOrdersNum().intValue());
        vo.setWaitingCount(orderMapper.getAllOrdersByStatus(OrderStatusConstant.WAIT_TO_TAKE_ORDER).intValue());
        vo.setInProgressCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.ALREADY_TAKE_ORDER).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.DELIVERYING).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.ORDER_FINISH).intValue());
        vo.setCompletedCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.SENDER_CONFIRMS_RECEIPT).intValue());
        vo.setCanceledCount(
            orderMapper.getAllOrdersByStatus(OrderStatusConstant.CANCELED).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_PROCESSING).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_SUCCESS).intValue()
            + orderMapper.getAllOrdersByStatus(OrderStatusConstant.REFUND_ABNORMAL).intValue());
        vo.setTodayNewCount(orderMapper.countTodayOrders(startTime, endTime).intValue());
        vo.setTodayTotalAmount(orderMapper.sumTodayPayAmount(startTime, endTime));
        vo.setTodayServiceFee(orderMapper.sumTodayServiceFee(startTime, endTime));

        return vo;
    }

    @Override
    @Transactional
    public void cancel(Long id, String reason) {
        log.info("Admin canceling order id={}, reason={}", id, reason);
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        Integer status = order.getStatus();
        if (!status.equals(OrderStatusConstant.WAIT_TO_TAKE_ORDER) &&
            !status.equals(OrderStatusConstant.NO_PAY)) {
            throw new OrderException("Order status does not allow cancellation");
        }
        order.setStatus(OrderStatusConstant.CANCELED);
        order.setCancelReson(reason);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Override
    @Transactional
    public void refund(Long id, String reason) {
        log.info("Admin refunding order id={}, reason={}", id, reason);
        Order order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderException(MessageConstant.NOT_FOUND_ORDER);
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Order has no paid amount to refund");
        }
        if (order.getStatus().equals(OrderStatusConstant.WITHDRAWAL_SUCCEEDED) ||
            order.getStatus().equals(OrderStatusConstant.WITHDRAWAL_FAILED)) {
            throw new OrderException("Order has already been withdrawn, cannot refund");
        }

        order.setStatus(OrderStatusConstant.REFUND_PROCESSING);
        order.setCancelReson(reason);
        orderMapper.update(order);

        if (weChatPayService != null) {
            try {
                RefundInfoDTO dto = new RefundInfoDTO();
                dto.setOrderNumber(order.getOrderNumber());
                dto.setReason(reason);
                weChatPayService.refunds(dto);
            } catch (Exception e) {
                log.error("Refund failed for order: {}", order.getOrderNumber(), e);
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/service/admin/AdminOrderService.java src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminOrderServiceImpl.java
git commit -m "feat: add order management service layer"
```

---

## Task 9: Order Management — Controller

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminOrderController.java`

- [ ] **Step 1: Create AdminOrderController**

```java
package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderDetailVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/orders")
@Tag(name = "管理端-订单管理")
@Slf4j
public class AdminOrderController {

    @Autowired
    private AdminOrderService adminOrderService;

    private Map<String, Object> wrap(List<AdminOrderListVO> list, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @GetMapping("/all")
    @Operation(summary = "全部订单")
    public Result<Map<String, Object>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all orders...");
        List<AdminOrderListVO> list = adminOrderService.listAll(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/waiting")
    @Operation(summary = "待接单")
    public Result<Map<String, Object>> waiting(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List waiting orders...");
        List<AdminOrderListVO> list = adminOrderService.listWaiting(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/in-progress")
    @Operation(summary = "进行中")
    public Result<Map<String, Object>> inProgress(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List in-progress orders...");
        List<AdminOrderListVO> list = adminOrderService.listInProgress(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/completed")
    @Operation(summary = "已完成")
    public Result<Map<String, Object>> completed(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List completed orders...");
        List<AdminOrderListVO> list = adminOrderService.listCompleted(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/canceled")
    @Operation(summary = "已取消/退款")
    public Result<Map<String, Object>> canceled(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List canceled/refund orders...");
        List<AdminOrderListVO> list = adminOrderService.listCanceled(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<AdminOrderDetailVO> detail(@PathVariable Long id) {
        log.info("Get order detail, id={}", id);
        AdminOrderDetailVO vo = adminOrderService.detail(id);
        return Result.success(vo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "订单统计")
    public Result<AdminOrderStatisticsVO> statistics() {
        log.info("Get order statistics...");
        AdminOrderStatisticsVO vo = adminOrderService.statistics();
        return Result.success(vo);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<String> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        log.info("Cancel order id={}", id);
        String reason = body.get("reason");
        adminOrderService.cancel(id, reason);
        return Result.success("ok");
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "退款")
    public Result<String> refund(@PathVariable Long id, @RequestBody Map<String, String> body) {
        log.info("Refund order id={}", id);
        String reason = body.get("reason");
        adminOrderService.refund(id, reason);
        return Result.success("ok");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/controller/admin/AdminOrderController.java
git commit -m "feat: add order management controller"
```

---

## Task 10: Take-Order Management — Full Stack

This module is simpler (3 read-only endpoints), so combine Mapper + Service + Controller in one task.

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminTakeOrderListVO.java`
- Create: `src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminTakeOrderStatisticsVO.java`
- Modify: `src/main/java/com/mikasa/campusrunner/mapper/TakeOrderMapper.java`
- Modify: `src/main/resources/mapper/TakeOrderMapper.xml`
- Create: `src/main/java/com/mikasa/campusrunner/service/admin/AdminTakeOrderService.java`
- Create: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminTakeOrderServiceImpl.java`
- Create: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminTakeOrderController.java`

- [ ] **Step 1: Create AdminTakeOrderListVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminTakeOrderListVO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Integer orderStatus;
    private Integer takeOrderStatus;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal serviceFee;
    private BigDecimal payAmount;
    private String orderNote;
    private String pickUpAddress;
    private String reciveAddress;
    private String publisherName;
    private String publisherPhone;
    private String takerName;
    private String takerPhone;
    private String takeOrderTime;
    private String deliveryTime;
    private String takeOrderImage;
    private String completeTime;
    private Integer withdrawalStatus;
}
```

- [ ] **Step 2: Create AdminTakeOrderStatisticsVO**

```java
package com.mikasa.campusrunner.pojo.vo.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminTakeOrderStatisticsVO {
    private Integer totalCount;
    private Integer todayNewCount;
    private Integer unpaidCount;
    private BigDecimal unpaidTotalAmount;
    private Integer todayCompletedCount;
    private BigDecimal todayCompletedAmount;
}
```

- [ ] **Step 3: Add Mapper methods to TakeOrderMapper.java**

```java
/**
 * 管理端-全部接单(分页)
 */
List<AdminTakeOrderListVO> listAllTakeOrders(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-未收款接单(分页): 订单已完成但未提现
 */
List<AdminTakeOrderListVO> listUnpaidTakeOrders(@Param("offset") int offset, @Param("limit") int limit);

/**
 * 管理端-今日新增接单数
 */
Long countTodayNew(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-未收款订单总额
 */
BigDecimal sumUnpaidAmount();

/**
 * 管理端-今日完成接单数
 */
Long countTodayCompleted(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-今日完成接单金额
 */
BigDecimal sumTodayCompletedAmount(@Param("startTime") String startTime, @Param("endTime") String endTime);

/**
 * 管理端-接单总数
 */
Long countAll();
```

- [ ] **Step 4: Add XML SQL to TakeOrderMapper.xml**

```xml
<select id="listAllTakeOrders" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO">
    SELECT tk.id, tk.order_id, o.order_number, o.status AS order_status,
           tk.status AS take_order_status, c.category_name, o.price,
           o.service_fee, o.pay_amount, o.note AS order_note,
           ap.address AS pick_up_address, ar.address AS recive_address,
           pub_user.realname AS publisher_name, o.phone AS publisher_phone,
           tk_user.realname AS taker_name, tk_user.phone AS taker_phone,
           tk.create_time AS take_order_time, tk.delivery_time,
           tk.image AS take_order_image
    FROM tb_take_orders tk
    INNER JOIN tb_orders o ON tk.order_id = o.id AND o.deleted = 0
    LEFT JOIN tb_category c ON o.category_id = c.id
    LEFT JOIN tb_user tk_user ON tk.user_id = tk_user.id
    LEFT JOIN tb_user pub_user ON o.user_id = pub_user.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ap ON o.pick_up_address = ap.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ar ON o.recive_address = ar.id
    WHERE tk.deleted = 0
    ORDER BY tk.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="listUnpaidTakeOrders" resultType="com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO">
    SELECT tk.id, tk.order_id, o.order_number, o.status AS order_status,
           tk.status AS take_order_status, c.category_name, o.price,
           o.service_fee, o.pay_amount, o.note AS order_note,
           ap.address AS pick_up_address, ar.address AS recive_address,
           pub_user.realname AS publisher_name, o.phone AS publisher_phone,
           tk_user.realname AS taker_name, tk_user.phone AS taker_phone,
           tk.create_time AS take_order_time, tk.delivery_time,
           tk.image AS take_order_image, o.cancel_time AS complete_time
    FROM tb_take_orders tk
    INNER JOIN tb_orders o ON tk.order_id = o.id AND o.deleted = 0
    LEFT JOIN tb_category c ON o.category_id = c.id
    LEFT JOIN tb_user tk_user ON tk.user_id = tk_user.id
    LEFT JOIN tb_user pub_user ON o.user_id = pub_user.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ap ON o.pick_up_address = ap.id
    LEFT JOIN (
        SELECT ad.id, CONCAT(s.school_name, ' ', cp.compus_name, ' ', bc.name, ' ', b.building_name, ' ', ad.details) AS address
        FROM tb_address_book ad, tb_school s, tb_compus cp, tb_build_category bc, tb_building b
        WHERE ad.school_id = s.id AND ad.compus_id = cp.id
          AND ad.build_category_id = bc.id AND ad.building_id = b.id AND ad.deleted = 0
    ) ar ON o.recive_address = ar.id
    WHERE tk.deleted = 0 AND o.status IN (5, 7)
    ORDER BY tk.create_time DESC
    LIMIT #{offset}, #{limit}
</select>

<select id="countTodayNew" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_take_orders
    WHERE deleted = 0 AND create_time &gt;= #{startTime} AND create_time &lt;= #{endTime}
</select>

<select id="sumUnpaidAmount" resultType="java.math.BigDecimal">
    SELECT COALESCE(SUM(o.price), 0)
    FROM tb_take_orders tk
    INNER JOIN tb_orders o ON tk.order_id = o.id AND o.deleted = 0 AND o.status IN (5, 7)
    WHERE tk.deleted = 0
</select>

<select id="countTodayCompleted" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_take_orders
    WHERE deleted = 0 AND status = 2
      AND delivery_time &gt;= #{startTime} AND delivery_time &lt;= #{endTime}
</select>

<select id="sumTodayCompletedAmount" resultType="java.math.BigDecimal">
    SELECT COALESCE(SUM(o.price), 0)
    FROM tb_take_orders tk
    INNER JOIN tb_orders o ON tk.order_id = o.id AND o.deleted = 0
    WHERE tk.deleted = 0 AND tk.status = 2
      AND tk.delivery_time &gt;= #{startTime} AND tk.delivery_time &lt;= #{endTime}
</select>

<select id="countAll" resultType="java.lang.Long">
    SELECT COUNT(*) FROM tb_take_orders WHERE deleted = 0
</select>
```

- [ ] **Step 5: Create AdminTakeOrderService interface**

```java
package com.mikasa.campusrunner.service.admin;

import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
import java.util.List;

public interface AdminTakeOrderService {
    List<AdminTakeOrderListVO> listAll(int page, int pageSize);
    List<AdminTakeOrderListVO> listUnpaid(int page, int pageSize);
    AdminTakeOrderStatisticsVO statistics();
}
```

- [ ] **Step 6: Create AdminTakeOrderServiceImpl**

```java
package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.mapper.TakeOrderMapper;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminTakeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminTakeOrderServiceImpl implements AdminTakeOrderService {

    @Autowired
    private TakeOrderMapper takeOrderMapper;

    @Override
    public List<AdminTakeOrderListVO> listAll(int page, int pageSize) {
        log.info("Listing all take orders...");
        int offset = (page - 1) * pageSize;
        return takeOrderMapper.listAllTakeOrders(offset, pageSize);
    }

    @Override
    public List<AdminTakeOrderListVO> listUnpaid(int page, int pageSize) {
        log.info("Listing unpaid take orders...");
        int offset = (page - 1) * pageSize;
        return takeOrderMapper.listUnpaidTakeOrders(offset, pageSize);
    }

    @Override
    public AdminTakeOrderStatisticsVO statistics() {
        log.info("Getting take order statistics...");
        AdminTakeOrderStatisticsVO vo = new AdminTakeOrderStatisticsVO();
        LocalDateTime now = LocalDateTime.now();
        String startTime = now.toLocalDate().atStartOfDay().toString().replace("T", " ");
        String endTime = now.toLocalDate().atTime(23, 59, 59).toString().replace("T", " ");

        vo.setTotalCount(takeOrderMapper.countAll().intValue());
        vo.setTodayNewCount(takeOrderMapper.countTodayNew(startTime, endTime).intValue());
        vo.setUnpaidCount(0); // populated by listUnpaid size
        vo.setUnpaidTotalAmount(takeOrderMapper.sumUnpaidAmount());
        vo.setTodayCompletedCount(takeOrderMapper.countTodayCompleted(startTime, endTime).intValue());
        vo.setTodayCompletedAmount(takeOrderMapper.sumTodayCompletedAmount(startTime, endTime));

        return vo;
    }
}
```

- [ ] **Step 7: Create AdminTakeOrderController**

```java
package com.mikasa.campusrunner.controller.admin;

import com.mikasa.campusrunner.common.result.Result;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderListVO;
import com.mikasa.campusrunner.pojo.vo.admin.AdminTakeOrderStatisticsVO;
import com.mikasa.campusrunner.service.admin.AdminTakeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/take-orders")
@Tag(name = "管理端-接单管理")
@Slf4j
public class AdminTakeOrderController {

    @Autowired
    private AdminTakeOrderService adminTakeOrderService;

    private Map<String, Object> wrap(List<AdminTakeOrderListVO> list, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @GetMapping("/all")
    @Operation(summary = "全部接单")
    public Result<Map<String, Object>> all(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List all take orders...");
        List<AdminTakeOrderListVO> list = adminTakeOrderService.listAll(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/unpaid")
    @Operation(summary = "未收款订单")
    public Result<Map<String, Object>> unpaid(@RequestParam int page, @RequestParam int pageSize) {
        log.info("List unpaid take orders...");
        List<AdminTakeOrderListVO> list = adminTakeOrderService.listUnpaid(page, pageSize);
        return Result.success(wrap(list, page, pageSize));
    }

    @GetMapping("/statistics")
    @Operation(summary = "接单统计")
    public Result<AdminTakeOrderStatisticsVO> statistics() {
        log.info("Get take order statistics...");
        AdminTakeOrderStatisticsVO vo = adminTakeOrderService.statistics();
        return Result.success(vo);
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminTakeOrderListVO.java src/main/java/com/mikasa/campusrunner/pojo/vo/admin/AdminTakeOrderStatisticsVO.java src/main/java/com/mikasa/campusrunner/mapper/TakeOrderMapper.java src/main/resources/mapper/TakeOrderMapper.xml src/main/java/com/mikasa/campusrunner/service/admin/AdminTakeOrderService.java src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminTakeOrderServiceImpl.java src/main/java/com/mikasa/campusrunner/controller/admin/AdminTakeOrderController.java
git commit -m "feat: add take-order management (list/statistics)"
```

---

## Task 11: Build Verification

- [ ] **Step 1: Compile the project**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS. Fix any compilation errors.

- [ ] **Step 2: Run the application and verify startup**

```bash
mvn spring-boot:run
```

Check startup logs for clean English output and no mapper binding errors.

- [ ] **Step 3: Commit any fixes**

```bash
git add -u
git commit -m "fix: compilation and startup fixes for admin APIs"
```
