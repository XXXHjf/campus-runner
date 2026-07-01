/**
 * 敬请期待页面 - 用于未完成的功能模块
 */

import { useNavigate, useLocation } from 'react-router-dom'
import './ComingSoon.css'

export default function ComingSoon() {
  const navigate = useNavigate()
  const location = useLocation()

  const getPageTitle = () => {
    const path = location.pathname
    const titleMap: Record<string, string> = {
      '/orders/all': '全部订单',
      '/orders/pending': '待接单订单',
      '/orders/progress': '进行中订单',
      '/orders/completed': '已完成订单',
      '/orders/stats': '订单统计',
      '/takes/all': '全部接单',
      '/takes/withdrawn': '未收款订单',
      '/takes/stats': '接单统计',
      '/users/all': '全部用户',
      '/users/authenticated': '已认证用户',
      '/users/pending-auth': '待审核认证',
      '/users/stats': '用户统计',
      '/address/system': '系统地址',
      '/address/user': '用户地址',
      '/address/school': '学校管理',
      '/category': '分类管理',
      '/finance/overview': '财务概览',
      '/finance/income': '收益统计',
      '/finance/withdraw': '提现记录',
      '/message/subscribe': '订阅消息',
      '/message/template': '消息模板',
      '/system/config': '系统配置',
      '/system/admin': '管理员管理',
      '/system/logs': '操作日志',
    }
    return titleMap[path] || '功能模块'
  }

  return (
    <div className="coming-soon">
      <div className="coming-soon-content">
        <div className="icon-wrapper">
          <span className="icon">🚧</span>
        </div>
        <h1>{getPageTitle()}</h1>
        <p className="subtitle">该功能正在开发中，敬请期待</p>
        <div className="features">
          <h3>📋 即将支持的功能</h3>
          <ul>
            <li>✅ 数据列表展示</li>
            <li>✅ 搜索和筛选</li>
            <li>✅ 分页功能</li>
            <li>✅ 数据详情查看</li>
            <li>✅ 增删改查操作</li>
            <li>✅ 数据导出</li>
          </ul>
        </div>
        <div className="actions">
          <button onClick={() => navigate('/dashboard')} className="primary-btn">
            返回首页
          </button>
          <button onClick={() => navigate(-1)} className="secondary-btn">
            返回上一页
          </button>
        </div>
      </div>
    </div>
  )
}
