# 当前 API 汇总

本页只维护稳定的接口分组和已下线能力。请求参数、响应模型和具体状态码应以当前 API
启动后生成的 OpenAPI 文档以及本目录的领域文档为准，避免复制生成文档后长期失真。

## 用户端

| 前缀 | 领域 |
| --- | --- |
| `/api/user` | 登录、个人资料和校园认证 |
| `/api/media` | 按用途上传、释放临时图片 |
| `/api/order` | 跑腿订单 |
| `/api/takeOrders` | 接单与送达凭证 |
| `/api/category` | 跑腿分类 |
| `/api/address` | 地址簿 |
| `/api/school` | 学校信息 |
| `/api/second-hand` | 二手商品、收藏、议价、私信、线下交易下单与交付记录；历史支付接口保留但当前禁用 |
| `/api/wx-pay` | 跑腿微信支付、查询与退款 |
| `/api/wx-transfer` | 骑手商家转账 |

用户登录、资料完整度与校园认证的先后约束见
[用户登录、资料注册与校园认证](user-onboarding.md)。

地址详情修改及可选文本字段的清空语义见 [地址簿接口](address-book.md)。

二手商品收藏、线下交易模式、私信、下单后联系方式及历史订单兼容见
[二手交易接口与流程](second-hand.md)。

## 管理端

管理端登录使用 `POST /admin/api/login`，不提供公开注册接口。其余接口按
`/admin/api/media`、`orders`、`take-orders`、`users`、`auth`、`categories`、
`second-hand`、`banner`、`address`、`config` 和 `kpi` 分组。

## 图片接口

图片只通过 `POST /api/media/images` 或 `POST /admin/api/media/images` 上传，业务提交时只传
媒体资源 id。用途、限制和生命周期见 [统一图片资源接口](media-assets.md)。

## 开发支付联调

`/api/dev/**` 只在后端 `mock-payment-enabled=true` 时可用。当前二手模块为线下交易模式，
不使用二手模拟支付或模拟收款；生产必须关闭该开关。

## 已下线

- 通用旧上传接口及其目录参数。
- 用户收款码维护、查询和私下转账流程。
- 管理员公开注册。
- 旧图片 URL、二手旧配送字段及其兼容请求参数。

这些能力不会再提供兼容响应；客户端不得继续调用。
